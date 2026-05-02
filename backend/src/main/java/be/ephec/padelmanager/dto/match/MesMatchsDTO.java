package be.ephec.padelmanager.dto.match;

import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.TypeMatch;

import java.time.LocalDateTime;

// DTO enrichi pour les matchs de l'utilisateur authentifié
// Contient le contexte du match + la position de l'utilisateur dans ce match
public record MesMatchsDTO(
        Long id,
        String siteNom,
        Integer terrainNumero,
        LocalDateTime dateHeureDebut,
        LocalDateTime dateHeureFin,
        TypeMatch type,
        StatutMatch statut,
        String organisateurNom,
        MonRole monRole,
        Boolean maPartPayee,
        Integer nombreInscrits
) {
    // Le rôle de l'utilisateur authentifié dans ce match
    public enum MonRole {
        ORGANISATEUR,
        INVITE  // pour PRIVE invité par l'organisateur, ou PUBLIC ayant rejoint
    }
}