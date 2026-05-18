import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { HoraireSite, HoraireCreateRequest } from './horaire.types';

@Injectable({ providedIn: 'root' })
export class HoraireService {
  private http = inject(HttpClient);

  // Liste des horaires d'un site, toutes années confondues (endpoint public)

  getBySite(siteId: number): Observable<HoraireSite[]> {
    return this.http.get<HoraireSite[]>(
      `${API_BASE_URL}/sites/${siteId}/horaires`
    );
  }

  // Horaire d'un site pour une année donnée (endpoint public)

  getBySiteAndAnnee(siteId: number, annee: number): Observable<HoraireSite> {
    return this.http.get<HoraireSite>(
      `${API_BASE_URL}/sites/${siteId}/horaires/${annee}`
    );
  }

  // Crée un horaire pour une année. Réservé admin

  create(siteId: number, req: HoraireCreateRequest): Observable<HoraireSite> {
    return this.http.post<HoraireSite>(
      `${API_BASE_URL}/sites/${siteId}/horaires`,
      req
    );
  }

  // Supprime un horaire. Réservé admin.
  // Une fois supprimé, on peut en recréer un pour la même année (équivalent d'une modification puisqu'il n'y a pas de PUT côté backend)

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/horaires/${id}`);
  }
}
