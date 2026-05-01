package be.ephec.padelmanager.service;

import be.ephec.padelmanager.dto.planning.*;
import be.ephec.padelmanager.entity.*;
import be.ephec.padelmanager.mapper.PlanningMapper;
import be.ephec.padelmanager.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

// Construit la grille planning d'un site à une date donnée
@Service
@RequiredArgsConstructor
public class PlanningService {

    private static final int NB_PLACES_PAR_MATCH = 4;

    private final SiteRepository siteRepository;
    private final TerrainRepository terrainRepository;
    private final MatchRepository matchRepository;
    private final JourFermetureRepository jourFermetureRepository;
    private final InscriptionMatchRepository inscriptionMatchRepository;
    private final GenerateurCreneau generateurCreneau;
    private final PlanningMapper planningMapper;

    // Retourne la grille planning d'un site pour une date, en appliquant les règles d'accès du membre
    @Transactional(readOnly = true)
    public PlanningDTO consulterPlanning(Long siteId, LocalDate date, Utilisateur membre) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new EntityNotFoundException("Site introuvable : " + siteId));

        validerAccesAuSite(site, membre);

        Optional<JourFermeture> fermeture = trouverFermeture(siteId, date);
        if (fermeture.isPresent()) {
            return planningFerme(site, date, fermeture.get());
        }

        return planningOuvert(site, date);
    }

    // Bloque un Membre Site qui consulterait un autre site que son rattachement
    private void validerAccesAuSite(Site site, Utilisateur membre) {
        if (membre.getRole() != RoleUtilisateur.MEMBRE_SITE) {
            return;
        }
        Site rattachement = membre.getSiteRattachement();
        if (rattachement == null || !rattachement.getId().equals(site.getId())) {
            throw new AccessDeniedException(
                    "Un Membre Site ne peut consulter que le planning de son site de rattachement");
        }
    }

    // Cherche une fermeture (globale ou site-spécifique) pour la date demandée
    private Optional<JourFermeture> trouverFermeture(Long siteId, LocalDate date) {
        Optional<JourFermeture> globale = jourFermetureRepository.findGlobaleParDate(date);
        if (globale.isPresent()) {
            return globale;
        }
        return jourFermetureRepository.findParSiteEtDate(siteId, date);
    }

    // Construit un PlanningDTO marqué fermé, avec creneaux vide
    private PlanningDTO planningFerme(Site site, LocalDate date, JourFermeture fermeture) {
        return new PlanningDTO(
                site.getId(), site.getNom(), date,
                true, fermeture.getRaison(),
                List.of(), List.of()
        );
    }

    // Construit le planning ouvert : terrains actifs × créneaux générés, état dérivé des matchs du jour
    private PlanningDTO planningOuvert(Site site, LocalDate date) {
        List<Terrain> terrains = terrainRepository
                .findBySiteIdAndActiveTrueOrderByNumeroAsc(site.getId());
        List<CreneauDTO> creneaux = generateurCreneau.genererCreneaux(site.getId(), date.getYear());

        List<Match> matchsDuJour = matchRepository.findBySiteAndPeriode(
                site.getId(),
                date.atStartOfDay(),
                date.atTime(LocalTime.MAX)
        );

        Map<Long, Integer> joueursPayesParMatch = compterJoueursPayes(matchsDuJour);
        Map<String, Match> matchsParCle = indexerMatchsParTerrainEtHeure(matchsDuJour);

        List<TerrainPlanningDTO> terrainsDto = terrains.stream()
                .map(planningMapper::toTerrainDto)
                .toList();

        List<LigneCreneauDTO> lignes = creneaux.stream()
                .map(creneau -> construireLigne(creneau, date, terrains, matchsParCle, joueursPayesParMatch))
                .toList();

        return new PlanningDTO(
                site.getId(), site.getNom(), date,
                false, null,
                terrainsDto, lignes
        );
    }

    // Compte les inscrits payés par match en une seule requête
    private Map<Long, Integer> compterJoueursPayes(List<Match> matchs) {
        if (matchs.isEmpty()) {
            return Map.of();
        }
        List<Long> matchIds = matchs.stream().map(Match::getId).toList();
        Map<Long, Integer> resultats = new HashMap<>();
        for (Object[] ligne : inscriptionMatchRepository.countJoueursPayesByMatchIdIn(matchIds)) {
            Long matchId = (Long) ligne[0];
            Long count = (Long) ligne[1];
            resultats.put(matchId, count.intValue());
        }
        return resultats;
    }

    // Indexe les matchs par clé "terrainId|dateHeureDebut"
    private Map<String, Match> indexerMatchsParTerrainEtHeure(List<Match> matchs) {
        Map<String, Match> index = new HashMap<>();
        for (Match m : matchs) {
            index.put(cle(m.getTerrain().getId(), m.getDateHeureDebut()), m);
        }
        return index;
    }

    private String cle(Long terrainId, LocalDateTime dateHeureDebut) {
        return terrainId + "|" + dateHeureDebut;
    }

    // Construit une ligne (un créneau) avec une cellule par terrain
    private LigneCreneauDTO construireLigne(
            CreneauDTO creneau, LocalDate date, List<Terrain> terrains,
            Map<String, Match> matchsParCle, Map<Long, Integer> joueursPayesParMatch
    ) {
        LocalDateTime debut = date.atTime(creneau.debut());
        List<CelluleDTO> cellules = terrains.stream()
                .map(t -> construireCellule(t, debut, matchsParCle, joueursPayesParMatch))
                .toList();
        return new LigneCreneauDTO(creneau.debut(), creneau.fin(), cellules);
    }

    // Détermine le statut d'une cellule à partir du match éventuel sur (terrain, créneau)
    private CelluleDTO construireCellule(
            Terrain terrain, LocalDateTime debut,
            Map<String, Match> matchsParCle, Map<Long, Integer> joueursPayesParMatch
    ) {
        Match match = matchsParCle.get(cle(terrain.getId(), debut));
        if (match == null) {
            return new CelluleDTO(terrain.getId(), StatutCellule.LIBRE, null, null, null);
        }
        if (match.getType() == TypeMatch.PRIVE) {
            return new CelluleDTO(terrain.getId(), StatutCellule.PRIVE, match.getId(), null, null);
        }
        int payes = joueursPayesParMatch.getOrDefault(match.getId(), 0);
        int placesRestantes = NB_PLACES_PAR_MATCH - payes;
        StatutCellule statut = (placesRestantes > 0) ? StatutCellule.PUBLIC_DISPO : StatutCellule.COMPLET;
        return new CelluleDTO(
                terrain.getId(), statut, match.getId(),
                placesRestantes, planningMapper.nomOrganisateur(match)
        );
    }
}