package com.calculator.aa.db;

import com.calculator.aa.ui.AATableModel;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Instrument {
    private final String ticker;
    private List<String> head;
    private List<List<String>> body;

    private String[] periods;
    private double[] values;

    public Instrument(String tk, InstrumentsMeta.InstrumentType type) {
        ticker = tk;

        String name = ticker.toLowerCase() + "_" + type.toString().toLowerCase() + ".csv";
        ReaderCSV reader = new ReaderCSV(ReaderCSV.dbMark, ReaderCSV.dbDelim);
        reader.readFromFile("db/data/" + name);

        try {
            head = reader.head().toList().get(0);
            body = reader.body().toList();
        } catch (Exception ignored) {
            head = null;
            body = null;
        }
    }

    public boolean isValid() {
        return head != null && body != null;
    }

    public void applyFilter(InstrumentsMeta.ValueType vt, InstrumentsMeta.PeriodType pt, int month) {
        periods = body.stream()
                .map(el -> el.get(0))
                .collect(Collectors.toList())
                .toArray(new String[0]);

        int index = head.indexOf(InstrumentsMeta.ValueName.get(vt));
        values = body.stream()
                .mapToDouble(el -> Double.valueOf(el.get(index).replace(",", ".")))
                .toArray();

        if (pt == InstrumentsMeta.PeriodType.YEAR) {
            annualize(month);
        }
    }

    private void annualize(int month) {
        List<String> dts = new LinkedList<>();
        List<Double> vls = new LinkedList<>();

        int length = periods.length;
        for (int i = 0; i < length; i++) {
            int period = Integer.valueOf(periods[i].split("-")[1]) - 1;
            if (period == month) {
                dts.add(periods[i]);
                vls.add(values[i]);
            }
        }

        periods = dts.toArray(new String[0]);
        values = vls.stream().mapToDouble(d -> d).toArray();
    }

    public AATableModel getModel() {
        int length = values.length;
        double[][] vals = new double[length][1];
        for (int i = 0; i < length; i++) {
            vals[i][0] = values[i];
        }
        return new AATableModel(1, values.length, vals, periods, new String[]{ticker}, true);
    }

}
