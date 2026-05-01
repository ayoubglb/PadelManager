--liquibase formatted sql
--changeset ayoubglb:011-seed-membres-supplementaires-dev context:dev
--comment: Seed dev — 15 MEMBRE_SITE supplémentaires (3 Anderlecht + 4 par autre site) pour démo riche et tests métier

INSERT INTO utilisateur (
    matricule, nom, prenom, email, telephone, password_hash,
    role, site_rattachement_id, active, date_inscription
)
SELECT 'S200002', N'Membre', N'Anderlecht 2',
       N'membre.anderlecht.2@padelmanager.be', N'+32 475 60 00 02',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Anderlecht'
UNION ALL
SELECT 'S200003', N'Membre', N'Anderlecht 3',
       N'membre.anderlecht.3@padelmanager.be', N'+32 475 60 00 03',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Anderlecht'
UNION ALL
SELECT 'S200004', N'Membre', N'Anderlecht 4',
       N'membre.anderlecht.4@padelmanager.be', N'+32 475 60 00 04',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Anderlecht'
UNION ALL
SELECT 'S200005', N'Membre', N'Forest 1',
       N'membre.forest.1@padelmanager.be', N'+32 475 60 00 05',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Forest'
UNION ALL
SELECT 'S200006', N'Membre', N'Forest 2',
       N'membre.forest.2@padelmanager.be', N'+32 475 60 00 06',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Forest'
UNION ALL
SELECT 'S200007', N'Membre', N'Forest 3',
       N'membre.forest.3@padelmanager.be', N'+32 475 60 00 07',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Forest'
UNION ALL
SELECT 'S200008', N'Membre', N'Forest 4',
       N'membre.forest.4@padelmanager.be', N'+32 475 60 00 08',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Forest'
UNION ALL
SELECT 'S200009', N'Membre', N'Drogenbos 1',
       N'membre.drogenbos.1@padelmanager.be', N'+32 475 60 00 09',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Drogenbos'
UNION ALL
SELECT 'S200010', N'Membre', N'Drogenbos 2',
       N'membre.drogenbos.2@padelmanager.be', N'+32 475 60 00 10',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Drogenbos'
UNION ALL
SELECT 'S200011', N'Membre', N'Drogenbos 3',
       N'membre.drogenbos.3@padelmanager.be', N'+32 475 60 00 11',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Drogenbos'
UNION ALL
SELECT 'S200012', N'Membre', N'Drogenbos 4',
       N'membre.drogenbos.4@padelmanager.be', N'+32 475 60 00 12',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Drogenbos'
UNION ALL
SELECT 'S200013', N'Membre', N'SPL 1',
       N'membre.spl.1@padelmanager.be', N'+32 475 60 00 13',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Sint-Pieters-Leeuw'
UNION ALL
SELECT 'S200014', N'Membre', N'SPL 2',
       N'membre.spl.2@padelmanager.be', N'+32 475 60 00 14',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Sint-Pieters-Leeuw'
UNION ALL
SELECT 'S200015', N'Membre', N'SPL 3',
       N'membre.spl.3@padelmanager.be', N'+32 475 60 00 15',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Sint-Pieters-Leeuw'
UNION ALL
SELECT 'S200016', N'Membre', N'SPL 4',
       N'membre.spl.4@padelmanager.be', N'+32 475 60 00 16',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Sint-Pieters-Leeuw';