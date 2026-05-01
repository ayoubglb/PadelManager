package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.inscription.InscriptionMatchDTO;
import be.ephec.padelmanager.dto.inscription.InviterJoueurRequest;
import be.ephec.padelmanager.dto.match.CreateMatchRequest;
import be.ephec.padelmanager.dto.match.MatchDTO;
import be.ephec.padelmanager.dto.transaction.TransactionDTO;
import be.ephec.padelmanager.security.UtilisateurPrincipal;
import be.ephec.padelmanager.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/matchs")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MEMBRE_LIBRE', 'MEMBRE_SITE', 'MEMBRE_GLOBAL')")
    public ResponseEntity<MatchDTO> creerMatch(
            @Valid @RequestBody CreateMatchRequest requete,
            @AuthenticationPrincipal UtilisateurPrincipal principal
    ) {
        MatchDTO match = matchService.creerMatch(requete, principal.getUtilisateur());
        return ResponseEntity.status(HttpStatus.CREATED).body(match);
    }

    // Invite un joueur (par matricule) à un match privé
    @PostMapping("/{id}/joueurs")
    @PreAuthorize("hasAnyRole('MEMBRE_LIBRE', 'MEMBRE_SITE', 'MEMBRE_GLOBAL')")
    public ResponseEntity<InscriptionMatchDTO> inviterJoueur(
            @PathVariable Long id,
            @Valid @RequestBody InviterJoueurRequest requete,
            @AuthenticationPrincipal UtilisateurPrincipal principal
    ) {
        InscriptionMatchDTO inscription = matchService.inviterJoueur(
                id, requete.matricule(), principal.getUtilisateur());
        return ResponseEntity.status(HttpStatus.CREATED).body(inscription);
    }

    // Paye sa part d'un match privé
    @PostMapping("/{id}/payer")
    @PreAuthorize("hasAnyRole('MEMBRE_LIBRE', 'MEMBRE_SITE', 'MEMBRE_GLOBAL')")
    public ResponseEntity<TransactionDTO> payerSaPart(
            @PathVariable Long id,
            @AuthenticationPrincipal UtilisateurPrincipal principal
    ) {
        TransactionDTO transaction = matchService.payerSaPart(id, principal.getUtilisateur());
        return ResponseEntity.ok(transaction);
    }

}