export type TypeTransaction =
  | 'RECHARGE'
  | 'PAIEMENT_MATCH'
  | 'SOLDE_DU_ORGANISATEUR'
  | 'REMBOURSEMENT'
  | 'REMBOURSEMENT_SOLDE_DU_ORGANISATEUR';

// Transaction telle que retournée par le backend.
// Le sens (crédit/débit) sur le solde est déterminé par le type :
// - Crédits : RECHARGE, REMBOURSEMENT, REMBOURSEMENT_SOLDE_DU_ORGANISATEUR
// - Débits : PAIEMENT_MATCH, SOLDE_DU_ORGANISATEUR

export interface Transaction {
  id: number;
  utilisateurId: number;
  type: TypeTransaction;
  montant: number;
  date: string; // ISO 8601
  matchId: number | null;
}

export interface RechargeRequest {
  montant: number;
}

export interface TransactionFilters {
  type?: TypeTransaction;
  dateDebut?: string; // ISO date YYYY-MM-DD
  dateFin?: string;
}

// Helpers métier — détermine si un type est un crédit ou un débit
export const TYPES_CREDITS: TypeTransaction[] = [
  'RECHARGE',
  'REMBOURSEMENT',
  'REMBOURSEMENT_SOLDE_DU_ORGANISATEUR',
];

export function isCredit(type: TypeTransaction): boolean {
  return TYPES_CREDITS.includes(type);
}

//  Libellés humains pour l'affichage dans les tableaux
export const TYPE_LABELS: Record<TypeTransaction, string> = {
  RECHARGE: 'Recharge',
  PAIEMENT_MATCH: 'Paiement match',
  SOLDE_DU_ORGANISATEUR: 'Solde dû organisateur',
  REMBOURSEMENT: 'Remboursement',
  REMBOURSEMENT_SOLDE_DU_ORGANISATEUR: 'Remb. solde organisateur',
};

export interface SoldeResponse {
  solde: number;
}
