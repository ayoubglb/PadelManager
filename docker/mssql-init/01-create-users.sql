-- 01-create-users.sql
-- Exécuté une seule fois au premier démarrage du conteneur MSSQL.
-- Crée la base de données et les deux utilisateurs applicatifs.
--
-- Utilisateurs créés :
--   - padel_liquibase : DDL + DML, utilisé par Liquibase pour les migrations
--   - padel_app       : DML uniquement, utilisé par Spring Boot en runtime
--
-- Le compte SA n'est jamais utilisé par l'application.

-- =============================================================================
-- Création de la base
-- =============================================================================
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'PadelManager')
BEGIN
    CREATE DATABASE PadelManager
        COLLATE French_CI_AI;
    PRINT 'Database PadelManager created.';
END
ELSE
BEGIN
    PRINT 'Database PadelManager already exists.';
END
GO

USE PadelManager;
GO

-- =============================================================================
-- Utilisateur padel_liquibase (DDL + DML)
-- =============================================================================
IF NOT EXISTS (SELECT name FROM sys.server_principals WHERE name = 'padel_liquibase')
BEGIN
    CREATE LOGIN padel_liquibase
        WITH PASSWORD = 'Padel_Liquibase_2026!',
             DEFAULT_DATABASE = PadelManager,
             CHECK_POLICY = OFF;
    PRINT 'Login padel_liquibase created.';
END
GO

IF NOT EXISTS (SELECT name FROM sys.database_principals WHERE name = 'padel_liquibase')
BEGIN
    CREATE USER padel_liquibase FOR LOGIN padel_liquibase;
    ALTER ROLE db_owner ADD MEMBER padel_liquibase;
    PRINT 'User padel_liquibase added to db_owner role.';
END
GO

-- =============================================================================
-- Utilisateur padel_app (DML uniquement)
-- =============================================================================
IF NOT EXISTS (SELECT name FROM sys.server_principals WHERE name = 'padel_app')
BEGIN
    CREATE LOGIN padel_app
        WITH PASSWORD = 'Padel_App_2026!',
             DEFAULT_DATABASE = PadelManager,
             CHECK_POLICY = OFF;
    PRINT 'Login padel_app created.';
END
GO

IF NOT EXISTS (SELECT name FROM sys.database_principals WHERE name = 'padel_app')
BEGIN
    CREATE USER padel_app FOR LOGIN padel_app;
    ALTER ROLE db_datareader ADD MEMBER padel_app;
    ALTER ROLE db_datawriter ADD MEMBER padel_app;
    -- Pas de db_ddladmin : padel_app ne doit PAS pouvoir modifier le schéma
    PRINT 'User padel_app added to db_datareader and db_datawriter roles.';
END
GO

PRINT 'Init script completed successfully.';
GO