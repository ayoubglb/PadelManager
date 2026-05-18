// Horaire d'ouverture d'un site pour une année civile
// Une seule entrée par couple (site, année) — garanti par UK composite

export interface HoraireSite {
  id: number;
  siteId: number;
  annee: number;
  heureDebut: string; // HH:mm:ss
  heureFin: string;   // HH:mm:ss
}

// Body pour POST /sites/{siteId}/horaires.
// Pas de PUT côté backend (décision métier : on ne modifie pas un horaire,
// on le supprime et on le recrée si besoin sur une année future)

export interface HoraireCreateRequest {
  annee: number;
  heureDebut: string; // HH:mm ou HH:mm:ss
  heureFin: string;   // HH:mm ou HH:mm:ss
}
