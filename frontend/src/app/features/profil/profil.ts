import { Component, computed, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { PenaliteService } from '../../core/api/penalite.service';
import { RoleLabelPipe } from '../../shared/pipes/role-label.pipe';
import { SoldeBadge } from '../../shared/components/solde-badge/solde-badge';

@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    RoleLabelPipe,
    SoldeBadge,
    RouterLink
  ],
  templateUrl: './profil.html',
  styleUrl: './profil.css',
})
export class Profil {
  private authService = inject(AuthService);
  private penaliteService = inject(PenaliteService);

  // Profil exposé par AuthService

  readonly profil = this.authService.profil;

  // Pénalité active, chargée à l'init
  // Si null, le bloc pénalité est caché

  readonly penaliteActive = toSignal(this.penaliteService.getActive(), {
    initialValue: null,
  });

  // Statut booléen dérivé pour affichage rapide

  readonly hasPenalite = computed(() => this.penaliteActive() !== null);

  logout(): void {
    this.authService.logout();
  }
}
