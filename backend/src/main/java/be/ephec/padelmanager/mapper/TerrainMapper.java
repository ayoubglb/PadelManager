package be.ephec.padelmanager.mapper;

import be.ephec.padelmanager.dto.site.CreateTerrainRequest;
import be.ephec.padelmanager.dto.site.TerrainDTO;
import be.ephec.padelmanager.dto.site.UpdateTerrainRequest;
import be.ephec.padelmanager.entity.Terrain;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;


@Mapper(componentModel = "spring")
public interface TerrainMapper {

    @Mapping(target = "siteId", source = "site.id")
    TerrainDTO versDTO(Terrain terrain);

    List<TerrainDTO> versListeDTOs(List<Terrain> terrains);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "site", ignore = true)
    @Mapping(target = "active", ignore = true)
    Terrain versEntite(CreateTerrainRequest requete);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "site", ignore = true)
    @Mapping(target = "active", ignore = true)
    void mettreAJour(UpdateTerrainRequest requete, @MappingTarget Terrain terrain);
}