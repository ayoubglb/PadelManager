package be.ephec.padelmanager.dto.match;

import be.ephec.padelmanager.dto.transaction.TransactionDTO;

import java.util.List;

// Réponse pour l'annulation d'un match. Contient le match annulé et la liste des transactions de remboursement créées
public record AnnulationMatchResponse(
        Long matchId,
        int nombreRemboursements,
        List<TransactionDTO> remboursements
) {
}