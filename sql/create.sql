-- Use forein keys support
PRAGMA foreign_keys = ON;

-- Drop data first
DROP INDEX IF EXISTS "INSTRUMENTS_SINCE";
DROP INDEX IF EXISTS "DATA_DATE";

DROP TABLE IF EXISTS "INSTRUMENTS";
DROP TABLE IF EXISTS "DOWNLOADERS";
DROP TABLE IF EXISTS "DATA";

-- Create tables
CREATE TABLE "INSTRUMENTS" (
	"ID"            INTEGER     PRIMARY KEY     NOT NULL,
	"TICKER"        TEXT                        NOT NULL    UNIQUE
	                                                            ON CONFLICT ABORT,
	"DOWNLOADER"    INTEGER                     NOT NULL    REFERENCES "DOWNLOADERS" ("ID")
	                                                            ON DELETE RESTRICT
	                                                            ON UPDATE CASCADE,
	"SINCE"         TEXT,
	"UPDATED"       TEXT
);

CREATE TABLE "DOWNLOADERS" (
	"ID"            INTEGER     PRIMARY KEY     NOT NULL,
	"NAME"          TEXT                        NOT NULL,
	"URI"           TEXT                        NOT NULL
);

CREATE TABLE "DATA" (
	"ID"            INTEGER     PRIMARY KEY     NOT NULL,
	"INSTRUMENT"    INTEGER                     NOT NULL    REFERENCES "INSTRUMENTS" ("ID")
	                                                            ON DELETE CASCADE
	                                                            ON UPDATE CASCADE,
	"DATE"          TEXT                        NOT NULL,
	"OPEN"          REAL                        NOT NULL,
	"HIGH"          REAL                        NOT NULL,
	"LOW"           REAL                        NOT NULL,
	"CLOSE"         REAL                        NOT NULL,
	"CLOSEADJ"      REAL                        NOT NULL,
	"VOLUME"        REAL                        NOT NULL
);

-- Create indexes
CREATE INDEX "INSTRUMENTS_DOWNLOADER" ON "INSTRUMENTS" ("DOWNLOADER");
CREATE INDEX "INSTRUMENTS_SINCE" ON "INSTRUMENTS" ("SINCE");
CREATE INDEX "DATA_INSTRUMENT" ON "DATA" ("INSTRUMENT");
CREATE INDEX "DATA_DATE" ON "DATA" ("DATE");
