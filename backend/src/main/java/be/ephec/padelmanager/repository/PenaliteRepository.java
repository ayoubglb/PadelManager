package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.Penalite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PenaliteRepository extends JpaRepository<Penalite, Long> {

    // Récupère la pénalité active d'un utilisateur à un instant donné
     //  Une pénalité est active si maintenant inclus dans [dateDebut, dateFin]
    @Query("""
        SELECT p FROM Penalite p
        WHERE p.utilisateur.id = :utilisateurId
          AND :maintenant BETWEEN p.dateDebut AND p.dateFin
        """)
    Optional<Penalite> findActiveByUtilisateurId(@Param("utilisateurId") Long utilisateurId,
                                                 @Param("maintenant") LocalDateTime maintenant);
}