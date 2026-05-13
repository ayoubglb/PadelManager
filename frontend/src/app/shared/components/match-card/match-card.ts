import { Component, computed, input, output } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';

import { MesMatch } from '../../../core/api/match.types';

@Component({
  selector: 'app-match-card',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatTooltipModule,
  ],
  templateUrl: './match-card.html',
  styleUrl: './match-card.css',
})
export class MatchCard {
  // Le match à afficher
  readonly match = input.required<MesMatch>();

  // Affiche le bouton "Voir le détail" si true (par défaut)
  readonly showDetailButton = input<boolean>(true);

  // Émis quand l'utilisateur clique sur "Voir le détail"
  readonly detailClick = output<number>();

  // Date complète sous forme d'objet Date pour le pipe date
  readonly dateObj = computed(() => new Date(this.match().dateHeureDebut));
  readonly endDateObj = computed(() => new Date(this.match().dateHeureFin));

  // Couleur du chip de type de match.
  readonly typeChipClass = computed(() => {
    const type = this.match().type;
    return type === 'PRIVE'
      ? '!bg-red-50 !text-red-700'
      : '!bg-orange-50 !text-orange-700';
  });

  // Couleur du chip du rôle utilisateur
  readonly roleChipClass = computed(() => {
    const role = this.match().monRole;
    return role === 'ORGANISATEUR'
      ? '!bg-blue-50 !text-blue-700'
      : '!bg-gray-100 !text-gray-700';
  });

  // Label du rôle dans le match
  readonly roleLabel = computed(() =>
    this.match().monRole === 'ORGANISATEUR' ? 'Organisateur' : 'Invité'
  );

  // Statut de paiement de l'utilisateur courant
  readonly paymentStatusLabel = computed(() =>
    this.match().maPartPayee ? 'Ma part payée' : 'À payer'
  );

  readonly paymentStatusClass = computed(() =>
    this.match().maPartPayee
      ? 'text-green-700'
      : 'text-orange-700 font-semibold'
  );

  readonly paymentStatusIcon = computed(() =>
    this.match().maPartPayee ? 'check_circle' : 'pending'
  );

  formatTime(date: Date): string {
    return date.toLocaleTimeString('fr-BE', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  onDetailClick(): void {
    this.detailClick.emit(this.match().id);
  }
}
