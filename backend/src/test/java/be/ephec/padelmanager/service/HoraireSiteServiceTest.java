package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.site.CreateHoraireSiteRequest;
import be.ephec.padelmanager.dto.site.HoraireSiteDTO;
import be.ephec.padelmanager.entity.HoraireSite;
import be.ephec.padelmanager.entity.Site;
import be.ephec.padelmanager.mapper.HoraireSiteMapper;
import be.ephec.padelmanager.repository.HoraireSiteRepository;
import be.ephec.padelmanager.repository.SiteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HoraireSiteService — création, cohérence temporelle, unicité année")
class HoraireSiteServiceTest {

    @Mock private HoraireSiteRepository horaireSiteRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private HoraireSiteMapper horaireSiteMapper;

    @InjectMocks
    private HoraireSiteService horaireSiteService;

    @Test
    @DisplayName("Création nominale → horaire attaché au bon site")
    void creerNominal() {
        CreateHoraireSiteRequest req = new CreateHoraireSiteRequest(
                2026, LocalTime.of(9, 0), LocalTime.of(22, 0)
        );
        Site site = Site.builder().id(1L).nom("Anderlecht").active(true).build();

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(horaireSiteRepository.existsBySiteIdAndAnnee(1L, 2026)).thenReturn(false);
        when(horaireSiteMapper.versEntite(req))
                .thenReturn(HoraireSite.builder()
                        .annee(2026).heureDebut(LocalTime.of(9, 0)).heureFin(LocalTime.of(22, 0))
                        .build());
        when(horaireSiteRepository.save(any(HoraireSite.class)))
                .thenAnswer(inv -> {
                    HoraireSite h = inv.getArgument(0);
                    h.setId(10L);
                    return h;
                });
        when(horaireSiteMapper.versDTO(any(HoraireSite.class)))
                .thenReturn(new HoraireSiteDTO(10L, 1L, 2026, LocalTime.of(9, 0), LocalTime.of(22, 0)));

        horaireSiteService.creer(1L, req);

        verify(horaireSiteRepository).save(any(HoraireSite.class));
    }

    @Test
    @DisplayName("Création avec heureDebut >= heureFin → IllegalArgumentException")
    void creerRefuseHeuresIncoherentes() {
        CreateHoraireSiteRequest req = new CreateHoraireSiteRequest(
                2026, LocalTime.of(22, 0), LocalTime.of(9, 0)
        );

        assertThatThrownBy(() -> horaireSiteService.creer(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement antérieure");

        verify(horaireSiteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Création avec année déjà prise → IllegalArgumentException")
    void creerRefuseAnneeDuplique() {
        CreateHoraireSiteRequest req = new CreateHoraireSiteRequest(
                2026, LocalTime.of(9, 0), LocalTime.of(22, 0)
        );
        Site site = Site.builder().id(1L).build();

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(horaireSiteRepository.existsBySiteIdAndAnnee(1L, 2026)).thenReturn(true);

        assertThatThrownBy(() -> horaireSiteService.creer(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe déjà");

        verify(horaireSiteRepository, never()).save(any());
    }

    @Test
    @DisplayName("recupererParSiteEtAnnee → EntityNotFoundException si pas d'horaire")
    void recupererInexistantLeveException() {
        when(horaireSiteRepository.findBySiteIdAndAnnee(1L, 2026)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> horaireSiteService.recupererParSiteEtAnnee(1L, 2026))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("2026");
    }
}