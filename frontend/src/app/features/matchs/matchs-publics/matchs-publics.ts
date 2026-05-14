import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import {
  catchError,
  combineLatest,
  debounceTime,
  of,
  startWith,
  switchMap,
} from 'rxjs';

import { MatchService } from '../../../core/api/match.service';
import {
  MatchPublicCatalogue,
  MatchsPublicsFiltres,
} from '../../../core/api/match.types';
import { ApiError } from '../../../core/api/auth.types';
import { PublicMatchCard } from '../../../shared/components/public-match-card/public-match-card';
import { SiteSelector } from '../../../shared/components/site-selector/site-selector';

@Component({
  selector: 'app-matchs-publics',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatProgressSpinnerModule,
    PublicMatchCard,
    SiteSelector,
  ],
  templateUrl: './matchs-publics.html',
  styleUrl: './matchs-publics.css',
})
export class MatchsPublics {
  private matchService = inject(MatchService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);

  // Date minimum sélectionnable = aujourd'hui (pas de filtre sur le passé)
  readonly minDate = new Date();

  readonly siteId = signal<number | null>(null);
  readonly dateDebutCtrl = new FormControl<Date | null>(null);
  readonly dateFinCtrl = new FormControl<Date | null>(null);
  readonly placesMin = signal<number | null>(null);

  readonly loading = signal(false);

  // Filtres consolidés en un seul signal pour déclencher la recherche. On combine signal + observables RxJS pour FormControl
  private readonly filtres$ = combineLatest([
    toObservable(this.siteId),
    this.dateDebutCtrl.valueChanges.pipe(startWith(this.dateDebutCtrl.value)),
    this.dateFinCtrl.valueChanges.pipe(startWith(this.dateFinCtrl.value)),
    toObservable(this.placesMin),
  ]).pipe(
    debounceTime(150), // évite plusieurs requêtes lors d'un reset
    switchMap(([siteId, dateDebut, dateFin, placesMin]) => {
      const filtres: MatchsPublicsFiltres = {
        siteId: siteId ?? undefined,
        dateDebut: this.toIsoDate(dateDebut),
        dateFin: this.toIsoDate(dateFin),
        placesMin: placesMin ?? undefined,
      };

      this.loading.set(true);
      return this.matchService.getMatchsPublics(filtres).pipe(
        catchError((err: HttpErrorResponse) => {
          this.loading.set(false);
          console.error('Erreur chargement matchs publics', err);
          const apiErr = err.error as ApiError | undefined;
          const msg =
            apiErr?.message ?? 'Impossible de charger les matchs publics';
          this.snack.open(msg, 'OK', { duration: 4000 });
          return of<MatchPublicCatalogue[]>([]);
        })
      );
    })
  );

  readonly matchs = toSignal(this.filtres$, {
    initialValue: [] as MatchPublicCatalogue[],
  });

  constructor() {
    // Désactive le loading dès que les résultats arrivent
    this.filtres$.subscribe(() => this.loading.set(false));
  }

  private toIsoDate(date: Date | null | undefined): string | undefined {
    if (!date) return undefined;
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  readonly hasResults = computed(() => this.matchs().length > 0);

  // Reset tous les filtres
  reset(): void {
    this.siteId.set(null);
    this.dateDebutCtrl.setValue(null);
    this.dateFinCtrl.setValue(null);
    this.placesMin.set(null);
  }

  onDetailClick(matchId: number): void {
    this.router.navigate(['/matchs', matchId]);
  }
}
