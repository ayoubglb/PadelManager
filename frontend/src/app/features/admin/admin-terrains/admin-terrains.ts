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
import {toObservable, toSignal} from '@angular/core/rxjs-interop';
import {
  BehaviorSubject,
  Observable,
  catchError,
  combineLatest,
  of,
  switchMap,
  tap,
} from 'rxjs';

import { TerrainService } from '../../../core/api/terrain.service';
import { SiteService } from '../../../core/api/site.service';
import { Terrain } from '../../../core/api/terrain.types';
import { Site } from '../../../core/api/site.types';
import { ApiError } from '../../../core/api/auth.types';
import { AuthService } from '../../../core/auth/auth.service';
import { SiteSelector } from '../../../shared/components/site-selector/site-selector';
import {
  TerrainFormDialog,
  TerrainFormDialogData,
} from '../terrain-form-dialog/terrain-form-dialog';

@Component({
  selector: 'app-admin-terrains',
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
  templateUrl: './admin-terrains.html',
  styleUrl: './admin-terrains.css',
})
export class AdminTerrains {
  private terrainService = inject(TerrainService);
  private siteService = inject(SiteService);
  private authService = inject(AuthService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  readonly profil = this.authService.profil;
  readonly role = this.authService.currentRole;

  // Admin Site → site fixé sur son site de rattachement.
  // Admin Global → peut basculer entre sites via le selector.

  readonly isAdminSite = computed(() => this.role() === 'ADMIN_SITE');

  //  ID du site courant
  readonly selectedSiteId = signal<number | null>(null);

  // Trigger pour rafraîchir la liste
  private reload$ = new BehaviorSubject<void>(undefined);
  readonly loading = signal(false);

  // Nom du site courant pour l'affichage (label, dialogs).
  // Récupéré séparément du site service.

  private readonly siteCourant$: Observable<Site | null> = combineLatest([
    toObservable(this.selectedSiteId),
  ]).pipe(
    switchMap(([id]) => {
      if (id === null) return of(null);
      return this.siteService.getById(id).pipe(catchError(() => of(null)));
    })
  );

  readonly siteCourant = toSignal(this.siteCourant$, { initialValue: null });

  // Liste des terrains du site courant (actifs + désactivés)
  readonly terrains = toSignal(
    combineLatest([
      toObservable(this.selectedSiteId),
      this.reload$,
    ]).pipe(
      switchMap(([siteId]): Observable<Terrain[]> => {
        if (siteId === null) return of([]);
        this.loading.set(true);
        return this.terrainService.getAllBySiteAdmin(siteId).pipe(
          tap(() => this.loading.set(false)),
          catchError((err: HttpErrorResponse) => {
            this.loading.set(false);
            console.error('Erreur chargement terrains', err);
            const apiErr = err.error as ApiError | undefined;
            const msg = apiErr?.message ?? 'Impossible de charger les terrains';
            this.snack.open(msg, 'OK', { duration: 4000 });
            return of([] as Terrain[]);
          })
        );
      })
    ),
    { initialValue: [] as Terrain[] }
  );

  readonly nbActifs = computed(() => this.terrains().filter((t) => t.active).length);
  readonly nbInactifs = computed(() => this.terrains().filter((t) => !t.active).length);

  readonly displayedColumns = ['numero', 'nom', 'statut', 'actions'];

  constructor() {
    // Pour Admin Site : verrouiller le site selectionné sur son site de rattachement
    effect(() => {
      const p = this.profil();
      if (p && this.isAdminSite() && p.siteRattachementId !== null) {
        this.selectedSiteId.set(p.siteRattachementId);
      }
    });
  }

  openCreateDialog(): void {
    const site = this.siteCourant();
    if (!site) return;

    const data: TerrainFormDialogData = {
      siteId: site.id,
      siteNom: site.nom,
    };
    const ref = this.dialog.open(TerrainFormDialog, { width: '500px', data });

    ref.afterClosed().subscribe((result?: { success: boolean; terrain: Terrain }) => {
      if (result?.success) {
        const nom = result.terrain.nom ? ` (${result.terrain.nom})` : '';
        this.snack.open(
          `Terrain ${result.terrain.numero}${nom} créé.`,
          'OK',
          { duration: 3000 }
        );
        this.reload$.next();
      }
    });
  }

  openEditDialog(terrain: Terrain): void {
    const site = this.siteCourant();
    if (!site) return;

    const data: TerrainFormDialogData = {
      siteId: site.id,
      siteNom: site.nom,
      terrain,
    };
    const ref = this.dialog.open(TerrainFormDialog, { width: '500px', data });

    ref.afterClosed().subscribe((result?: { success: boolean; terrain: Terrain }) => {
      if (result?.success) {
        this.snack.open('Terrain mis à jour.', 'OK', { duration: 3000 });
        this.reload$.next();
      }
    });
  }

  deactivate(terrain: Terrain): void {
    const label = terrain.nom ? `${terrain.numero} (${terrain.nom})` : `${terrain.numero}`;
    if (!confirm(
      `Désactiver le terrain ${label} ?\n\n` +
      `Les matchs existants restent attachés, mais aucune nouvelle réservation ne sera possible.`
    )) {
      return;
    }
    this.terrainService.deactivate(terrain.id).subscribe({
      next: () => {
        this.snack.open(`Terrain ${label} désactivé.`, 'OK', { duration: 3000 });
        this.reload$.next();
      },
      error: (err: HttpErrorResponse) => {
        const apiErr = err.error as ApiError | undefined;
        const msg = apiErr?.message ?? 'Échec de la désactivation';
        this.snack.open(msg, 'OK', { duration: 4000 });
      },
    });
  }

  activate(terrain: Terrain): void {
    const label = terrain.nom ? `${terrain.numero} (${terrain.nom})` : `${terrain.numero}`;
    this.terrainService.activate(terrain.id).subscribe({
      next: () => {
        this.snack.open(`Terrain ${label} réactivé.`, 'OK', { duration: 3000 });
        this.reload$.next();
      },
      error: (err: HttpErrorResponse) => {
        const apiErr = err.error as ApiError | undefined;
        const msg = apiErr?.message ?? 'Échec de la réactivation';
        this.snack.open(msg, 'OK', { duration: 4000 });
      },
    });
  }
}


