package be.ephec.padelmanager.mapper;

import be.ephec.padelmanager.dto.site.CreateHoraireSiteRequest;
import be.ephec.padelmanager.dto.site.HoraireSiteDTO;
import be.ephec.padelmanager.entity.HoraireSite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HoraireSiteMapper {

    @Mapping(target = "siteId", source = "site.id")
    HoraireSiteDTO versDTO(HoraireSite horaire);

    List<HoraireSiteDTO> versListeDTOs(List<HoraireSite> horaires);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "site", ignore = true)
    HoraireSite versEntite(CreateHoraireSiteRequest requete);
}