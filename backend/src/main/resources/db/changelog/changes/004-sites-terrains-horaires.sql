--liquibase formatted sql

--changeset ayoubglb:004-create-site-table
--comment: Table site  — 4 sites
CREATE TABLE site (
                      id           BIGINT        IDENTITY(1,1) NOT NULL,
                      nom          NVARCHAR(100) NOT NULL,
                      adresse      NVARCHAR(200) NOT NULL,
                      code_postal  NVARCHAR(10)  NOT NULL,
                      ville        NVARCHAR(100) NOT NULL,
                      active       BIT           NOT NULL CONSTRAINT df_site_active DEFAULT 1,
                      CONSTRAINT pk_site     PRIMARY KEY (id),
                      CONSTRAINT uk_site_nom UNIQUE (nom)
);

--changeset ayoubglb:004-create-terrain-table
--comment: Table terrain — numéro unique par site, nom optionnel
CREATE TABLE terrain (
                         id       BIGINT        NOT NULL IDENTITY(1,1),
                         numero   INT           NOT NULL,
                         nom      NVARCHAR(100) NULL,
                         site_id  BIGINT        NOT NULL,
                         active   BIT           NOT NULL CONSTRAINT df_terrain_active DEFAULT 1,
                         CONSTRAINT pk_terrain             PRIMARY KEY (id),
                         CONSTRAINT fk_terrain_site        FOREIGN KEY (site_id) REFERENCES site(id) ON DELETE CASCADE,
                         CONSTRAINT uk_terrain_site_numero UNIQUE (site_id, numero),
                         CONSTRAINT ck_terrain_numero      CHECK (numero > 0)
);

--changeset ayoubglb:004-create-horaire-site-table
--comment: Table horaire_site un horaire par site et par année
CREATE TABLE horaire_site (
                              id           BIGINT NOT NULL IDENTITY(1,1),
                              site_id      BIGINT NOT NULL,
                              annee        INT    NOT NULL,
                              heure_debut  TIME   NOT NULL,
                              heure_fin    TIME   NOT NULL,
                              CONSTRAINT pk_horaire_site            PRIMARY KEY (id),
                              CONSTRAINT fk_horaire_site            FOREIGN KEY (site_id) REFERENCES site(id) ON DELETE CASCADE,
                              CONSTRAINT uk_horaire_site_annee      UNIQUE (site_id, annee),
                              CONSTRAINT ck_horaire_site_annee      CHECK (annee >= 2000 AND annee <= 2100),
                              CONSTRAINT ck_horaire_site_coherence  CHECK (heure_debut < heure_fin)
);

--changeset ayoubglb:004-create-jour-fermeture-table
--comment: Table jour_fermeture — fermetures globales (site NULL) ou site-spécifiques
CREATE TABLE jour_fermeture (
                                id              BIGINT        NOT NULL IDENTITY(1,1),
                                date_fermeture  DATE          NOT NULL,
                                site_id         BIGINT        NULL,
                                raison          NVARCHAR(200) NOT NULL,
                                CONSTRAINT pk_jour_fermeture      PRIMARY KEY (id),
                                CONSTRAINT fk_jour_fermeture_site FOREIGN KEY (site_id) REFERENCES site(id) ON DELETE CASCADE
);

--changeset ayoubglb:004-index-jour-fermeture-unicite-site
--comment: Unicité (date, site) pour les fermetures site-spécifiques (site NOT NULL)
CREATE UNIQUE INDEX uk_jour_fermeture_date_site
    ON jour_fermeture (date_fermeture, site_id)
    WHERE site_id IS NOT NULL;

--changeset ayoubglb:004-index-jour-fermeture-unicite-globale
--comment: Unicité de la fermeture globale pour une date donnée (site NULL)
CREATE UNIQUE INDEX uk_jour_fermeture_date_globale
    ON jour_fermeture (date_fermeture)
    WHERE site_id IS NULL;