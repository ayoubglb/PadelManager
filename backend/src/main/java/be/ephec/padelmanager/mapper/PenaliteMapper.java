package be.ephec.padelmanager.mapper;

import be.ephec.padelmanager.dto.penalite.PenaliteDTO;
import be.ephec.padelmanager.entity.Penalite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Clock;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public abstract class PenaliteMapper {

    // Injecté par Spring pour figer le temps en tests
    protected Clock clock = Clock.systemUTC();

    @Mapping(target = "matchId", source = "match.id")
    @Mapping(target = "active", expression = "java(estActive(penalite))")
    public abstract PenaliteDTO toDto(Penalite penalite);

    @Named("estActive")
    protected boolean estActive(Penalite p) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        return !maintenant.isBefore(p.getDateDebut()) && !maintenant.isAfter(p.getDateFin());
    }
}