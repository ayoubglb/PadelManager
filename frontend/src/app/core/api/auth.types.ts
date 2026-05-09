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
  password: string;
  role: 'MEMBRE_LIBRE' | 'MEMBRE_SITE' | 'MEMBRE_GLOBAL';
  siteRattachementId?: number;
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
}
