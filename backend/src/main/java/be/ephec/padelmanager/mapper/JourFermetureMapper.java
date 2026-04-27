package be.ephec.padelmanager.mapper;

import be.ephec.padelmanager.dto.site.CreateJourFermetureRequest;
import be.ephec.padelmanager.dto.site.JourFermetureDTO;
import be.ephec.padelmanager.entity.JourFermeture;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JourFermetureMapper {

    @Mapping(target = "siteId", source = "site.id")
    JourFermetureDTO versDTO(JourFermeture jour);

    List<JourFermetureDTO> versListeDTOs(List<JourFermeture> jours);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "site", ignore = true)
    JourFermeture versEntite(CreateJourFermetureRequest requete);
}