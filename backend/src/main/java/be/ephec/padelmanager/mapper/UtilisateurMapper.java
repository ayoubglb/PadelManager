package be.ephec.padelmanager.mapper;

import be.ephec.padelmanager.dto.utilisateur.UtilisateurDTO;
import be.ephec.padelmanager.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UtilisateurMapper {

    // Aplatit la référence siteRattachement en id + nom
    @Mapping(source = "siteRattachement.id",  target = "siteRattachementId")
    @Mapping(source = "siteRattachement.nom", target = "siteRattachementNom")
    UtilisateurDTO toDto(Utilisateur utilisateur);
}