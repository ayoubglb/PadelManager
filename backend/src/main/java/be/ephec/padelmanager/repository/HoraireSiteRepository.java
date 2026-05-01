package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.HoraireSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface    HoraireSiteRepository extends JpaRepository<HoraireSite, Long> {

    // Récent preums
    List<HoraireSite> findBySiteIdOrderByAnneeDesc(Long siteId);

    Optional<HoraireSite> findBySiteIdAndAnnee(Long siteId, Integer annee);

    boolean existsBySiteIdAndAnnee(Long siteId, Integer annee);
}