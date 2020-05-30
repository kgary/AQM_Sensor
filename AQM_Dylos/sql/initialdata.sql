-- Script to initial some reading data to DB, to be run from ij tool
-- connect 'jdbc:derby:derby_home/AQMdb';
insert into PARTICLE_READING values ('aqm0', 'user0', CURRENT_TIMESTAMP, 0, 0, 0.0, 0.0, 'unknown');
insert into SERVER_PUSH_EVENT values (CURRENT_TIMESTAMP, 1, 1, 'test Pushed 1 DylosReading to the server');
-- disconnect;
-- exit;

