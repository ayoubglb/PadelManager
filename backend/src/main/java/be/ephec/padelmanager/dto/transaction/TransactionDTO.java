package be.ephec.padelmanager.dto.transaction;

import be.ephec.padelmanager.entity.TypeTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// DTO de réponse pour ledger et machid null pour recharge
public record TransactionDTO(
        Long id,
        Long utilisateurId,
        TypeTransaction type,
        BigDecimal montant,
        LocalDateTime date,
        Long matchId
) {
}