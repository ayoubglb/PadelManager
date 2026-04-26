--liquibase formatted sql

--changeset ayoubglb:006-seed-sites context:dev
--comment: Seed dev — 4 sites réels
INSERT INTO site (nom, adresse, code_postal, ville, active) VALUES
                                                                (N'Anderlecht',         N'Avenue de Scherdemael 1',  N'1070', N'Anderlecht',         1),
                                                                (N'Forest',              N'Avenue du Globe 36',       N'1190', N'Forest',             1),
                                                                (N'Drogenbos',           N'Grote Baan 222',           N'1620', N'Drogenbos',          1),
                                                                (N'Sint-Pieters-Leeuw',  N'Bergensesteenweg 426',     N'1600', N'Sint-Pieters-Leeuw', 1);

--changeset ayoubglb:006-seed-terrains context:dev
--comment: Seed dev — Anderlecht 6 terrains, Forest 5, Drogenbos 5, SPL 4
INSERT INTO terrain (numero, site_id, active)
SELECT v.numero, s.id, 1
FROM site s
         JOIN (
    VALUES
        (N'Anderlecht',         1), (N'Anderlecht',         2), (N'Anderlecht',         3),
        (N'Anderlecht',         4), (N'Anderlecht',         5), (N'Anderlecht',         6),
        (N'Forest',              1), (N'Forest',              2), (N'Forest',              3),
        (N'Forest',              4), (N'Forest',              5),
        (N'Drogenbos',           1), (N'Drogenbos',           2), (N'Drogenbos',           3),
        (N'Drogenbos',           4), (N'Drogenbos',           5),
        (N'Sint-Pieters-Leeuw',  1), (N'Sint-Pieters-Leeuw',  2),
        (N'Sint-Pieters-Leeuw',  3), (N'Sint-Pieters-Leeuw',  4)
) AS v(nom_site, numero) ON v.nom_site = s.nom;

--changeset ayoubglb:006-seed-horaires-2026 context:dev
--comment: Seed dev — horaires 2026 pour les 4 sites (09:00 - 22:00 par défaut)
INSERT INTO horaire_site (site_id, annee, heure_debut, heure_fin)
SELECT id, 2026, '09:00:00', '22:00:00' FROM site;