-- Script to delete all tuples from DB, to be run from ij tool
connect 'jdbc:derby:derby_home/AQMdb';
delete from "APP"."PARTICLE_READING";
delete from "APP"."SERVER_PUSH_EVENT";
disconnect;
exit;
