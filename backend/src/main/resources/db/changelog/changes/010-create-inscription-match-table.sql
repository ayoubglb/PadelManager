--liquibase formatted sql

--changeset ayoubglb:010-create-inscription-match-table
CREATE TABLE [dbo].[inscription_match] (
                                           id                BIGINT IDENTITY(1,1) NOT NULL,
    match_id          BIGINT NOT NULL,
    joueur_id         BIGINT NOT NULL,
    date_inscription  DATETIME2 NOT NULL,
    paye              BIT NOT NULL CONSTRAINT df_inscription_match_paye DEFAULT 0,
    statut            VARCHAR(32) NOT NULL,
    est_organisateur  BIT NOT NULL CONSTRAINT df_inscription_match_est_organisateur DEFAULT 0,
    CONSTRAINT pk_inscription_match PRIMARY KEY (id),
    CONSTRAINT fk_inscription_match_match FOREIGN KEY (match_id) REFERENCES [dbo].[match](id),
    CONSTRAINT fk_inscription_match_joueur FOREIGN KEY (joueur_id) REFERENCES [dbo].[utilisateur](id),
    CONSTRAINT uk_inscription_match_joueur UNIQUE (match_id, joueur_id),
    CONSTRAINT ck_inscription_match_statut CHECK (statut IN ('INSCRIT', 'ANNULE', 'LIBERE_NON_PAIEMENT'))
    );

--changeset ayoubglb:010-index-inscription-match-match
CREATE INDEX ix_inscription_match_match ON [dbo].[inscription_match] (match_id);

--changeset ayoubglb:010-index-inscription-match-joueur
CREATE INDEX ix_inscription_match_joueur ON [dbo].[inscription_match] (joueur_id);

--changeset ayoubglb:010-index-inscription-match-paye-statut
-- Optimise COUNT joueurs payés d'un match
CREATE INDEX ix_inscription_match_paye_statut ON [dbo].[inscription_match] (paye, statut);