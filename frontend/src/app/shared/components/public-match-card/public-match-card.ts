import { Component, computed, input, output } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';

import { MatchPublicCatalogue } from '../../../core/api/match.types';

@Component({
  selector: 'app-public-match-card',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
  ],
  templateUrl: './public-match-card.html',
  styleUrl: './public-match-card.css',
})
export class PublicMatchCard {
  // Le match public à afficher
  readonly match = input.required<MatchPublicCatalogue>();

  // Émis quand l'utilisateur clique sur "Voir le détail"
  readonly detailClick = output<number>();

  readonly dateObj = computed(() => new Date(this.match().dateHeureDebut));
  readonly endDateObj = computed(() => new Date(this.match().dateHeureFin));

  // Couleur du chip places restantes : vert si beaucoup, orange si peu
  readonly placesChipClass = computed(() => {
    const n = this.match().placesRestantes;
    if (n >= 2) return '!bg-green-50 !text-green-700';
    return '!bg-orange-50 !text-orange-700';
  });

  readonly placesLabel = computed(() => {
    const n = this.match().placesRestantes;
    return `${n} place${n > 1 ? 's' : ''} libre${n > 1 ? 's' : ''}`;
  });

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
