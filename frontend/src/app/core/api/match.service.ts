import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  Match,
  MatchCreateRequest,
  MatchDetail,
  MesMatch,
} from './match.types';
import { TransactionService } from './transaction.service';

@Injectable({ providedIn: 'root' })
export class MatchService {
  private http = inject(HttpClient);
  private transactionService = inject(TransactionService);

  // Crée un nouveau match (privé ou public)
  // L'organisateur est facturé 15€ immédiatement (transaction PAIEMENT_MATCH)
  // Le solde est rafraîchi automatiquement après succès
  create(req: MatchCreateRequest): Observable<Match> {
    return this.http.post<Match>(`${API_BASE_URL}/matchs`, req).pipe(
      tap(() => {
        this.transactionService.refreshSolde().subscribe();
      })
    );
  }

  // Récupère les matchs de l'utilisateur connecté
  // @param aVenir true = matchs à venir, false = historique
  getMesMatchs(aVenir: boolean): Observable<MesMatch[]> {
    const params = new HttpParams().set('aVenir', aVenir);
    return this.http.get<MesMatch[]>(`${API_BASE_URL}/matchs/mes-matchs`, {
      params,
    });
  }

  // Détails complets d'un match avec sa liste d'inscriptions
  // - Match PUBLIC : tout authentifié
  //  - Match PRIVE : organisateur + joueurs inscrits + admins

  getById(id: number): Observable<MatchDetail> {
    return this.http.get<MatchDetail>(`${API_BASE_URL}/matchs/${id}`);
  }

  // Les méthodes payer, rejoindre, annuler, inviter, publics seront ajoutées
}
