--liquibase formatted sql

--changeset ayoubglb:014-uk-penalite-match
-- garantit qu'une seule Penalite existe par match d'origine
-- (idempotence du job EF-SYS-001 qui convertit privé→public + applique pénalité)
CREATE UNIQUE INDEX uk_penalite_match
    ON [dbo].[penalite] (match_id)
    WHERE match_id IS NOT NULL; -- permet d'avoir des pénalités sans match (admin) mais pas plusieurs par match