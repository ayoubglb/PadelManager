import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
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

import { JourFermetureService } from '../../../core/api/jour-fermeture.service';
import { SiteService } from '../../../core/api/site.service';
import { JourFermeture } from '../../../core/api/jour-fermeture.types';
import { Site } from '../../../core/api/site.types';
import { ApiError } from '../../../core/api/auth.types';
import { AuthService } from '../../../core/auth/auth.service';
import { SiteSelector } from '../../../shared/components/site-selector/site-selector';
import {
  FermetureFormDialog,
  FermetureFormDialogData,
} from '../fermeture-form-dialog/fermeture-form-dialog';

@Component({
  selector: 'app-admin-fermetures',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatTableModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    SiteSelector,
  ],
  templateUrl: './admin-fermetures.html',
  styleUrl: './admin-fermetures.css',
})
export class AdminFermetures {
  private fermetureService = inject(JourFermetureService);
  private siteService = inject(SiteService);
  private authService = inject(AuthService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  readonly profil = this.authService.profil;
  readonly role = this.authService.currentRole;
  readonly isAdminSite = computed(() => this.role() === 'ADMIN_SITE');
  readonly isAdminGlobal = computed(() => this.role() === 'ADMIN_GLOBAL');

  readonly selectedSiteId = signal<number | null>(null);
  private reload$ = new BehaviorSubject<void>(undefined);
  readonly loading = signal(false);

  // Site courant (pour affichage du nom dans dialogs et tableau)
  readonly siteCourant = toSignal(
    toObservable(this.selectedSiteId).pipe(
      switchMap((id): Observable<Site | null> => {
        if (id === null) return of(null);
        return this.siteService.getById(id).pipe(catchError(() => of(null)));
      })
    ),
    { initialValue: null }
  );

  /** Fermetures globales (admin global uniquement). */
  readonly fermeturesGlobales = toSignal(
    this.reload$.pipe(
      switchMap((): Observable<JourFermeture[]> => {
        if (!this.isAdminGlobal()) return of([]);
        return this.fermetureService.getGlobales().pipe(
          catchError((err: HttpErrorResponse) => {
            console.error('Erreur chargement fermetures globales', err);
            const apiErr = err.error as ApiError | undefined;
            const msg = apiErr?.message ?? 'Impossible de charger les fermetures globales';
            this.snack.open(msg, 'OK', { duration: 4000 });
            return of([] as JourFermeture[]);
          })
        );
      })
    ),
    { initialValue: [] as JourFermeture[] }
  );

  // Fermetures spécifiques au site sélectionné
  readonly fermeturesSite = toSignal(
    combineLatest([toObservable(this.selectedSiteId), this.reload$]).pipe(
      switchMap(([siteId]): Observable<JourFermeture[]> => {
        if (siteId === null) return of([]);
        this.loading.set(true);
        return this.fermetureService.getBySite(siteId).pipe(
          tap(() => this.loading.set(false)),
          catchError((err: HttpErrorResponse) => {
            this.loading.set(false);
            console.error('Erreur chargement fermetures site', err);
            const apiErr = err.error as ApiError | undefined;
            const msg = apiErr?.message ?? 'Impossible de charger les fermetures de ce site';
            this.snack.open(msg, 'OK', { duration: 4000 });
            return of([] as JourFermeture[]);
          })
        );
      })
    ),
    { initialValue: [] as JourFermeture[] }
  );

  // Fermetures globales triées par date croissante
  readonly globalesTriees = computed(() =>
    [...this.fermeturesGlobales()].sort((a, b) =>
      a.dateFermeture.localeCompare(b.dateFermeture)
    )
  );

  // Fermetures site triées par date croissante
  readonly siteTriees = computed(() =>
    [...this.fermeturesSite()].sort((a, b) =>
      a.dateFermeture.localeCompare(b.dateFermeture)
    )
  );

  readonly displayedColumns = ['date', 'raison', 'statut', 'actions'];

  constructor() {
    // Admin Site : verrouiller sur son site
    effect(() => {
      const p = this.profil();
      if (p && this.isAdminSite() && p.siteRattachementId !== null) {
        this.selectedSiteId.set(p.siteRattachementId);
      }
    });
  }

  // Date passée ou aujourd'hui ? (non supprimable)
  isPassee(dateStr: string): boolean {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const d = new Date(dateStr);
    d.setHours(0, 0, 0, 0);
    return d <= today;
  }

  openCreateGlobaleDialog(): void {
    const data: FermetureFormDialogData = {
      siteId: null,
      siteNom: null,
    };
    const ref = this.dialog.open(FermetureFormDialog, { width: '500px', data });

    ref.afterClosed().subscribe((result?: { success: boolean; fermeture: JourFermeture }) => {
      if (result?.success) {
        this.snack.open(
          `Fermeture globale du ${this.formatDate(result.fermeture.dateFermeture)} créée.`,
          'OK',
          { duration: 3000 }
        );
        this.reload$.next();
      }
    });
  }

  openCreateSiteDialog(): void {
    const site = this.siteCourant();
    if (!site) return;

    const data: FermetureFormDialogData = {
      siteId: site.id,
      siteNom: site.nom,
    };
    const ref = this.dialog.open(FermetureFormDialog, { width: '500px', data });

    ref.afterClosed().subscribe((result?: { success: boolean; fermeture: JourFermeture }) => {
      if (result?.success) {
        this.snack.open(
          `Fermeture du ${this.formatDate(result.fermeture.dateFermeture)} créée pour ${site.nom}.`,
          'OK',
          { duration: 3000 }
        );
        this.reload$.next();
      }
    });
  }

  delete(fermeture: JourFermeture): void {
    if (this.isPassee(fermeture.dateFermeture)) return;

    if (!confirm(
      `Supprimer la fermeture du ${this.formatDate(fermeture.dateFermeture)} ?\n\n` +
      `Raison : ${fermeture.raison}`
    )) {
      return;
    }

    this.fermetureService.delete(fermeture.id).subscribe({
      next: () => {
        this.snack.open('Fermeture supprimée.', 'OK', { duration: 3000 });
        this.reload$.next();
      },
      error: (err: HttpErrorResponse) => {
        const apiErr = err.error as ApiError | undefined;
        const msg = apiErr?.message ?? 'Échec de la suppression';
        this.snack.open(msg, 'OK', { duration: 4000 });
      },
    });
  }

  private formatDate(dateStr: string): string {
    const d = new Date(dateStr);
    return d.toLocaleDateString('fr-BE');
  }
}
