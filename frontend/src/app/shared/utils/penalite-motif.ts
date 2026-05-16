// Convertit un motif technique de pénalité en texte lisible pour l'utilisateur
// Si le motif n'est pas connu, on retourne le motif brut

const MOTIF_LABELS: Record<string, string> = {
  CONVERSION_AUTO_PRIVE_PUBLIC:
    "Votre match privé est devenu public 24h avant la date prévue car moins de 4 joueurs étaient inscrits. C'est la raison de cette pénalité.",
};

export function humaniserMotifPenalite(motif: string): string {
  return MOTIF_LABELS[motif] ?? motif;
}
