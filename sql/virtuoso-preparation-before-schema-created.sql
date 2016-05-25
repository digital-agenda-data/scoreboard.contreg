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

-- ---------------------------------------------------------------------------
-- Set up full-text indexing in the triplestore.
-- ---------------------------------------------------------------------------

DB.DBA.RDF_OBJ_FT_RULE_ADD (null, null, 'All');
DB.DBA.VT_INC_INDEX_DB_DBA_RDF_OBJ ();
DB.DBA.VT_BATCH_UPDATE ('DB.DBA.RDF_OBJ', 'OFF', null);
DB.DBA.VT_BATCH_UPDATE ('DB.DBA.RDF_OBJ', 'ON', 10);

-- ---------------------------------------------------------------------------
-- Some Virtuoso 7.2.x specific permissions.
-- ---------------------------------------------------------------------------

grant execute on DB.DBA.RL_I2ID to cr3user;
grant execute on DB.DBA.L_O_LOOK to cr3user;
grant execute on DB.DBA.RL_I2ID_NP to cr3user;
grant execute on DB.DBA.RDF_INSERT_TRIPLE_C to cr3user;
grant execute on DB.DBA.RDF_CLEAR_GRAPHS_C to cr3user;
grant execute on DB.DBA.TTLP to cr3user;
grant execute on DB.DBA.TTLP_RL_NEW_GRAPH to cr3user;

grant execute on DB.DBA.RL_I2ID to cr3test;
grant execute on DB.DBA.L_O_LOOK to cr3test;
grant execute on DB.DBA.RL_I2ID_NP to cr3test;
grant execute on DB.DBA.RDF_INSERT_TRIPLE_C to cr3test;
grant execute on DB.DBA.RDF_CLEAR_GRAPHS_C to cr3test;
grant execute on DB.DBA.TTLP to cr3test;
grant execute on DB.DBA.TTLP_RL_NEW_GRAPH to cr3test;

-- ---------------------------------------------------------------------------
-- Some Content Registry specific permissions.
-- ---------------------------------------------------------------------------

grant execute on DB.DBA.dump_one_graph to cr3user;
grant execute on file_to_string_output to cr3user;
grant execute on file_to_string_session to cr3user;

-- -------------------------------------------------------------------------------------
-- Define CR's default inference rule set.
-- -------------------------------------------------------------------------------------

rdfs_rule_set ('CRInferenceRule', 'http://cr.eionet.europa.eu/ontologies/contreg.rdf');
