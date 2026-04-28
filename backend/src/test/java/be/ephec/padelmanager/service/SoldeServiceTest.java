package be.ephec.padelmanager.service;

import be.ephec.padelmanager.config.PricingConstants;
import be.ephec.padelmanager.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SoldeService — calcul du solde via ledger, vérif de provision")
class SoldeServiceTest {

    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private SoldeService soldeService;

    @Test
    @DisplayName("calculerSolde() délègue au repository et retourne sa valeur")
    void calculerSoldeDelegueAuRepository() {
        when(transactionRepository.calculerSoldeUtilisateur(42L))
                .thenReturn(new BigDecimal("35.00"));

        BigDecimal solde = soldeService.calculerSolde(42L);

        assertThat(solde).isEqualByComparingTo("35.00");
    }

    @Test
    @DisplayName("calculerSolde() sur utilisateur sans transaction → 0 (COALESCE en SQL)")
    void calculerSoldeUtilisateurSansTransaction() {
        // Le repository retourne 0 grâce au COALESCE de la requête JPQL.
        when(transactionRepository.calculerSoldeUtilisateur(99L))
                .thenReturn(BigDecimal.ZERO);

        BigDecimal solde = soldeService.calculerSolde(99L);

        assertThat(solde).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("disposeAuMoinsDe() → true si solde > montant requis")
    void disposeAuMoinsDeTrueSiSoldeSuffisant() {
        when(transactionRepository.calculerSoldeUtilisateur(1L))
                .thenReturn(new BigDecimal("50.00"));

        assertThat(soldeService.disposeAuMoinsDe(1L, PricingConstants.PART_JOUEUR)).isTrue();
    }

    @Test
    @DisplayName("disposeAuMoinsDe() → true si solde EXACTEMENT égal au montant requis")
    void disposeAuMoinsDeTrueSiSoldeEgal() {
        when(transactionRepository.calculerSoldeUtilisateur(1L))
                .thenReturn(new BigDecimal("15.00"));

        assertThat(soldeService.disposeAuMoinsDe(1L, PricingConstants.PART_JOUEUR)).isTrue();
    }

    @Test
    @DisplayName("disposeAuMoinsDe() → false si solde insuffisant")
    void disposeAuMoinsDeFalseSiSoldeInsuffisant() {
        when(transactionRepository.calculerSoldeUtilisateur(1L))
                .thenReturn(new BigDecimal("10.00"));

        assertThat(soldeService.disposeAuMoinsDe(1L, PricingConstants.PART_JOUEUR)).isFalse();
    }

    @Test
    @DisplayName("disposeAuMoinsDe() → false si solde négatif (organisateur après SOLDE_DU)")
    void disposeAuMoinsDeFalseSiSoldeNegatif() {
        // Cas réaliste : un organisateur de match public incomplet a écopé d'une
        // transaction SOLDE_DU_ORGANISATEUR qui a fait passer son solde à -30 € (CF-RV-020).
        // Il est naturellement bloqué tant qu'il n'a pas rechargé (CF-RV-011).
        when(transactionRepository.calculerSoldeUtilisateur(1L))
                .thenReturn(new BigDecimal("-30.00"));

        assertThat(soldeService.disposeAuMoinsDe(1L, PricingConstants.PART_JOUEUR)).isFalse();
    }
}