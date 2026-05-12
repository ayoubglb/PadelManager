import {
  Component,
  computed,
  inject,
  input,
  model,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';

import { SiteService } from '../../../core/api/site.service';
import { Site } from '../../../core/api/site.types';

/**
 * Dropdown de sélection d'un site
 *
 * Usage :
 *   <app-site-selector [(selectedSiteId)]="currentSiteId" />
 *   <app-site-selector
 *     [selectedSiteId]="siteId()"
 *     (selectedSiteIdChange)="onSiteChange($event)"
 *   />
 *
 * Inputs :
 *   - selectedSiteId : id sélectionné (two-way binding via model())
 *   - label : libellé du champ (défaut "Site")
 *   - allowEmpty : autoriser "Tous les sites" (défaut false)
 *   - includeInactive : afficher aussi les sites désactivés (admin uniquement)
 */
@Component({
  selector: 'app-site-selector',
  standalone: true,
  imports: [CommonModule, MatFormFieldModule, MatSelectModule, MatIconModule],
  templateUrl: './site-selector.html',
  styleUrl: './site-selector.css',
})
export class SiteSelector {
  private siteService = inject(SiteService);

  // Site actuellement sélectionné (two-way binding). null = aucun
  readonly selectedSiteId = model<number | null>(null);

  // Libellé affiché dans le mat-form-field
  readonly label = input<string>('Site');

  // Si true, ajoute une option "Tous les sites" en tête
  readonly allowEmpty = input<boolean>(false);

  // Si true, inclut les sites désactivés (vue admin)
  readonly includeInactive = input<boolean>(false);

  // Sites chargés depuis le backend
  readonly sites = toSignal(
    this.siteService.getActiveSites(this.includeInactive()).pipe(
      catchError((err) => {
        console.error('Erreur chargement sites', err);
        return of([] as Site[]);
      })
    ),
    { initialValue: [] as Site[] }
  );

  onChange(value: number | null): void {
    this.selectedSiteId.set(value);
  }
}
