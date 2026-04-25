package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.site.CreateJourFermetureRequest;
import be.ephec.padelmanager.dto.site.JourFermetureDTO;
import be.ephec.padelmanager.entity.JourFermeture;
import be.ephec.padelmanager.entity.Site;
import be.ephec.padelmanager.mapper.JourFermetureMapper;
import be.ephec.padelmanager.repository.JourFermetureRepository;
import be.ephec.padelmanager.repository.SiteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JourFermetureService {

    private final JourFermetureRepository jourFermetureRepository;
    private final SiteRepository siteRepository;
    private final JourFermetureMapper jourFermetureMapper;

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<JourFermetureDTO> listerToutesGlobales() {
        return jourFermetureMapper.versListeDTOs(jourFermetureRepository.findToutesGlobales());
    }

    @Transactional(readOnly = true)
    public List<JourFermetureDTO> listerParSite(Long siteId) {
        if (!siteRepository.existsById(siteId)) {
            throw new EntityNotFoundException("Site introuvable : id=" + siteId);
        }
        return jourFermetureMapper.versListeDTOs(
                jourFermetureRepository.findBySiteIdOrderByDateFermetureAsc(siteId)
        );
    }

    // pour la création de match
    @Transactional(readOnly = true)
    public boolean siteFermeALaDate(Long siteId, LocalDate date) {
        return jourFermetureRepository.estFermeAUneDate(siteId, date);
    }

    // ------------------------------------------------------------------
    // Écriture
    // ------------------------------------------------------------------

    @Transactional
    public JourFermetureDTO creer(CreateJourFermetureRequest requete) {
        Site site = null;
        if (requete.siteId() != null) {
            site = siteRepository.findById(requete.siteId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Site introuvable : id=" + requete.siteId()
                    ));
        }

        // pas deux fermetures pour même (date, site).
        // autorisé : une globale + une site-spécifique à la même date OK.
        verifierUniciteFermeture(requete);

        JourFermeture nouveau = jourFermetureMapper.versEntite(requete);
        nouveau.setSite(site);
        nouveau = jourFermetureRepository.save(nouveau);
        log.info("JourFermeture créé : id={}, date={}, site={}, raison={}",
                nouveau.getId(), nouveau.getDateFermeture(),
                site != null ? site.getId() : "GLOBALE", nouveau.getRaison());
        return jourFermetureMapper.versDTO(nouveau);
    }

    @Transactional
    public void supprimer(Long id) {
        if (!jourFermetureRepository.existsById(id)) {
            throw new EntityNotFoundException("JourFermeture introuvable : id=" + id);
        }
        jourFermetureRepository.deleteById(id);
        log.info("JourFermeture supprimé : id={}", id);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void verifierUniciteFermeture(CreateJourFermetureRequest requete) {
        boolean existe;
        if (requete.siteId() == null) {
            existe = jourFermetureRepository.findGlobaleParDate(requete.dateFermeture()).isPresent();
            if (existe) {
                throw new IllegalArgumentException(
                        "Une fermeture globale existe déjà pour le " + requete.dateFermeture()
                );
            }
        } else {
            existe = jourFermetureRepository
                    .findParSiteEtDate(requete.siteId(), requete.dateFermeture())
                    .isPresent();
            if (existe) {
                throw new IllegalArgumentException(
                        "Une fermeture existe déjà pour le site " + requete.siteId()
                                + " le " + requete.dateFermeture()
                );
            }
        }
    }
}