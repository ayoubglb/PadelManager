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

import { TerrainService } from '../../../core/api/terrain.service';
import {
  Terrain,
  TerrainCreateUpdateRequest,
} from '../../../core/api/terrain.types';
import { ApiError } from '../../../core/api/auth.types';

// - Si terrain est fourni → mode édition (PUT /terrains/{id})
// - Sinon → mode création (POST /sites/{siteId}/terrains), siteId requis

export interface TerrainFormDialogData {
  siteId: number;
  siteNom: string;
  terrain?: Terrain;
}

@Component({
  selector: 'app-terrain-form-dialog',
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
  templateUrl: './terrain-form-dialog.html',
  styleUrl: './terrain-form-dialog.css',
})
export class TerrainFormDialog {
  private fb = inject(FormBuilder);
  private terrainService = inject(TerrainService);
  private snack = inject(MatSnackBar);
  private dialogRef = inject(MatDialogRef<TerrainFormDialog>);

  readonly data = inject<TerrainFormDialogData>(MAT_DIALOG_DATA);
  readonly isEdit = !!this.data.terrain;
  readonly saving = signal(false);

  readonly form = this.fb.nonNullable.group({
    numero: [
      this.data.terrain?.numero ?? 1,
      [Validators.required, Validators.min(1), Validators.max(999)],
    ],
    nom: [this.data.terrain?.nom ?? ''],
  });

  save(): void {
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true);

    const raw = this.form.getRawValue();
    const body: TerrainCreateUpdateRequest = {
      numero: raw.numero,
      nom: raw.nom.trim() || null,
    };

    const action$ = this.isEdit
      ? this.terrainService.update(this.data.terrain!.id, body)
      : this.terrainService.create(this.data.siteId, body);

    action$.subscribe({
      next: (terrain) => {
        this.saving.set(false);
        this.dialogRef.close({ success: true, terrain });
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
