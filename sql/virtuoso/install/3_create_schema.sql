create table "CR"."cr3user"."documentation"
(
  "page_id" VARCHAR(255),
  "content_type" VARCHAR(100),
  "title" VARCHAR(512),
  PRIMARY KEY ("page_id")
);

create table "CR"."cr3user"."harvest_message"
(
  "harvest_message_id" INTEGER IDENTITY,
  "harvest_id" INTEGER,
  "type" VARCHAR(3),
  "message" LONG VARCHAR,
  "stack_trace" LONG VARCHAR,
  PRIMARY KEY ("harvest_message_id")
);

create table "CR"."cr3user"."harvest"
(
  "harvest_id" INTEGER IDENTITY,
  "harvest_source_id" INTEGER,
  "type" VARCHAR(20),
  "username" VARCHAR(45),
  "status" VARCHAR(10),
  "started" DATETIME,
  "finished" DATETIME,
  "tot_statements" INTEGER,
  "lit_statements" INTEGER,
  "res_statements" INTEGER,
  "enc_schemes" INTEGER,
  "http_code" INTEGER,
  PRIMARY KEY ("harvest_id")
);

create table "CR"."cr3user"."harvest_source"
(
  "url_hash" BIGINT,
  "harvest_source_id" INTEGER IDENTITY,
  "url" VARCHAR(1024),
  "emails" VARCHAR(255),
  "time_created" DATETIME,
  "statements" INTEGER DEFAULT 0,
  "count_unavail" INTEGER DEFAULT 0,
  "last_harvest" DATETIME,
  "interval_minutes" INTEGER,
  "source" BIGINT,
  "gen_time" BIGINT,
  "last_harvest_failed" VARCHAR(1),
  "priority_source" VARCHAR(1),
  "source_owner" VARCHAR(20),
  "permanent_error" VARCHAR(1),
  "media_type" VARCHAR(255),
  "last_harvest_id" INTEGER,
  "is_sparql_endpoint" VARCHAR(1),
  PRIMARY KEY ("url_hash")
);

ALTER TABLE "CR"."cr3user"."harvest_message"
  ADD CONSTRAINT "harvest_message_harvest_fk" FOREIGN KEY ("harvest_id")
    REFERENCES "CR"."cr3user"."harvest" ("harvest_id") ON DELETE CASCADE;

ALTER TABLE "CR"."cr3user"."harvest"
  ADD CONSTRAINT "harvest_harvest_source_fk" FOREIGN KEY ("harvest_source_id")
    REFERENCES "CR"."cr3user"."harvest_source" ("harvest_source_id") ON DELETE CASCADE;

create table "CR"."cr3user"."urgent_harvest_queue"
(
  "url" VARCHAR(1024),
  "timestamp" DATETIME,
  "pushed_content" LONG VARCHAR
);

create table "CR"."cr3user"."spo_binary"
(
  "subject" BIGINT,
  "obj_lang" VARCHAR(10),
  "datatype" VARCHAR(50),
  "must_embed" VARCHAR(1),
  PRIMARY KEY ("subject")
);

create table "CR"."cr3user"."remove_source_queue"
(
  "url" VARCHAR(1024),
  PRIMARY KEY ("url")
);

create table "CR"."cr3user"."post_harvest_script"
(
  "post_harvest_script_id" INTEGER IDENTITY,
  "target_source_url" VARCHAR(1024),
  "target_type_url" VARCHAR(1024),
  "title" VARCHAR(255),
  "script" LONG VARCHAR,
  "position_number" INTEGER,
  "active" VARCHAR(1),
  "run_once" VARCHAR(1),
  "last_modified" DATETIME,
  PRIMARY KEY ("post_harvest_script_id")
);

ALTER TABLE "CR"."cr3user"."post_harvest_script"
  ADD CHECK ( target_source_url  IS NULL OR  target_type_url  IS NULL);


create table "CR"."cr3user"."delivery_filter"
(
  "delivery_filter_id" INTEGER IDENTITY,
  "obligation" VARCHAR(255),
  "obligation_label" VARCHAR(255),
  "locality" VARCHAR(255),
  "locality_label" VARCHAR(255),
  "year" VARCHAR(10),
  "username" VARCHAR(10)
);

create table "CR"."cr3user"."acls"
(
  "acl_id" INTEGER IDENTITY,
  "acl_name" VARCHAR(100),
  "parent_name" VARCHAR(100),
  "owner" VARCHAR(255),
  "description" VARCHAR(255),
  PRIMARY KEY ("acl_id")
);

create table "CR"."cr3user"."acl_rows"
(
  "acl_id" INTEGER,
  "type" VARCHAR(50),
  "entry_type" VARCHAR(50),
  "principal" VARCHAR(16),
  "status" INTEGER,
  "permissions" VARCHAR(255),
  PRIMARY KEY ("acl_id", "type", "entry_type", "principal", "status")
);

ALTER TABLE "CR"."cr3user"."acl_rows"
  ADD CHECK ( entry_type  = 'owner'  OR  entry_type  = 'user'  OR  entry_type  = 'localgroup'  OR  entry_type  = 'other'  OR  entry_type  = 'foreign'  OR  entry_type  = 'unauthenticated'  OR  entry_type  = 'authenticated'  OR  entry_type  = 'mask' );

ALTER TABLE "CR"."cr3user"."acl_rows"
  ADD CHECK ( type  = 'object'  OR  type  = 'doc'  OR  type  = 'dcc' );


create table "CR"."cr3user"."staging_db"
(
  "database_id" INTEGER IDENTITY,
  "name" VARCHAR(150),
  "creator" VARCHAR(80),
  "created" DATETIME,
  "description" LONG VARCHAR,
  "import_status" VARCHAR(30),
  "import_log" LONG VARCHAR,
  "default_query" LONG VARCHAR,
  PRIMARY KEY ("database_id")
);

create table "CR"."cr3user"."staging_db_rdf_export"
(
  "export_id" INTEGER IDENTITY,
  "database_id" INTEGER,
  "export_name" VARCHAR(150),
  "user_name" VARCHAR(80),
  "query_conf" LONG VARCHAR,
  "started" DATETIME,
  "finished" DATETIME,
  "status" VARCHAR(30),
  "export_log" LONG VARCHAR,
  "row_count" INTEGER,
  "noof_subjects" INTEGER,
  "noof_triples" INTEGER,
  "missing_concepts" LONG VARCHAR,
  "graphs" LONG VARCHAR,
  PRIMARY KEY ("export_id")
);

create table "CR"."cr3user"."endpoint_harvest_query"
(
  "endpoint_harvest_query_id" INTEGER IDENTITY,
  "title" VARCHAR(255),
  "query" LONG VARCHAR,
  "endpoint_url" VARCHAR(1024),
  "endpoint_url_hash" BIGINT,
  "position_number" INTEGER,
  "active" VARCHAR(1),
  "last_modified" DATETIME,
  PRIMARY KEY ("endpoint_harvest_query_id")
);

ALTER TABLE "CR"."cr3user"."endpoint_harvest_query"
  ADD CONSTRAINT "fk_url_hash" FOREIGN KEY ("endpoint_url_hash")
    REFERENCES "CR"."cr3user"."harvest_source" ("url_hash") ON UPDATE CASCADE ON DELETE CASCADE;

