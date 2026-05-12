import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';

import { SiteService } from '../../core/api/site.service';
import { Site } from '../../core/api/site.types';

@Component({
  selector: 'app-sites',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatChipsModule,
  ],
  templateUrl: './sites.html',
  styleUrl: './sites.css',
})
export class Sites {
  private siteService = inject(SiteService);

  // Liste publique des sites actifs

  readonly sites = toSignal<Site[] | null>(
    this.siteService.getActiveSites().pipe(
      catchError((err) => {
        console.error('Erreur chargement sites', err);
        return of([] as Site[]);
      })
    ),
    { initialValue: null }
  );
}
