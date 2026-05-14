import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { UtilisateurRechercheResultat } from './utilisateur-recherche.types';

@Injectable({ providedIn: 'root' })
export class UtilisateurService {
  private http = inject(HttpClient);

  // Recherche d'utilisateurs invitables à un match
  // - Min 3 caractères dans `q` (sinon retourne [] sans appel HTTP)
  // - Backend cherche LIKE insensible à la casse dans matricule, nom, prénom
  // - Exclut les admins et les comptes inactifs
  // - Limite max 10 résultats

  recherche(q: string, limit = 10): Observable<UtilisateurRechercheResultat[]> {
    if (!q || q.length < 3) {
      return of([]);
    }
    const params = new HttpParams().set('q', q).set('limit', limit);
    return this.http.get<UtilisateurRechercheResultat[]>(
      `${API_BASE_URL}/utilisateurs/recherche`,
      { params }
    );
  }
}
