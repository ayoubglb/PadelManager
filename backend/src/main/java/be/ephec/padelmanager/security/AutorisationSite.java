package be.ephec.padelmanager.security;

import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.entity.Utilisateur;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

// helper d'autorisation par ressource pour les actions site-spécifiques

@Component
public class AutorisationSite {

    // Vérifie qu'un utilisateur peut agir sur un site donné.
    public void verifierDroitsSurSite(Utilisateur utilisateur, Long siteId) {
        if (utilisateur == null) {
            throw new AccessDeniedException("Utilisateur non authentifié.");
        }

        // ADMIN_GLOBAL passe partout
        if (utilisateur.getRole() == RoleUtilisateur.ADMIN_GLOBAL) {
            return;
        }

        // ADMIN_SITE doit être rattaché au site ciblé
        if (utilisateur.getRole() == RoleUtilisateur.ADMIN_SITE) {
            if (utilisateur.getSiteRattachement() == null) {
                throw new AccessDeniedException(
                        "Compte ADMIN_SITE incohérent : aucun site de rattachement."
                );
            }
            if (!siteId.equals(utilisateur.getSiteRattachement().getId())) {
                throw new AccessDeniedException(
                        "Vous ne pouvez agir que sur votre site de rattachement."
                );
            }
            return;
        }

        // Tout autre rôle (MEMBRE_*) → ne devrait pas arriver ici grâce au @PreAuthorize.
        // Filet de sécurité défensif si la chaîne d'autorisation est mal configurée.
        throw new AccessDeniedException(
                "Rôle insuffisant pour cette action : " + utilisateur.getRole()
        );
    }
}