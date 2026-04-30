package be.ephec.padelmanager.mapper;

import be.ephec.padelmanager.dto.transaction.TransactionDTO;
import be.ephec.padelmanager.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "utilisateur.id", target = "utilisateurId")
    @Mapping(source = "match.id",       target = "matchId")
    TransactionDTO toDto(Transaction transaction);
}