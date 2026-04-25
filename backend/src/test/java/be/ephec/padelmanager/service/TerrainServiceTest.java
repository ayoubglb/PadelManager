package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.site.CreateTerrainRequest;
import be.ephec.padelmanager.dto.site.TerrainDTO;
import be.ephec.padelmanager.entity.Site;
import be.ephec.padelmanager.entity.Terrain;
import be.ephec.padelmanager.mapper.TerrainMapper;
import be.ephec.padelmanager.repository.SiteRepository;
import be.ephec.padelmanager.repository.TerrainRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("TerrainService — création, lecture, unicité numéro/site")
class TerrainServiceTest {

    @Mock private TerrainRepository terrainRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private TerrainMapper terrainMapper;

    @InjectMocks
    private TerrainService terrainService;

    @Test
    @DisplayName("Création nominale → terrain attaché au bon site")
    void creerAttacheLeBonSite() {
        Site site = Site.builder().id(1L).nom("Anderlecht").active(true).build();
        CreateTerrainRequest req = new CreateTerrainRequest(3, "Court Central");

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(terrainRepository.existsBySiteIdAndNumero(1L, 3)).thenReturn(false);
        when(terrainMapper.versEntite(req))
                .thenReturn(Terrain.builder().numero(3).nom("Court Central").build());
        when(terrainRepository.save(any(Terrain.class)))
                .thenAnswer(inv -> {
                    Terrain t = inv.getArgument(0);
                    t.setId(42L);
                    return t;
                });
        when(terrainMapper.versDTO(any(Terrain.class)))
                .thenReturn(new TerrainDTO(42L, 3, "Court Central", 1L, true));

        TerrainDTO resultat = terrainService.creer(1L, req);

        ArgumentCaptor<Terrain> captor = ArgumentCaptor.forClass(Terrain.class);
        verify(terrainRepository).save(captor.capture());
        Terrain sauve = captor.getValue();

        assertThat(sauve.getSite()).isSameAs(site);
        assertThat(sauve.getNumero()).isEqualTo(3);
        assertThat(resultat.id()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Création terrain numéro déjà pris → IllegalArgumentException (CF-RS-006)")
    void creerRefuseNumeroDuplique() {
        Site site = Site.builder().id(1L).build();
        CreateTerrainRequest req = new CreateTerrainRequest(1, null);

        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(terrainRepository.existsBySiteIdAndNumero(1L, 1)).thenReturn(true);

        assertThatThrownBy(() -> terrainService.creer(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe déjà");

        verify(terrainRepository, never()).save(any());
    }

    @Test
    @DisplayName("Création terrain sur site inexistant → EntityNotFoundException")
    void creerSurSiteInconnu() {
        when(siteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> terrainService.creer(999L, new CreateTerrainRequest(1, null)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Site introuvable");
    }

    @Test
    @DisplayName("listerActifsParSite() → utilise findBySiteIdAndActiveTrueOrderByNumeroAsc")
    void listerActifsUtiliseLeBonRepository() {
        when(siteRepository.existsById(1L)).thenReturn(true);
        when(terrainRepository.findBySiteIdAndActiveTrueOrderByNumeroAsc(1L))
                .thenReturn(List.of());
        when(terrainMapper.versListeDTOs(List.of())).thenReturn(List.of());

        terrainService.listerActifsParSite(1L);

        verify(terrainRepository).findBySiteIdAndActiveTrueOrderByNumeroAsc(1L);
    }

    @Test
    @DisplayName("desactiver() passe active à false")
    void desactiverPasseActiveAFalse() {
        Site site = Site.builder().id(1L).build();
        Terrain terrain = Terrain.builder().id(5L).numero(2).site(site).active(true).build();

        when(terrainRepository.findById(5L)).thenReturn(Optional.of(terrain));
        when(terrainMapper.versDTO(terrain))
                .thenReturn(new TerrainDTO(5L, 2, null, 1L, false));

        terrainService.desactiver(5L);

        assertThat(terrain.getActive()).isFalse();
    }
}