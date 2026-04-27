package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.Terrain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TerrainRepository extends JpaRepository<Terrain, Long> {

    // Tous les terrains d'un site (actifs et inactifs) — pour les  admin.
    List<Terrain> findBySiteIdOrderByNumeroAsc(Long siteId);

    // Terrains actifs d'un site — pour la grille de réservation côté membre
    List<Terrain> findBySiteIdAndActiveTrueOrderByNumeroAsc(Long siteId);

    // Vérification d'unicité
    boolean existsBySiteIdAndNumero(Long siteId, Integer numero);

    boolean existsBySiteIdAndNumeroAndIdNot(Long siteId, Integer numero, Long id);
}