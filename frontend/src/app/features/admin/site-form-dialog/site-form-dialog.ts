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
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';

import { SiteService } from '../../../core/api/site.service';
import { Site, SiteCreateUpdateRequest } from '../../../core/api/site.types';
import { ApiError } from '../../../core/api/auth.types';

// Données injectées au dialog.
// - Si `site` est fourni → mode édition (PUT)
// - Sinon → mode création (POST)

export interface SiteFormDialogData {
  site?: Site;
}

@Component({
  selector: 'app-site-form-dialog',
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
  templateUrl: './site-form-dialog.html',
  styleUrl: './site-form-dialog.css',
})
export class SiteFormDialog {
  private fb = inject(FormBuilder);
  private siteService = inject(SiteService);
  private snack = inject(MatSnackBar);
  private dialogRef = inject(MatDialogRef<SiteFormDialog>);

  readonly data = inject<SiteFormDialogData>(MAT_DIALOG_DATA);
  readonly isEdit = !!this.data.site;
  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    nom: [this.data.site?.nom ?? '', [Validators.required, Validators.maxLength(100)]],
    adresse: [this.data.site?.adresse ?? '', [Validators.required, Validators.maxLength(255)]],
    codePostal: [this.data.site?.codePostal ?? '', [Validators.required, Validators.maxLength(20)]],
    ville: [this.data.site?.ville ?? '', [Validators.required, Validators.maxLength(100)]],
  });

  save(): void {
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true);

    const body: SiteCreateUpdateRequest = this.form.getRawValue();

    const action$ = this.isEdit
      ? this.siteService.update(this.data.site!.id, body)
      : this.siteService.create(body);

    action$.subscribe({
      next: (site) => {
        this.saving.set(false);
        this.dialogRef.close({ success: true, site });
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        const apiErr = err.error as ApiError | undefined;
        const msg =
          apiErr?.message ??
          (this.isEdit ? 'Échec de la modification' : 'Échec de la création');
        this.snack.open(msg, 'OK', { duration: 4000 });
      },
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
