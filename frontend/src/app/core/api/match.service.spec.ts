import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { of } from 'rxjs';

import { MatchService } from './match.service';
import { TransactionService } from './transaction.service';
import {
  MatchCreateRequest,
  Match,
  MesMatch,
  MatchPublicCatalogue,
  MatchDetail,
  InviterJoueurRequest,
} from './match.types';

describe('MatchService', () => {
  let service: MatchService;
  let httpMock: HttpTestingController;
  let transactionServiceMock: {
    refreshSolde: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    transactionServiceMock = {
      refreshSolde: vi.fn().mockReturnValue(of(0)),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        MatchService,
        { provide: TransactionService, useValue: transactionServiceMock },
      ],
    });

    service = TestBed.inject(MatchService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('create()', () => {
    it('appelle POST /matchs avec le bon payload et rafraîchit le solde', () => {
      const request: MatchCreateRequest = {
        terrainId: 1,
        dateHeureDebut: '2026-12-15T10:00:00',
        type: 'PRIVE',
      } as MatchCreateRequest;

      const mockMatch = { id: 99 } as Match;

      service.create(request).subscribe();

      const req = httpMock.expectOne((r) => r.url.endsWith('/matchs'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockMatch);

      expect(transactionServiceMock.refreshSolde).toHaveBeenCalled();
    });
  });

  describe('getMesMatchs()', () => {
    it('appelle GET /matchs/mes-matchs avec le query param aVenir=true', () => {
      service.getMesMatchs(true).subscribe();

      const req = httpMock.expectOne((r) =>
        r.url.endsWith('/matchs/mes-matchs')
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('aVenir')).toBe('true');
      req.flush([]);
    });

    it('appelle GET /matchs/mes-matchs avec aVenir=false pour l\'historique', () => {
      service.getMesMatchs(false).subscribe();

      const req = httpMock.expectOne((r) =>
        r.url.endsWith('/matchs/mes-matchs')
      );
      expect(req.request.params.get('aVenir')).toBe('false');
      req.flush([]);
    });
  });

  describe('getMatchsPublics()', () => {
    it('appelle GET /matchs/publics sans paramètre quand aucun filtre', () => {
      service.getMatchsPublics().subscribe();

      const req = httpMock.expectOne((r) => r.url.endsWith('/matchs/publics'));
      expect(req.request.method).toBe('GET');
      expect(req.request.params.keys().length).toBe(0);
      req.flush([]);
    });

    it('ajoute les query params siteId, dateDebut, dateFin, placesMin quand fournis', () => {
      service
        .getMatchsPublics({
          siteId: 1,
          dateDebut: '2026-01-01',
          dateFin: '2026-12-31',
          placesMin: 2,
        })
        .subscribe();

      const req = httpMock.expectOne((r) => r.url.endsWith('/matchs/publics'));
      expect(req.request.params.get('siteId')).toBe('1');
      expect(req.request.params.get('dateDebut')).toBe('2026-01-01');
      expect(req.request.params.get('dateFin')).toBe('2026-12-31');
      expect(req.request.params.get('placesMin')).toBe('2');
      req.flush([]);
    });
  });

  describe('rejoindre()', () => {
    it('appelle POST /matchs/:id/rejoindre et rafraîchit le solde', () => {
      service.rejoindre(42).subscribe();

      const req = httpMock.expectOne((r) =>
        r.url.endsWith('/matchs/42/rejoindre')
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(null);

      expect(transactionServiceMock.refreshSolde).toHaveBeenCalled();
    });
  });

  describe('annuler()', () => {
    it('appelle POST /matchs/:id/annuler et rafraîchit le solde', () => {
      service.annuler(42).subscribe();

      const req = httpMock.expectOne((r) =>
        r.url.endsWith('/matchs/42/annuler')
      );
      expect(req.request.method).toBe('POST');
      req.flush({ nombreRemboursements: 3 });

      expect(transactionServiceMock.refreshSolde).toHaveBeenCalled();
    });
  });

  describe('inviter()', () => {
    it('appelle POST /matchs/:id/joueurs avec le matricule du joueur', () => {
      const request: InviterJoueurRequest = {
        matricule: 'L100002',
      } as InviterJoueurRequest;

      service.inviter(42, request).subscribe();

      const req = httpMock.expectOne((r) =>
        r.url.endsWith('/matchs/42/joueurs')
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(null);

      // L'invitation ne change pas le solde de l'organisateur
      // (c'est l'invité qui devra payer plus tard)
    });
  });
});
