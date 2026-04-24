--liquibase formatted sql

--changeset ayoubglb:005-fk-utilisateur-site
--comment: FK utilisateur.site_rattachement_id → site.id
--comment: ON DELETE NO ACTION : un site ne peut être supprimé s'il a des utilisateurs rattachés.
--comment: Le support de la suppression de site passera par une anonymisation/réaffectation préalable.
ALTER TABLE utilisateur
    ADD CONSTRAINT fk_utilisateur_site
        FOREIGN KEY (site_rattachement_id) REFERENCES site(id);