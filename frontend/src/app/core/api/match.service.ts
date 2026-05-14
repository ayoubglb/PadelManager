import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  AnnulationMatchResponse,
  InviterJoueurRequest,
  Match,
  MatchCreateRequest,
  MatchDetail,
  MatchPublicCatalogue,
  MatchsPublicsFiltres,
  MesMatch,
} from './match.types';
import { TransactionService } from './transaction.service';

@Injectable({ providedIn: 'root' })
export class MatchService {
  private http = inject(HttpClient);
  private transactionService = inject(TransactionService);

  create(req: MatchCreateRequest): Observable<Match> {
    return this.http.post<Match>(`${API_BASE_URL}/matchs`, req).pipe(
      tap(() => this.transactionService.refreshSolde().subscribe())
    );
  }

  getMesMatchs(aVenir: boolean): Observable<MesMatch[]> {
    const params = new HttpParams().set('aVenir', aVenir);
    return this.http.get<MesMatch[]>(`${API_BASE_URL}/matchs/mes-matchs`, {
      params,
    });
  }
  // Catalogue des matchs publics ouverts. Filtres optionnels : siteId, dateDebut, dateFin, placesMin
  getMatchsPublics(filtres: MatchsPublicsFiltres = {}): Observable<MatchPublicCatalogue[]> {
    let params = new HttpParams();
    if (filtres.siteId != null) params = params.set('siteId', filtres.siteId);
    if (filtres.dateDebut) params = params.set('dateDebut', filtres.dateDebut);
    if (filtres.dateFin) params = params.set('dateFin', filtres.dateFin);
    if (filtres.placesMin != null) params = params.set('placesMin', filtres.placesMin);

    return this.http.get<MatchPublicCatalogue[]>(`${API_BASE_URL}/matchs/publics`, {
      params,
    });
  }

  getById(id: number): Observable<MatchDetail> {
    return this.http.get<MatchDetail>(`${API_BASE_URL}/matchs/${id}`);
  }

  // Paie sa part d'un match auquel on a été invité (15€)
  // Le solde est rafraîchi automatiquement après succès
  payer(matchId: number): Observable<void> {
    return this.http
      .post<void>(`${API_BASE_URL}/matchs/${matchId}/payer`, {})
      .pipe(tap(() => this.transactionService.refreshSolde().subscribe()));
  }

  // Rejoint un match public (paie 15€ immédiatement)
  // Premier payé = premier servi (race condition gérée côté backend)

  rejoindre(matchId: number): Observable<void> {
    return this.http
      .post<void>(`${API_BASE_URL}/matchs/${matchId}/rejoindre`, {})
      .pipe(tap(() => this.transactionService.refreshSolde().subscribe()));
  }

  // Invite un joueur dans un match privé via son matricule
  // Réservé à l'organisateur d'un match PRIVE

  inviter(matchId: number, req: InviterJoueurRequest): Observable<void> {
    return this.http.post<void>(
      `${API_BASE_URL}/matchs/${matchId}/joueurs`,
      req
    );
  }

   // Annule un match. Réservé à l'organisateur
   // Tous les joueurs ayant payé sont remboursés (transactions REMBOURSEMENT créées)
   // Le solde est rafraîchi automatiquement après succès

  annuler(matchId: number): Observable<AnnulationMatchResponse> {
    return this.http
      .post<AnnulationMatchResponse>(
        `${API_BASE_URL}/matchs/${matchId}/annuler`,
        {}
      )
      .pipe(tap(() => this.transactionService.refreshSolde().subscribe()));
  }
}
