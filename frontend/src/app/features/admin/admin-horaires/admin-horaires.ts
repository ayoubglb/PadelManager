import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import {
  BehaviorSubject,
  Observable,
  catchError,
  combineLatest,
  of,
  switchMap,
  tap,
} from 'rxjs';

import { HoraireService } from '../../../core/api/horaire.service';
import { SiteService } from '../../../core/api/site.service';
import { HoraireSite } from '../../../core/api/horaire.types';
import { Site } from '../../../core/api/site.types';
import { ApiError } from '../../../core/api/auth.types';
import { AuthService } from '../../../core/auth/auth.service';
import { SiteSelector } from '../../../shared/components/site-selector/site-selector';
import {
  HoraireFormDialog,
  HoraireFormDialogData,
} from '../horaire-form-dialog/horaire-form-dialog';

@Component({
  selector: 'app-admin-horaires',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatTableModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    SiteSelector,
  ],
  templateUrl: './admin-horaires.html',
  styleUrl: './admin-horaires.css',
})
export class AdminHoraires {
  private horaireService = inject(HoraireService);
  private siteService = inject(SiteService);
  private authService = inject(AuthService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  readonly profil = this.authService.profil;
  readonly role = this.authService.currentRole;
  readonly isAdminSite = computed(() => this.role() === 'ADMIN_SITE');

  readonly selectedSiteId = signal<number | null>(null);
  private reload$ = new BehaviorSubject<void>(undefined);
  readonly loading = signal(false);

  readonly anneeCourante = new Date().getFullYear();

  // Site courant (pour affichage du nom dans le dialog)
  readonly siteCourant = toSignal(
    toObservable(this.selectedSiteId).pipe(
      switchMap((id): Observable<Site | null> => {
        if (id === null) return of(null);
        return this.siteService.getById(id).pipe(catchError(() => of(null)));
      })
    ),
    { initialValue: null }
  );

  // Liste des horaires du site courant, triés par année décroissante

  readonly horaires = toSignal(
    combineLatest([toObservable(this.selectedSiteId), this.reload$]).pipe(
      switchMap(([siteId]): Observable<HoraireSite[]> => {
        if (siteId === null) return of([]);
        this.loading.set(true);
        return this.horaireService.getBySite(siteId).pipe(
          tap(() => this.loading.set(false)),
          catchError((err: HttpErrorResponse) => {
            this.loading.set(false);
            console.error('Erreur chargement horaires', err);
            const apiErr = err.error as ApiError | undefined;
            const msg = apiErr?.message ?? 'Impossible de charger les horaires';
            this.snack.open(msg, 'OK', { duration: 4000 });
            return of([] as HoraireSite[]);
          })
        );
      })
    ),
    { initialValue: [] as HoraireSite[] }
  );

  // Horaires triés (année décroissante : plus récent en premier)
  readonly horairesTries = computed(() =>
    [...this.horaires()].sort((a, b) => b.annee - a.annee)
  );

  // Liste des années déjà utilisées pour empêcher les doublons à la création
  readonly anneesDejaUtilisees = computed(() =>
    this.horaires().map((h) => h.annee)
  );

  readonly displayedColumns = ['annee', 'heureDebut', 'heureFin', 'statut', 'actions'];

  constructor() {
    // Pour Admin Site : verrouiller sur son site de rattachement
    effect(() => {
      const p = this.profil();
      if (p && this.isAdminSite() && p.siteRattachementId !== null) {
        this.selectedSiteId.set(p.siteRattachementId);
      }
    });
  }

  // L'année est-elle passée ? (horaire non supprimable)
  isAnneePassee(annee: number): boolean {
    return annee < this.anneeCourante;
  }

  // L'année est-elle en cours ? (horaire non supprimable)
  isAnneeCourante(annee: number): boolean {
    return annee === this.anneeCourante;
  }

  // Format HH:mm depuis HH:mm:ss
  formatHeure(h: string): string {
    return h.slice(0, 5);
  }

  openCreateDialog(): void {
    const site = this.siteCourant();
    if (!site) return;

    const data: HoraireFormDialogData = {
      siteId: site.id,
      siteNom: site.nom,
      anneesDejaUtilisees: this.anneesDejaUtilisees(),
    };
    const ref = this.dialog.open(HoraireFormDialog, { width: '500px', data });

    ref.afterClosed().subscribe((result?: { success: boolean; horaire: HoraireSite }) => {
      if (result?.success) {
        this.snack.open(
          `Horaire ${result.horaire.annee} créé.`,
          'OK',
          { duration: 3000 }
        );
        this.reload$.next();
      }
    });
  }

  delete(horaire: HoraireSite): void {
    if (this.isAnneePassee(horaire.annee) || this.isAnneeCourante(horaire.annee)) {
      // Sécurité : on ne devrait jamais arriver ici via l'UI, mais on protège quand même
      return;
    }

    if (!confirm(
      `Supprimer l'horaire de ${horaire.annee} ?\n\n` +
      `Sans horaire défini pour cette année, aucune réservation ne pourra être faite sur ce site.`
    )) {
      return;
    }

    this.horaireService.delete(horaire.id).subscribe({
      next: () => {
        this.snack.open(`Horaire ${horaire.annee} supprimé.`, 'OK', { duration: 3000 });
        this.reload$.next();
      },
      error: (err: HttpErrorResponse) => {
        const apiErr = err.error as ApiError | undefined;
        const msg = apiErr?.message ?? 'Échec de la suppression';
        this.snack.open(msg, 'OK', { duration: 4000 });
      },
    });
  }
}
