import { Component, computed, inject, signal } from '@angular/core';
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
import { toSignal } from '@angular/core/rxjs-interop';
import {BehaviorSubject, catchError, Observable, of, switchMap, tap} from 'rxjs';

import { SiteService } from '../../../core/api/site.service';
import { Site } from '../../../core/api/site.types';
import { ApiError } from '../../../core/api/auth.types';
import {
  SiteFormDialog,
  SiteFormDialogData,
} from '../site-form-dialog/site-form-dialog';

@Component({
  selector: 'app-admin-sites',
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
  ],
  templateUrl: './admin-sites.html',
  styleUrl: './admin-sites.css',
})
export class AdminSites {
  private siteService = inject(SiteService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  // Trigger pour rafraîchir la liste après une action
  private reload$ = new BehaviorSubject<void>(undefined);
  readonly loading = signal(false);

  // Liste de tous les sites (actifs + désactivés) — vue admin globale.
  // Rechargée à chaque action via reload$
  readonly sites = toSignal(
    this.reload$.pipe(
      tap(() => this.loading.set(true)),
      switchMap((): Observable<Site[]> =>
        this.siteService.getAllSitesAdmin().pipe(
          tap(() => this.loading.set(false)),
          catchError((err: HttpErrorResponse) => {
            this.loading.set(false);
            console.error('Erreur chargement sites', err);
            const apiErr = err.error as ApiError | undefined;
            const msg = apiErr?.message ?? 'Impossible de charger les sites';
            this.snack.open(msg, 'OK', { duration: 4000 });
            return of([] as Site[]);
          })
        )
      )
    ),
    { initialValue: [] as Site[] }
  );

  readonly nbActifs = computed(() => this.sites().filter((s) => s.active).length);
  readonly nbInactifs = computed(() => this.sites().filter((s) => !s.active).length);

  readonly displayedColumns = ['nom', 'adresse', 'codePostal', 'ville', 'statut', 'actions'];

  openCreateDialog(): void {
    const data: SiteFormDialogData = {};
    const ref = this.dialog.open(SiteFormDialog, { width: '500px', data });

    ref.afterClosed().subscribe((result?: { success: boolean; site: Site }) => {
      if (result?.success) {
        this.snack.open(`Site "${result.site.nom}" créé.`, 'OK', { duration: 3000 });
        this.reload$.next();
      }
    });
  }

  openEditDialog(site: Site): void {
    const data: SiteFormDialogData = { site };
    const ref = this.dialog.open(SiteFormDialog, { width: '500px', data });

    ref.afterClosed().subscribe((result?: { success: boolean; site: Site }) => {
      if (result?.success) {
        this.snack.open(`Site "${result.site.nom}" mis à jour.`, 'OK', { duration: 3000 });
        this.reload$.next();
      }
    });
  }

  deactivate(site: Site): void {
    if (!confirm(`Désactiver le site "${site.nom}" ?\n\nLes matchs existants restent consultables, mais aucune nouvelle réservation ne sera possible.`)) {
      return;
    }
    this.siteService.deactivate(site.id).subscribe({
      next: () => {
        this.snack.open(`Site "${site.nom}" désactivé.`, 'OK', { duration: 3000 });
        this.reload$.next();
      },
      error: (err: HttpErrorResponse) => {
        const apiErr = err.error as ApiError | undefined;
        const msg = apiErr?.message ?? 'Échec de la désactivation';
        this.snack.open(msg, 'OK', { duration: 4000 });
      },
    });
  }

  activate(site: Site): void {
    this.siteService.activate(site.id).subscribe({
      next: () => {
        this.snack.open(`Site "${site.nom}" réactivé.`, 'OK', { duration: 3000 });
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
