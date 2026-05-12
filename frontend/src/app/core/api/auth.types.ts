export type Role =
  | 'MEMBRE_GLOBAL'
  | 'MEMBRE_SITE'
  | 'MEMBRE_LIBRE'
  | 'ADMIN_SITE'
  | 'ADMIN_GLOBAL';

export interface LoginRequest {
  login: string;       // email ou matricule
  motDePasse: string;
}

export interface RegisterRequest {
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  motDePasse: string;
  role: 'MEMBRE_LIBRE' | 'MEMBRE_SITE' | 'MEMBRE_GLOBAL';
  siteRattachementId?: number; // requis uniquement si role -> MEMBRE_SITE
}

// Réponse du backend après login/register

export interface AuthResponse {
  token: string;
  matricule: string;
  email: string;
  nom: string;
  prenom: string;
  role: Role;
  expirationMinutes: number;
}

export interface ApiError {
  code:
    | 'BUSINESS_RULE_VIOLATED'
    | 'INVALID_CREDENTIALS'
    | 'ACCESS_DENIED'
    | 'NOT_FOUND'
    | 'INTERNAL_ERROR';
  message: string;
  details?: string[];   // détails optionnels (validation, etc.)
  timestamp?: string;   // ISO 8601, ajouté par le backend
}
