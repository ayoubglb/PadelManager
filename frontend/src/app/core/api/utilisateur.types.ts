import { Role } from './auth.types';

// Profil complet retourné par GET /utilisateurs/me
// appeler après login pour récupérer les infos manquantes
// du AuthResponse (notamment siteRattachementId).

export interface UtilisateurProfil {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  role: Role;
  siteRattachementId: number | null;
  siteRattachementNom: string | null;
  active: boolean;
  dateInscription: string; // ISO
}
