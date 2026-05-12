// Type d'un match : privé (organisateur + invités) ou public (n'importe qui peut rejoindre).

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
