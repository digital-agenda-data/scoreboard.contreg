-- ---------------------------------------------------------------------
-- Create DB users.
-- ---------------------------------------------------------------------

USER_CREATE('cr3user', 'xxx', vector ('SQL_ENABLE',1,'DAV_ENABLE',1));
USER_CREATE('cr3rouser', 'yyy', vector ('DAV_ENABLE',1));
USER_CREATE('cr3test', 'zzz', vector ('SQL_ENABLE',1,'DAV_ENABLE',1));

-- ---------------------------------------------------------------------
-- Grant permissions to DB users.
-- ---------------------------------------------------------------------

USER_GRANT_ROLE('cr3user','SPARQL_SELECT',0);
USER_GRANT_ROLE('cr3user','SPARQL_UPDATE',0);
GRANT SELECT ON sys_rdf_schema TO cr3user;
GRANT execute ON rdfs_rule_set TO cr3user;


USER_GRANT_ROLE('cr3test','SPARQL_SELECT',0);
USER_GRANT_ROLE('cr3test','SPARQL_UPDATE',0);
GRANT SELECT ON sys_rdf_schema TO cr3test;
GRANT execute ON rdfs_rule_set TO cr3test;

USER_GRANT_ROLE('cr3rouser','SPARQL_SELECT',0);

-- ---------------------------------------------------------------------------
-- Set DB users' default databases.
-- ---------------------------------------------------------------------------

user_set_qualifier ('cr3user', 'CR');
user_set_qualifier ('cr3rouser', 'CR');
user_set_qualifier ('cr3test', 'CRTEST');
