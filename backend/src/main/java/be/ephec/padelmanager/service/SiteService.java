package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.site.CreateSiteRequest;
import be.ephec.padelmanager.dto.site.SiteDTO;
import be.ephec.padelmanager.dto.site.UpdateSiteRequest;
import be.ephec.padelmanager.entity.Site;
import be.ephec.padelmanager.mapper.SiteMapper;
import be.ephec.padelmanager.repository.SiteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class SiteService {

    private final SiteRepository siteRepository;
    private final SiteMapper siteMapper;

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    // Liste des sites actifs — endpoint public.
    @Transactional(readOnly = true)
    public List<SiteDTO> listerActifs() {
        return siteMapper.versListeDTOs(siteRepository.findByActiveTrue());
    }

    // Liste de tous les sites, actifs et désactivés — aux admins
    @Transactional(readOnly = true)
    public List<SiteDTO> listerTous() {
        return siteMapper.versListeDTOs(siteRepository.findAll());
    }

    // Récupération d'un site par id
    @Transactional(readOnly = true)
    public SiteDTO recuperer(Long id) {
        return siteMapper.versDTO(chargerOuLever(id));
    }

    // ------------------------------------------------------------------
    // Écriture ADMIN_GLOBAL uniquement — contrôlé avec @PreAuthorize dans contrôleur
    // ------------------------------------------------------------------

    @Transactional
    public SiteDTO creer(CreateSiteRequest requete) {
        if (siteRepository.existsByNom(requete.nom())) {
            throw new IllegalArgumentException("Un site avec ce nom existe déjà : " + requete.nom());
        }
        Site nouveau = siteMapper.versEntite(requete);
        nouveau = siteRepository.save(nouveau);
        log.info("Site créé : id={}, nom={}", nouveau.getId(), nouveau.getNom());
        return siteMapper.versDTO(nouveau);
    }

    @Transactional
    public SiteDTO mettreAJour(Long id, UpdateSiteRequest requete) {
        Site site = chargerOuLever(id);

        if (siteRepository.existsByNomAndIdNot(requete.nom(), id)) {
            throw new IllegalArgumentException("Un autre site utilise déjà ce nom : " + requete.nom());
        }

        siteMapper.mettreAJour(requete, site);
        log.info("Site mis à jour : id={}", id);
        return siteMapper.versDTO(site);
    }

    @Transactional
    public SiteDTO activer(Long id) {
        Site site = chargerOuLever(id);
        site.setActive(true);
        log.info("Site activé : id={}", id);
        return siteMapper.versDTO(site);
    }

    @Transactional
    public SiteDTO desactiver(Long id) {
        Site site = chargerOuLever(id);
        site.setActive(false);
        log.info("Site désactivé : id={}", id);
        return siteMapper.versDTO(site);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Site chargerOuLever(Long id) {
        return siteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Site introuvable : id=" + id));
    }
}