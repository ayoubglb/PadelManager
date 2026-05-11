import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Site } from './site.types';

@Injectable({ providedIn: 'root' })
export class SiteService {
  private http = inject(HttpClient);

  // Liste des sites actifs (endpoint public, accessible sans authentification)
  // Utilisé notamment pour le sélecteur de site lors de l'inscription

  getActiveSites(): Observable<Site[]> {
    return this.http.get<Site[]>(`${API_BASE_URL}/sites`);
  }

  // Liste tous les sites (actifs + inactifs).
  // Réservé aux ADMIN_GLOBAL via /sites/admin.

  getAllSites(): Observable<Site[]> {
    return this.http.get<Site[]>(`${API_BASE_URL}/sites/admin`);
  }

  //Détails d'un site (public).

  getById(id: number): Observable<Site> {
    return this.http.get<Site>(`${API_BASE_URL}/sites/${id}`);
  }
}
