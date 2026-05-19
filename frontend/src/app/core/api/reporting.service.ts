import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Reporting, ReportingParams } from './reporting.types';

@Injectable({ providedIn: 'root' })
export class ReportingService {
  private http = inject(HttpClient);

  // Reporting global (tous sites confondus). Réservé ADMIN_GLOBAL
  getGlobal(params: ReportingParams): Observable<Reporting> {
    const httpParams = new HttpParams()
      .set('dateDebut', params.dateDebut)
      .set('dateFin', params.dateFin);
    return this.http.get<Reporting>(`${API_BASE_URL}/admin/reporting/global`, {
      params: httpParams,
    });
  }

  // Reporting d'un site spécifique. Réservé ADMIN_GLOBAL ou ADMIN_SITE de ce site
  getBySite(siteId: number, params: ReportingParams): Observable<Reporting> {
    const httpParams = new HttpParams()
      .set('dateDebut', params.dateDebut)
      .set('dateFin', params.dateFin);
    return this.http.get<Reporting>(
      `${API_BASE_URL}/admin/reporting/sites/${siteId}`,
      { params: httpParams }
    );
  }
}
