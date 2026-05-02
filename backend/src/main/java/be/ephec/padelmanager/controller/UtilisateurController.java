package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.utilisateur.UtilisateurDTO;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.mapper.UtilisateurMapper;
import be.ephec.padelmanager.repository.UtilisateurRepository;
import be.ephec.padelmanager.security.UtilisateurPrincipal;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}