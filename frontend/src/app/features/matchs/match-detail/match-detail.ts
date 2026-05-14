import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { switchMap, catchError, of, BehaviorSubject } from 'rxjs';

import { MatchService } from '../../../core/api/match.service';
import { MatchDetail, InscriptionMatch } from '../../../core/api/match.types';
import { ApiError } from '../../../core/api/auth.types';
import { AuthService } from '../../../core/auth/auth.service';
import { TransactionService } from '../../../core/api/transaction.service';

const PRIX_PAR_JOUEUR = 15;
const DELAI_ANNULATION_PRIVE_HEURES = 48;
const DELAI_ANNULATION_PUBLIC_HEURES = 24;

@Component({
  selector: 'app-match-detail',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './match-detail.html',
  styleUrl: './match-detail.css',
})
export class MatchDetailPage {
  private matchService = inject(MatchService);
  private authService = inject(AuthService);
  private transactionService = inject(TransactionService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  readonly PRIX_PAR_JOUEUR = PRIX_PAR_JOUEUR;

  // ID du match extrait de l'URL
  readonly matchId = Number(this.route.snapshot.paramMap.get('id'));

  // Trigger pour recharger le match après une action
  private reloadTrigger$ = new BehaviorSubject<void>(undefined);

  // Détail du match. Rechargé à chaque action (paye, annule, etc.)
  readonly match = toSignal<MatchDetail | null>(
    this.reloadTrigger$.pipe(
      switchMap(() =>
        this.matchService.getById(this.matchId).pipe(
          catchError((err: HttpErrorResponse) => {
            console.error('Erreur chargement match', err);
            const apiErr = err.error as ApiError | undefined;
            const msg = apiErr?.message ?? 'Impossible de charger ce match';
            this.snack.open(msg, 'OK', { duration: 4000 });
            return of(null);
          })
        )
      )
    ),
    { initialValue: null }
  );

  readonly loading = signal(false);
  readonly profil = this.authService.profil;
  readonly solde = this.transactionService.solde;

  // L'utilisateur courant est-il l'organisateur ?
  readonly isOrganisateur = computed(() => {
    const m = this.match();
    const p = this.profil();
    return !!(m && p && m.organisateurId === p.id);
  });

  // Mon inscription dans ce match (null si pas inscrit)
  readonly monInscription = computed<InscriptionMatch | null>(() => {
    const m = this.match();
    const p = this.profil();
    if (!m || !p) return null;
    return (
      m.inscriptions.find(
        (i) => i.joueurId === p.id && i.statut === 'INSCRIT'
      ) ?? null
    );
  });

  readonly suisInscrit = computed(() => this.monInscription() !== null);
  readonly aPaye = computed(() => this.monInscription()?.paye === true);

  // Le match est-il dans le futur ?
  readonly estFutur = computed(() => {
    const m = this.match();
    if (!m) return false;
    return new Date(m.dateHeureDebut) > new Date();
  });

  // Délai d'annulation respecté ?
  readonly delaiAnnulationOk = computed(() => {
    const m = this.match();
    if (!m) return false;
    const delaiHeures =
      m.type === 'PRIVE'
        ? DELAI_ANNULATION_PRIVE_HEURES
        : DELAI_ANNULATION_PUBLIC_HEURES;
    const limite = new Date(m.dateHeureDebut);
    limite.setHours(limite.getHours() - delaiHeures);
    return new Date() < limite;
  });

  // Affiche "Annuler le match" ?
  readonly peutAnnuler = computed(() => {
    const m = this.match();
    return !!(
      m &&
      m.statut === 'PROGRAMME' &&
      this.isOrganisateur() &&
      this.estFutur() &&
      this.delaiAnnulationOk()
    );
  });

  // Affiche "Inviter un joueur" ?
  readonly peutInviter = computed(() => {
    const m = this.match();
    return !!(
      m &&
      m.statut === 'PROGRAMME' &&
      m.type === 'PRIVE' &&
      this.isOrganisateur() &&
      this.estFutur() &&
      m.placesDisponibles > 0
    );
  });

  // Affiche "Payer ma part" ?
  readonly peutPayer = computed(() => {
    const m = this.match();
    return !!(
      m &&
      m.statut === 'PROGRAMME' &&
      this.suisInscrit() &&
      !this.aPaye() &&
      this.estFutur()
    );
  });

  // Affiche "Rejoindre" ?
  readonly peutRejoindre = computed(() => {
    const m = this.match();
    return !!(
      m &&
      m.statut === 'PROGRAMME' &&
      m.type === 'PUBLIC' &&
      !this.suisInscrit() &&
      m.placesDisponibles > 0 &&
      this.estFutur()
    );
  });

  // Solde suffisant pour payer 15€ ?
  readonly soldeSuffisant = computed(() => {
    const s = this.solde();
    return s !== null && s >= PRIX_PAR_JOUEUR;
  });

  // === Actions ===

  payer(): void {
    this.loading.set(true);
    this.matchService.payer(this.matchId).subscribe({
      next: () => {
        this.loading.set(false);
        this.snack.open('Paiement effectué', 'OK', { duration: 3000 });
        this.reloadTrigger$.next();
      },
      error: (err: HttpErrorResponse) => this.handleError(err),
    });
  }

  rejoindre(): void {
    this.loading.set(true);
    this.matchService.rejoindre(this.matchId).subscribe({
      next: () => {
        this.loading.set(false);
        this.snack.open('Vous avez rejoint le match', 'OK', { duration: 3000 });
        this.reloadTrigger$.next();
      },
      error: (err: HttpErrorResponse) => this.handleError(err),
    });
  }

  annulerMatch(): void {
    if (!confirm('Annuler ce match ? Tous les joueurs ayant payé seront remboursés.')) {
      return;
    }
    this.loading.set(true);
    this.matchService.annuler(this.matchId).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.snack.open(
          `Match annulé. ${res.nombreRemboursements} remboursement(s) émis.`,
          'OK',
          { duration: 4000 }
        );
        this.reloadTrigger$.next();
      },
      error: (err: HttpErrorResponse) => this.handleError(err),
    });
  }

  openInviteDialog(): void {
    this.snack.open("Dialog d'invitation à venir", 'OK', { duration: 2500 });
  }

  retour(): void {
    this.router.navigate(['/matchs/mes-matchs']);
  }

  private handleError(err: HttpErrorResponse): void {
    this.loading.set(false);
    const apiErr = err.error as ApiError | undefined;
    const msg = apiErr?.message ?? 'Une erreur est survenue';
    this.snack.open(msg, 'OK', { duration: 4000 });
  }
}
