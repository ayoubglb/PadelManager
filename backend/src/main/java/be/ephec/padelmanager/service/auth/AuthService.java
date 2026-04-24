package be.ephec.padelmanager.service.auth;

import be.ephec.padelmanager.dto.auth.AuthResponse;
import be.ephec.padelmanager.dto.auth.LoginRequest;
import be.ephec.padelmanager.dto.auth.RegisterRequest;
import be.ephec.padelmanager.entity.RoleUtilisateur;
import be.ephec.padelmanager.entity.Site;
import be.ephec.padelmanager.entity.Utilisateur;
import be.ephec.padelmanager.mapper.AuthMapper;
import be.ephec.padelmanager.repository.UtilisateurRepository;
import be.ephec.padelmanager.security.UtilisateurPrincipal;
import be.ephec.padelmanager.security.jwt.JwtProperties;
import be.ephec.padelmanager.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import be.ephec.padelmanager.repository.SiteRepository;


import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.Set;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {


    private static final Set<RoleUtilisateur> ROLES_INSCRIPTION_PUBLIQUE = EnumSet.of(
            RoleUtilisateur.MEMBRE_LIBRE,
            RoleUtilisateur.MEMBRE_SITE,
            RoleUtilisateur.MEMBRE_GLOBAL
    );


    private static final int NB_TENTATIVES_MAX_MATRICULE = 10;

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthMapper authMapper;
    private final SecureRandom random = new SecureRandom();
    private final SiteRepository siteRepository;

    // ------------------------------------------------------------------
    // Inscription publique
    // ------------------------------------------------------------------

    @Transactional
    public AuthResponse inscrire(RegisterRequest requete) {

        if (!ROLES_INSCRIPTION_PUBLIQUE.contains(requete.role())) {
            throw new IllegalArgumentException(
                    "L'inscription publique ne permet de créer que des comptes membre."
            );
        }


        validerCoherenceSite(requete.role(), requete.siteRattachementId());


        if (utilisateurRepository.existsByEmail(requete.email())) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email.");
        }

        //  Chargement du site si rattachement demandé
        Site site = null;
        if (requete.siteRattachementId() != null) {
            site = siteRepository.findById(requete.siteRattachementId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Site introuvable : id=" + requete.siteRattachementId()
                    ));
        }
        //  Persistance
        Utilisateur nouvel = Utilisateur.builder()
                .matricule(genererMatriculeUnique(requete.role()))
                .nom(requete.nom())
                .prenom(requete.prenom())
                .email(requete.email())
                .telephone(requete.telephone())
                .passwordHash(passwordEncoder.encode(requete.motDePasse()))
                .role(requete.role())
                .siteRattachement(site)                    // ← CHANGÉ (avant: .siteRattachementId(requete.siteRattachementId()))
                .active(true)
                .build();

        nouvel = utilisateurRepository.save(nouvel);
        log.info("Nouvel utilisateur inscrit : matricule={}, role={}", nouvel.getMatricule(), nouvel.getRole());


        String token = jwtService.genererToken(new UtilisateurPrincipal(nouvel));
        return authMapper.versReponse(nouvel, token, jwtProperties.getExpirationMinutes());
    }

    // ------------------------------------------------------------------
    // Connexion
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AuthResponse connecter(LoginRequest requete) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requete.login(), requete.motDePasse())
        );

        UtilisateurPrincipal principal = (UtilisateurPrincipal) auth.getPrincipal();
        String token = jwtService.genererToken(principal);
        return authMapper.versReponse(principal.getUtilisateur(), token, jwtProperties.getExpirationMinutes());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------


    private String genererMatriculeUnique(RoleUtilisateur role) {
        String prefixe = role.getPrefixeMatricule();
        for (int i = 0; i < NB_TENTATIVES_MAX_MATRICULE; i++) {
            int suffixe = random.nextInt(1_000_000);                // 0 à 999 999
            String candidat = String.format("%s%06d", prefixe, suffixe);
            if (!utilisateurRepository.existsByMatricule(candidat)) {
                return candidat;
            }
        }
        throw new IllegalStateException(
                "Impossible de générer un matricule unique après "
                        + NB_TENTATIVES_MAX_MATRICULE + " tentatives (espace saturé ?)."
        );
    }


    private void validerCoherenceSite(RoleUtilisateur role, Long siteId) {
        if (role.exigeSiteRattachement() && siteId == null) {
            throw new IllegalArgumentException(
                    "Le rôle " + role + " exige un siteRattachementId."
            );
        }
        if (!role.exigeSiteRattachement() && siteId != null) {
            throw new IllegalArgumentException(
                    "Le rôle " + role + " ne doit pas avoir de siteRattachementId."
            );
        }
    }


}