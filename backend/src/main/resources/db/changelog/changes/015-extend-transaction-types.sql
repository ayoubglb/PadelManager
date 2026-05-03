--liquibase formatted sql

--changeset ayoubglb:015-drop-old-transaction-type-constraint
ALTER TABLE [dbo].[transaction] DROP CONSTRAINT ck_transaction_type;

--changeset ayoubglb:015-drop-uk-solde-du-temporary
-- Drop temporaire de l'index UK partial (dépendant de la colonne type)
-- pour permettre l'ALTER COLUMN. Recréé dans le dernier changeset de ce fichier.
DROP INDEX uk_transaction_solde_du_par_match ON [dbo].[transaction];

--changeset ayoubglb:015-extend-transaction-type-column-size
-- Élargit la colonne type de VARCHAR(32) à VARCHAR(64) pour accueillir
-- REMBOURSEMENT_SOLDE_DU_ORGANISATEUR (35 caractères, dépasse les 32 initiaux).
ALTER TABLE [dbo].[transaction] ALTER COLUMN type VARCHAR(64) NOT NULL;

--changeset ayoubglb:015-add-extended-transaction-type-constraint
-- Ajoute REMBOURSEMENT_SOLDE_DU_ORGANISATEUR
-- Cette transaction contre-passe une dette SOLDE_DU_ORGANISATEUR quand un joueur
-- rejoint un match public après la création de la dette à T-24h.
ALTER TABLE [dbo].[transaction]
    ADD CONSTRAINT ck_transaction_type
    CHECK (type IN (
    'RECHARGE',
    'PAIEMENT_MATCH',
    'SOLDE_DU_ORGANISATEUR',
    'REMBOURSEMENT',
    'REMBOURSEMENT_SOLDE_DU_ORGANISATEUR'
    ));

--changeset ayoubglb:015-recreate-uk-solde-du-par-match
-- Recréation de l'index UK partial après l'ALTER COLUMN
-- ( idempotence du job EF-SYS-003)
CREATE UNIQUE INDEX uk_transaction_solde_du_par_match
    ON [dbo].[transaction] (match_id)
    WHERE type = 'SOLDE_DU_ORGANISATEUR';