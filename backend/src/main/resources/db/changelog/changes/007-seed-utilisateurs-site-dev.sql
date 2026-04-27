--liquibase formatted sql

--changeset ayoubglb:007-seed-utilisateurs-site context:dev
--comment: Seed dev — 1 ADMIN_SITE par site (4 au total) + 1 MEMBRE_SITE pour tester l'autorisation par ressource
INSERT INTO utilisateur (
    matricule, nom, prenom, email, telephone, password_hash,
    role, site_rattachement_id, active, date_inscription
)
SELECT 'AS200001', N'Admin', N'Anderlecht',
       N'admin.site.anderlecht@padelmanager.be', N'+32 475 11 22 33',
       '$2a$10$o96jRtX3ZqXqZ9.H0.Si8e74k2wSEnkjPIK.DGSyPqTI3jycA9JkW',
       'ADMIN_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Anderlecht'
UNION ALL
SELECT 'AS200002', N'Admin', N'Forest',
       N'admin.site.forest@padelmanager.be', N'+32 475 22 33 44',
       '$2a$10$o96jRtX3ZqXqZ9.H0.Si8e74k2wSEnkjPIK.DGSyPqTI3jycA9JkW',
       'ADMIN_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Forest'
UNION ALL
SELECT 'AS200003', N'Admin', N'Drogenbos',
       N'admin.site.drogenbos@padelmanager.be', N'+32 475 33 44 55',
       '$2a$10$o96jRtX3ZqXqZ9.H0.Si8e74k2wSEnkjPIK.DGSyPqTI3jycA9JkW',
       'ADMIN_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Drogenbos'
UNION ALL
SELECT 'AS200004', N'Admin', N'Sint-Pieters-Leeuw',
       N'admin.site.spl@padelmanager.be', N'+32 475 44 55 66',
       '$2a$10$o96jRtX3ZqXqZ9.H0.Si8e74k2wSEnkjPIK.DGSyPqTI3jycA9JkW',
       'ADMIN_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Sint-Pieters-Leeuw'
UNION ALL
SELECT 'S200001', N'Membre', N'Site',
       N'membre.site@padelmanager.be', N'+32 475 55 66 77',
       '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',
       'MEMBRE_SITE', s.id, 1, SYSUTCDATETIME()
FROM site s WHERE s.nom = N'Anderlecht';