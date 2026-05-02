package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByMatricule(String matricule);

    @Query("SELECT u FROM Utilisateur u WHERE u.email = :login OR u.matricule = :login")
    Optional<Utilisateur> findByEmailOrMatricule(@Param("login") String login);

    boolean existsByEmail(String email);

    boolean existsByMatricule(String matricule);

    // Récupère un utilisateur avec son site rattaché chargé en eager (évite LazyInitializationException)
    @Query("SELECT u FROM Utilisateur u LEFT JOIN FETCH u.siteRattachement WHERE u.id = :id")
    Optional<Utilisateur> findByIdWithSite(@Param("id") Long id);
}