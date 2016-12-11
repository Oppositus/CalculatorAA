package com.calculator.aa.db;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;

import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Date;

public class SQLiteSupport {

    private Connection conn;

    public SQLiteSupport() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:db/instruments.sqlite");
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
            StringBuilder sql = new StringBuilder("SELECT * FROM `INSTRUMENTS` ");
            List<String> wheres = new ArrayList<>();

            if (it != null) {
                wheres.add(" `TYPE` = '" + escapeSQLite(it.toString()) + "' ");
            }
            if (provider != null && !provider.isEmpty()) {
                wheres.add(" `PROVIDER` = '" + escapeSQLite(provider) + "' ");
            }
            if (minMonths > 0) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.add(Calendar.MONTH, -minMonths);
                wheres.add(" date(`FROM`) <= date(" + printDate(cal) + ") ");
            }
            if (name != null && !name.isEmpty()) {
                String textFilter = " `TICKER` LIKE '" + escapeSQLite(name) + "%' ";
                if (name.length() >= 3) {
                    textFilter = " (" + textFilter + " OR `NAME` LIKE '%" + escapeSQLite(name) + "%') ";
                }
                wheres.add(textFilter);
            }

            if (wheres.size() > 0) {
                sql.append("WHERE (");
                sql.append(String.join(" AND ", wheres));
                sql.append(" )");
            }

            sql.append(" ORDER BY `TICKER` ;");

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

    public void setInstrumentHistory(Instrument instr, Instrument.ValueType value) {
        try {
            LinkedList<Date> datesRead = new LinkedList<>();
            LinkedList<Double> valuesRead = new LinkedList<>();

            Statement stmt = conn.createStatement();
            String sql = "SELECT `DATE`, `" + value.toString() + "` AS `VALUE` FROM `HISTORY` " +
                    "WHERE `TICKER` = '" + instr.getTicker() + "' ORDER BY date(`DATE`);";

            ResultSet result = stmt.executeQuery(sql);

            while (result.next()) {
                datesRead.add(convertToDate(result.getString("DATE")));
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
            String sql = "SELECT * FROM `INSTRUMENTS` WHERE (`TICKER` = '" + escapeSQLite(ticker) + "');";

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

    public List<String> getTypes() {
        List<String> types = new LinkedList<>();

        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT DISTINCT `TYPE` FROM `INSTRUMENTS`;";

            ResultSet result = stmt.executeQuery(sql);

            while (result.next()) {
                types.add(result.getString("TYPE"));
            }

            result.close();
            stmt.close();
            conn.commit();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }

        return types;
    }

    public List<String> getProviders() {
        List<String> providers = new LinkedList<>();

        try {
            Statement stmt = conn.createStatement();
            String sql = "SELECT DISTINCT `PROVIDER` FROM `INSTRUMENTS`;";

            ResultSet result = stmt.executeQuery(sql);

            while (result.next()) {
                providers.add(result.getString("PROVIDER"));
            }

            result.close();
            stmt.close();
            conn.commit();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }

        return providers;
    }

    static String printUnquotedDate(Date dt) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        return String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
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

    private Date convertToDate(String date) {
        String[] parts = date.split("-");
        Calendar cal = Calendar.getInstance();
        cal.set(Calc.safeParseInt(parts[0], 1900), Calc.safeParseInt(parts[1], 1) - 1, 1);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Instrument fromResultSet(ResultSet result) throws SQLException {
        return new Instrument(
                result.getString("TICKER"),
                result.getString("NAME"),
                getInstrumentType(result.getString("TYPE")),
                convertToDate(result.getString("FROM")),
                convertToDate(result.getString("TO")),
                result.getString("PROVIDER"),
                result.getString("SITE")
        );
    }
}
