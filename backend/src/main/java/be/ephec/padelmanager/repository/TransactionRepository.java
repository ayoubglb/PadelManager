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
                                be.ephec.padelmanager.entity.TypeTransaction.REMBOURSEMENT,
                                be.ephec.padelmanager.entity.TypeTransaction.REMBOURSEMENT_SOLDE_DU_ORGANISATEUR)
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

    // Récupère les transactions d'un utilisateur avec filtres optionnels
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.utilisateur.id = :utilisateurId
          AND (:type IS NULL OR t.type = :type)
          AND (:dateDebut IS NULL OR t.date >= :dateDebut)
          AND (:dateFin IS NULL OR t.date <= :dateFin)
        ORDER BY t.date DESC
        """)
    List<Transaction> findMesTransactions(@Param("utilisateurId") Long utilisateurId,
                                          @Param("type") TypeTransaction type,
                                          @Param("dateDebut") LocalDateTime dateDebut,
                                          @Param("dateFin") LocalDateTime dateFin);

    // Somme des transactions d'un type donné dans une période, filtrable par site (via match.terrain.site).
    // Si siteId est null, somme sur tous les sites
    @Query("""
        SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t
        LEFT JOIN t.match m
        LEFT JOIN m.terrain te
        LEFT JOIN te.site s
        WHERE t.type = :type
          AND t.date BETWEEN :debut AND :fin
          AND (:siteId IS NULL OR s.id = :siteId)
        """)
    BigDecimal sommerParTypeEtPeriode(@Param("type") TypeTransaction type,
                                      @Param("debut") LocalDateTime debut,
                                      @Param("fin") LocalDateTime fin,
                                      @Param("siteId") Long siteId);

    // Somme de plusieurs types de transactions (utilisé pour grouper PAIEMENT_MATCH + SOLDE_DU)
    @Query("""
        SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t
        LEFT JOIN t.match m
        LEFT JOIN m.terrain te
        LEFT JOIN te.site s
        WHERE t.type IN :types
          AND t.date BETWEEN :debut AND :fin
          AND (:siteId IS NULL OR s.id = :siteId)
        """)
    BigDecimal sommerParTypesEtPeriode(@Param("types") List<TypeTransaction> types,
                                       @Param("debut") LocalDateTime debut,
                                       @Param("fin") LocalDateTime fin,
                                       @Param("siteId") Long siteId);
}