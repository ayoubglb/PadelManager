package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.site.CreateSiteRequest;
import be.ephec.padelmanager.dto.site.SiteDTO;
import be.ephec.padelmanager.dto.site.UpdateSiteRequest;
import be.ephec.padelmanager.entity.Site;
import be.ephec.padelmanager.mapper.SiteMapper;
import be.ephec.padelmanager.repository.SiteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiteService — lecture, création, activation")
class SiteServiceTest {

    @Mock private SiteRepository siteRepository;
    @Mock private SiteMapper siteMapper;

    @InjectMocks
    private SiteService siteService;

    @Test
    @DisplayName("listerActifs() → ne retourne que les sites actifs via findByActiveTrue")
    void listerActifsNeRetourneQueActifs() {
        Site actif = Site.builder().id(1L).nom("Anderlecht").active(true).build();
        when(siteRepository.findByActiveTrue()).thenReturn(List.of(actif));
        when(siteMapper.versListeDTOs(List.of(actif)))
                .thenReturn(List.of(new SiteDTO(1L, "Anderlecht", "", "", "", true)));

        List<SiteDTO> resultat = siteService.listerActifs();

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).active()).isTrue();
        verify(siteRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("Création nom déjà pris → IllegalArgumentException")
    void creerRefuseNomDuplique() {
        CreateSiteRequest req = new CreateSiteRequest("Anderlecht", "Rue X", "1070", "Anderlecht");
        when(siteRepository.existsByNom("Anderlecht")).thenReturn(true);

        assertThatThrownBy(() -> siteService.creer(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe déjà");

        verify(siteRepository, never()).save(any());
    }

    @Test
    @DisplayName("recuperer(id inconnu) → EntityNotFoundException")
    void recupererSiteInconnuLeveException() {
        when(siteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> siteService.recuperer(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("desactiver() passe active à false")
    void desactiverPasseActiveAFalse() {
        Site site = Site.builder().id(1L).nom("Anderlecht").active(true).build();
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(siteMapper.versDTO(site))
                .thenReturn(new SiteDTO(1L, "Anderlecht", "", "", "", false));

        SiteDTO resultat = siteService.desactiver(1L);

        assertThat(site.getActive()).isFalse();
        assertThat(resultat.active()).isFalse();
    }
}