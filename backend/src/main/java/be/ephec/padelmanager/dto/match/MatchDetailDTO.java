package be.ephec.padelmanager.dto.match;

import be.ephec.padelmanager.dto.inscription.InscriptionMatchDTO;
import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.TypeMatch;

import java.time.LocalDateTime;
import java.util.List;

// DTO détaillé d'un match avec la liste de ses joueurs inscrits

public record MatchDetailDTO(
        // Infos du match
        Long id,
        Long terrainId,
        Integer terrainNumero,
        Long siteId,
        String siteNom,
        LocalDateTime dateHeureDebut,
        LocalDateTime dateHeureFin,
        Long organisateurId,
        String organisateurNom,
        String organisateurMatricule,
        TypeMatch type,
        StatutMatch statut,
        Boolean devenuPublicAutomatiquement,
        LocalDateTime dateCreation,
        boolean termine,

        // Liste des inscriptions (joueurs avec leur statut paye/non payé)
        List<InscriptionMatchDTO> inscriptions,

        // Stats calculées
        int nombreJoueursPayes,
        int placesDisponibles
) {
}