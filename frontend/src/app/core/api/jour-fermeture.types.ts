// Représente un jour de fermeture d'un site (ou globale)
// - siteId null = fermeture globale (tous sites)
// - siteId non null = fermeture spécifique à ce site
export interface JourFermeture {
  id: number;
  dateFermeture: string; // ISO YYYY-MM-DD
  siteId: number | null;
  raison: string;
}

// Body pour POST /jours-fermeture
// Une seule API pour créer globale ou site-spécifique selon la valeur de siteId
export interface JourFermetureCreateRequest {
  dateFermeture: string;
  siteId: number | null;
  raison: string;
}
