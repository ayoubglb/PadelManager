import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { combineLatest, switchMap, of, catchError, tap } from 'rxjs';

import { PlanningService } from '../../core/api/planning.service';
import { PlanningView, CelluleView } from '../../core/api/planning.types';
import { SiteSelector } from '../../shared/components/site-selector/site-selector';

@Component({
  selector: 'app-planning',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    SiteSelector,
  ],
  templateUrl: './planning.html',
  styleUrl: './planning.css',
})
export class Planning {
  private planningService = inject(PlanningService);
  private snack = inject(MatSnackBar);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  readonly selectedSiteId = signal<number | null>(this.getInitialSiteId());
  readonly selectedDate = signal<Date>(new Date());
  readonly loading = signal(false);

  // Planning chargé depuis le backend selon (siteId, date)

  readonly planning = toSignal<PlanningView | null>(
    combineLatest([
      toObservable(this.selectedSiteId),
      toObservable(this.selectedDate),
    ]).pipe(
      switchMap(([siteId, date]) => {
        if (siteId === null) return of(null);
        this.loading.set(true);
        return this.planningService.getPlanning(siteId, this.toIsoDate(date)).pipe(
          tap(() => this.loading.set(false)),
          catchError((err) => {
            this.loading.set(false);
            console.error('Erreur chargement planning', err);
            this.snack.open('Impossible de charger le planning', 'OK', {
              duration: 4000,
            });
            return of(null);
          })
        );
      })
    ),
    { initialValue: null }
  );

  constructor() {
    // Synchroniser le siteId avec le query param de l'URL
    effect(() => {
      const siteId = this.selectedSiteId();
      if (siteId !== null) {
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { siteId },
          queryParamsHandling: 'merge',
          replaceUrl: true,
        });
      }
    });
  }

  private getInitialSiteId(): number | null {
    const param = this.route.snapshot.queryParamMap.get('siteId');
    if (param) {
      const id = Number(param);
      return isNaN(id) ? null : id;
    }
    return null;
  }

  private toIsoDate(date: Date): string {
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  previousDay(): void {
    const d = new Date(this.selectedDate());
    d.setDate(d.getDate() - 1);
    this.selectedDate.set(d);
  }

  nextDay(): void {
    const d = new Date(this.selectedDate());
    d.setDate(d.getDate() + 1);
    this.selectedDate.set(d);
  }

  today(): void {
    this.selectedDate.set(new Date());
  }

  onDateChange(date: Date | null): void {
    if (date) {
      this.selectedDate.set(date);
    }
  }

  cellClass(c: CelluleView): string {
    switch (c.statut) {
      case 'LIBRE':
        return 'bg-green-100 hover:bg-green-200 cursor-pointer text-green-800';
      case 'PUBLIC_DISPO':
        return 'bg-orange-100 hover:bg-orange-200 cursor-pointer text-orange-800';
      case 'PRIVE':
        return 'bg-red-100 cursor-not-allowed text-red-800';
      case 'COMPLET':
        return 'bg-gray-200 cursor-not-allowed text-gray-600';
      case 'FERME':
        return 'bg-gray-300 cursor-not-allowed text-gray-500';
    }
  }

  cellLabel(c: CelluleView): string {
    switch (c.statut) {
      case 'LIBRE':
        return 'Libre';
      case 'PUBLIC_DISPO':
        return `Public · ${c.placesRestantes} place${c.placesRestantes !== 1 ? 's' : ''}`;
      case 'PRIVE':
        return 'Privé';
      case 'COMPLET':
        return 'Complet';
      case 'FERME':
        return '—';
    }
  }

  cellTooltip(c: CelluleView): string {
    if (c.statut === 'LIBRE') return 'Cliquer pour réserver ce créneau';
    if (c.statut === 'PUBLIC_DISPO')
      return `Match public organisé par ${c.organisateurNom}. Cliquer pour rejoindre.`;
    if (c.statut === 'PRIVE') return 'Match privé, non rejoignable';
    if (c.statut === 'COMPLET') return 'Match complet (4 joueurs)';
    return 'Créneau indisponible';
  }

  onCellClick(c: CelluleView, creneauDebut: string): void {
    if (c.statut === 'LIBRE') {
      this.snack.open(
        `Réserver à ${this.formatTime(creneauDebut)} - À implémenter`,
        'OK',
        { duration: 2500 }
      );
      return;
    }
    if (c.statut === 'PUBLIC_DISPO' && c.matchId) {
      this.snack.open(
        `Rejoindre le match #${c.matchId} - À implémenter`,
        'OK',
        { duration: 2500 }
      );
      return;
    }
  }

  isCellClickable(c: CelluleView): boolean {
    return c.statut === 'LIBRE' || c.statut === 'PUBLIC_DISPO';
  }

  formatTime(time: string): string {
    return time.slice(0, 5);
  }
}
