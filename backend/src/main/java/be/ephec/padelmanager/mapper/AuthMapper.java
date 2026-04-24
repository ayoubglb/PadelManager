package be.ephec.padelmanager.mapper;

import be.ephec.padelmanager.dto.auth.AuthResponse;
import be.ephec.padelmanager.entity.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "token",             source = "token")
    @Mapping(target = "expirationMinutes", source = "expirationMinutes")
    @Mapping(target = "matricule",         source = "utilisateur.matricule")
    @Mapping(target = "email",             source = "utilisateur.email")
    @Mapping(target = "nom",               source = "utilisateur.nom")
    @Mapping(target = "prenom",            source = "utilisateur.prenom")
    @Mapping(target = "role",              source = "utilisateur.role")
    AuthResponse versReponse(Utilisateur utilisateur, String token, long expirationMinutes);
}