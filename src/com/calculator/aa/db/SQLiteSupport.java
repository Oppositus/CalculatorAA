package com.calculator.aa.db;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SQLiteSupport {

    public static final String dataBaseName = "instruments.sqlite";
    private static final String sourceBaseName = "instruments.source";

    private Connection conn;

    public SQLiteSupport() {

        if (!copyDatabase()) {
            JOptionPane.showMessageDialog(null,
                    Main.resourceBundle.getString("text.no_base"),
                    Main.resourceBundle.getString("text.error"),
                    JOptionPane.ERROR_MESSAGE);
            conn = null;
            return;
        }

        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dataBaseName);
            conn.setAutoCommit(false);
        } catch (Exception e) {
            conn = null;
            JOptionPane.showMessageDialog(null, e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public void dispose() {
        try {
            conn.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public List<Instrument> filterInstruments(Instrument.InstrumentType it, String provider, int minMonths, String name) {

        List<Instrument> instruments = new LinkedList<>();

        try {
            Statement stmt = conn.createStatement();
            StringBuilder sql = new StringBuilder(
                    "SELECT " +
                        "`INSTRUMENTS`.`INSTRUMENT` AS `INSTRUMENT`, " +
                        "`INSTRUMENTS`.`TICKER` AS `TICKER`, " +
                        "`INSTRUMENTS`.`NAME` AS `NAME`, " +
                        "`CLASSES`.`NAME` AS `CLASS`, " +
                        "`INSTRUMENTS`.`SINCE` AS `SINCE`, " +
                        "`INSTRUMENTS`.`UPDATED` AS `UPDATED`, " +
                        "`DOWNLOADERS`.`NAME` AS `DOWNLOADER`, " +
                        "`DOWNLOADERS`.`URI` AS `URI` " +
                    "FROM `INSTRUMENTS` " +
                    "JOIN `CLASSES` ON (`INSTRUMENTS`.`CLASS` = `CLASSES`.`CLASS`) " +
                    "JOIN `DOWNLOADERS` ON (`INSTRUMENTS`.`DOWNLOADER` = `DOWNLOADERS`.`DOWNLOADER`) "
            );
            List<String> wheres = new ArrayList<>();

            if (it != null) {
                wheres.add(" `CLASSES`.`NAME` = '" + escapeSQLite(it.toString()) + "' ");
            }
            if (provider != null && !provider.isEmpty()) {
                wheres.add(" `DOWNLOADERS`.`NAME` = '" + escapeSQLite(provider) + "' ");
            }
            if (minMonths > 0) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.add(Calendar.MONTH, -minMonths);
                wheres.add(" DATE(`INSTRUMENTS`.`SINCE`) <= DATE(" + printDate(cal) + ") ");
            }
            if (name != null && !name.isEmpty()) {
                String textFilter = " `INSTRUMENTS`.`TICKER` LIKE '" + escapeSQLite(name) + "%' ";
                if (name.length() >= 3) {
                    textFilter = " (" + textFilter + " OR `INSTRUMENTS`.`NAME` LIKE '%" + escapeSQLite(name) + "%') ";
                }
                wheres.add(textFilter);
            }

            if (wheres.size() > 0) {
                sql.append("WHERE (");
                sql.append(String.join(" AND ", wheres));
                sql.append(" )");
            }

            sql.append(" ORDER BY `INSTRUMENTS`.`TICKER` ;");

            ResultSet result = stmt.executeQuery(sql.toString());

            while (result.next()) {
                instruments.add(fromResultSet(result));
            }

            result.close();
            stmt.close();
            conn.commit();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }

        return instruments;
    }

    Date getLastUpdateDate(Instrument instr, boolean reload) {
        String last = "1900-01-01";
        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT `SINCE`, `UPDATED` FROM `INSTRUMENTS` WHERE `TICKER` = '" + escapeSQLite(instr.getTicker()) + "';";

            ResultSet result = stmt.executeQuery(sql);

            if (result.next()) {
                last = result.getString("UPDATED");
                if (reload || last == null) {
                    last = result.getString("SINCE");
                    if (last == null) {
                        last = "1900-01-01";
                    }
                }
            } else {
                last = "1900-01-01";
            }

            result.close();
            stmt.close();
            conn.commit();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }

        return convertToDate(last, null);
    }

    public void updateInstrumentHistory(Instrument instr, boolean reload, JLabel progress, Consumer<Boolean> after) {
        DataDownloader downloader = DownloaderFactory.getDownloader(instr.getDownloaderName());
        if (downloader == null) {
            after.accept(false);
        } else {
            progress.setText(String.format(Main.resourceBundle.getString("text.progress_download"), instr.getTicker()));
            SwingUtilities.invokeLater(() -> downloader.download(instr, reload, (s, r) -> {
                progress.setText(String.format(Main.resourceBundle.getString("text.progress_insert"), instr.getTicker()));
                SwingUtilities.invokeLater(() -> {
                    if (reload) {
                        clearInstrumentData(instr);
                    }
                    updateInstrumentHistoryResult(downloader, instr, after, s, r);
                });
            }));
        }
    }

    int getDownloaderId(DataDownloader downloader) {
        int id = -1;

        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT `DOWNLOADER` FROM `DOWNLOADERS` WHERE `NAME` = '" + escapeSQLite(downloader.getName()) + "';";

            ResultSet result = stmt.executeQuery(sql);

            if (result.next()) {
                id = result.getInt("DOWNLOADER");
            }

            conn.commit();
            stmt.close();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }

        return id;
    }

    public String getDatabaseVersion() {
        String version = "0";

        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT `VER` FROM `VERSION` LIMIT 1;";

            ResultSet result = stmt.executeQuery(sql);

            if (result.next()) {
                version = result.getString("VER");
            }

            conn.commit();
            stmt.close();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }

        return version;
    }

    private void clearInstrumentData(Instrument instr) {
        try {
            Statement stmt = conn.createStatement();
            String sql = "DELETE FROM `DATA` WHERE `INSTRUMENT` = " + instr.getId() + ";";

            stmt.executeUpdate(sql);

            conn.commit();
            stmt.close();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateInstrumentHistoryResult(DataDownloader downloader, Instrument instr, Consumer<Boolean> after, Boolean success, String data) {
        if (!success) {
            after.accept(false);
        } else {
            boolean result = false;

            if (!data.isEmpty()) {
                ReaderCSV csv = downloader.createReader();
                ReaderCSV csvBody = csv.readFromString(data).body();
                List<String> lines = csvBody.lines()
                        .sorted(downloader.getDateComparator())
                        .map(line -> "(NULL, " +
                                instr.getId() + ", " +
                                printDate(downloader.getDate(line)) + ", " +
                                downloader.getOpen(line) + ", " +
                                downloader.getHigh(line) + ", " +
                                downloader.getLow(line) + ", " +
                                downloader.getClose(line) + ", " +
                                downloader.getCloseAdj(line) + ", " +
                                downloader.getVolume(line) + ")")
                        .collect(Collectors.toList());

                insertOrUpdate(String.join(",", lines));

                try {
                    Date now = dateNow();
                    Statement stmt = conn.createStatement();
                    String sql = "UPDATE `INSTRUMENTS` SET " +
                            "`UPDATED`=" + printDate(now) + " " +
                            "WHERE `TICKER`='" + escapeSQLite(instr.getTicker()) + "';";

                    stmt.executeUpdate(sql);

                    conn.commit();
                    stmt.close();

                    result = true;

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
                }
            } else {
                result = true;
            }

            after.accept(result);
        }
    }

    private void insertOrUpdate(String values) {
        try {
            Statement stmt = conn.createStatement();
            String sql = "INSERT OR REPLACE INTO `DATA` " +
                    "VALUES " + values + ";";

            stmt.executeUpdate(sql);

            conn.commit();
            stmt.close();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setInstrumentHistory(Instrument instr, Instrument.ValueType value) {
        try {
            LinkedList<Date> datesRead = new LinkedList<>();
            LinkedList<Double> valuesRead = new LinkedList<>();

            Statement stmt = conn.createStatement();
            String sql = "SELECT " +
                            "`DATE`, `" + value.toString() + "` AS `VALUE` " +
                         "FROM `DATA` JOIN `INSTRUMENTS` USING (`INSTRUMENT`) " +
                         "WHERE `INSTRUMENTS`.`TICKER` = '" + escapeSQLite(instr.getTicker()) + "' " +
                         "ORDER BY date(`DATE`);";

            ResultSet result = stmt.executeQuery(sql);

            while (result.next()) {
                datesRead.add(convertToDate(result.getString("DATE"), null));
                valuesRead.add(result.getDouble("VALUE"));
            }

            result.close();
            stmt.close();
            conn.commit();

            instr.fillHistory(datesRead, valuesRead);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public Instrument findInstrument(String ticker) {
        Instrument instr = null;

        try {
            Statement stmt = conn.createStatement();

            String sql =
                    "SELECT " +
                        "`INSTRUMENTS`.`INSTRUMENT` AS `INSTRUMENT`, " +
                        "`INSTRUMENTS`.`TICKER` AS `TICKER`, " +
                        "`INSTRUMENTS`.`NAME` AS `NAME`, " +
                        "`CLASSES`.`NAME` AS `CLASS`, " +
                        "`INSTRUMENTS`.`SINCE` AS `SINCE`, " +
                        "`INSTRUMENTS`.`UPDATED` AS `UPDATED`, " +
                        "`DOWNLOADERS`.`NAME` AS `DOWNLOADER`, " +
                        "`DOWNLOADERS`.`URI` AS `URI` " +
                    "FROM `INSTRUMENTS` " +
                    "JOIN `CLASSES` ON (`INSTRUMENTS`.`CLASS` = `CLASSES`.`CLASS`) " +
                    "JOIN `DOWNLOADERS` ON (`INSTRUMENTS`.`DOWNLOADER` = `DOWNLOADERS`.`DOWNLOADER`) " +
                    "WHERE (`TICKER` = '" + escapeSQLite(ticker) + "');";

            ResultSet result = stmt.executeQuery(sql);

            if (result.next()) {
                instr = fromResultSet(result);
            }

            result.close();
            stmt.close();
            conn.commit();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }

        return instr;
    }

    public List<String> getClasses() {
        List<String> classes = new LinkedList<>();

        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT `NAME` FROM `CLASSES`;";

            ResultSet result = stmt.executeQuery(sql);

            while (result.next()) {
                classes.add(result.getString("NAME"));
            }

            result.close();
            stmt.close();
            conn.commit();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }

        return classes;
    }

    private int getClassId(Instrument.InstrumentType type) {
        int classId = -1;
        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT `CLASS` FROM `CLASSES` WHERE `NAME`='" + type.toString() + "';";

            ResultSet result = stmt.executeQuery(sql);

            while (result.next()) {
                classId = result.getInt("CLASS");
            }

            result.close();
            stmt.close();
            conn.commit();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }

        return classId;
    }

    public List<String> getProviders() {
        List<String> providers = new LinkedList<>();

        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT `NAME` FROM `DOWNLOADERS`;";

            ResultSet result = stmt.executeQuery(sql);

            while (result.next()) {
                providers.add(result.getString("NAME"));
            }

            result.close();
            stmt.close();
            conn.commit();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }

        return providers;
    }

    public static String printUnquotedDate(Date dt) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        return String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    static private String printDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return printDate(cal);
    }

    static private String printDate(Calendar cal) {
        return String.format("'%04d-%02d-%02d'", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    private String escapeSQLite(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c: s.toCharArray()) {
            sb.append(c == '\'' ? "''" : c);
        }
        return sb.toString();
    }

    public static Instrument.InstrumentType getInstrumentType(String type) {
        String t = type.toUpperCase();
        switch (t) {
            case "ETF":
                return Instrument.InstrumentType.ETF;
            case "FUND":
                return Instrument.InstrumentType.FUND;
            case "INDEX":
                return Instrument.InstrumentType.INDEX;
            default:
                return null;
        }
    }

    private Date convertToDate(String date, String defaultDate) {

        String useDate = date;
        if (useDate == null) {
            useDate = defaultDate;
        }
        if (useDate == null) {
            useDate = "1900-01-01";
        }

        String[] parts = useDate.split("-");
        Calendar cal = Calendar.getInstance();
        cal.set(Calc.safeParseInt(parts[0], 1900), Calc.safeParseInt(parts[1], 1) - 1, Calc.safeParseInt(parts[2], 1));
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date dateNow() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Instrument fromResultSet(ResultSet result) throws SQLException {
        return new Instrument(
                result.getInt("INSTRUMENT"),
                result.getString("TICKER"),
                result.getString("NAME"),
                getInstrumentType(result.getString("CLASS")),
                convertToDate(result.getString("SINCE"), null),
                convertToDate(result.getString("UPDATED"), result.getString("SINCE")),
                result.getString("DOWNLOADER"),
                result.getString("URI")
        );
    }

    private boolean copyDatabase() {
        if (Files.exists(Paths.get(dataBaseName))) {
            return true;
        } else if (Files.exists(Paths.get(sourceBaseName))) {
            try {
                Files.copy(Paths.get(SQLiteSupport.sourceBaseName), Paths.get(SQLiteSupport.dataBaseName));
                return true;
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else {
            return false;
        }
    }
}
