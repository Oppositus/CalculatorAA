package com.calculator.aa.db;

import com.calculator.aa.ui.AATableModel;

import java.util.*;

public class Instrument {
    public enum InstrumentType {
        ETF,
        FUND,
        INDEX
    }

    public enum ValueType {
        OPEN,
        HIGH,
        LOW,
        CLOSE,
        CLOSEADJ
    }

    private final String ticker;
    private final String name;
    private final InstrumentType type;
    private final Date fromDate;
    private final Date toDate;
    private final String provider;
    private final String site;

    private final List<Date> dates;
    private final List<Double> values;

    public Instrument(String tk, String nm, InstrumentType it, Date fr, Date to, String pr, String st) {
        ticker = tk;
        name = nm;
        type = it;
        fromDate = fr;
        toDate = to;
        provider = pr;
        site = st;

        dates = new LinkedList<>();
        values = new LinkedList<>();
    }

    @Override
    public String toString() {
        return ticker + " (" + name + ")";
    }

    public String getTicker() {
        return ticker;
    }

    public String getName() {
        return name;
    }

    public InstrumentType getType() {
        return type;
    }

    void fillHistory(List<Date> dts, List<Double> vls) {
        if (dts.size() != vls.size()) {
            throw new IllegalArgumentException("dates.size() != values.size()");
        }

        dates.addAll(dts);
        values.addAll(vls);
    }

    public void makeAnnual(int month) {
        List<Date> dts = new ArrayList<>(dates);
        List<Double> vls = new ArrayList<>(values);

        dates.clear();
        values.clear();

        Calendar cal = Calendar.getInstance();
        int index = 0;
        for (Date dt : dts) {
            cal.setTime(dt);
            if (cal.get(Calendar.MONTH) == month) {
                dates.add(dt);
                values.add(vls.get(index));
            }
            index += 1;
        }
    }

    public AATableModel getModel() {
        int length = values.size();
        double[][] vals = new double[length][1];
        String[] periods = new String[length];
        for (int i = 0; i < length; i++) {
            vals[i][0] = values.get(i);
            periods[i] = SQLiteSupport.printUnquotedDate(dates.get(i));
        }

        return new AATableModel(
                1,
                length,
                vals,
                periods,
                new String[]{ ticker },
                true
        );
    }

    String getDownloaderName() {
        return provider;
    }
}
