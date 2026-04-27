package be.ephec.padelmanager.repository;

import be.ephec.padelmanager.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    // Liste des sites actifs uniquement (pour l'accueil public et les membres).
    List<Site> findByActiveTrue();

    // Recherche par nom (unique, servira pour la validation d'unicité au create/update).
    Optional<Site> findByNom(String nom);

    // Vérification d'unicité lors de la création d'un site.
    boolean existsByNom(String nom);

    // Vérification d'unicité au rename d'un site (exclut l'id courant).
    boolean existsByNomAndIdNot(String nom, Long id);
}