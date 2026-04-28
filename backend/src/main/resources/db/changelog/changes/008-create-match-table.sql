--liquibase formatted sql

--changeset ayoubglb:008-create-match-table
CREATE TABLE [dbo].[match] (
                               id                              BIGINT IDENTITY(1,1) NOT NULL,
    terrain_id                      BIGINT NOT NULL,
    date_heure_debut                DATETIME2 NOT NULL,
    date_heure_fin                  DATETIME2 NOT NULL,
    organisateur_id                 BIGINT NOT NULL,
    type                            VARCHAR(16) NOT NULL,
    statut                          VARCHAR(16) NOT NULL,
    devenu_public_automatiquement   BIT NOT NULL CONSTRAINT df_match_devenu_public DEFAULT 0,
    date_creation                   DATETIME2 NOT NULL,
    CONSTRAINT pk_match PRIMARY KEY (id),
    CONSTRAINT fk_match_terrain FOREIGN KEY (terrain_id) REFERENCES [dbo].[terrain](id),
    CONSTRAINT fk_match_organisateur FOREIGN KEY (organisateur_id) REFERENCES [dbo].[utilisateur](id),
    CONSTRAINT uk_match_terrain_dateheuredebut UNIQUE (terrain_id, date_heure_debut),
    CONSTRAINT ck_match_type CHECK (type IN ('PRIVE', 'PUBLIC')),
    CONSTRAINT ck_match_statut CHECK (statut IN ('PROGRAMME', 'ANNULE')),
    CONSTRAINT ck_match_dates_coherentes CHECK (date_heure_fin > date_heure_debut)
    );

--changeset ayoubglb:008-index-match-organisateur
CREATE INDEX ix_match_organisateur ON [dbo].[match] (organisateur_id);

--changeset ayoubglb:008-index-match-date-debut
CREATE INDEX ix_match_date_debut ON [dbo].[match] (date_heure_debut);

--changeset ayoubglb:008-index-match-statut-type
CREATE INDEX ix_match_statut_type ON [dbo].[match] (statut, type);