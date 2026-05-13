import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';

import { MatchService } from '../../../core/api/match.service';
import { MesMatch } from '../../../core/api/match.types';
import { MatchCard } from '../../../shared/components/match-card/match-card';

@Component({
  selector: 'app-mes-matchs',
  standalone: true,
  imports: [
    CommonModule,
    MatTabsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatchCard,
  ],
  templateUrl: './mes-matchs.html',
  styleUrl: './mes-matchs.css',
})
export class MesMatchs {
  private matchService = inject(MatchService);
  private snack = inject(MatSnackBar);
  private router = inject(Router);

  // Matchs à venir. null pendant le chargement
  readonly matchsAVenir = toSignal<MesMatch[] | null>(
    this.matchService.getMesMatchs(true).pipe(
      catchError((err) => {
        console.error('Erreur chargement matchs à venir', err);
        this.snack.open('Impossible de charger vos matchs à venir', 'OK', {
          duration: 4000,
        });
        return of([] as MesMatch[]);
      })
    ),
    { initialValue: null }
  );

  // Matchs passés. null pendant le chargement
  readonly matchsHistorique = toSignal<MesMatch[] | null>(
    this.matchService.getMesMatchs(false).pipe(
      catchError((err) => {
        console.error('Erreur chargement historique', err);
        this.snack.open('Impossible de charger votre historique', 'OK', {
          duration: 4000,
        });
        return of([] as MesMatch[]);
      })
    ),
    { initialValue: null }
  );

  // Compteur des matchs à venir, pour affichage dans le badge de l'onglet
  readonly nbAVenir = computed(() => this.matchsAVenir()?.length ?? 0);
  readonly nbHistorique = computed(() => this.matchsHistorique()?.length ?? 0);

  onDetailClick(matchId: number): void {
    this.router.navigate(['/matchs', matchId]);
  }
}
