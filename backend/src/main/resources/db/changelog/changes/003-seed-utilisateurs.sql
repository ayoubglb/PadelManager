--liquibase formatted sql

--changeset ayoubglb:003-seed-utilisateurs-dev context:dev
--comment: Comptes de seed.
--comment: Les comptes MEMBRE_SITE et ADMIN_SITE seront ajoutés aoprès
--comment: Mots de passe : Admin2026! pour l'admin, Dev2026! pour les membres.
--comment: Hashs BCrypt cost=10 générés via GenererHashsBcryptUtil.

INSERT INTO utilisateur (matricule, nom, prenom, email, telephone, password_hash, role, site_rattachement_id, active, date_inscription)
VALUES
    ('AG100001', 'Admin',  'Global', 'admin.global@padelmanager.be',  '+32475000001',
     '$2a$10$o96jRtX3ZqXqZ9.H0.Si8e74k2wSEnkjPIK.DGSyPqTI3jycA9JkW', 'ADMIN_GLOBAL',  NULL, 1, SYSUTCDATETIME()),

    ('G100001',  'Membre', 'Global', 'membre.global@padelmanager.be', '+32475000003',
     '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy!',   'MEMBRE_GLOBAL', NULL, 1, SYSUTCDATETIME()),

    ('L100001',  'Membre', 'Libre',  'membre.libre@padelmanager.be',  '+32475000005',
     '$2a$10$M4ylgr8Ot1wDRAcWbJnHwOjF7pIymXVezdsUi1uASbipZF9oNZUsy',   'MEMBRE_LIBRE',  NULL, 1, SYSUTCDATETIME());