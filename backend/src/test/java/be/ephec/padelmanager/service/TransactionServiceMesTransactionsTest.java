package be.ephec.padelmanager.service;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService — consulter mes transactions")
class TransactionServiceMesTransactionsTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = Utilisateur.builder()
                .id(100L).matricule("L600001")
                .role(RoleUtilisateur.MEMBRE_LIBRE).active(true).build();

        lenient().when(transactionMapper.toDto(any(Transaction.class)))
                .thenReturn(new TransactionDTO(
                        1L, 100L, TypeTransaction.RECHARGE,
                        new BigDecimal("50.00"), LocalDateTime.now(), null));
    }

    @Test
    @DisplayName("consulterMesTransactions sans filtre → tous les types et toutes dates")
    void sansFiltre() {
        Transaction t1 = Transaction.builder().id(1L).utilisateur(utilisateur).build();
        Transaction t2 = Transaction.builder().id(2L).utilisateur(utilisateur).build();
        when(transactionRepository.findMesTransactions(eq(100L), isNull(), isNull(), isNull()))
                .thenReturn(List.of(t1, t2));

        List<TransactionDTO> resultats = transactionService
                .consulterMesTransactions(utilisateur, null, null, null);

        assertThat(resultats).hasSize(2);
        verify(transactionRepository).findMesTransactions(100L, null, null, null);
    }

    @Test
    @DisplayName("consulterMesTransactions avec filtre type RECHARGE → ne retourne que les RECHARGE")
    void filtreType() {
        Transaction t1 = Transaction.builder().id(1L).utilisateur(utilisateur)
                .type(TypeTransaction.RECHARGE).build();
        when(transactionRepository.findMesTransactions(eq(100L), eq(TypeTransaction.RECHARGE), isNull(), isNull()))
                .thenReturn(List.of(t1));

        List<TransactionDTO> resultats = transactionService
                .consulterMesTransactions(utilisateur, TypeTransaction.RECHARGE, null, null);

        assertThat(resultats).hasSize(1);
        verify(transactionRepository).findMesTransactions(100L, TypeTransaction.RECHARGE, null, null);
    }

    @Test
    @DisplayName("consulterMesTransactions avec dates → conversion atStartOfDay et MAX time")
    void filtreDates() {
        when(transactionRepository.findMesTransactions(eq(100L), isNull(), any(), any()))
                .thenReturn(List.of());

        transactionService.consulterMesTransactions(
                utilisateur, null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31));

        // Vérifie qu'on a bien appelé avec des LocalDateTime, pas null
        verify(transactionRepository).findMesTransactions(
                eq(100L), isNull(),
                any(LocalDateTime.class),
                any(LocalDateTime.class));
    }

    @Test
    @DisplayName("consulterMesTransactions aucune transaction → liste vide")
    void aucuneTransaction() {
        when(transactionRepository.findMesTransactions(eq(100L), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        List<TransactionDTO> resultats = transactionService
                .consulterMesTransactions(utilisateur, null, null, null);

        assertThat(resultats).isEmpty();
    }
}