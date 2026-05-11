package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.penalite.PenaliteDTO;
import be.ephec.padelmanager.mapper.PenaliteMapper;
import be.ephec.padelmanager.repository.PenaliteRepository;
import be.ephec.padelmanager.security.UtilisateurPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/utilisateurs/me/penalites")
@RequiredArgsConstructor
public class PenaliteController {

    private final PenaliteRepository penaliteRepository;
    private final PenaliteMapper penaliteMapper;
    private final Clock clock;

    // Récupère toutes les pénalités de l'utilisateur connecté (actives + passées)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PenaliteDTO>> mesPenalites(
            @AuthenticationPrincipal UtilisateurPrincipal principal
    ) {
        List<PenaliteDTO> penalites = penaliteRepository
                .findAllByUtilisateurIdOrderByDateDebutDesc(principal.getUtilisateur().getId())
                .stream()
                .map(penaliteMapper::toDto)
                .toList();
        return ResponseEntity.ok(penalites);
    }

    // Récupère la pénalité active de l'utilisateur connecté
    // Renvoie 200 avec body null si aucune pénalité active
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PenaliteDTO> mapenaliteActive(
            @AuthenticationPrincipal UtilisateurPrincipal principal
    ) {
        Optional<PenaliteDTO> active = penaliteRepository
                .findActiveByUtilisateurId(principal.getUtilisateur().getId(), LocalDateTime.now(clock))
                .map(penaliteMapper::toDto);
        return ResponseEntity.ok(active.orElse(null));
    }
}