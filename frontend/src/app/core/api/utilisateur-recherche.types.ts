import { Role } from './auth.types';

// DTO retourné par GET /utilisateurs/recherche. Allégé (pas d'email pour la privacy RGPD)
export interface UtilisateurRechercheResultat {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  role: Role;
}
