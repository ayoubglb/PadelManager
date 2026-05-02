package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.Match;
import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.TypeMatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Match m WHERE m.id = :id")
    Optional<Match> findByIdForUpdate(@Param("id") Long id);

    boolean existsByTerrainIdAndDateHeureDebut(Long terrainId, LocalDateTime dateHeureDebut);

    @Query("""
        SELECT m FROM Match m
        WHERE m.type = be.ephec.padelmanager.entity.TypeMatch.PUBLIC
          AND m.statut = be.ephec.padelmanager.entity.StatutMatch.PROGRAMME
          AND m.dateHeureDebut > :now
        ORDER BY m.dateHeureDebut ASC
        """)
    List<Match> findPublicsAVenir(@Param("now") LocalDateTime now);

    // Recherche les matchs publics PROGRAMME futurs avec filtres optionnels
    @Query("""
        SELECT m FROM Match m
        WHERE m.type = be.ephec.padelmanager.entity.TypeMatch.PUBLIC
          AND m.statut = be.ephec.padelmanager.entity.StatutMatch.PROGRAMME
          AND m.dateHeureDebut >= :dateDebut
          AND (:dateFin IS NULL OR m.dateHeureDebut <= :dateFin)
          AND (:siteId IS NULL OR m.terrain.site.id = :siteId)
        ORDER BY m.dateHeureDebut ASC
        """)
    List<Match> rechercherPublics(@Param("dateDebut") LocalDateTime dateDebut,
                                  @Param("dateFin") LocalDateTime dateFin,
                                  @Param("siteId") Long siteId);

    @Query("""
        SELECT m FROM Match m
        WHERE m.type = be.ephec.padelmanager.entity.TypeMatch.PRIVE
          AND m.statut = be.ephec.padelmanager.entity.StatutMatch.PROGRAMME
          AND m.devenuPublicAutomatiquement = false
          AND m.dateHeureDebut BETWEEN :now AND :limite
        """)
    List<Match> findPrivesAConvertir(
            @Param("now") LocalDateTime now,
            @Param("limite") LocalDateTime limite
    );

    List<Match> findByOrganisateurIdOrderByDateHeureDebutDesc(Long organisateurId);

    @Query("""
        SELECT m FROM Match m
        WHERE m.terrain.site.id = :siteId
          AND m.statut = be.ephec.padelmanager.entity.StatutMatch.PROGRAMME
          AND m.dateHeureDebut BETWEEN :debut AND :fin
        ORDER BY m.dateHeureDebut ASC, m.terrain.numero ASC
        """)
    List<Match> findBySiteAndPeriode(
            @Param("siteId") Long siteId,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin
    );
}