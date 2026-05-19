import { Component, inject, signal } from '@angular/core';
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
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';

import { JourFermetureService } from '../../../core/api/jour-fermeture.service';
import {
  JourFermeture,
  JourFermetureCreateRequest,
} from '../../../core/api/jour-fermeture.types';
import { ApiError } from '../../../core/api/auth.types';

// Données injectées au dialog.
// - Si `siteId` null → fermeture globale (badge "Globale")
// - Si `siteId` non null → fermeture spécifique au site (`siteNom` requis pour affichage)
export interface FermetureFormDialogData {
  siteId: number | null;
  siteNom: string | null;
}

@Component({
  selector: 'app-fermeture-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './fermeture-form-dialog.html',
  styleUrl: './fermeture-form-dialog.css',
})
export class FermetureFormDialog {
  private fb = inject(FormBuilder);
  private fermetureService = inject(JourFermetureService);
  private snack = inject(MatSnackBar);
  private dialogRef = inject(MatDialogRef<FermetureFormDialog>);

  readonly data = inject<FermetureFormDialogData>(MAT_DIALOG_DATA);
  readonly saving = signal(false);
  readonly isGlobale = this.data.siteId === null;

  // Date minimum sélectionnable = demain (pas le passé ni aujourd'hui)
  readonly minDate = (() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d;
  })();

  readonly form = this.fb.nonNullable.group({
    dateFermeture: this.fb.nonNullable.control<Date | null>(null, [Validators.required]),
    raison: ['', [Validators.required, Validators.maxLength(200)]],
  });

  save(): void {
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true);

    const raw = this.form.getRawValue();
    const body: JourFermetureCreateRequest = {
      dateFermeture: this.toIsoDate(raw.dateFermeture!),
      siteId: this.data.siteId,
      raison: raw.raison.trim(),
    };

    this.fermetureService.create(body).subscribe({
      next: (fermeture) => {
        this.saving.set(false);
        this.dialogRef.close({ success: true, fermeture });
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        const apiErr = err.error as ApiError | undefined;
        const msg = apiErr?.message ?? 'Échec de la création de la fermeture';
        this.snack.open(msg, 'OK', { duration: 4000 });
      },
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  private toIsoDate(date: Date): string {
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
}
