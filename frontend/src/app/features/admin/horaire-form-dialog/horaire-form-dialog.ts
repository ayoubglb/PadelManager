import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';

import { HoraireService } from '../../../core/api/horaire.service';
import {
  HoraireSite,
  HoraireCreateRequest,
} from '../../../core/api/horaire.types';
import { ApiError } from '../../../core/api/auth.types';
import {toSignal} from '@angular/core/rxjs-interop';

// Données injectées au dialog (mode création uniquement).
// `anneesDejaUtilisees` permet d'empêcher de saisir une année qui a déjà un horaire
// (sinon le backend renverra une erreur de contrainte d'unicité)

export interface HoraireFormDialogData {
  siteId: number;
  siteNom: string;
  anneesDejaUtilisees: number[];
}

@Component({
  selector: 'app-horaire-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './horaire-form-dialog.html',
  styleUrl: './horaire-form-dialog.css',
})
export class HoraireFormDialog {
  private fb = inject(FormBuilder);
  private horaireService = inject(HoraireService);
  private snack = inject(MatSnackBar);
  private dialogRef = inject(MatDialogRef<HoraireFormDialog>);

  readonly data = inject<HoraireFormDialogData>(MAT_DIALOG_DATA);
  readonly saving = signal(false);

  readonly anneeCourante = new Date().getFullYear();

  readonly form = this.fb.nonNullable.group({
    annee: [
      this.anneeCourante,
      [
        Validators.required,
        Validators.min(this.anneeCourante),
        Validators.max(this.anneeCourante + 10),
      ],
    ],
    heureDebut: ['08:00', [Validators.required, this.timeFormatValidator]],
    heureFin: ['22:00', [Validators.required, this.timeFormatValidator]],
  });

  // Conversion en signal du valueChanges de l'année.
  // Nécessaire parce que les FormControl ne sont pas des Signals natifs :
  // sans cette conversion, le computed `anneeDejaUtilisee` ne se mettrait
  // jamais à jour quand l'utilisateur change la valeur.

  private readonly anneeValue = toSignal(
    this.form.controls.annee.valueChanges,
    { initialValue: this.form.controls.annee.value }
  );

  private readonly heureDebutValue = toSignal(
    this.form.controls.heureDebut.valueChanges,
    { initialValue: this.form.controls.heureDebut.value }
  );

  private readonly heureFinValue = toSignal(
    this.form.controls.heureFin.valueChanges,
    { initialValue: this.form.controls.heureFin.value }
  );

  // Vérifie que l'année saisie n'est pas déjà utilisée
  readonly anneeDejaUtilisee = computed(() => {
    return this.data.anneesDejaUtilisees.includes(this.anneeValue());
  });

  // Vérifie que heureDebut < heureFin
  readonly heuresInvalides = computed(() => {
    const debut = this.heureDebutValue();
    const fin = this.heureFinValue();
    if (!debut || !fin) return false;
    return debut >= fin;
  });

  // Valide le format HH:mm
  private timeFormatValidator(control: any) {
    const value = control.value;
    if (!value) return null;
    const regex = /^([01]\d|2[0-3]):[0-5]\d$/;
    return regex.test(value) ? null : { timeFormat: true };
  }

  save(): void {
    if (this.form.invalid || this.anneeDejaUtilisee() || this.heuresInvalides() || this.saving()) {
      return;
    }
    this.saving.set(true);

    const raw = this.form.getRawValue();
    const body: HoraireCreateRequest = {
      annee: raw.annee,
      heureDebut: raw.heureDebut.length === 5 ? `${raw.heureDebut}:00` : raw.heureDebut,
      heureFin: raw.heureFin.length === 5 ? `${raw.heureFin}:00` : raw.heureFin,
    };

    this.horaireService.create(this.data.siteId, body).subscribe({
      next: (horaire) => {
        this.saving.set(false);
        this.dialogRef.close({ success: true, horaire });
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        const apiErr = err.error as ApiError | undefined;
        const msg = apiErr?.message ?? "Échec de la création de l'horaire";
        this.snack.open(msg, 'OK', { duration: 4000 });
      },
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
