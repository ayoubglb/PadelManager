import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { PlanningView } from './planning.types';

@Injectable({ providedIn: 'root' })
export class PlanningService {
  private http = inject(HttpClient);

  // Récupère la vue planning d'un site pour une date donnée.
  // Le backend agrège terrains, créneaux et statut de chaque cellule.
  // @param siteId id du site
  // @param date date au format YYYY-MM-DD

  getPlanning(siteId: number, date: string): Observable<PlanningView> {
    const params = new HttpParams().set('date', date);
    return this.http.get<PlanningView>(
      `${API_BASE_URL}/sites/${siteId}/planning`,
      { params }
    );
  }
}
