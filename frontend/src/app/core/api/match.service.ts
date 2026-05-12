import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Match, MatchCreateRequest } from './match.types';
import { TransactionService } from './transaction.service';

@Injectable({ providedIn: 'root' })
export class MatchService {
  private http = inject(HttpClient);
  private transactionService = inject(TransactionService);

  // Crée un nouveau match (privé ou public).
  // L'organisateur est facturé 15€ immédiatement (transaction PAIEMENT_MATCH).
  // Le solde est rafraîchi automatiquement après succès.

  create(req: MatchCreateRequest): Observable<Match> {
    return this.http.post<Match>(`${API_BASE_URL}/matchs`, req).pipe(
      tap(() => {
        // La création débite 15€ du solde organisateur, on le rafraîchit.
        this.transactionService.refreshSolde().subscribe();
      })
    );
  }

  // Les méthodes payer, rejoindre, annuler, joueurs, mes-matchs, publics sont à rajouter
}
