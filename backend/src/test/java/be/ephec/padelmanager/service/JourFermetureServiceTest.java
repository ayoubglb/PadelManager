package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.site.CreateJourFermetureRequest;
import be.ephec.padelmanager.dto.site.JourFermetureDTO;
import be.ephec.padelmanager.entity.JourFermeture;
import be.ephec.padelmanager.entity.Site;
import be.ephec.padelmanager.mapper.JourFermetureMapper;
import be.ephec.padelmanager.repository.JourFermetureRepository;
import be.ephec.padelmanager.repository.SiteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JourFermetureService — globales, site-spécifiques, unicité")
class JourFermetureServiceTest {

    @Mock private JourFermetureRepository jourFermetureRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private JourFermetureMapper jourFermetureMapper;

    @InjectMocks
    private JourFermetureService jourFermetureService;

    private static final LocalDate NOEL_2026 = LocalDate.of(2026, 12, 25);

    @Test
    @DisplayName("Création fermeture globale → site = null sauvegardé")
    void creerFermetureGlobale() {
        CreateJourFermetureRequest req = new CreateJourFermetureRequest(
                NOEL_2026, null, "Noël"
        );

        when(jourFermetureRepository.findGlobaleParDate(NOEL_2026)).thenReturn(Optional.empty());
        when(jourFermetureMapper.versEntite(req))
                .thenReturn(JourFermeture.builder().dateFermeture(NOEL_2026).raison("Noël").build());
        when(jourFermetureRepository.save(any(JourFermeture.class)))
                .thenAnswer(inv -> {
                    JourFermeture j = inv.getArgument(0);
                    j.setId(1L);
                    return j;
                });
        when(jourFermetureMapper.versDTO(any(JourFermeture.class)))
                .thenReturn(new JourFermetureDTO(1L, NOEL_2026, null, "Noël"));

        jourFermetureService.creer(req);

        ArgumentCaptor<JourFermeture> captor = ArgumentCaptor.forClass(JourFermeture.class);
        verify(jourFermetureRepository).save(captor.capture());
        assertThat(captor.getValue().getSite()).isNull();
    }

    @Test
    @DisplayName("Création fermeture site-spécifique → site attaché correctement")
    void creerFermetureSiteSpecifique() {
        CreateJourFermetureRequest req = new CreateJourFermetureRequest(
                NOEL_2026, 1L, "Travaux Anderlecht"
        );
        Site site = Site.builder().id(1L).nom("Anderlecht").build();

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(jourFermetureRepository.findParSiteEtDate(1L, NOEL_2026)).thenReturn(Optional.empty());
        when(jourFermetureMapper.versEntite(req))
                .thenReturn(JourFermeture.builder()
                        .dateFermeture(NOEL_2026).raison("Travaux Anderlecht").build());
        when(jourFermetureRepository.save(any(JourFermeture.class)))
                .thenAnswer(inv -> {
                    JourFermeture j = inv.getArgument(0);
                    j.setId(2L);
                    return j;
                });
        when(jourFermetureMapper.versDTO(any(JourFermeture.class)))
                .thenReturn(new JourFermetureDTO(2L, NOEL_2026, 1L, "Travaux Anderlecht"));

        jourFermetureService.creer(req);

        ArgumentCaptor<JourFermeture> captor = ArgumentCaptor.forClass(JourFermeture.class);
        verify(jourFermetureRepository).save(captor.capture());
        assertThat(captor.getValue().getSite()).isSameAs(site);
    }

    @Test
    @DisplayName("Doublon fermeture globale même date → IllegalArgumentException")
    void creerRefuseDoublonGlobal() {
        CreateJourFermetureRequest req = new CreateJourFermetureRequest(
                NOEL_2026, null, "Noël"
        );
        when(jourFermetureRepository.findGlobaleParDate(NOEL_2026))
                .thenReturn(Optional.of(JourFermeture.builder().dateFermeture(NOEL_2026).build()));

        assertThatThrownBy(() -> jourFermetureService.creer(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("globale");

        verify(jourFermetureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Doublon fermeture site même date → IllegalArgumentException")
    void creerRefuseDoublonSite() {
        CreateJourFermetureRequest req = new CreateJourFermetureRequest(
                NOEL_2026, 1L, "Travaux"
        );
        Site site = Site.builder().id(1L).build();

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(jourFermetureRepository.findParSiteEtDate(1L, NOEL_2026))
                .thenReturn(Optional.of(JourFermeture.builder().build()));

        assertThatThrownBy(() -> jourFermetureService.creer(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe déjà");

        verify(jourFermetureRepository, never()).save(any());
    }

    @Test
    @DisplayName("Création fermeture site avec siteId inconnu → EntityNotFoundException")
    void creerSurSiteInconnu() {
        CreateJourFermetureRequest req = new CreateJourFermetureRequest(
                NOEL_2026, 999L, "Travaux"
        );
        when(siteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jourFermetureService.creer(req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("999");

        verify(jourFermetureRepository, never()).save(any());
    }

    @Test
    @DisplayName("siteFermeALaDate délègue au repository")
    void siteFermeDelegate() {
        when(jourFermetureRepository.estFermeAUneDate(1L, NOEL_2026)).thenReturn(true);

        boolean ferme = jourFermetureService.siteFermeALaDate(1L, NOEL_2026);

        assertThat(ferme).isTrue();
    }
}