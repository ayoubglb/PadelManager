--liquibase formatted sql

--changeset ayoubglb:002-create-utilisateur-table
--comment: Création de la table utilisateur
CREATE TABLE utilisateur (
                             id                    BIGINT        IDENTITY(1,1) NOT NULL,
                             matricule             NVARCHAR(10)  NOT NULL,
                             nom                   NVARCHAR(100) NOT NULL,
                             prenom                NVARCHAR(100) NOT NULL,
                             email                 NVARCHAR(255) NOT NULL,
                             telephone             NVARCHAR(30)  NOT NULL,
                             password_hash         NVARCHAR(255) NOT NULL,
                             role                  NVARCHAR(20)  NOT NULL,
                             site_rattachement_id  BIGINT        NULL,
                             active                BIT           NOT NULL CONSTRAINT df_utilisateur_active           DEFAULT 1,
                             date_inscription      DATETIME2     NOT NULL CONSTRAINT df_utilisateur_date_inscription DEFAULT SYSUTCDATETIME(),
                             CONSTRAINT pk_utilisateur           PRIMARY KEY (id),
                             CONSTRAINT uk_utilisateur_matricule UNIQUE (matricule),
                             CONSTRAINT uk_utilisateur_email     UNIQUE (email),
                             CONSTRAINT ck_utilisateur_role      CHECK (role IN ('MEMBRE_GLOBAL','MEMBRE_SITE','MEMBRE_LIBRE','ADMIN_SITE','ADMIN_GLOBAL'))
);

--changeset ayoubglb:002-index-utilisateur-site-rattachement
--comment: Index pour accélérer les listings filtrés par site (écrans ADMIN_SITE)
CREATE INDEX idx_utilisateur_site_rattachement
    ON utilisateur (site_rattachement_id);