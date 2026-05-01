package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.planning.CreneauDTO;
import be.ephec.padelmanager.entity.HoraireSite;
import be.ephec.padelmanager.entity.Site;
import be.ephec.padelmanager.repository.HoraireSiteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Tests unitaires Mockito de GenerateurCreneau
@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateurCreneau — génération des créneaux")
class GenerateurCreneauTest {

    @Mock private HoraireSiteRepository horaireSiteRepository;

    @InjectMocks
    private GenerateurCreneau generateurCreneau;

    private HoraireSite horairePour(LocalTime debut, LocalTime fin) {
        return HoraireSite.builder()
                .id(1L)
                .site(Site.builder().id(10L).build())
                .annee(2026)
                .heureDebut(debut)
                .heureFin(fin)
                .build();
    }

    @Test
    @DisplayName("Horaire 08:00-22:00 → 8 créneaux selon CF-RC-005 (durée 1h30 + pause 15 min)")
    void genererCreneauxStandard() {
        when(horaireSiteRepository.findBySiteIdAndAnnee(10L, 2026))
                .thenReturn(Optional.of(horairePour(LocalTime.of(8, 0), LocalTime.of(22, 0))));

        List<CreneauDTO> creneaux = generateurCreneau.genererCreneaux(10L, 2026);

        assertThat(creneaux).containsExactly(
                new CreneauDTO(LocalTime.of(8, 0),  LocalTime.of(9, 30)),
                new CreneauDTO(LocalTime.of(9, 45), LocalTime.of(11, 15)),
                new CreneauDTO(LocalTime.of(11, 30), LocalTime.of(13, 0)),
                new CreneauDTO(LocalTime.of(13, 15), LocalTime.of(14, 45)),
                new CreneauDTO(LocalTime.of(15, 0), LocalTime.of(16, 30)),
                new CreneauDTO(LocalTime.of(16, 45), LocalTime.of(18, 15)),
                new CreneauDTO(LocalTime.of(18, 30), LocalTime.of(20, 0)),
                new CreneauDTO(LocalTime.of(20, 15), LocalTime.of(21, 45))
        );
    }

    @Test
    @DisplayName("Horaire 08:00-21:45 → 8 créneaux, dernier finit exactement à 21:45 (borne incluse)")
    void genererCreneauxDernierExactementSurLaBorne() {
        when(horaireSiteRepository.findBySiteIdAndAnnee(10L, 2026))
                .thenReturn(Optional.of(horairePour(LocalTime.of(8, 0), LocalTime.of(21, 45))));

        List<CreneauDTO> creneaux = generateurCreneau.genererCreneaux(10L, 2026);

        assertThat(creneaux).hasSize(8);
        assertThat(creneaux.get(creneaux.size() - 1))
                .isEqualTo(new CreneauDTO(LocalTime.of(20, 15), LocalTime.of(21, 45)));
    }

    @Test
    @DisplayName("Horaire trop court (08:00-09:29) → aucun créneau (1h30 ne tient pas)")
    void genererCreneauxHoraireTropCourt() {
        when(horaireSiteRepository.findBySiteIdAndAnnee(10L, 2026))
                .thenReturn(Optional.of(horairePour(LocalTime.of(8, 0), LocalTime.of(9, 29))));

        List<CreneauDTO> creneaux = generateurCreneau.genererCreneaux(10L, 2026);

        assertThat(creneaux).isEmpty();
    }

    @Test
    @DisplayName("Horaire pile 1h30 (08:00-09:30) → exactement 1 créneau")
    void genererCreneauxHorairePileUnCreneau() {
        when(horaireSiteRepository.findBySiteIdAndAnnee(10L, 2026))
                .thenReturn(Optional.of(horairePour(LocalTime.of(8, 0), LocalTime.of(9, 30))));

        List<CreneauDTO> creneaux = generateurCreneau.genererCreneaux(10L, 2026);

        assertThat(creneaux).containsExactly(
                new CreneauDTO(LocalTime.of(8, 0), LocalTime.of(9, 30))
        );
    }

    @Test
    @DisplayName("Aucun horaire défini pour l'année → liste vide")
    void genererCreneauxAucunHoraireRetourneListeVide() {
        when(horaireSiteRepository.findBySiteIdAndAnnee(10L, 2026))
                .thenReturn(Optional.empty());

        List<CreneauDTO> creneaux = generateurCreneau.genererCreneaux(10L, 2026);

        assertThat(creneaux).isEmpty();
    }

    @Test
    @DisplayName("Horaire avec heureFin = heureDebut + 1h45 → 1 créneau (pas de second car la pause + 1h30 dépassent)")
    void genererCreneauxHoraireCourtEvitelaPause() {
        when(horaireSiteRepository.findBySiteIdAndAnnee(10L, 2026))
                .thenReturn(Optional.of(horairePour(LocalTime.of(8, 0), LocalTime.of(9, 45))));

        List<CreneauDTO> creneaux = generateurCreneau.genererCreneaux(10L, 2026);

        assertThat(creneaux).containsExactly(
                new CreneauDTO(LocalTime.of(8, 0), LocalTime.of(9, 30))
        );
    }
}