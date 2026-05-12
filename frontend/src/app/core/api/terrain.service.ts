import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Terrain } from './terrain.types';

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

  // Les méthodes create/update/delete/activate à ajouter
}
