package be.ephec.padelmanager.service;

import be.ephec.padelmanager.config.PricingConstants;
import be.ephec.padelmanager.dto.inscription.InscriptionMatchDTO;
import be.ephec.padelmanager.dto.inscription.RejoindreMatchResponse;
import be.ephec.padelmanager.dto.match.CreateMatchRequest;
import be.ephec.padelmanager.dto.match.MatchDTO;
import be.ephec.padelmanager.dto.match.MatchPublicDTO;
import be.ephec.padelmanager.dto.transaction.TransactionDTO;
import be.ephec.padelmanager.entity.*;
import be.ephec.padelmanager.mapper.InscriptionMatchMapper;
import be.ephec.padelmanager.mapper.MatchMapper;
import be.ephec.padelmanager.mapper.TransactionMapper;
import be.ephec.padelmanager.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final InscriptionMatchRepository inscriptionMatchRepository;
    private final TransactionRepository transactionRepository;
    private final TerrainRepository terrainRepository;
    private final HoraireSiteRepository horaireSiteRepository;
    private final JourFermetureRepository jourFermetureRepository;
    private final SoldeService soldeService;
    private final MatchMapper matchMapper;
    private final Clock clock;
    private final UtilisateurRepository utilisateurRepository;
    private final InscriptionMatchMapper inscriptionMatchMapper;
    private final TransactionMapper transactionMapper;

    @Transactional
    public MatchDTO creerMatch(CreateMatchRequest requete, Utilisateur organisateur) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        LocalDateTime dateMatch = requete.dateHeureDebut();

        // Validations statiques
        validerOrganisateurActif(organisateur);
        validerDateFuture(dateMatch, maintenant);
        validerDelaiReservation(organisateur, dateMatch, maintenant);

        // Récupération du terrain
        Terrain terrain = terrainRepository.findById(requete.terrainId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Terrain introuvable : id=" + requete.terrainId()));
        if (Boolean.FALSE.equals(terrain.getActive())) {
            throw new IllegalArgumentException(
                    "Le terrain " + terrain.getNumero() + " est désactivé");
        }

        // MEMBRE_SITE limité à son site de rattachement
        validerSiteRattachement(organisateur, terrain);

        // Jour pas fermé
        validerJourNonFerme(terrain.getSite().getId(), dateMatch.toLocalDate());

        // Créneau dans les horaires du site
        validerCreneauDansHoraires(terrain.getSite().getId(), dateMatch);

        // Créneau libre sur ce terrain
        if (matchRepository.existsByTerrainIdAndDateHeureDebut(terrain.getId(), dateMatch)) {
            throw new IllegalArgumentException(
                    "Ce créneau est déjà réservé sur le terrain " + terrain.getNumero());
        }

        // Solde organisateur ≥ 15 €
        if (!soldeService.disposeAuMoinsDe(organisateur.getId(), PricingConstants.PART_JOUEUR)) {
            throw new IllegalArgumentException(
                    "Solde insuffisant pour réserver. Veuillez recharger votre compte.");
        }

        // Création atomique : Match + InscriptionMatch organisateur + Transaction
        Match match = Match.builder()
                .terrain(terrain)
                .dateHeureDebut(dateMatch)
                .dateHeureFin(dateMatch.plus(Match.DUREE))
                .organisateur(organisateur)
                .type(requete.type())
                .statut(StatutMatch.PROGRAMME)
                .devenuPublicAutomatiquement(false)
                .build();
        match = matchRepository.save(match);

        InscriptionMatch inscriptionOrganisateur = InscriptionMatch.builder()
                .match(match)
                .joueur(organisateur)
                .paye(true)
                .statut(StatutInscription.INSCRIT)
                .estOrganisateur(true)
                .build();
        inscriptionMatchRepository.save(inscriptionOrganisateur);

        Transaction transaction = Transaction.builder()
                .utilisateur(organisateur)
                .type(TypeTransaction.PAIEMENT_MATCH)
                .montant(PricingConstants.PART_JOUEUR)
                .match(match)
                .build();
        transactionRepository.save(transaction);

        log.info("Match créé : id={}, organisateur={}, terrain={}, dateHeureDebut={}",
                match.getId(), organisateur.getMatricule(),
                terrain.getNumero(), dateMatch);

        return matchMapper.toDto(match);
    }

    // ─── Méthodes privées de validation ───────────────────────────────────

    private void validerOrganisateurActif(Utilisateur organisateur) {
        if (Boolean.FALSE.equals(organisateur.getActive())) {
            throw new IllegalArgumentException("Compte utilisateur désactivé");
        }
    }

    private void validerDateFuture(LocalDateTime dateMatch, LocalDateTime maintenant) {
        if (!dateMatch.isAfter(maintenant)) {
            throw new IllegalArgumentException(
                    "La date du match doit être strictement dans le futur");
        }
    }

    private void validerDelaiReservation(Utilisateur organisateur, LocalDateTime dateMatch,
                                         LocalDateTime maintenant) {
        int delaiMaxJours = switch (organisateur.getRole()) {
            case MEMBRE_LIBRE -> 5;
            case MEMBRE_SITE -> 14;
            case MEMBRE_GLOBAL -> 21;
            // Les administrateurs sont autorisés sans délai (cas non documenté
            // explicitement, mais cohérent avec leur rôle de gestion)
            case ADMIN_SITE, ADMIN_GLOBAL -> Integer.MAX_VALUE;
        };

        LocalDateTime limite = maintenant.plusDays(delaiMaxJours);
        if (dateMatch.isAfter(limite)) {
            throw new IllegalArgumentException(
                    "Vous ne pouvez réserver que jusqu'à " + delaiMaxJours
                            + " jours à l'avance avec votre abonnement");
        }
    }

    private void validerSiteRattachement(Utilisateur organisateur, Terrain terrain) {
        if (organisateur.getRole() != RoleUtilisateur.MEMBRE_SITE) {
            return;
        }
        Site siteRattachement = organisateur.getSiteRattachement();
        if (siteRattachement == null
                || !siteRattachement.getId().equals(terrain.getSite().getId())) {
            throw new IllegalArgumentException(
                    "En tant que Membre Site, vous ne pouvez réserver que sur votre site de rattachement");
        }
    }


    private void validerJourNonFerme(Long siteId, LocalDate date) {
        boolean ferme = jourFermetureRepository.existsByDateAndSiteIdOrSiteIsNull(date, siteId);
        if (ferme) {
            throw new IllegalArgumentException(
                    "Le site est fermé le " + date);
        }
    }


    private void validerCreneauDansHoraires(Long siteId, LocalDateTime dateMatch) {
        int annee = dateMatch.getYear();
        HoraireSite horaire = horaireSiteRepository
                .findBySiteIdAndAnnee(siteId, annee)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun horaire défini pour ce site en " + annee
                                + ". Veuillez contacter l'administrateur."));

        LocalTime heureDebut = dateMatch.toLocalTime();
        LocalDateTime fin = dateMatch.plus(Match.DUREE);

        // Vérifie que début ET fin du match sont dans les horaires du même jour
        boolean finJourSuivant = !fin.toLocalDate().equals(dateMatch.toLocalDate());
        boolean debutAvantOuverture = heureDebut.isBefore(horaire.getHeureDebut());
        boolean finApresFermeture = fin.toLocalTime().isAfter(horaire.getHeureFin());

        if (finJourSuivant || debutAvantOuverture || finApresFermeture) {
            throw new IllegalArgumentException(
                    "Le créneau " + heureDebut + " - " + fin.toLocalTime()
                            + " est en dehors des horaires d'ouverture du site ("
                            + horaire.getHeureDebut() + " - " + horaire.getHeureFin() + ")");
        }
    }


    // ------------------------------------------------------------------------------
    // Invite un joueur à un match privé
    @Transactional
    public InscriptionMatchDTO inviterJoueur(Long matchId, String matriculeJoueur, Utilisateur organisateur) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match introuvable : " + matchId));

        validerOrganisateur(match, organisateur);
        validerMatchPriveProgramme(match);
        validerPlacesRestantes(match);

        Utilisateur joueur = utilisateurRepository.findByMatricule(matriculeJoueur)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun utilisateur avec le matricule " + matriculeJoueur));

        validerJoueurActif(joueur);
        validerPasDejaInscrit(match, joueur);

        InscriptionMatch inscription = InscriptionMatch.builder()
                .match(match)
                .joueur(joueur)
                .paye(false)
                .statut(StatutInscription.INSCRIT)
                .estOrganisateur(false)
                .build();

        InscriptionMatch enregistree = inscriptionMatchRepository.save(inscription);

        log.info("Joueur invité : match={}, joueur={}, organisateur={}",
                match.getId(), joueur.getMatricule(), organisateur.getMatricule());

        return inscriptionMatchMapper.toDto(enregistree);
    }

    // Refuse si l'utilisateur authentifié n'est pas l'organisateur du match
    private void validerOrganisateur(Match match, Utilisateur utilisateur) {
        if (!match.getOrganisateur().getId().equals(utilisateur.getId())) {
            throw new AccessDeniedException(
                    "Seul l'organisateur du match peut inviter des joueurs");
        }
    }

    // Refuse si le match n'est pas privé ou pas programmé
    private void validerMatchPriveProgramme(Match match) {
        if (match.getType() != TypeMatch.PRIVE) {
            throw new IllegalArgumentException(
                    "Les invitations ne sont possibles que sur un match privé");
        }
        if (match.getStatut() != StatutMatch.PROGRAMME) {
            throw new IllegalArgumentException(
                    "Les invitations ne sont possibles que sur un match programmé");
        }
    }

    // Refuse si le match a déjà 4 joueurs (factorisée pour invitation et rejoindre)
    private void validerPlacesRestantes(Match match) {
        long inscrits = inscriptionMatchRepository.findInscritsByMatchId(match.getId()).size();
        if (inscrits >= PricingConstants.NB_JOUEURS_MAX) {
            throw new IllegalArgumentException(
                    "Le match est complet (4 joueurs)");
        }
    }

    // Refuse si le joueur cible est désactivé
    private void validerJoueurActif(Utilisateur joueur) {
        if (Boolean.FALSE.equals(joueur.getActive())) {
            throw new IllegalArgumentException(
                    "Le joueur " + joueur.getMatricule() + " est désactivé");
        }
    }

    // Refuse si le joueur est déjà inscrit au match
    private void validerPasDejaInscrit(Match match, Utilisateur joueur) {
        if (inscriptionMatchRepository.existsByMatchIdAndJoueurId(match.getId(), joueur.getId())) {
            throw new IllegalArgumentException(
                    "Le joueur " + joueur.getMatricule() + " est déjà inscrit à ce match");
        }
    }

    // ----------------------------------------------------------------------------------------
    // Paye sa part d'un match privé auquel on a été invité
    @Transactional
    public TransactionDTO payerSaPart(Long matchId, Utilisateur joueur) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match introuvable : " + matchId));

        validerMatchPriveProgrammePourPaiement(match);

        InscriptionMatch inscription = inscriptionMatchRepository
                .findByMatchIdAndJoueurId(matchId, joueur.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vous n'êtes pas inscrit à ce match"));

        validerInscriptionPayable(inscription);
        validerSoldeSuffisant(joueur);

        // Création atomique : Transaction PAIEMENT_MATCH + maj inscription.paye
        Transaction transaction = Transaction.builder()
                .utilisateur(joueur)
                .type(TypeTransaction.PAIEMENT_MATCH)
                .montant(PricingConstants.PART_JOUEUR)
                .match(match)
                .build();
        transaction = transactionRepository.save(transaction);

        inscription.setPaye(true);
        inscriptionMatchRepository.save(inscription);

        log.info("Part payée : match={}, joueur={}, montant={}€",
                match.getId(), joueur.getMatricule(), PricingConstants.PART_JOUEUR);

        return transactionMapper.toDto(transaction);
    }

    // Refuse si le match n'est pas privé ou pas programmé
    private void validerMatchPriveProgrammePourPaiement(Match match) {
        if (match.getType() != TypeMatch.PRIVE) {
            throw new IllegalArgumentException(
                    "Le paiement de part s'applique uniquement aux matchs privés");
        }
        if (match.getStatut() != StatutMatch.PROGRAMME) {
            throw new IllegalArgumentException(
                    "Le paiement n'est possible que sur un match programmé");
        }
    }

    // Refuse si l'organisateur tente de payer (déjà payé à la création) ou si déjà payé
    private void validerInscriptionPayable(InscriptionMatch inscription) {
        if (Boolean.TRUE.equals(inscription.getEstOrganisateur())) {
            throw new IllegalArgumentException(
                    "L'organisateur a déjà payé sa part lors de la création du match");
        }
        if (Boolean.TRUE.equals(inscription.getPaye())) {
            throw new IllegalArgumentException(
                    "Vous avez déjà payé votre part pour ce match");
        }
    }

    // Refuse si le solde est insuffisant pour 15€
    private void validerSoldeSuffisant(Utilisateur joueur) {
        if (!soldeService.disposeAuMoinsDe(joueur.getId(), PricingConstants.PART_JOUEUR)) {
            throw new IllegalArgumentException(
                    "Solde insuffisant pour payer votre part. Veuillez recharger votre compte.");
        }
    }

    // -------------------------------------------
    // Recherche les matchs publics avec filtres pour le catalogue
    @Transactional(readOnly = true)
    public List<MatchPublicDTO> rechercherMatchsPublics(Long siteId,
                                                        LocalDate dateDebut,
                                                        LocalDate dateFin,
                                                        Integer placesMin) {
        LocalDateTime debut = dateDebut != null
                ? dateDebut.atStartOfDay()
                : LocalDateTime.now(clock);
        LocalDateTime fin = dateFin != null ? dateFin.atTime(LocalTime.MAX) : null;
        int placesMinEffectif = placesMin != null ? placesMin : 1;

        List<Match> matchs = matchRepository.rechercherPublics(debut, fin, siteId);
        if (matchs.isEmpty()) {
            return List.of();
        }

        // Compte les joueurs payés par match (anti N+1)
        List<Long> matchIds = matchs.stream().map(Match::getId).toList();
        Map<Long, Integer> joueursPayesParMatch = inscriptionMatchRepository
                .countJoueursPayesByMatchIdIn(matchIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()));

        return matchs.stream()
                .map(m -> {
                    int payes = joueursPayesParMatch.getOrDefault(m.getId(), 0);
                    int placesRestantes = PricingConstants.NB_JOUEURS_MAX - payes;
                    return new MatchPublicDTO(
                            m.getId(),
                            m.getTerrain().getSite().getId(),
                            m.getTerrain().getSite().getNom(),
                            m.getTerrain().getNumero(),
                            m.getDateHeureDebut(),
                            m.getDateHeureFin(),
                            m.getOrganisateur().getPrenom() + " " + m.getOrganisateur().getNom(),
                            placesRestantes
                    );
                })
                .filter(dto -> dto.placesRestantes() >= placesMinEffectif)
                .toList();
    }

    // ------------------------------------------------------------------
    // Rejoint un match public en payant sa part 15€
    // Utilise un verrou pessimiste sur le match pour gérer la concurrence
    // (premier payé, premier servi).
    @Transactional
    public RejoindreMatchResponse rejoindreMatchPublic(Long matchId, Utilisateur joueur) {
        // Verrou pessimiste : bloque toute autre transaction sur ce match
        Match match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match introuvable : " + matchId));

        validerMatchPublicProgramme(match);
        validerNonOrganisateur(match, joueur);
        validerNonDejaInscrit(match, joueur);
        validerPlacesRestantes(match);
        validerSoldeSuffisantPourRejoindre(joueur);

        // Création atomique : InscriptionMatch + Transaction PAIEMENT_MATCH
        InscriptionMatch inscription = InscriptionMatch.builder()
                .match(match)
                .joueur(joueur)
                .paye(true)
                .statut(StatutInscription.INSCRIT)
                .estOrganisateur(false)
                .build();
        inscription = inscriptionMatchRepository.save(inscription);

        Transaction transaction = Transaction.builder()
                .utilisateur(joueur)
                .type(TypeTransaction.PAIEMENT_MATCH)
                .montant(PricingConstants.PART_JOUEUR)
                .match(match)
                .build();
        transaction = transactionRepository.save(transaction);

        log.info("Match public rejoint : match={}, joueur={}, montant={}€",
                match.getId(), joueur.getMatricule(), PricingConstants.PART_JOUEUR);

        return new RejoindreMatchResponse(
                inscriptionMatchMapper.toDto(inscription),
                transactionMapper.toDto(transaction));
    }

    // Refuse si le match n'est pas public ou pas programmé
    private void validerMatchPublicProgramme(Match match) {
        if (match.getType() != TypeMatch.PUBLIC) {
            throw new IllegalArgumentException(
                    "Cet endpoint est réservé aux matchs publics");
        }
        if (match.getStatut() != StatutMatch.PROGRAMME) {
            throw new IllegalArgumentException(
                    "Le match n'est pas ouvert aux inscriptions");
        }
    }

    // Refuse si le user est l'organisateur (déjà inscrit nativement)
    private void validerNonOrganisateur(Match match, Utilisateur joueur) {
        if (match.getOrganisateur().getId().equals(joueur.getId())) {
            throw new IllegalArgumentException(
                    "Vous êtes l'organisateur de ce match, vous y êtes déjà inscrit");
        }
    }

    // Refuse si le user a déjà une inscription pour ce match
    private void validerNonDejaInscrit(Match match, Utilisateur joueur) {
        if (inscriptionMatchRepository.existsByMatchIdAndJoueurId(match.getId(), joueur.getId())) {
            throw new IllegalArgumentException(
                    "Vous êtes déjà inscrit à ce match");
        }
    }

    // Refuse si solde < 15€
    private void validerSoldeSuffisantPourRejoindre(Utilisateur joueur) {
        if (!soldeService.disposeAuMoinsDe(joueur.getId(), PricingConstants.PART_JOUEUR)) {
            throw new IllegalArgumentException(
                    "Solde insuffisant pour rejoindre. Veuillez recharger votre compte.");
        }
    }

}