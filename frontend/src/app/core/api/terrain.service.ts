import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Terrain, TerrainCreateUpdateRequest } from './terrain.types';

@Injectable({ providedIn: 'root' })
export class TerrainService {
  private http = inject(HttpClient);

  // Liste des terrains actifs d'un site (endpoint public)
  getActiveBySite(siteId: number): Observable<Terrain[]> {
    return this.http.get<Terrain[]>(`${API_BASE_URL}/sites/${siteId}/terrains`);
  }

  // Liste de tous les terrains d'un site (actifs + désactivés)
  // Réservé ADMIN_GLOBAL ou ADMIN_SITE de ce site
  getAllBySiteAdmin(siteId: number): Observable<Terrain[]> {
    return this.http.get<Terrain[]>(
      `${API_BASE_URL}/sites/${siteId}/terrains/admin`
    );
  }

  // Détails d'un terrain (endpoint public)
  getById(id: number): Observable<Terrain> {
    return this.http.get<Terrain>(`${API_BASE_URL}/terrains/${id}`);
  }

  // Crée un nouveau terrain dans un site. Réservé admin du site ou global.
  create(siteId: number, req: TerrainCreateUpdateRequest): Observable<Terrain> {
    return this.http.post<Terrain>(
      `${API_BASE_URL}/sites/${siteId}/terrains`,
      req
    );
  }

  // Met à jour un terrain. Réservé admin.
  update(id: number, req: TerrainCreateUpdateRequest): Observable<Terrain> {
    return this.http.put<Terrain>(`${API_BASE_URL}/terrains/${id}`, req);
  }

  // Désactive un terrain (soft delete pour maintenance). Réservé admin.
  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/terrains/${id}`);
  }

  // Réactive un terrain désactivé. Réservé admin.
  activate(id: number): Observable<Terrain> {
    return this.http.put<Terrain>(`${API_BASE_URL}/terrains/${id}/activer`, {});
  }
}
