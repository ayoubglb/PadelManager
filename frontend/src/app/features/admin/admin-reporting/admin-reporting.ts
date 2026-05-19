import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
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
  tap, startWith,
} from 'rxjs';

import { ReportingService } from '../../../core/api/reporting.service';
import { Reporting } from '../../../core/api/reporting.types';
import { ApiError } from '../../../core/api/auth.types';
import { AuthService } from '../../../core/auth/auth.service';
import { SiteSelector } from '../../../shared/components/site-selector/site-selector';

/**
 * Mode d'affichage de la page de reporting.
 * - 'global' : reporting tous sites, accessible Admin Global uniquement
 * - 'site' : reporting d'un site, accessible Admin Global (avec SiteSelector) ou Admin Site (site verrouillé)
 */
export type ReportingMode = 'global' | 'site';

@Component({
  selector: 'app-admin-reporting',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe,
    ReactiveFormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    SiteSelector,
  ],
  templateUrl: './admin-reporting.html',
  styleUrl: './admin-reporting.css',
})
export class AdminReporting {
  private reportingService = inject(ReportingService);
  private authService = inject(AuthService);
  private snack = inject(MatSnackBar);

  /** Mode reçu via la route (resolver de route ou input). */
  readonly mode = input.required<ReportingMode>();

  readonly profil = this.authService.profil;
  readonly role = this.authService.currentRole;
  readonly isAdminSite = computed(() => this.role() === 'ADMIN_SITE');

  /** Site sélectionné en mode 'site'. */
  readonly selectedSiteId = signal<number | null>(null);

  /** Période par défaut : année en cours. */
  readonly dateDebutCtrl = new FormControl<Date>(
    new Date(new Date().getFullYear(), 0, 1),
    { nonNullable: true }
  );
  readonly dateFinCtrl = new FormControl<Date>(
    new Date(new Date().getFullYear(), 11, 31),
    { nonNullable: true }
  );

  readonly loading = signal(false);

  /** Trigger pour recharger après changement de période. */
  private reload$ = new BehaviorSubject<void>(undefined);

  /**
   * Le reporting courant, recalculé à chaque changement de mode/site/période.
   */
  readonly reporting = toSignal(
    combineLatest([
      toObservable(this.selectedSiteId),
      this.dateDebutCtrl.valueChanges.pipe(startWith(this.dateDebutCtrl.value)),
      this.dateFinCtrl.valueChanges.pipe(startWith(this.dateFinCtrl.value)),
      this.reload$,
    ]).pipe(
      switchMap((): Observable<Reporting | null> => {
        return this.charger();
      })
    ),
    { initialValue: null as Reporting | null }
  );

  constructor() {
    // Admin Site : verrouiller sur son site (mode 'site')
    effect(() => {
      const p = this.profil();
      if (p && this.isAdminSite() && p.siteRattachementId !== null && this.mode() === 'site') {
        this.selectedSiteId.set(p.siteRattachementId);
      }
    });

    // Premier chargement après init
    effect(() => {
      // Watch mode et profil pour le chargement initial
      const m = this.mode();
      const p = this.profil();
      if (m === 'global' && p) {
        // Charge immédiatement pour le mode global
        queueMicrotask(() => this.reload$.next());
      }
    });
  }

  /**
   * Lance un appel reporting selon le mode et l'état courant.
   */
  private charger(): Observable<Reporting | null> {
    const dateDebut = this.toIsoDate(this.dateDebutCtrl.value);
    const dateFin = this.toIsoDate(this.dateFinCtrl.value);

    if (!dateDebut || !dateFin) return of(null);

    // Détermine la source selon le mode
    let source$: Observable<Reporting | null>;
    if (this.mode() === 'global') {
      source$ = this.reportingService.getGlobal({ dateDebut, dateFin });
    } else if (this.selectedSiteId() !== null) {
      source$ = this.reportingService.getBySite(this.selectedSiteId()!, { dateDebut, dateFin });
    } else {
      // Mode 'site' sans site sélectionné → on retourne null sans appel HTTP
      return of(null);
    }

    this.loading.set(true);

    return source$.pipe(
      tap(() => this.loading.set(false)),
      catchError((err: HttpErrorResponse) => {
        this.loading.set(false);
        console.error('Erreur chargement reporting', err);
        const apiErr = err.error as ApiError | undefined;
        const msg = apiErr?.message ?? 'Impossible de charger le reporting';
        this.snack.open(msg, 'OK', { duration: 4000 });
        return of(null);
      })
    );
  }

  /**
   * Bouton "Rafraîchir" pour forcer un rechargement (utile si la période n'a pas changé).
   */
  refresh(): void {
    this.reload$.next();
  }

  /** Conversion Date → YYYY-MM-DD. */
  private toIsoDate(date: Date | null): string {
    if (!date) return '';
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  /** Taux d'annulation en %. */
  readonly tauxAnnulation = computed(() => {
    const r = this.reporting();
    if (!r || r.nombreMatchsTotaux === 0) return 0;
    return (r.nombreMatchsAnnules / r.nombreMatchsTotaux) * 100;
  });

  /** Ratio publics/privés en %. */
  readonly partPublics = computed(() => {
    const r = this.reporting();
    if (!r || r.nombreMatchsTotaux === 0) return 0;
    return (r.nombreMatchsPublics / r.nombreMatchsTotaux) * 100;
  });

  readonly displayedColumns = ['rang', 'nomComplet', 'matricule', 'nombreMatchsOrganises'];
}
