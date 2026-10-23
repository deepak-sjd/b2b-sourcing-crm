-- Run ONCE as the postgres superuser, connected to the doctutorials_crm database.
-- Creates the login the application will use. It is not the table owner, so
-- row-level security applies to it. Change the password for any real environment.
CREATE ROLE crm_app LOGIN PASSWORD 'change-me-app-password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;

GRANT CONNECT ON DATABASE doctutorials_crm TO crm_app;
GRANT USAGE ON SCHEMA public TO crm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO crm_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO crm_app;
REVOKE ALL ON flyway_schema_history FROM crm_app;

-- Tables and sequences that future migrations create (as crm_user) get the same grants.
ALTER DEFAULT PRIVILEGES FOR ROLE crm_user IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO crm_app;
ALTER DEFAULT PRIVILEGES FOR ROLE crm_user IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO crm_app;