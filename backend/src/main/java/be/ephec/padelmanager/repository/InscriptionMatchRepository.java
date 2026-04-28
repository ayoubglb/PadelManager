package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.InscriptionMatch;
import be.ephec.padelmanager.entity.StatutInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InscriptionMatchRepository extends JpaRepository<InscriptionMatch, Long> {


    @Query("""
        SELECT COUNT(i) FROM InscriptionMatch i
        WHERE i.match.id = :matchId
          AND i.paye = true
          AND i.statut = be.ephec.padelmanager.entity.StatutInscription.INSCRIT
        """)
    long countJoueursPayesByMatchId(@Param("matchId") Long matchId);

    List<InscriptionMatch> findByMatchId(Long matchId);

    @Query("""
        SELECT i FROM InscriptionMatch i
        WHERE i.match.id = :matchId
          AND i.statut = be.ephec.padelmanager.entity.StatutInscription.INSCRIT
        """)
    List<InscriptionMatch> findInscritsByMatchId(@Param("matchId") Long matchId);

    List<InscriptionMatch> findByJoueurIdAndStatut(Long joueurId, StatutInscription statut);

    boolean existsByMatchIdAndJoueurId(Long matchId, Long joueurId);

    Optional<InscriptionMatch> findByMatchIdAndJoueurId(Long matchId, Long joueurId);

    @Query("""
        SELECT i FROM InscriptionMatch i
        WHERE i.paye = false
          AND i.statut = be.ephec.padelmanager.entity.StatutInscription.INSCRIT
          AND i.match.dateHeureDebut BETWEEN :debut AND :fin
        """)
    List<InscriptionMatch> findUnpaidUpcomingWithin(@Param("debut") LocalDateTime debut,
                                                    @Param("fin") LocalDateTime fin);
}