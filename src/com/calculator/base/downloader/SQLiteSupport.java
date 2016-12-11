package com.calculator.base.downloader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

class SQLiteSupport {

    private Connection conn;

    SQLiteSupport() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:output/instruments.sqlite");
            conn.setAutoCommit(false);
            System.out.println("Opened database successfully");
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }

    void createTables() {
        Statement stmt;
        String sql;
        try {
            stmt = conn.createStatement();

            sql = "PRAGMA foreign_keys = ON;";
            stmt.executeUpdate(sql);
            conn.commit();

            sql = "DROP TABLE IF EXISTS `INSTRUMENTS`;";
            stmt.executeUpdate(sql);
            conn.commit();

            sql = "CREATE TABLE `INSTRUMENTS` " +
                    "(`ID`       INTEGER      PRIMARY KEY         , " +
                    " `TICKER`   VARCHAR(16)              NOT NULL " +
                    "                                     UNIQUE ON CONFLICT ABORT, " +
                    " `NAME`     VARCHAR(255)             NOT NULL, " +
                    " `TYPE`     VARCHAR(8)               NOT NULL, " +
                    " `FROM`     CHAR(10)                 NOT NULL, " +
                    " `TO`       CHAR(10)                 NOT NULL, " +
                    " `PROVIDER` VARCHAR(255)             NOT NULL, " +
                    " `SITE`     VARCHAR(255)             NOT NULL);";
            stmt.executeUpdate(sql);

            sql = "CREATE INDEX `NAME_INDEX` ON `INSTRUMENTS` (`NAME`);";
            stmt.executeUpdate(sql);

            sql = "CREATE INDEX `FROM_INDEX` ON `INSTRUMENTS` (`FROM`);";
            stmt.executeUpdate(sql);

            sql = "CREATE INDEX `PROVIDER_INDEX` ON `INSTRUMENTS` (`PROVIDER`);";
            stmt.executeUpdate(sql);

            conn.commit();
            System.out.println("Table `INSTRUMENTS` created successfully");

            sql = "DROP TABLE IF EXISTS `HISTORY`;";
            stmt.executeUpdate(sql);
            conn.commit();

            sql = "CREATE TABLE `HISTORY` " +
                    "(`ID`       INTEGER  PRIMARY KEY         , " +
                    " `TICKER`   VARCHAR(16)          NOT NULL REFERENCES `INSTRUMENTS` (`TICKER`) ON DELETE RESTRICT  " +
                    "                                                                              ON UPDATE RESTRICT, " +
                    " `DATE`     CHAR(10)             NOT NULL, " +
                    " `OPEN`     REAL                 NOT NULL, " +
                    " `HIGH`     REAL                 NOT NULL, " +
                    " `LOW`      REAL                 NOT NULL, " +
                    " `CLOSE`    REAL                 NOT NULL, " +
                    " `CLOSEADJ` REAL                 NOT NULL, " +
                    " `VOLUME`   REAL                 NOT NULL);";
            stmt.executeUpdate(sql);

            sql = "CREATE INDEX `TICKER_INDEX` ON `HISTORY` (`TICKER`);";
            stmt.executeUpdate(sql);

            conn.commit();
            stmt.close();

            System.out.println("Table `HISTORY` created successfully");

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    void saveInstrument(Instrument instrument) {
        Statement stmt;
        String sql;
        try {
            stmt = conn.createStatement();

            sql = "INSERT INTO INSTRUMENTS VALUES (NULL, " + instrument.valuesToInsert() + ");";
            stmt.executeUpdate(sql);

            sql = "INSERT INTO HISTORY VALUES " + instrument.historyToInsert() + ";";
            stmt.executeUpdate(sql);

            conn.commit();
            stmt.close();

            System.out.println("\t" + instrument.getTicker() + " inserted successfully");

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void dispose() {
        try {
            conn.close();
            System.out.println("Closed database successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
