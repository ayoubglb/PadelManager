import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  JourFermeture,
  JourFermetureCreateRequest,
} from './jour-fermeture.types';

@Injectable({ providedIn: 'root' })
export class JourFermetureService {
  private http = inject(HttpClient);

  // Liste de toutes les fermetures globales (siteId null), tous sites confondus
  // Endpoint public
  getGlobales(): Observable<JourFermeture[]> {
    return this.http.get<JourFermeture[]>(
      `${API_BASE_URL}/jours-fermeture/globaux`
    );
  }

  // Liste des fermetures spécifiques à un site (hors globales).
  // Endpoint public
  getBySite(siteId: number): Observable<JourFermeture[]> {
    return this.http.get<JourFermeture[]>(
      `${API_BASE_URL}/sites/${siteId}/jours-fermeture`
    );
  }

  //  Crée une fermeture
  // - siteId null → globale (ADMIN_GLOBAL uniquement, contrôlé côté backend)
  // - siteId non null → spécifique au site (ADMIN_GLOBAL ou ADMIN_SITE)
  create(req: JourFermetureCreateRequest): Observable<JourFermeture> {
    return this.http.post<JourFermeture>(
      `${API_BASE_URL}/jours-fermeture`,
      req
    );
  }

  // Supprime une fermeture. Réservé admin.
  // Le backend doit vérifier que la fermeture est dans le futur

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/jours-fermeture/${id}`);
  }
}
