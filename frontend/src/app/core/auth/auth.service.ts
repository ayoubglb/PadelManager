import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, switchMap, of } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  Role,
} from '../api/auth.types';
import { UtilisateurProfil } from '../api/utilisateur.types';

const TOKEN_KEY = 'padel_token';
const AUTH_KEY = 'padel_auth';
const PROFIL_KEY = 'padel_profil';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  // État réactif via Signals
  private _auth = signal<AuthResponse | null>(this.loadFromStorage(AUTH_KEY));
  private _profil = signal<UtilisateurProfil | null>(
    this.loadFromStorage(PROFIL_KEY)
  );

  // Lectures publiques (read-only signals)
  readonly auth = this._auth.asReadonly();
  readonly profil = this._profil.asReadonly();
  readonly isAuthenticated = computed(() => this._auth() !== null);
  readonly currentRole = computed<Role | null>(() => this._auth()?.role ?? null);
  readonly isAdmin = computed(() => {
    const r = this.currentRole();
    return r === 'ADMIN_GLOBAL' || r === 'ADMIN_SITE';
  });
  readonly isAdminGlobal = computed(() => this.currentRole() === 'ADMIN_GLOBAL');
  readonly isAdminSite = computed(() => this.currentRole() === 'ADMIN_SITE');
  readonly displayName = computed(() => {
    const a = this._auth();
    return a ? `${a.prenom} ${a.nom}` : '';
  });

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  // Login : récupère AuthResponse, stocke le token,
  // puis charge le profil complet via GET /utilisateurs/me.

  login(req: LoginRequest): Observable<UtilisateurProfil> {
    return this.http
      .post<AuthResponse>(`${API_BASE_URL}/auth/login`, req)
      .pipe(
        tap((res) => this.persistAuth(res)),
        switchMap(() => this.loadProfil())
      );
  }

  register(req: RegisterRequest): Observable<UtilisateurProfil> {
    return this.http
      .post<AuthResponse>(`${API_BASE_URL}/auth/register`, req)
      .pipe(
        tap((res) => this.persistAuth(res)),
        switchMap(() => this.loadProfil())
      );
  }

  loadProfil(): Observable<UtilisateurProfil> {
    return this.http
      .get<UtilisateurProfil>(`${API_BASE_URL}/utilisateurs/me`)
      .pipe(
        tap((profil) => {
          this._profil.set(profil);
          localStorage.setItem(PROFIL_KEY, JSON.stringify(profil));
        })
      );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(AUTH_KEY);
    localStorage.removeItem(PROFIL_KEY);
    this._auth.set(null);
    this._profil.set(null);
    this.router.navigate(['/login']);
  }

  private persistAuth(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(AUTH_KEY, JSON.stringify(res));
    this._auth.set(res);
  }

  private loadFromStorage<T>(key: string): T | null {
    try {
      const raw = localStorage.getItem(key);
      return raw ? (JSON.parse(raw) as T) : null;
    } catch {
      return null;
    }
  }
}
