// Statuts possibles d'une cellule de la grille planning:
// - LIBRE : créneau disponible, cliquable pour réserver
// - PRIVE : match privé, non cliquable
// - PUBLIC_DISPO : match public avec places restantes, cliquable pour rejoindre
// - COMPLET : match complet (4/4 joueurs payés), non cliquable
//- FERME : créneau fermé (hors horaires, jour fermé), non cliquable

export type StatutCellule =
  | 'LIBRE'
  | 'PRIVE'
  | 'PUBLIC_DISPO'
  | 'COMPLET'
  | 'FERME';

export interface TerrainView {
  id: number;
  numero: number;
  nom: string | null;
}

export interface CelluleView {
  terrainId: number;
  statut: StatutCellule;
  matchId: number | null;
  placesRestantes: number | null;
  organisateurNom: string | null;
}

export interface CreneauView {
  debut: string;  // "HH:MM:SS"
  fin: string;    // "HH:MM:SS"
  cellules: CelluleView[];
}

export interface PlanningView {
  siteId: number;
  siteNom: string;
  date: string;       // "YYYY-MM-DD"
  ferme: boolean;
  raison: string | null;
  terrains: TerrainView[];
  creneaux: CreneauView[];
}
