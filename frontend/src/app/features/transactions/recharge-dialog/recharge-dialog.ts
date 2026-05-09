import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
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

import { TransactionService } from '../../../core/api/transaction.service';
import { ApiError } from '../../../core/api/auth.types';

@Component({
  selector: 'app-recharge-dialog',
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
  templateUrl: './recharge-dialog.html',
  styleUrl: './recharge-dialog.css',
})
export class RechargeDialog {
  private fb = inject(FormBuilder);
  private transactionService = inject(TransactionService);
  private dialogRef = inject(MatDialogRef<RechargeDialog>);
  private snack = inject(MatSnackBar);

  loading = signal(false);

  // Montants prédéfinis pour faciliter le clic
  readonly montantsPredefinis = [15, 30, 60, 100];

  form = this.fb.nonNullable.group({
    montant: [
      30,
      [Validators.required, Validators.min(1), Validators.max(1000)],
    ],
  });

  setMontant(montant: number): void {
    this.form.controls.montant.setValue(montant);
  }

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);

    this.transactionService.recharger(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        this.dialogRef.close('success');
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        const apiErr = err.error as ApiError | undefined;
        const msg = apiErr?.message ?? 'Échec du rechargement';
        this.snack.open(msg, 'OK', { duration: 4000 });
      },
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
