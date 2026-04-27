package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.site.CreateHoraireSiteRequest;
import be.ephec.padelmanager.dto.site.HoraireSiteDTO;
import be.ephec.padelmanager.entity.HoraireSite;
import be.ephec.padelmanager.entity.Site;
import be.ephec.padelmanager.mapper.HoraireSiteMapper;
import be.ephec.padelmanager.repository.HoraireSiteRepository;
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
public class HoraireSiteService {

    private final HoraireSiteRepository horaireSiteRepository;
    private final SiteRepository siteRepository;
    private final HoraireSiteMapper horaireSiteMapper;

    @Transactional(readOnly = true)
    public List<HoraireSiteDTO> listerParSite(Long siteId) {
        verifierSiteExiste(siteId);
        return horaireSiteMapper.versListeDTOs(
                horaireSiteRepository.findBySiteIdOrderByAnneeDesc(siteId)
        );
    }

    @Transactional(readOnly = true)
    public HoraireSiteDTO recupererParSiteEtAnnee(Long siteId, Integer annee) {
        return horaireSiteRepository.findBySiteIdAndAnnee(siteId, annee)
                .map(horaireSiteMapper::versDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun horaire pour le site " + siteId + " en " + annee
                ));
    }

    @Transactional
    public HoraireSiteDTO creer(Long siteId, CreateHoraireSiteRequest requete) {
        // Cohérence temporelle
        if (!requete.heureDebut().isBefore(requete.heureFin())) {
            throw new IllegalArgumentException(
                    "L'heure de début doit être strictement antérieure à l'heure de fin."
            );
        }

        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new EntityNotFoundException("Site introuvable : id=" + siteId));

        // un seul horaire par (site, année)
        if (horaireSiteRepository.existsBySiteIdAndAnnee(siteId, requete.annee())) {
            throw new IllegalArgumentException(
                    "Un horaire existe déjà pour le site " + siteId + " en " + requete.annee()
            );
        }

        HoraireSite nouveau = horaireSiteMapper.versEntite(requete);
        nouveau.setSite(site);
        nouveau = horaireSiteRepository.save(nouveau);
        log.info("HoraireSite créé : id={}, site={}, annee={}",
                nouveau.getId(), siteId, nouveau.getAnnee());
        return horaireSiteMapper.versDTO(nouveau);
    }

    @Transactional
    public void supprimer(Long id) {
        if (!horaireSiteRepository.existsById(id)) {
            throw new EntityNotFoundException("HoraireSite introuvable : id=" + id);
        }
        horaireSiteRepository.deleteById(id);
        log.info("HoraireSite supprimé : id={}", id);
    }

    private void verifierSiteExiste(Long siteId) {
        if (!siteRepository.existsById(siteId)) {
            throw new EntityNotFoundException("Site introuvable : id=" + siteId);
        }
    }
}