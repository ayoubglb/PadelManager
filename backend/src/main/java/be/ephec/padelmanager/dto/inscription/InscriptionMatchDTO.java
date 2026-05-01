package be.ephec.padelmanager.dto.inscription;

import be.ephec.padelmanager.entity.StatutInscription;

import java.time.LocalDateTime;

// DTO de réponse pour une inscription à un match
public record InscriptionMatchDTO(
        Long id,
        Long matchId,
        Long joueurId,
        String joueurMatricule,
        String joueurNom,
        LocalDateTime dateInscription,
        Boolean paye,
        StatutInscription statut,
        Boolean estOrganisateur
) {
}