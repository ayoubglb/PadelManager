--liquibase formatted sql

--changeset padelmanager:001-initial
--comment: Initial sanity check — verifies Liquibase connectivity

SELECT 1;

--rollback SELECT 1;