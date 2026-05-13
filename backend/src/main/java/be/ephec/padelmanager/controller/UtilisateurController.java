package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.utilisateur.UtilisateurDTO;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.mapper.UtilisateurMapper;
import be.ephec.padelmanager.repository.UtilisateurRepository;
import be.ephec.padelmanager.security.UtilisateurPrincipal;
import be.ephec.padelmanager.dto.utilisateur.UtilisateurRechercheDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurMapper utilisateurMapper;

    // Profil de l'utilisateur connecté
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UtilisateurDTO> moi(
            @AuthenticationPrincipal UtilisateurPrincipal principal
    ) {
        Utilisateur utilisateur = utilisateurRepository
                .findByIdWithSite(principal.getUtilisateur().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Utilisateur introuvable : " + principal.getUtilisateur().getId()));
        return ResponseEntity.ok(utilisateurMapper.toDto(utilisateur));
    }

    private static final int LIMIT_MAX = 10;
    private static final int MIN_QUERY_LENGTH = 3;

    // Recherche d'utilisateurs invitables à un match.
    // Sécurité anti-scraping : min 3 chars dans q, limite 10 résultats, exclusion des admins.
    // Pas d'email retourné (privé)
    @GetMapping("/recherche")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UtilisateurRechercheDTO>> rechercherUtilisateurs(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        // Min 3 caractères pour éviter le scraping
        if (q == null || q.trim().length() < MIN_QUERY_LENGTH) {
            return ResponseEntity.ok(List.of());
        }

        // Cap la limite à 10 même si l'appelant demande plus
        int effectiveLimit = Math.min(limit == null ? LIMIT_MAX : limit, LIMIT_MAX);
        Pageable pageable = PageRequest.of(0, effectiveLimit);

        List<UtilisateurRechercheDTO> resultats = utilisateurRepository
                .rechercherPourInvitation(q.trim(), pageable)
                .stream()
                .map(utilisateurMapper::toRechercheDto)
                .toList();

        return ResponseEntity.ok(resultats);
    }
}