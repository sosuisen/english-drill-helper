-- Schema of the database of the app (~/.english-drill-player/drill.db).
-- Used both by the jOOQ code generator (parsed with H2) and at runtime
-- to create the tables in SQLite, so keep it portable between the two.
-- The primary key is the SHA-256 of the audio file content in lower-case
-- hex (see ADR 002). Times are epoch milliseconds in UTC.
CREATE TABLE IF NOT EXISTS unit (
  fingerprint VARCHAR NOT NULL PRIMARY KEY,
  last_played_at BIGINT NOT NULL
);
-- Cache of the segments of a unit, in time order (see docs/plans.md). It can
-- be rebuilt from the audio file, so it is not a record of the user.
CREATE TABLE IF NOT EXISTS segment (
  fingerprint VARCHAR NOT NULL,
  position INTEGER NOT NULL,
  start_ms BIGINT NOT NULL,
  duration_ms BIGINT NOT NULL,
  kind VARCHAR NOT NULL,
  PRIMARY KEY (fingerprint, position)
);
-- Folders of drill audio files the learner registered, in the order of
-- registration. The name is the drill name the learner gave the folder.
CREATE TABLE IF NOT EXISTS audio_folder (
  id INTEGER NOT NULL PRIMARY KEY,
  name VARCHAR NOT NULL,
  path VARCHAR NOT NULL
);
