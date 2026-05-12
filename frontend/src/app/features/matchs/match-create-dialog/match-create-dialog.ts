import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';

import { MatchService } from '../../../core/api/match.service';
import { TypeMatch } from '../../../core/api/match.types';
import { ApiError } from '../../../core/api/auth.types';
import { TransactionService } from '../../../core/api/transaction.service';

// Données injectées au dialog au moment de l'ouverture
export interface MatchCreateDialogData {
  siteNom: string;
  terrainNumero: number;
  terrainId: number;
  date: string;          // "YYYY-MM-DD"
  creneauDebut: string;  // "HH:MM:SS"
  creneauFin: string;    // "HH:MM:SS"
}

// Résultat du dialog en cas de succès.

export interface MatchCreateDialogResult {
  type: TypeMatch;
  matchId: number;
}

const PRIX_PAR_JOUEUR = 15;

@Component({
  selector: 'app-match-create-dialog',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatRadioModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './match-create-dialog.html',
  styleUrl: './match-create-dialog.css',
})
export class MatchCreateDialog {
  private fb = inject(FormBuilder);
  private matchService = inject(MatchService);
  private transactionService = inject(TransactionService);
  private dialogRef = inject(MatDialogRef<MatchCreateDialog>);
  private snack = inject(MatSnackBar);

  readonly data = inject<MatchCreateDialogData>(MAT_DIALOG_DATA);

  loading = signal(false);

  readonly prixOrganisateur = PRIX_PAR_JOUEUR;

  // Solde courant via le Signal partagé
  readonly solde = this.transactionService.solde;

  // Vrai si le solde permet de payer les 15€ de l'organisateur
  readonly soldeOk = computed(() => {
    const s = this.solde();
    return s !== null && s >= PRIX_PAR_JOUEUR;
  });

  // Construit la date au format "YYYY-MM-DD" en objet Date pour le template (utilisation du pipe date avec la locale fr-BE)
  readonly dateFormatted = computed(() => {
    // "2026-05-13" + créneau "09:00:00" -> "2026-05-13T09:00:00"
    return new Date(`${this.data.date}T${this.data.creneauDebut}`);
  });

  form = this.fb.nonNullable.group({
    type: ['PRIVE' as TypeMatch, [Validators.required]],
  });

  // Formate l'heure HH:MM:SS -> HH:MM.

  formatTime(time: string): string {
    return time.slice(0, 5);
  }

  submit(): void {
    if (this.form.invalid || !this.soldeOk()) return;
    this.loading.set(true);

    // Construire dateHeureDebut au format LocalDateTime sans timezone
    const dateHeureDebut = `${this.data.date}T${this.data.creneauDebut}`;

    this.matchService
      .create({
        terrainId: this.data.terrainId,
        dateHeureDebut,
        type: this.form.controls.type.value,
      })
      .subscribe({
        next: (match) => {
          this.loading.set(false);
          const result: MatchCreateDialogResult = {
            type: match.type,
            matchId: match.id,
          };
          this.dialogRef.close(result);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          const apiErr = err.error as ApiError | undefined;
          const msg = apiErr?.message ?? 'Échec de la création du match';
          this.snack.open(msg, 'OK', { duration: 5000 });
        },
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
