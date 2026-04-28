--liquibase formatted sql

--changeset ayoubglb:009-create-transaction-table
CREATE TABLE [dbo].[transaction] (
                                     id              BIGINT IDENTITY(1,1) NOT NULL,
    utilisateur_id  BIGINT NOT NULL,
    type            VARCHAR(32) NOT NULL,
    montant         DECIMAL(10, 2) NOT NULL,
    date            DATETIME2 NOT NULL,
    match_id        BIGINT NULL,
    CONSTRAINT pk_transaction PRIMARY KEY (id),
    CONSTRAINT fk_transaction_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES [dbo].[utilisateur](id),
    CONSTRAINT fk_transaction_match FOREIGN KEY (match_id) REFERENCES [dbo].[match](id),
    CONSTRAINT ck_transaction_montant_positif CHECK (montant > 0),
    CONSTRAINT ck_transaction_type CHECK (type IN ('RECHARGE', 'PAIEMENT_MATCH', 'SOLDE_DU_ORGANISATEUR', 'REMBOURSEMENT')),
    -- match_id obligatoire sauf pour RECHARGE
    CONSTRAINT ck_transaction_match_coherent CHECK (
(type = 'RECHARGE' AND match_id IS NULL)
    OR (type <> 'RECHARGE' AND match_id IS NOT NULL)
    )
    );

--changeset ayoubglb:009-index-transaction-utilisateur
CREATE INDEX ix_transaction_utilisateur ON [dbo].[transaction] (utilisateur_id);

--changeset ayoubglb:009-index-transaction-utilisateur-type
-- Optimise le SUM(CASE WHEN type IN ...) du calcul de solde
CREATE INDEX ix_transaction_utilisateur_type ON [dbo].[transaction] (utilisateur_id, type);

--changeset ayoubglb:009-index-transaction-date
-- Optimise les requêtes de chiffre d'affaires par période
CREATE INDEX ix_transaction_date ON [dbo].[transaction] (date);

--changeset ayoubglb:009-uk-partielle-solde-du-organisateur
-- Index unique filtré : une seule transaction SOLDE_DU_ORGANISATEUR par match
CREATE UNIQUE INDEX uk_transaction_solde_du_par_match
    ON [dbo].[transaction] (match_id)
    WHERE type = 'SOLDE_DU_ORGANISATEUR';