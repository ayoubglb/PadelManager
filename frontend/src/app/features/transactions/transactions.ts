import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { startWith, switchMap, catchError, of, debounceTime } from 'rxjs';

import { TransactionService } from '../../core/api/transaction.service';
import {
  Transaction,
  TypeTransaction,
  TYPE_LABELS,
  isCredit,
} from '../../core/api/transaction.types';
import { SoldeBadge } from '../../shared/components/solde-badge/solde-badge';
import { RechargeDialog } from './recharge-dialog/recharge-dialog';

interface FilterForm {
  type: TypeTransaction | null;
  dateDebut: Date | null;
  dateFin: Date | null;
}

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    SoldeBadge,
    RouterLink
  ],
  templateUrl: './transactions.html',
  styleUrl: './transactions.css',
})
export class Transactions {
  private fb = inject(FormBuilder);
  private transactionService = inject(TransactionService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  readonly TYPE_LABELS = TYPE_LABELS;
  readonly typesTransaction: TypeTransaction[] = [
    'RECHARGE',
    'PAIEMENT_MATCH',
    'SOLDE_DU_ORGANISATEUR',
    'REMBOURSEMENT',
    'REMBOURSEMENT_SOLDE_DU_ORGANISATEUR',
  ];

  readonly displayedColumns = ['date', 'type', 'montant', 'matchId'];

  readonly filterForm = this.fb.nonNullable.group<FilterForm>({
    type: null,
    dateDebut: null,
    dateFin: null,
  });

// Liste des transactions, automatiquement re-chargée
// à chaque changement des filtres (avec debounce)
  readonly transactions = toSignal(
    this.filterForm.valueChanges.pipe(
      startWith(this.filterForm.value),
      debounceTime(300),
      switchMap((filters) =>
        this.transactionService
          .list({
            type: filters.type ?? undefined,
            dateDebut: this.toIsoDate(filters.dateDebut),
            dateFin: this.toIsoDate(filters.dateFin),
          })
          .pipe(
            catchError((err) => {
              console.error('Erreur chargement transactions', err);
              this.snack.open(
                'Impossible de charger les transactions',
                'OK',
                { duration: 4000 }
              );
              return of([] as Transaction[]);
            })
          )
      )
    ),
    { initialValue: [] as Transaction[] }
  );

  readonly loading = signal(false);

// Affichage signé du montant : -15,00 € pour un débit, +15,00 € pour un crédit.
// Utilisé directement dans le template avec un pipe currency
  signedAmount(t: Transaction): number {
    return isCredit(t.type) ? t.montant : -t.montant;
  }

  isCredit(t: Transaction): boolean {
    return isCredit(t.type);
  }

  getTypeLabel(t: Transaction): string {
    return TYPE_LABELS[t.type];
  }

  resetFilters(): void {
    this.filterForm.reset({ type: null, dateDebut: null, dateFin: null });
  }

  openRechargeDialog(): void {
    const ref = this.dialog.open(RechargeDialog, {
      width: '400px',
      disableClose: false,
    });

    ref.afterClosed().subscribe((result) => {
      if (result === 'success') {
        this.snack.open('Compte rechargé avec succès', 'OK', { duration: 3000 });
        // Recharger la liste pour voir la nouvelle transaction
        this.filterForm.updateValueAndValidity({ emitEvent: true });
      }
    });
  }

  // Convertit une Date en string ISO YYYY-MM-DD pour le backend.
  private toIsoDate(date: Date | null | undefined): string | undefined {
    if (!date) return undefined;
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
}
