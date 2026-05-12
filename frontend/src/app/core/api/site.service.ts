import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Site, SiteCreateUpdateRequest } from './site.types';

@Injectable({ providedIn: 'root' })
export class SiteService {
  private http = inject(HttpClient);

   // Liste des sites actifs (endpoint public)
   // Si inclureInactifs = true, retourne aussi les désactivés.
   // Pour la vue admin complète -> getAllSitesAdmin()

  getActiveSites(inclureInactifs = false): Observable<Site[]> {
    const params = new HttpParams().set('inclureInactifs', inclureInactifs);
    return this.http.get<Site[]>(`${API_BASE_URL}/sites`, { params });
  }

  // Liste tous les sites (actifs + désactivés). Réservé ADMIN_GLOBAL

  getAllSitesAdmin(): Observable<Site[]> {
    return this.http.get<Site[]>(`${API_BASE_URL}/sites/admin`);
  }

  // Détails d'un site (endpoint public).

  getById(id: number): Observable<Site> {
    return this.http.get<Site>(`${API_BASE_URL}/sites/${id}`);
  }

  // Création d'un nouveau site. Réservé ADMIN_GLOBAL.

  create(req: SiteCreateUpdateRequest): Observable<Site> {
    return this.http.post<Site>(`${API_BASE_URL}/sites`, req);
  }

  // Mise à jour d'un site existant. Réservé ADMIN_GLOBAL.

  update(id: number, req: SiteCreateUpdateRequest): Observable<Site> {
    return this.http.put<Site>(`${API_BASE_URL}/sites/${id}`, req);
  }

  // Désactivation (soft delete) d'un site. Préserve l'historique. Réservé ADMIN_GLOBAL.

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/sites/${id}`);
  }

  // Réactivation d'un site désactivé. Réservé ADMIN_GLOBAL.

  activate(id: number): Observable<Site> {
    return this.http.put<Site>(`${API_BASE_URL}/sites/${id}/activer`, {});
  }
}
