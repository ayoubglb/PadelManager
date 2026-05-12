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
import { MatDialog } from '@angular/material/dialog';
import {
  MatchCreateDialog,
  MatchCreateDialogData,
  MatchCreateDialogResult,
} from '../matchs/match-create-dialog/match-create-dialog';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { combineLatest, switchMap, of, catchError, tap } from 'rxjs';

import { PlanningService } from '../../core/api/planning.service';
import { PlanningView, CelluleView } from '../../core/api/planning.types';
import { SiteSelector } from '../../shared/components/site-selector/site-selector';
import { CreneauView } from '../../core/api/planning.types';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../../core/api/auth.types';

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
  private dialog = inject(MatDialog);

  /// Date minimum sélectionnable dans le datepicker = aujourd'hui.
  /// Empêche la sélection de dates passées (qui seraient refusées par le backend).
  readonly minDate = new Date();

  readonly selectedSiteId = signal<number | null>(this.getInitialSiteId());
  readonly selectedDate = signal<Date>(new Date());
  readonly loading = signal(false);

  /**
   * Indique si on peut reculer d'un jour (faux si on est déjà sur aujourd'hui ou avant).
   * Utilisé pour désactiver le bouton "Jour précédent".
   */
  readonly canGoBack = computed(() => {
    const selected = new Date(this.selectedDate());
    selected.setHours(0, 0, 0, 0);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return selected.getTime() > today.getTime();
  });

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
          catchError((err: HttpErrorResponse) => {
            this.loading.set(false);
            console.error('Erreur chargement planning', err);
            const apiErr = err.error as ApiError | undefined;
            const msg = apiErr?.message ?? 'Impossible de charger le planning';
            this.snack.open(msg, 'OK', { duration: 4000 });
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

  onCellClick(c: CelluleView, creneau: CreneauView): void {
    if (c.statut === 'LIBRE') {
      this.openCreateMatchDialog(c, creneau);
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

  private openCreateMatchDialog(c: CelluleView, creneau: CreneauView): void {
    const planning = this.planning();
    if (!planning) return;

    const terrain = planning.terrains.find((t) => t.id === c.terrainId);
    if (!terrain) return;

    const data: MatchCreateDialogData = {
      siteNom: planning.siteNom,
      terrainId: terrain.id,
      terrainNumero: terrain.numero,
      date: planning.date,
      creneauDebut: creneau.debut,
      creneauFin: creneau.fin,
    };

    const ref = this.dialog.open(MatchCreateDialog, {
      width: '500px',
      data,
    });

    ref.afterClosed().subscribe((result: MatchCreateDialogResult | undefined) => {
      if (!result) return;
      if (result.type === 'PRIVE') {
        this.snack.open(
          `Match privé #${result.matchId} créé. Invitez vos joueurs.`,
          'OK',
          { duration: 4000 }
        );
      } else {
        this.snack.open(
          `Match public #${result.matchId} créé. Visible dans le catalogue.`,
          'OK',
          { duration: 4000 }
        );
      }
      this.refreshPlanning();
    });
  }

  private refreshPlanning(): void {
    const currentDate = this.selectedDate();
    this.selectedDate.set(new Date(currentDate));
  }

  isCellClickable(c: CelluleView): boolean {
    return c.statut === 'LIBRE' || c.statut === 'PUBLIC_DISPO';
  }

  formatTime(time: string): string {
    return time.slice(0, 5);
  }
}
