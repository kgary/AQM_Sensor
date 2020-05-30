-- Script to select * all tuples from DB, to be run from ij tool
connect 'jdbc:derby:derby_home/AQMdb';
select * from "APP"."PARTICLE_READING";
select * from "APP"."SERVER_PUSH_EVENT";
disconnect;
exit;
