import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, catchError, of } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Penalite } from './penalite.types';

@Injectable({ providedIn: 'root' })
export class PenaliteService {
  private http = inject(HttpClient);

  // Retourne la pénalité active de l'utilisateur connecté, ou null s'il n'en a pas.
  // Le backend renvoie 200 + body vide quand aucune
  // pénalité n'est active : HttpClient interprète ça comme null,donc pas de logique spéciale à gérer
  getActive(): Observable<Penalite | null> {
    return this.http
      .get<Penalite | null>(`${API_BASE_URL}/utilisateurs/me/penalites/active`)
      .pipe(
        // Sécurité : si la réponse arrive avec un objet vide {} (ce qui ne devrait
        // pas arriver d'après les tests, mais ceinture-bretelle), on retourne null.
        map((p) => (p && (p as Penalite).id !== undefined ? p : null)),
        catchError((err) => {
          console.error('Erreur chargement pénalité active', err);
          return of(null);
        })
      );
  }

  // Historique complet des pénalités de l'utilisateur connecté

  getAll(): Observable<Penalite[]> {
    return this.http
      .get<Penalite[]>(`${API_BASE_URL}/utilisateurs/me/penalites`)
      .pipe(
        catchError((err) => {
          console.error('Erreur chargement pénalités', err);
          return of([] as Penalite[]);
        })
      );
  }
}
