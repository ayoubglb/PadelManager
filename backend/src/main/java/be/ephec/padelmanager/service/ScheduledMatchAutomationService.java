package be.ephec.padelmanager.service;

import be.ephec.padelmanager.config.PricingConstants;
import be.ephec.padelmanager.entity.Match;
import be.ephec.padelmanager.entity.Penalite;
import be.ephec.padelmanager.entity.StatutMatch;
import be.ephec.padelmanager.entity.TypeMatch;
import be.ephec.padelmanager.entity.Transaction;
import be.ephec.padelmanager.entity.TypeTransaction;
import be.ephec.padelmanager.repository.PenaliteRepository;
import be.ephec.padelmanager.repository.InscriptionMatchRepository;
import be.ephec.padelmanager.repository.MatchRepository;
import be.ephec.padelmanager.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

// Service d'automatisation des matchs
// Le job @Scheduled orchestre les traitements automatiques 24h avant chaque match
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledMatchAutomationService {

    private final MatchRepository matchRepository;
    private final InscriptionMatchRepository inscriptionMatchRepository;
    private final PenaliteRepository penaliteRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    // Job orchestrateur : exécute les 3 traitements dans l'ordre toutes les 15 min.
    // L'ordre est important : libération d'abord (peut affecter "complet/incomplet"),
    // puis conversion privé→public, puis facturation organisateur
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void executerJobsAutomatisation() {
        log.info("Démarrage cycle d'automatisation des matchs");
        libererPlacesNonPayees();
        convertirPrivesIncomplets();
        facturerOrganisateursPublicsIncomplets();
        log.info("Fin cycle d'automatisation des matchs");
    }

    // Libère les places non payées 24h avant un match.
    // Marque les inscriptions paye=false comme LIBERE_NON_PAIEMENT pour conserver
    // l'historique tout en libérant la place pour d'autres joueurs. */
    @Transactional
    public void libererPlacesNonPayees() {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        LocalDateTime limite24h = maintenant.plusHours(24);

        List<Match> matchsAEcheance = matchRepository.findMatchsAEcheance24h(maintenant, limite24h);

        if (matchsAEcheance.isEmpty()) {
            log.debug("Aucun match à échéance 24h, libération non nécessaire");
            return;
        }

        int totalLiberees = 0;
        for (Match match : matchsAEcheance) {
            int liberees = inscriptionMatchRepository.marquerLibereesNonPayees(match.getId());
            if (liberees > 0) {
                log.info("Match id={} : {} inscription(s) non payée(s) marquée(s) LIBERE_NON_PAIEMENT",
                        match.getId(), liberees);
                totalLiberees += liberees;
            }
        }
        log.info("SYS : {} inscription(s) libérée(s) sur {} match(s)",
                totalLiberees, matchsAEcheance.size());
    }

    // Convertit les matchs PRIVÉS incomplets en PUBLIC 24h avant + pénalité organisateur.
    // Si un match privé n'est pas plein (moins de 4 joueurs payés) à T-24h, on le rend public
    // pour que d'autres joueurs puissent rejoindre. L'organisateur reçoit une pénalité d'1 semaine.
    // Idempotence :
    //   - Un match déjà PUBLIC ne sera pas retraité (filtre type=PRIVE).
    //   - La UK partial sur penalite.match_id empêche la création de doublons */
    @Transactional
    public void convertirPrivesIncomplets() {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        LocalDateTime limite24h = maintenant.plusHours(24);

        // Récupère les matchs PROGRAMME à échéance, on filtre type=PRIVE et incomplets en Java
        List<Match> matchsAEcheance = matchRepository.findMatchsAEcheance24h(maintenant, limite24h);

        if (matchsAEcheance.isEmpty()) {
            log.debug("Aucun match à échéance 24h, conversion non nécessaire");
            return;
        }

        int totalConvertis = 0;
        for (Match match : matchsAEcheance) {
            if (match.getType() != TypeMatch.PRIVE) {
                continue;  // Match déjà PUBLIC ou autre type
            }
            long joueursPayes = inscriptionMatchRepository.countJoueursPayesByMatchId(match.getId());
            if (joueursPayes >= 4) {
                continue;  // Match complet, rien à faire
            }

            // Conversion en PUBLIC
            match.setType(TypeMatch.PUBLIC);
            match.setDevenuPublicAutomatiquement(true);
            matchRepository.save(match);

            // Pénalité d'1 semaine pour l'organisateur
            Penalite penalite = Penalite.builder()
                    .utilisateur(match.getOrganisateur())
                    .dateDebut(maintenant)
                    .dateFin(maintenant.plusWeeks(1))
                    .motif("CONVERSION_AUTO_PRIVE_PUBLIC")
                    .match(match)
                    .build();
            penaliteRepository.save(penalite);

            log.info("Match id={} converti PRIVE→PUBLIC ({} joueur(s) payé(s) sur 4) - "
                            + "pénalité 1 semaine pour organisateur {}",
                    match.getId(), joueursPayes, match.getOrganisateur().getMatricule());
            totalConvertis++;
        }
        log.info("SYS : {} match(s) converti(s) PRIVE→PUBLIC sur {} match(s) à échéance",
                totalConvertis, matchsAEcheance.size());
    }


    // Facture les organisateurs des matchs publics incomplets 24h avant le match.
    // Pour chaque match PUBLIC en statut PROGRAMME à T-24h avec moins de 4 joueurs payés,
    // on crée une transaction SOLDE_DU_ORGANISATEUR de 15€ × (places vides).
    // Idempotence : la UK partial sur transaction(match_id) WHERE type='SOLDE_DU_ORGANISATEUR'
    // garantit qu'une seule dette existe par match. On vérifie d'abord avec
    // existsSoldeDuOrganisateurForMatch pour éviter de provoquer l'exception SQL inutilement
    @Transactional
    public void facturerOrganisateursPublicsIncomplets() {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        LocalDateTime limite24h = maintenant.plusHours(24);

        List<Match> matchsAEcheance = matchRepository.findMatchsAEcheance24h(maintenant, limite24h);

        if (matchsAEcheance.isEmpty()) {
            log.debug("Aucun match à échéance 24h, facturation non nécessaire");
            return;
        }

        int totalFactures = 0;
        for (Match match : matchsAEcheance) {
            if (match.getType() != TypeMatch.PUBLIC) {
                continue;  // On ne facture que les matchs PUBLIC
            }
            long joueursPayes = inscriptionMatchRepository.countJoueursPayesByMatchId(match.getId());
            if (joueursPayes >= 4) {
                continue;  // Match complet, rien à facturer
            }

            // Idempotence : on ne crée pas une 2ème dette si elle existe déjà
            if (transactionRepository.existsSoldeDuOrganisateurForMatch(match.getId())) {
                continue;
            }

            int placesVides = 4 - (int) joueursPayes;
            BigDecimal montantDette = PricingConstants.PART_JOUEUR
                    .multiply(BigDecimal.valueOf(placesVides));

            Transaction dette = Transaction.builder()
                    .utilisateur(match.getOrganisateur())
                    .type(TypeTransaction.SOLDE_DU_ORGANISATEUR)
                    .montant(montantDette)
                    .match(match)
                    .build();
            transactionRepository.save(dette);

            log.info("SYS : Match id={} ({}/4 payés) → facturation organisateur {} : {}€",
                    match.getId(), joueursPayes,
                    match.getOrganisateur().getMatricule(), montantDette);
            totalFactures++;
        }
        log.info("SYS : {} match(s) facturé(s) sur {} match(s) à échéance",
                totalFactures, matchsAEcheance.size());
    }

}