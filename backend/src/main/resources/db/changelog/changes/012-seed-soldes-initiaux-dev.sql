--liquibase formatted sql
--changeset ayoubglb:012-seed-soldes-initiaux-dev context:dev
--comment: Seed dev — Soldes initiaux 60€ pour les 4 membres Anderlecht (S200001 à S200004) via transactions RECHARGE

INSERT INTO [transaction] (utilisateur_id, type, montant, date, match_id)
SELECT u.id, 'RECHARGE', 60.00, SYSUTCDATETIME(), NULL
FROM utilisateur u
WHERE u.matricule IN ('S200001', 'S200002', 'S200003', 'S200004');