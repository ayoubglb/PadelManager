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

    // Récupère tous les matchs où le joueur a une inscription, avec filtre temporel optionnel
    @Query("""
        SELECT DISTINCT m FROM Match m
        JOIN InscriptionMatch i ON i.match = m
        WHERE i.joueur.id = :joueurId
          AND (:aVenir = false OR m.dateHeureDebut > :maintenant)
          AND (:aVenir = true OR m.dateHeureDebut <= :maintenant)
        """)
    List<Match> findMesMatchs(@Param("joueurId") Long joueurId,
                              @Param("aVenir") boolean aVenir,
                              @Param("maintenant") LocalDateTime maintenant);

    // Récupère tous les matchs en statut PROGRAMME dont la date est dans la fenêtre [maintenant, limite24h].
     //  Utilisé par les jobs @Scheduled
    @Query("""
        SELECT m FROM Match m
        WHERE m.statut = be.ephec.padelmanager.entity.StatutMatch.PROGRAMME
          AND m.dateHeureDebut BETWEEN :maintenant AND :limite24h
        """)
    List<Match> findMatchsAEcheance24h(@Param("maintenant") LocalDateTime maintenant,
                                       @Param("limite24h") LocalDateTime limite24h);

    // Compte les matchs créés dans une période, optionnellement filtrés par site et type
    @Query("""
        SELECT COUNT(m) FROM Match m
        WHERE m.dateHeureDebut BETWEEN :debut AND :fin
          AND (:siteId IS NULL OR m.terrain.site.id = :siteId)
          AND (:type IS NULL OR m.type = :type)
        """)
    long compterMatchs(@Param("debut") LocalDateTime debut,
                       @Param("fin") LocalDateTime fin,
                       @Param("siteId") Long siteId,
                       @Param("type") TypeMatch type);

    //  Compte les matchs ANNULE dans une période, optionnellement filtré par site
    @Query("""
        SELECT COUNT(m) FROM Match m
        WHERE m.dateHeureDebut BETWEEN :debut AND :fin
          AND m.statut = be.ephec.padelmanager.entity.StatutMatch.ANNULE
          AND (:siteId IS NULL OR m.terrain.site.id = :siteId)
        """)
    long compterMatchsAnnules(@Param("debut") LocalDateTime debut,
                              @Param("fin") LocalDateTime fin,
                              @Param("siteId") Long siteId);

    // Top organisateurs par nombre de matchs créés dans la période.
    //  Retourne Object[] avec : id, matricule, prenom, nom, count. Limit côté service
    @Query("""
        SELECT m.organisateur.id,
               m.organisateur.matricule,
               m.organisateur.prenom,
               m.organisateur.nom,
               COUNT(m)
        FROM Match m
        WHERE m.dateHeureDebut BETWEEN :debut AND :fin
          AND (:siteId IS NULL OR m.terrain.site.id = :siteId)
        GROUP BY m.organisateur.id, m.organisateur.matricule,
                 m.organisateur.prenom, m.organisateur.nom
        ORDER BY COUNT(m) DESC
        """)
    List<Object[]> topOrganisateurs(@Param("debut") LocalDateTime debut,
                                    @Param("fin") LocalDateTime fin,
                                    @Param("siteId") Long siteId);


}