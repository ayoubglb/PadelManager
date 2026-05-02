package be.ephec.padelmanager.dto.inscription;

import be.ephec.padelmanager.dto.transaction.TransactionDTO;

// Réponse combinée pour rejoindre un match public
// Le frontend reçoit l'inscription créée et la transaction de paiement en une seule réponse
public record RejoindreMatchResponse(
        InscriptionMatchDTO inscription,
        TransactionDTO transaction
) {
}