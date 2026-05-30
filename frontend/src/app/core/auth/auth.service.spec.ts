import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService } from './auth.service';
import { TransactionService } from '../api/transaction.service';
import { AuthResponse, LoginRequest } from '../api/auth.types';
import { UtilisateurProfil } from '../api/utilisateur.types';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let transactionServiceMock: { refreshSolde: ReturnType<typeof vi.fn>; clear: ReturnType<typeof vi.fn> };

  const mockAuthResponse: AuthResponse = {
    token: 'fake-jwt-token',
    matricule: 'L100001',
    email: 'jean@example.com',
    nom: 'Dupont',
    prenom: 'Jean',
    role: 'MEMBRE_LIBRE',
    expirationMinutes: 45,
  };

  const mockProfil: UtilisateurProfil = {
    id: 1,
    matricule: 'L100001',
    nom: 'Dupont',
    prenom: 'Jean',
    email: 'jean@example.com',
    telephone: '0470123456',
    role: 'MEMBRE_LIBRE',
    siteRattachementId: null,
    siteRattachementNom: null,
    active: true,
    dateInscription: '2026-01-01T00:00:00',
  } as UtilisateurProfil;

  beforeEach(() => {
    // localStorage propre avant chaque test
    localStorage.clear();

    // Mock du TransactionService : on retourne juste un Observable vide
    transactionServiceMock = {
      refreshSolde: vi.fn().mockReturnValue(of(null)),
      clear: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', children: [] }]),
        AuthService,
        { provide: TransactionService, useValue: transactionServiceMock },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('login()', () => {
    it('appelle POST /auth/login, stocke le token et charge le profil', () => {
      const loginRequest: LoginRequest = {
        login: 'jean@example.com',
        motDePasse: 'monMotDePasse123',
      };

      let resultProfil: UtilisateurProfil | undefined;
      service.login(loginRequest).subscribe((p) => (resultProfil = p));

      // Première requête : login
      const reqLogin = httpMock.expectOne((r) => r.url.endsWith('/auth/login'));
      expect(reqLogin.request.method).toBe('POST');
      expect(reqLogin.request.body).toEqual(loginRequest);
      reqLogin.flush(mockAuthResponse);

      // Le token doit être stocké
      expect(service.getToken()).toBe('fake-jwt-token');
      expect(service.isAuthenticated()).toBe(true);
      expect(service.auth()?.role).toBe('MEMBRE_LIBRE');

      // Deuxième requête : chargement du profil
      const reqProfil = httpMock.expectOne((r) =>
        r.url.endsWith('/utilisateurs/me')
      );
      expect(reqProfil.request.method).toBe('GET');
      reqProfil.flush(mockProfil);

      expect(resultProfil).toEqual(mockProfil);
      expect(service.profil()).toEqual(mockProfil);
    });
  });

  describe('logout()', () => {
    it('efface le token, le profil et appelle TransactionService.clear()', () => {
      // Setup : on simule un utilisateur connecté
      localStorage.setItem('padel_token', 'some-token');
      localStorage.setItem('padel_auth', JSON.stringify(mockAuthResponse));
      localStorage.setItem('padel_profil', JSON.stringify(mockProfil));

      service.logout();

      expect(localStorage.getItem('padel_token')).toBeNull();
      expect(localStorage.getItem('padel_auth')).toBeNull();
      expect(localStorage.getItem('padel_profil')).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
      expect(service.profil()).toBeNull();
      expect(transactionServiceMock.clear).toHaveBeenCalled();
    });
  });

  describe('isAuthenticated()', () => {
    it('retourne false quand aucun utilisateur n\'est connecté', () => {
      expect(service.isAuthenticated()).toBe(false);
    });
  });

  describe('Signals dérivés', () => {
    it('isAdminGlobal() est true seulement pour ADMIN_GLOBAL', () => {
      const adminResponse = { ...mockAuthResponse, role: 'ADMIN_GLOBAL' as const };
      service.login({ login: 'a@a.be', motDePasse: 'xxxxxxxxxxxx' }).subscribe();

      httpMock.expectOne((r) => r.url.endsWith('/auth/login')).flush(adminResponse);
      httpMock.expectOne((r) => r.url.endsWith('/utilisateurs/me')).flush(mockProfil);

      expect(service.isAdminGlobal()).toBe(true);
      expect(service.isAdminSite()).toBe(false);
      expect(service.isAdmin()).toBe(true);
    });

    it('displayName() concatène prénom et nom', () => {
      service.login({ login: 'a@a.be', motDePasse: 'xxxxxxxxxxxx' }).subscribe();

      httpMock.expectOne((r) => r.url.endsWith('/auth/login')).flush(mockAuthResponse);
      httpMock.expectOne((r) => r.url.endsWith('/utilisateurs/me')).flush(mockProfil);

      expect(service.displayName()).toBe('Jean Dupont');
    });
  });
});
