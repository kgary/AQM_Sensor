-- ----------------------------------------------
-- This file is the DDL for the AQM app
-- ----------------------------------------------

-- ----------------------------------------------
-- DDL Statements for sequences
-- ----------------------------------------------
-- CREATE SEQUENCE "APP"."AQM_SEQ"
--     AS INTEGER
--     START WITH -2147483648
--     INCREMENT BY 1
--     MAXVALUE 9999999
--     MINVALUE -2147483648
--     CYCLE
-- ;

-- ----------------------------------------------
-- DDL Statements for tables
-- ----------------------------------------------
CREATE TABLE "APP"."PARTICLE_READING" ("DEVICEID" VARCHAR(16) NOT NULL, "USERID" VARCHAR(16) NOT NULL, "DATETIME" TIMESTAMP NOT NULL, "SMALLPARTICLE" INTEGER, "LARGEPARTICLE" INTEGER, "GEOLATITUDE" DOUBLE, "GEOLONGITUDE" DOUBLE, "GEOMETHOD" VARCHAR(16));

CREATE TABLE "APP"."SERVER_PUSH_EVENT" ("EVENTTIME" TIMESTAMP NOT NULL, "RESPONSECODE" INTEGER NOT NULL, "DEVICETYPE" INTEGER, "MESSAGE" VARCHAR(1024));

-- ----------------------------------------------
-- DDL Statements for keys
-- ----------------------------------------------
-- primary/unique

ALTER TABLE "APP"."PARTICLE_READING" ADD CONSTRAINT "PARTICLE_READING_PK" PRIMARY KEY ("DEVICEID", "USERID", "DATETIME");
