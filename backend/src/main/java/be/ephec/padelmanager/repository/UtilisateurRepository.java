package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    // Recherche d'utilisateurs invitables
    // Recherche LIKE insensible à la casse dans matricule + nom + prénom.
    // Exclut les admins (ADMIN_GLOBAL, ADMIN_SITE) et les comptes inactifs.
    // Pagination par PageRequest.
    // Sécurité anti-scraping : appeler uniquement si q.length() >= 3
    @Query("""
        SELECT u FROM Utilisateur u
        WHERE u.active = true
          AND u.role NOT IN (be.ephec.padelmanager.entity.RoleUtilisateur.ADMIN_GLOBAL,
                              be.ephec.padelmanager.entity.RoleUtilisateur.ADMIN_SITE)
          AND (LOWER(u.matricule) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY u.nom ASC, u.prenom ASC
        """)
    List<Utilisateur> rechercherPourInvitation(@Param("query") String query, Pageable pageable);
}