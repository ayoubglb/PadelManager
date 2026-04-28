package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.JourFermeture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JourFermetureRepository extends JpaRepository<JourFermeture, Long> {

    // Toutes les fermetures globales (site = null), triées par date croissante.
    @Query("SELECT j FROM JourFermeture j WHERE j.site IS NULL ORDER BY j.dateFermeture ASC")
    List<JourFermeture> findToutesGlobales();

    // Toutes les fermetures spécifiques à un site (hors fermetures globales)
    List<JourFermeture> findBySiteIdOrderByDateFermetureAsc(Long siteId);

    // Fermeture globale à une date donnée
    @Query("SELECT j FROM JourFermeture j WHERE j.site IS NULL AND j.dateFermeture = :date")
    Optional<JourFermeture> findGlobaleParDate(@Param("date") LocalDate date);

    // Fermeture site-spécifique à une date donnée
    @Query("""
        SELECT j FROM JourFermeture j
        WHERE j.site.id = :siteId AND j.dateFermeture = :date
        """)
    Optional<JourFermeture> findParSiteEtDate(@Param("siteId") Long siteId, @Param("date") LocalDate date);

    // Indique si un site est fermé à une date donnée. Vrai si une fermeture globale OU une fermeture site-spécifique existe.

    @Query("""
        SELECT COUNT(j) > 0 FROM JourFermeture j
        WHERE j.dateFermeture = :date
          AND (j.site IS NULL OR j.site.id = :siteId)
        """)
    boolean estFermeAUneDate(@Param("siteId") Long siteId, @Param("date") LocalDate date);

    @Query("""
    SELECT COUNT(j) > 0 FROM JourFermeture j
    WHERE j.date = :date
      AND (j.site IS NULL OR j.site.id = :siteId)
    """)
    boolean existsByDateAndSiteIdOrSiteIsNull(@Param("date") LocalDate date,
                                              @Param("siteId") Long siteId);
}