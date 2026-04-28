package be.ephec.padelmanager.service;

import be.ephec.padelmanager.config.PricingConstants;
import be.ephec.padelmanager.dto.match.CreateMatchRequest;
import be.ephec.padelmanager.dto.match.MatchDTO;
import be.ephec.padelmanager.entity.*;
import be.ephec.padelmanager.mapper.MatchMapper;
import be.ephec.padelmanager.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Service métier pour la gestion des matchs.
 *
 * <p>La méthode {@link #creerMatch(CreateMatchRequest, Utilisateur)} orchestre toutes les
 * validations CF-RV-002 à CF-RV-011 dans une seule transaction atomique (8.3.5 de l'analyse).
 * Si une seule validation échoue, rollback complet : ni Match, ni InscriptionMatch, ni
 * Transaction ne sont créés.</p>
 *
 * <p>L'ordre des validations privilégie l'échec rapide : on vérifie d'abord les règles
 * statiques (rôle, dates) avant tout accès en base.</p>
 *
 * <p><b>Note :</b> la vérification de pénalité active (CF-RV-010) sera ajoutée au commit 12
 * quand l'entité {@code Penalite} sera créée. Idem pour la vérification jour de fermeture
 * et horaires de site, on suppose les données présentes en base via les seeds 006/007.</p>
 */
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
}