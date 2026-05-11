import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';

import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';

import { AuthService } from '../../../core/auth/auth.service';
import { ApiError } from '../../../core/api/auth.types';
import { SiteService } from '../../../core/api/site.service';
import { Site } from '../../../core/api/site.types';

type AbonnementType = 'MEMBRE_LIBRE' | 'MEMBRE_SITE' | 'MEMBRE_GLOBAL';

interface AbonnementOption {
  value: AbonnementType;
  titre: string;
  description: string;
  icone: string;
  badge?: string;
}

const ABONNEMENTS: AbonnementOption[] = [
  {
    value: 'MEMBRE_LIBRE',
    titre: 'Libre',
    description: 'Gratuit. Réservation jusqu\'à 5 jours avant le match.',
    icone: 'person',
    badge: 'Gratuit',
  },
  {
    value: 'MEMBRE_SITE',
    titre: 'Site',
    description: 'Attaché à un site spécifique. Réservation jusqu\'à 2 semaines avant.',
    icone: 'business',
  },
  {
    value: 'MEMBRE_GLOBAL',
    titre: 'Global',
    description: 'Tous les sites. Réservation jusqu\'à 3 semaines avant.',
    icone: 'public',
  },
];

// Regex du backend : ^\+?[0-9 .\-()]{6,30}$
const TELEPHONE_REGEX = /^\+?[0-9 .\-()]{6,30}$/;

// Validator qui rend siteRattachementId requis uniquement si role === MEMBRE_SITE.

function siteRequiredIfMembreSite(): ValidatorFn {
  return (control: AbstractControl) => {
    const role = control.parent?.get('role')?.value as AbonnementType | null;
    if (role === 'MEMBRE_SITE' && (control.value === null || control.value === undefined)) {
      return { siteRequired: true };
    }
    return null;
  };
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatDividerModule,
  ],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private siteService = inject(SiteService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);

  readonly abonnements = ABONNEMENTS;

  loading = signal(false);
  hidePassword = signal(true);

  // Sites chargés depuis le backend (endpoint public)
  readonly sites = toSignal(
    this.siteService.getActiveSites().pipe(
      catchError((err) => {
        console.error('Erreur chargement sites', err);
        this.snack.open('Impossible de charger la liste des sites', 'OK', {
          duration: 4000,
        });
        return of([] as Site[]);
      })
    ),
    { initialValue: [] as Site[] }
  );

  form = this.fb.nonNullable.group({
    nom: ['', [Validators.required, Validators.maxLength(100)]],
    prenom: ['', [Validators.required, Validators.maxLength(100)]],
    email: [
      '',
      [Validators.required, Validators.email, Validators.maxLength(255)],
    ],
    telephone: [
      '',
      [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(30),
        Validators.pattern(TELEPHONE_REGEX),
      ],
    ],
    motDePasse: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(100),
      ],
    ],
    role: ['MEMBRE_LIBRE' as AbonnementType, [Validators.required]],
    siteRattachementId: [null as number | null, [siteRequiredIfMembreSite()]],
  });

  // Pour afficher / cacher conditionnellement le sélecteur de site
  readonly isMembreSite = computed(() => this.selectedRole() === 'MEMBRE_SITE');
  readonly selectedRole = toSignal(this.form.controls.role.valueChanges, {
    initialValue: this.form.controls.role.value,
  });

  constructor() {
    effect(() => {
      const role = this.selectedRole();
      // Vider siteRattachementId si on quitte MEMBRE_SITE
      if (role !== 'MEMBRE_SITE') {
        this.form.controls.siteRattachementId.setValue(null, {
          emitEvent: false,
        });
      }
      // Revalider siteRattachementId puisque son validator dépend du rôle
      this.form.controls.siteRattachementId.updateValueAndValidity();
    });
  }

  selectAbonnement(value: AbonnementType): void {
    this.form.controls.role.setValue(value);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);

    const raw = this.form.getRawValue();
    const payload = {
      nom: raw.nom,
      prenom: raw.prenom,
      email: raw.email,
      telephone: raw.telephone,
      motDePasse: raw.motDePasse,
      role: raw.role,
      // siteRattachementId est inclus seulement si MEMBRE_SITE
      ...(raw.role === 'MEMBRE_SITE' && raw.siteRattachementId
        ? { siteRattachementId: raw.siteRattachementId }
        : {}),
    };

    this.auth.register(payload).subscribe({
      next: (profil) => {
        this.loading.set(false);
        this.snack.open(
          `Bienvenue ${profil.prenom} ! Votre matricule : ${profil.matricule}`,
          'OK',
          { duration: 5000 }
        );
        this.router.navigate(['/']);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        const apiErr = err.error as ApiError | undefined;
        const msg = apiErr?.message ?? 'Échec de l\'inscription';
        this.snack.open(msg, 'OK', { duration: 5000 });
      },
    });
  }
}
