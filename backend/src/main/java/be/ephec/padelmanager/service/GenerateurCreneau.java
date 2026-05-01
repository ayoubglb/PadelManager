package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.planning.CreneauDTO;
import be.ephec.padelmanager.entity.HoraireSite;
import be.ephec.padelmanager.repository.HoraireSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// Génère les créneaux théoriques d'un site
@Service
@RequiredArgsConstructor
public class GenerateurCreneau {

    private static final Duration DUREE_MATCH = Duration.ofMinutes(90);
    private static final Duration PAUSE_ENTRE_MATCHS = Duration.ofMinutes(15);

    private final HoraireSiteRepository horaireSiteRepository;

    // Retourne les créneaux possibles pour un site et une année, ou liste vide si aucun horaire défini
    @Transactional(readOnly = true)
    public List<CreneauDTO> genererCreneaux(Long siteId, int annee) {
        return horaireSiteRepository.findBySiteIdAndAnnee(siteId, annee)
                .map(this::genererCreneauxDepuisHoraire)
                .orElseGet(List::of);
    }

    // Itère depuis heureDebut, ajoute des créneaux de 1h30 séparés par 15 min, tant que fin ≤ heureFin
    private List<CreneauDTO> genererCreneauxDepuisHoraire(HoraireSite horaire) {
        List<CreneauDTO> creneaux = new ArrayList<>();
        LocalTime curseur = horaire.getHeureDebut();
        LocalTime borneFin = horaire.getHeureFin();

        while (true) {
            LocalTime finCreneau = curseur.plus(DUREE_MATCH);
            if (finCreneau.isAfter(borneFin)) {
                break;
            }
            creneaux.add(new CreneauDTO(curseur, finCreneau));
            curseur = finCreneau.plus(PAUSE_ENTRE_MATCHS);
        }

        return creneaux;
    }
}