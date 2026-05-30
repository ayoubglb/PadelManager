import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';

import { TransactionService } from './transaction.service';
import {
  RechargeRequest,
  SoldeResponse,
  Transaction,
} from './transaction.types';

describe('TransactionService', () => {
  let service: TransactionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        TransactionService,
      ],
    });

    service = TestBed.inject(TransactionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('refreshSolde()', () => {
    it('appelle GET /transactions/solde et met à jour le Signal', () => {
      const mockResponse: SoldeResponse = { solde: 42.5 } as SoldeResponse;
      let resultSolde: number | undefined;

      service.refreshSolde().subscribe((s) => (resultSolde = s));

      const req = httpMock.expectOne((r) => r.url.endsWith('/transactions/solde'));
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);

      expect(resultSolde).toBe(42.5);
      expect(service.solde()).toBe(42.5);
    });
  });

  describe('recharger()', () => {
    it('appelle POST /transactions/recharge et déclenche un refresh du solde', () => {
      const rechargeRequest: RechargeRequest = { montant: 50 } as RechargeRequest;
      const mockTransaction: Transaction = {
        id: 1,
        utilisateurId: 42,
        type: 'RECHARGE',
        montant: 50,
        date: '2026-05-30T20:00:00',
        matchId: null,
      } as Transaction;

      service.recharger(rechargeRequest).subscribe();

      // Première requête : la recharge
      const reqRecharge = httpMock.expectOne((r) =>
        r.url.endsWith('/transactions/recharge')
      );
      expect(reqRecharge.request.method).toBe('POST');
      expect(reqRecharge.request.body).toEqual(rechargeRequest);
      reqRecharge.flush(mockTransaction);

      // Deuxième requête : le refresh automatique du solde
      const reqSolde = httpMock.expectOne((r) =>
        r.url.endsWith('/transactions/solde')
      );
      expect(reqSolde.request.method).toBe('GET');
      reqSolde.flush({ solde: 92.5 });

      expect(service.solde()).toBe(92.5);
    });
  });

  describe('list()', () => {
    it('appelle GET /transactions sans paramètre quand aucun filtre', () => {
      service.list().subscribe();

      const req = httpMock.expectOne((r) => r.url.endsWith('/transactions'));
      expect(req.request.method).toBe('GET');
      expect(req.request.params.keys().length).toBe(0);
      req.flush([]);
    });

    it('ajoute les query params type/dateDebut/dateFin quand fournis', () => {
      service
        .list({
          type: 'RECHARGE',
          dateDebut: '2026-01-01',
          dateFin: '2026-12-31',
        })
        .subscribe();

      const req = httpMock.expectOne((r) => r.url.endsWith('/transactions'));
      expect(req.request.params.get('type')).toBe('RECHARGE');
      expect(req.request.params.get('dateDebut')).toBe('2026-01-01');
      expect(req.request.params.get('dateFin')).toBe('2026-12-31');
      req.flush([]);
    });
  });

  describe('clear()', () => {
    it('remet le Signal solde à null', () => {
      // Setup : on simule un solde existant
      service.refreshSolde().subscribe();
      httpMock
        .expectOne((r) => r.url.endsWith('/transactions/solde'))
        .flush({ solde: 100 });
      expect(service.solde()).toBe(100);

      // Act
      service.clear();

      // Assert
      expect(service.solde()).toBeNull();
    });
  });
});
