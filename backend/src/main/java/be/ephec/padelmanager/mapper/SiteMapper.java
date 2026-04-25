package be.ephec.padelmanager.mapper;

import be.ephec.padelmanager.dto.site.CreateSiteRequest;
import be.ephec.padelmanager.dto.site.SiteDTO;
import be.ephec.padelmanager.dto.site.UpdateSiteRequest;
import be.ephec.padelmanager.entity.Site;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SiteMapper {

    SiteDTO versDTO(Site site);

    List<SiteDTO> versListeDTOs(List<Site> sites);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    Site versEntite(CreateSiteRequest requete);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    void mettreAJour(UpdateSiteRequest requete, @MappingTarget Site site);
}