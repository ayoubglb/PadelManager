import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  RechargeRequest,
  SoldeResponse,
  Transaction,
  TransactionFilters,
} from './transaction.types';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private http = inject(HttpClient);

  private _solde = signal<number | null>(null);
  readonly solde = this._solde.asReadonly();

  refreshSolde(): Observable<number> {
    return this.http
      .get<SoldeResponse>(`${API_BASE_URL}/transactions/solde`)
      .pipe(
        tap((response) => this._solde.set(response.solde)),
        map((response) => response.solde)
      );
  }

  recharger(req: RechargeRequest): Observable<Transaction> {
    return this.http
      .post<Transaction>(`${API_BASE_URL}/transactions/recharge`, req)
      .pipe(
        tap(() => {
          this.refreshSolde().subscribe();
        })
      );
  }

  list(filters: TransactionFilters = {}): Observable<Transaction[]> {
    let params = new HttpParams();
    if (filters.type) params = params.set('type', filters.type);
    if (filters.dateDebut) params = params.set('dateDebut', filters.dateDebut);
    if (filters.dateFin) params = params.set('dateFin', filters.dateFin);

    return this.http.get<Transaction[]>(`${API_BASE_URL}/transactions`, {
      params,
    });
  }

  clear(): void {
    this._solde.set(null);
  }
}
