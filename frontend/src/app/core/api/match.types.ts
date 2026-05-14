import { Transaction } from './transaction.types';
// Type d'un match : privé (organisateur + invités) ou public (n'importe qui peut rejoindre).

import {TypeTransaction} from './transaction.types';

export type TypeMatch = 'PRIVE' | 'PUBLIC';

// Statut décisionnel d'un match.
// Note : "terminé" n'est PAS un statut stocké, c'est dérivé du booléen `termine`
// dans le DTO (basé sur dateHeureFin < now côté backend).

export type StatutMatch = 'PROGRAMME' | 'ANNULE';

// DTO complet d'un match

export interface Match {
  id: number;
  terrainId: number;
  terrainNumero: number;
  siteId: number;
  siteNom: string;
  dateHeureDebut: string;   // ISO LocalDateTime "YYYY-MM-DDTHH:MM:SS"
  dateHeureFin: string;     // ISO LocalDateTime
  organisateurId: number;
  organisateurNom: string;
  type: TypeMatch;
  statut: StatutMatch;
  devenuPublicAutomatiquement: boolean;
  dateCreation: string;     // ISO LocalDateTime
  termine: boolean;         // dérivé côté backend (dateHeureFin < now)
}

// Body de POST /matchs.
// dateHeureDebut doit être au format "YYYY-MM-DDTHH:MM:SS" (LocalDateTime sans timezone).

export interface MatchCreateRequest {
  terrainId: number;
  dateHeureDebut: string;
  type: TypeMatch;
}


// Statut d'une inscription joueur dans un match

export type StatutInscription = 'INSCRIT' | 'ANNULE' | 'LIBERE_NON_PAIEMENT';

// Rôle de l'utilisateur courant dans un match.

export type MonRoleDansMatch = 'ORGANISATEUR' | 'JOUEUR';

// Inscription d'un joueur à un match
export interface InscriptionMatch {
  id: number;
  matchId: number;
  joueurId: number;
  joueurMatricule: string;
  joueurNom: string;
  dateInscription: string; // ISO LocalDateTime
  paye: boolean;
  statut: StatutInscription;
  estOrganisateur: boolean;
}

// DTO synthétique retourné par GET /matchs/mes-matchs. Allégé pour les vues catalogue (pas de liste d'inscriptions)
export interface MesMatch {
  id: number;
  siteNom: string;
  terrainNumero: number;
  dateHeureDebut: string;
  dateHeureFin: string;
  type: TypeMatch;
  statut: StatutMatch;
  organisateurNom: string;
  monRole: MonRoleDansMatch;
  maPartPayee: boolean;
  nombreInscrits: number;
}

// DTO complet retourné par GET /matchs/{id} avec la liste des inscriptions. Utilisé pour la page détail.

export interface MatchDetail extends Match {
  organisateurMatricule: string;
  inscriptions: InscriptionMatch[];
  nombreJoueursPayes: number;
  placesDisponibles: number;
}

// Body de POST /matchs/{id}/joueurs (inviter un joueur).

export interface InviterJoueurRequest {
  matricule: string;
}

// Réponse de POST /matchs/{id}/annuler.

export interface AnnulationMatchResponse {
  matchId: number;
  nombreRemboursements: number;
  remboursements: Transaction[];
}

// DTO retourné par GET /matchs/publics : catalogue des matchs publics ouverts.
// Encore plus léger que MesMatch — on n'est pas inscrit donc pas de monRole/maPartPayee,
// forcément PUBLIC + PROGRAMME donc pas de type/statut.
export interface MatchPublicCatalogue {
  id: number;
  siteId: number;
  siteNom: string;
  terrainNumero: number;
  dateHeureDebut: string;
  dateHeureFin: string;
  organisateurNom: string;
  placesRestantes: number;
}

// Filtres optionnels pour GET /matchs/publics
export interface MatchsPublicsFiltres {
  siteId?: number;
  dateDebut?: string; // ISO YYYY-MM-DD
  dateFin?: string; // ISO YYYY-MM-DD
  placesMin?: number;
}


