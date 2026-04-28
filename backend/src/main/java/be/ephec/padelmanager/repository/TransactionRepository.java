package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.Transaction;
import be.ephec.padelmanager.entity.TypeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {


    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN t.type IN (be.ephec.padelmanager.entity.TypeTransaction.RECHARGE,
                                be.ephec.padelmanager.entity.TypeTransaction.REMBOURSEMENT)
                THEN t.montant
                ELSE -t.montant
            END
        ), 0)
        FROM Transaction t
        WHERE t.utilisateur.id = :utilisateurId
        """)
    BigDecimal calculerSoldeUtilisateur(@Param("utilisateurId") Long utilisateurId);

    List<Transaction> findByUtilisateurIdOrderByDateDesc(Long utilisateurId);

    @Query("""
        SELECT COUNT(t) > 0 FROM Transaction t
        WHERE t.match.id = :matchId
          AND t.type = be.ephec.padelmanager.entity.TypeTransaction.SOLDE_DU_ORGANISATEUR
        """)
    boolean existsSoldeDuOrganisateurForMatch(@Param("matchId") Long matchId);

    @Query("""
        SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t
        WHERE t.type IN (be.ephec.padelmanager.entity.TypeTransaction.PAIEMENT_MATCH,
                         be.ephec.padelmanager.entity.TypeTransaction.SOLDE_DU_ORGANISATEUR)
          AND t.date BETWEEN :debut AND :fin
        """)
    BigDecimal calculerCaBrut(@Param("debut") LocalDateTime debut,
                              @Param("fin") LocalDateTime fin);

    @Query("""
        SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t
        WHERE t.type = be.ephec.padelmanager.entity.TypeTransaction.REMBOURSEMENT
          AND t.date BETWEEN :debut AND :fin
        """)
    BigDecimal calculerTotalRemboursements(@Param("debut") LocalDateTime debut,
                                           @Param("fin") LocalDateTime fin);
}