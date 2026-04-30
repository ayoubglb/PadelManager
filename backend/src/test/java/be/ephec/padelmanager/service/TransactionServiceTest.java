package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.transaction.RechargeRequest;
import be.ephec.padelmanager.dto.transaction.TransactionDTO;
import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.entity.Transaction;
import be.ephec.padelmanager.entity.TypeTransaction;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.mapper.TransactionMapper;
import be.ephec.padelmanager.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests unitaires Mockito de TransactionService pour la recharge
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService — recharge compte (EF-MB-009)")
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private Utilisateur membreActif;

    @BeforeEach
    void setUp() {
        membreActif = Utilisateur.builder()
                .id(100L)
                .matricule("L600001")
                .nom("Doe")
                .prenom("John")
                .role(RoleUtilisateur.MEMBRE_LIBRE)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("recharger() crée une Transaction RECHARGE pour l'utilisateur authentifié")
    void rechargerCreeUneTransactionDeTypeRECHARGE() {
        RechargeRequest requete = new RechargeRequest(new BigDecimal("50.00"));

        Transaction transactionEnregistree = Transaction.builder()
                .id(1L)
                .utilisateur(membreActif)
                .type(TypeTransaction.RECHARGE)
                .montant(new BigDecimal("50.00"))
                .date(LocalDateTime.now())
                .build();
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(transactionEnregistree);

        TransactionDTO dtoAttendu = new TransactionDTO(
                1L, 100L, TypeTransaction.RECHARGE,
                new BigDecimal("50.00"), transactionEnregistree.getDate(), null);
        when(transactionMapper.toDto(transactionEnregistree)).thenReturn(dtoAttendu);

        TransactionDTO resultat = transactionService.recharger(requete, membreActif);

        assertThat(resultat).isEqualTo(dtoAttendu);
    }

    @Test
    @DisplayName("recharger() persiste une transaction avec utilisateur, montant et type corrects")
    void rechargerPersisteUneTransactionAvecLesBonsAttributs() {
        RechargeRequest requete = new RechargeRequest(new BigDecimal("25.00"));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toDto(any(Transaction.class)))
                .thenReturn(new TransactionDTO(1L, 100L, TypeTransaction.RECHARGE,
                        new BigDecimal("25.00"), LocalDateTime.now(), null));

        transactionService.recharger(requete, membreActif);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction sauvee = captor.getValue();

        assertThat(sauvee.getUtilisateur()).isEqualTo(membreActif);
        assertThat(sauvee.getMontant()).isEqualByComparingTo("25.00");
        assertThat(sauvee.getType()).isEqualTo(TypeTransaction.RECHARGE);
        assertThat(sauvee.getMatch()).isNull();
    }

    @Test
    @DisplayName("recharger() refuse un compte désactivé → IllegalArgumentException")
    void rechargerRefuseCompteDesactive() {
        Utilisateur membreInactif = Utilisateur.builder()
                .id(101L)
                .matricule("L600002")
                .nom("Old")
                .prenom("Jane")
                .role(RoleUtilisateur.MEMBRE_LIBRE)
                .active(false)
                .build();

        RechargeRequest requete = new RechargeRequest(new BigDecimal("50.00"));

        assertThatThrownBy(() -> transactionService.recharger(requete, membreInactif))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("désactivé");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("recharger() accepte un montant à 2 décimales (cohérent avec DECIMAL(10,2))")
    void rechargerAccepteMontantADeuxDecimales() {
        RechargeRequest requete = new RechargeRequest(new BigDecimal("12.34"));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toDto(any(Transaction.class)))
                .thenReturn(new TransactionDTO(1L, 100L, TypeTransaction.RECHARGE,
                        new BigDecimal("12.34"), LocalDateTime.now(), null));

        transactionService.recharger(requete, membreActif);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getMontant()).isEqualByComparingTo("12.34");
    }

    @Test
    @DisplayName("recharger() retourne le DTO produit par le mapper, sans le modifier")
    void rechargerRetourneLeDtoDuMapper() {
        RechargeRequest requete = new RechargeRequest(new BigDecimal("100.00"));

        Transaction transactionEnregistree = Transaction.builder()
                .id(42L).utilisateur(membreActif).type(TypeTransaction.RECHARGE)
                .montant(new BigDecimal("100.00")).date(LocalDateTime.now()).build();
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(transactionEnregistree);

        TransactionDTO dtoMapper = new TransactionDTO(
                42L, 100L, TypeTransaction.RECHARGE,
                new BigDecimal("100.00"), transactionEnregistree.getDate(), null);
        when(transactionMapper.toDto(transactionEnregistree)).thenReturn(dtoMapper);

        TransactionDTO resultat = transactionService.recharger(requete, membreActif);

        assertThat(resultat).isSameAs(dtoMapper);
    }
}