// Organisateur figurant dans le top des plus actifs sur la période
export interface TopOrganisateur {
  utilisateurId: number;
  matricule: string;
  nomComplet: string;
  nombreMatchsOrganises: number;
}

// DTO de reporting (utilisé pour reporting global et par site).
// - caEncaisse : argent réel encaissé via les RECHARGE
// - volumeMatchs : montants facturés via PAIEMENT_MATCH + SOLDE_DU_ORGANISATEUR

export interface Reporting {
  dateDebut: string;
  dateFin: string;
  caEncaisse: number;
  volumeMatchs: number;
  nombreMatchsTotaux: number;
  nombreMatchsPrives: number;
  nombreMatchsPublics: number;
  nombreMatchsAnnules: number;
  topOrganisateurs: TopOrganisateur[];
}

// Paramètres de filtrage du reporting
export interface ReportingParams {
  dateDebut: string; // ISO YYYY-MM-DD
  dateFin: string;   // ISO YYYY-MM-DD
}
