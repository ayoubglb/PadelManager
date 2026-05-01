package be.ephec.padelmanager.mapper;

import be.ephec.padelmanager.dto.inscription.InscriptionMatchDTO;
import be.ephec.padelmanager.entity.InscriptionMatch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface InscriptionMatchMapper {

    // Convertit une InscriptionMatch en DTO en aplatissant joueur et match
    @Mapping(source = "match.id",         target = "matchId")
    @Mapping(source = "joueur.id",        target = "joueurId")
    @Mapping(source = "joueur.matricule", target = "joueurMatricule")
    @Mapping(source = "joueur",           target = "joueurNom", qualifiedByName = "nomComplet")
    InscriptionMatchDTO toDto(InscriptionMatch inscription);

    @Named("nomComplet")
    default String nomComplet(be.ephec.padelmanager.entity.Utilisateur u) {
        return u.getPrenom() + " " + u.getNom();
    }
}