package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.site.CreateTerrainRequest;
import be.ephec.padelmanager.dto.site.TerrainDTO;
import be.ephec.padelmanager.dto.site.UpdateTerrainRequest;
import be.ephec.padelmanager.entity.Site;
import be.ephec.padelmanager.entity.Terrain;
import be.ephec.padelmanager.mapper.TerrainMapper;
import be.ephec.padelmanager.repository.SiteRepository;
import be.ephec.padelmanager.repository.TerrainRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerrainService {

    private final TerrainRepository terrainRepository;
    private final SiteRepository siteRepository;
    private final TerrainMapper terrainMapper;

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    // Terrains actifs d'un site — endpoint public utilisé par la grille de réservation.
    @Transactional(readOnly = true)
    public List<TerrainDTO> listerActifsParSite(Long siteId) {
        verifierSiteExiste(siteId);
        return terrainMapper.versListeDTOs(
                terrainRepository.findBySiteIdAndActiveTrueOrderByNumeroAsc(siteId)
        );
    }

    //  Tous les terrains d'un site (actifs et désactivés) — réservé aux admins.
    @Transactional(readOnly = true)
    public List<TerrainDTO> listerTousParSite(Long siteId) {
        verifierSiteExiste(siteId);
        return terrainMapper.versListeDTOs(
                terrainRepository.findBySiteIdOrderByNumeroAsc(siteId)
        );
    }

    @Transactional(readOnly = true)
    public TerrainDTO recuperer(Long id) {
        return terrainMapper.versDTO(chargerOuLever(id));
    }

    // ------------------------------------------------------------------
    // Écriture (ADMIN_GLOBAL ou ADMIN_SITE)
    // ------------------------------------------------------------------

    @Transactional
    public TerrainDTO creer(Long siteId, CreateTerrainRequest requete) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new EntityNotFoundException("Site introuvable : id=" + siteId));

        // numéro unique au sein d'un site
        if (terrainRepository.existsBySiteIdAndNumero(siteId, requete.numero())) {
            throw new IllegalArgumentException(
                    "Un terrain n°" + requete.numero() + " existe déjà sur ce site."
            );
        }

        Terrain nouveau = terrainMapper.versEntite(requete);
        nouveau.setSite(site);
        nouveau = terrainRepository.save(nouveau);
        log.info("Terrain créé : id={}, site={}, numero={}",
                nouveau.getId(), siteId, nouveau.getNumero());
        return terrainMapper.versDTO(nouveau);
    }

    @Transactional
    public TerrainDTO mettreAJour(Long id, UpdateTerrainRequest requete) {
        Terrain terrain = chargerOuLever(id);

        // si le numéro change, vérifier qu'il n'est pas déjà pris sur ce site
        if (terrainRepository.existsBySiteIdAndNumeroAndIdNot(
                terrain.getSite().getId(), requete.numero(), id)) {
            throw new IllegalArgumentException(
                    "Un autre terrain n°" + requete.numero() + " existe déjà sur ce site."
            );
        }

        terrainMapper.mettreAJour(requete, terrain);
        log.info("Terrain mis à jour : id={}", id);
        return terrainMapper.versDTO(terrain);
    }

    @Transactional
    public TerrainDTO activer(Long id) {
        Terrain terrain = chargerOuLever(id);
        terrain.setActive(true);
        log.info("Terrain activé : id={}", id);
        return terrainMapper.versDTO(terrain);
    }

    @Transactional
    public TerrainDTO desactiver(Long id) {
        Terrain terrain = chargerOuLever(id);
        terrain.setActive(false);
        log.info("Terrain désactivé : id={}", id);
        return terrainMapper.versDTO(terrain);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Terrain chargerOuLever(Long id) {
        return terrainRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Terrain introuvable : id=" + id));
    }

    private void verifierSiteExiste(Long siteId) {
        if (!siteRepository.existsById(siteId)) {
            throw new EntityNotFoundException("Site introuvable : id=" + siteId);
        }
    }
}