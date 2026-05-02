package be.ephec.padelmanager.controller;

import be.ephec.padelmanager.dto.transaction.RechargeRequest;
import be.ephec.padelmanager.dto.transaction.TransactionDTO;
import be.ephec.padelmanager.security.UtilisateurPrincipal;
import be.ephec.padelmanager.service.SoldeService;
import be.ephec.padelmanager.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final SoldeService soldeService;

    @PostMapping("/recharge")
    @PreAuthorize("hasAnyRole('MEMBRE_LIBRE', 'MEMBRE_SITE', 'MEMBRE_GLOBAL')")
    public ResponseEntity<TransactionDTO> recharger(
            @Valid @RequestBody RechargeRequest requete,
            @AuthenticationPrincipal UtilisateurPrincipal principal
    ) {
        TransactionDTO transaction = transactionService.recharger(
                requete, principal.getUtilisateur());
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    // Solde courant de l'utilisateur connecté (calculé par agrégation du ledger)
    @GetMapping("/solde")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, BigDecimal>> consulterSolde(
            @AuthenticationPrincipal UtilisateurPrincipal principal
    ) {
        BigDecimal solde = soldeService.calculerSolde(principal.getUtilisateur().getId());
        return ResponseEntity.ok(Map.of("solde", solde));
    }


}