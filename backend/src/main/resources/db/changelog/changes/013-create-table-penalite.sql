--liquibase formatted sql

--changeset ayoubglb:013-create-table-penalite
CREATE TABLE [dbo].[penalite] (
                                  id                  BIGINT IDENTITY(1,1) NOT NULL,
    utilisateur_id      BIGINT NOT NULL,
    date_debut          DATETIME2 NOT NULL,
    date_fin            DATETIME2 NOT NULL,
    motif               VARCHAR(100) NOT NULL,
    match_id            BIGINT NULL,
    date_creation       DATETIME2 NOT NULL CONSTRAINT df_penalite_date_creation DEFAULT SYSUTCDATETIME(),
    CONSTRAINT pk_penalite PRIMARY KEY (id),
    CONSTRAINT fk_penalite_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES [dbo].[utilisateur](id),
    CONSTRAINT fk_penalite_match FOREIGN KEY (match_id) REFERENCES [dbo].[match](id),
    CONSTRAINT ck_penalite_dates_coherentes CHECK (date_fin > date_debut)
    );

--changeset ayoubglb:013-index-penalite-utilisateur
CREATE INDEX ix_penalite_utilisateur ON [dbo].[penalite] (utilisateur_id);

--changeset ayoubglb:013-index-penalite-dates
CREATE INDEX ix_penalite_dates ON [dbo].[penalite] (date_debut, date_fin);