package be.ephec.padelmanager.mapper;

import be.ephec.padelmanager.dto.match.MatchDTO;
import be.ephec.padelmanager.entity.Match;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MatchMapper {

    @Mapping(source = "terrain.id",            target = "terrainId")
    @Mapping(source = "terrain.numero",        target = "terrainNumero")
    @Mapping(source = "terrain.site.id",       target = "siteId")
    @Mapping(source = "terrain.site.nom",      target = "siteNom")
    @Mapping(source = "organisateur.id",       target = "organisateurId")
    @Mapping(target = "organisateurNom",       expression = "java(match.getOrganisateur().getPrenom() + \" \" + match.getOrganisateur().getNom())")
    @Mapping(target = "termine",               expression = "java(match.isTermine())")
    MatchDTO toDto(Match match);
}