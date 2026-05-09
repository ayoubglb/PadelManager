import { Component, computed, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TransactionService } from '../../../core/api/transaction.service';

@Component({
  selector: 'app-solde-badge',
  standalone: true,
  imports: [CurrencyPipe, MatIconModule, MatTooltipModule],
  templateUrl: './solde-badge.html',
  styleUrl: './solde-badge.css',
})
export class SoldeBadge {
  private transactionService = inject(TransactionService);

  readonly solde = this.transactionService.solde;

  // Couleur du badge selon le niveau du solde.
  // Retourne une classe Tailwind à appliquer

  readonly couleurClasse = computed<string>(() => {
    const s = this.solde();
    if (s === null) return 'text-gray-400';
    if (s < 0) return 'text-red-600';
    if (s < 30) return 'text-orange-600';
    return 'text-green-700';
  });

  // Tooltip explicatif selon l'état
  readonly tooltip = computed<string>(() => {
    const s = this.solde();
    if (s === null) return 'Solde en cours de chargement';
    if (s < 0) return 'Solde négatif : vous ne pouvez plus réserver tant que vous n\'avez pas rechargé';
    if (s < 15) return 'Solde insuffisant pour un nouveau match (15€ requis)';
    if (s < 30) return 'Solde faible : pensez à recharger';
    return 'Solde suffisant';
  });
}
