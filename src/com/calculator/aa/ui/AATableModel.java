package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.Zipper;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AATableModel extends AbstractTableModel {

    private final Pattern ptYYYY = Pattern.compile("^(\\d\\d\\d\\d)$");
    private final Pattern ptMM_YYYY = Pattern.compile("^(\\d\\d?)[^\\d](\\d\\d\\d\\d)$");
    private final Pattern ptYYYY_MM = Pattern.compile("^(\\d\\d\\d\\d)[^\\d](\\d\\d?)$");
    private final Pattern ptXX_XX_YYYY = Pattern.compile("^(\\d\\d?)[^\\d](\\d\\d?)[^\\d](\\d\\d\\d\\d)$");
    private final Pattern ptYYYY_MM_DD = Pattern.compile("^(\\d\\d\\d\\d)[^\\d](\\d\\d?)[^\\d](\\d\\d?)$");

    private final int width;
    private final int height;
    private double[][]data;
    private final String[] instruments;
    private Object[] periods;
    private final String[] periodsSource;
    private final double[] averages;
    private final double[] deviations;

    private MainWindow.DateFormats dateFormat;

    // Default constructor
    AATableModel() {
        this(1, 2);
    }

    // Create new table w*h
    private AATableModel(int w, int h) {
        width = w + 1;
        height = h + 2;
        data = new double[height - 2][width - 1];
        instruments = new String[width];
        periods = new Object[height - 2];
        periodsSource = new String[height - 2];
        averages = new double[width - 1];
        deviations = new double[width - 1];
        dateFormat = MainWindow.DateFormats.DATE_FORMAT_NONE;

        instruments[0] = "";
        for (int wh = 0; wh < width; wh++) {
            if (wh > 0) {
                instruments[wh] = String.format("%s %d", Main.resourceBundle.getString("text.instrument"), wh);
            } else {
                instruments[wh] = "";
            }

            for (int ht = 0; ht < height - 2; ht++) {
                if (wh < width - 1) {
                    data[ht][wh] = 0.0f;
                }
                periods[ht] = String.format("%s %d", Main.resourceBundle.getString("text.period"), ht);
                periodsSource[ht] = String.format("%s %d", Main.resourceBundle.getString("text.period"), ht);
            }
        }
    }

    // Create new table w*h and copy output from previous model
    AATableModel(int w, int h, AATableModel prev, int ignoredRow, int ignoredColumn) {
        this(w, h);

        dateFormat = prev.dateFormat;

        Object[] prevPeriods = prev.periods;
        String[] prevPeriodsSource = prev.periodsSource;
        String[] prevInstruments = prev.instruments;
        double[][] prevData = prev.data;
        int lengthPer = prevPeriods.length;
        int lengthInstr = prevInstruments.length;

        if (ignoredRow >= 0) {
            for (int ht = ignoredRow; ht < lengthPer - 1; ht++) {
                prevPeriods[ht] = prevPeriods[ht + 1];
                prevPeriodsSource[ht] = prev.periodsSource[ht + 1];
                prevData[ht] = prevData[ht + 1];
            }
        }

        if (ignoredColumn >= 0) {
            for (int wh = ignoredColumn; wh < lengthInstr - 1; wh++) {
                prevInstruments[wh] = prevInstruments[wh + 1];
                for (int ht = 0; ht < lengthPer; ht++) {
                    if (wh < lengthInstr - 1) {
                        prevData[ht][wh - 1] = prevData[ht][wh];
                    }
                }
            }
        }

        for (int wh = 0; wh < width; wh++) {
            if (wh > prev.width - 1) {
                break;
            }

            instruments[wh] = prev.instruments[wh];

            for (int ht = 0; ht < height - 2; ht++) {

                if (ht >= prev.height - 2) {
                    break;
                }

                if (wh < width - 1) {
                    data[ht][wh] = prevData[ht][wh];
                }

                periods[ht] = prevPeriods[ht];
                periodsSource[ht] = prevPeriodsSource[ht];
            }

            if (wh > 0) {
                updateAverage(wh - 1);
                updateStDev(wh - 1);
            }
        }
    }

    // Create new table w*h and copy output from output
    public AATableModel(int w, int h, double[][] d, String[] l, String[] i, boolean useDates) {
        this(w, h);

        for (int wh = 0; wh < width; wh++) {
            if (wh > 0) {
                instruments[wh] = i[wh - 1];
            } else {
                instruments[wh] = "";
            }

            for (int ht = 0; ht < height - 2; ht++) {
                if (wh < width - 1) {
                    data[ht][wh] = d[ht][wh];
                }
                periods[ht] = l[ht];
                periodsSource[ht] = l[ht];
            }

            if (wh > 0) {
                updateAverage(wh - 1);
                updateStDev(wh - 1);
            }
        }

        if (useDates) {
            if (tryFormatDates()) {
                sortByDates();
            }
        }
    }

    @Override
    public int getRowCount() {
        return height;
    }

    @Override
    public int getColumnCount() {
        return width;
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (row == height - 2) {
            return col == 0 ? Main.resourceBundle.getString("text.yield") : (height < 4 ? "" : Calc.formatPercent2(averages[col - 1]));
        } else if (row == height - 1) {
            return col == 0 ? Main.resourceBundle.getString("text.risk") : (height < 5 ? "" : Calc.formatPercent2(deviations[col - 1]));
        } else {
            if (col == 0) {
                return formatPeriod(row);
            }

            double v = data[row][col - 1];
            return v >= 0 ? v : "";
        }
    }

    @Override
    public String getColumnName(int col) {
        return instruments[col];
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return row < height - 2;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        String val = (String)value;
        if (col == 0) {
            periods[row] = val;
        } else {
            data[row][col - 1] = Double.valueOf(val);
            updateAverage(col - 1);
            fireTableCellUpdated(height - 2, col);

            updateStDev(col - 1);
            fireTableCellUpdated(height - 1, col);
        }
        fireTableCellUpdated(row, col);
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    double[][] getData() {
        return data;
    }

    public String[] getInstruments() {
        return instruments;
    }

    public Object[] getPeriods() {
        return periods;
    }

    String formatPeriod(int p) {
        Object period = periods[p];

        if (period instanceof String) {
            return  (String)period;
        } else if (period instanceof Date) {
            Calendar c = Calendar.getInstance();
            c.setTime((Date)period);

            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH) + 1;
            int day = c.get(Calendar.DAY_OF_MONTH);

            switch (dateFormat) {
                case DATE_FORMAT_YYYY:
                    return String.format(Main.resourceBundle.getString("text.date_format.yyyy"), year);
                case DATE_FORMAT_MM_YYYY:
                    return String.format(Main.resourceBundle.getString("text.date_format.mm_yyyy"), month, year);
                case DATE_FORMAT_YYYY_MM:
                    return String.format(Main.resourceBundle.getString("text.date_format.yyyy_mm"), year, month);
                case DATE_FORMAT_DD_MM_YYYY:
                    return String.format(Main.resourceBundle.getString("text.date_format.xx_xx_yyyy"), day, month, year);
                case DATE_FORMAT_MM_DD_YYYY:
                    return String.format(Main.resourceBundle.getString("text.date_format.xx_xx_yyyy"), month, day, year);
                case DATE_FORMAT_YYYY_MM_DD:
                    return String.format(Main.resourceBundle.getString("text.date_format.yyyy_mm_dd"), year, month, day);
            }

            return "?/?/?";
        } else {
            return period.toString();
        }
    }

    private void sortByDates() {
        int length = periods.length;
        List<AbstractMap.SimpleEntry<Date, Integer>> pairs = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            pairs.add(new AbstractMap.SimpleEntry<>((Date)periods[i], i));
        }
        pairs.sort(Comparator.comparing(AbstractMap.SimpleEntry::getKey));

        Object[] newPeriods = new Object[length];
        double[][] newData = new double[height - 2][width - 1];

        for (int i = 0; i < length; i++) {
            int oldPos = pairs.get(i).getValue();
            newPeriods[i] = periods[oldPos];
            newData[i] = data[oldPos];
        }

        periods = newPeriods;
        data = newData;
    }

    String[] formatPeriods() {

        int length = periods.length;
        String[] result = new String[length];

        for(int i = 0; i < length; i++) {
            result[i] = formatPeriod(i);
        }
        return result;
    }

    Zipper<String, Double, String> toZipper() {

        int cols = data[0].length;

        List<String> keys = Arrays.asList(periodsSource);
        List<List<Double>> values = new LinkedList<>();
        List<String> labels = Arrays.asList(Arrays.copyOfRange(instruments, 1, instruments.length));

        for (double[] row : data) {
            List<Double> r = new LinkedList<>();
            for (int col = 0; col < cols; col++) {
                r.add(row[col]);
            }
            values.add(r);
        }

        return new Zipper<>(keys, values, labels);
    }

    static AATableModel fromZipper(Zipper<String, Double, String> z, String[] options) {
        String dates = options != null ? options[3] : Main.properties.getProperty("import.date", "1");

        List<String> labels = z.keys();
        List<List<Double>> datas = z.values();
        List<String> columns = z.labels();

        int htLength = datas.size();
        int whLength = columns.size();
        double[][] rawData = new double[htLength][whLength];

        for (int ht = 0; ht < htLength; ht++) {
            for (int wh = 0; wh < whLength; wh++) {
                rawData[ht][wh] = datas.get(ht).get(wh);
            }
        }

        return new AATableModel(whLength, htLength, rawData, labels.toArray(new String[0]), columns.toArray(new String[0]), "1".equals(dates));
    }

    public boolean isRowValid(int row) {
        return Arrays.stream(data[row]).allMatch(d -> d > 0);
    }

    private double[] getCol(int col) {
        int length = height - 2;
        double[] values = new double[length];
        for (int i = 0; i < length; i++) {
            values[i] = data[i][col];
        }
        return values;
    }

    private void updateAverage(int col) {
        averages[col] = Calc.averagePercentYields(getCol(col));
    }

    private void updateStDev(int col) {

        if (height < 5) {
            return;
        }

        deviations[col] = Calc.stdevYields(getCol(col));
    }

    private boolean tryFormatDates() {
        if (Arrays.stream(periodsSource).allMatch(this::acceptYYYY)) {
            periods = Arrays.stream(periodsSource).map(this::formatYYYY).toArray();
            return true;
        } else if (Arrays.stream(periodsSource).allMatch(this::acceptMM_YYYY)) {
            periods = Arrays.stream(periodsSource).map(this::formatMM_YYYY).toArray();
            return true;
        } else if (Arrays.stream(periodsSource).allMatch(this::acceptYYYY_MM)) {
            periods = Arrays.stream(periodsSource).map(this::formatYYYY_MM).toArray();
            return true;
        } else if (Arrays.stream(periodsSource).allMatch(this::acceptXX_XX_YYYY)) {
            Locale l = Locale.getDefault();

            if ("US".equals(l.getCountry().toUpperCase())) {
                periods = Arrays.stream(periodsSource).map(this::formatMM_DD_YYYY).toArray();
                if (Arrays.stream(periods).anyMatch(Objects::isNull)) {
                    periods = Arrays.stream(periodsSource).map(this::formatDD_MM_YYYY).toArray();
                }
            } else {
                periods = Arrays.stream(periodsSource).map(this::formatDD_MM_YYYY).toArray();
                if (Arrays.stream(periods).anyMatch(Objects::isNull)) {
                    periods = Arrays.stream(periodsSource).map(this::formatMM_DD_YYYY).toArray();
                }
            }

            return true;
        } else if (Arrays.stream(periodsSource).allMatch(this::acceptYYYY_MM_DD)) {
            periods = Arrays.stream(periodsSource).map(this::formatYYYY_MM_DD).toArray();
            return true;
        }

        return false;
    }

    private boolean acceptYYYY(Object s) {
        return ptYYYY.matcher(s.toString()).matches();
    }

    private Date formatYYYY(Object s) {
        dateFormat = MainWindow.DateFormats.DATE_FORMAT_YYYY;
        Calendar c = Calendar.getInstance();
        c.set(Calc.safeParseInt(s.toString(), -1), 0, 1);

        return c.getTime();
    }

    private boolean acceptMM_YYYY(Object s) {
        Matcher m = ptMM_YYYY.matcher(s.toString());
        if (m.matches()) {
            try {
                int mm = Calc.safeParseInt(m.group(1), -1);
                return mm >= 1 && mm <= 12;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private Date formatMM_YYYY(Object s) {
        Matcher m = ptMM_YYYY.matcher(s.toString());
        m.matches();
        int mm = Calc.safeParseInt(m.group(1), -1);
        int yy = Calc.safeParseInt(m.group(2), -1);

        dateFormat = MainWindow.DateFormats.DATE_FORMAT_MM_YYYY;
        Calendar c = Calendar.getInstance();
        c.set(yy, mm - 1, 1);

        return c.getTime();
    }

    private boolean acceptYYYY_MM(Object s) {
        Matcher m = ptYYYY_MM.matcher(s.toString());
        if (m.matches()) {
            try {
                int mm = Calc.safeParseInt(m.group(2), -1);
                return mm >= 1 && mm <= 12;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private Date formatYYYY_MM(Object s) {
        Matcher m = ptYYYY_MM.matcher(s.toString());
        m.matches();
        int yy = Calc.safeParseInt(m.group(1), -1);
        int mm = Calc.safeParseInt(m.group(2), -1);

        dateFormat = MainWindow.DateFormats.DATE_FORMAT_YYYY_MM;
        Calendar c = Calendar.getInstance();
        c.set(yy, mm - 1, 1);

        return c.getTime();
    }

    private boolean acceptXX_XX_YYYY(Object s) {
        Matcher m = ptXX_XX_YYYY.matcher(s.toString());
        if (m.matches()) {
            try {
                int p1 = Calc.safeParseInt(m.group(1), -1);
                int p2 = Calc.safeParseInt(m.group(2), -1);
                return (p1 >= 1 && p1 <= 31 && p2 >= 1 && p2 <= 12) || (p1 >= 1 && p1 <= 12 && p2 >= 1 && p2 <= 31);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private Date formatDD_MM_YYYY(Object s) {
        Matcher m = ptXX_XX_YYYY.matcher(s.toString());
        m.matches();
        int dd = Calc.safeParseInt(m.group(1), -1);
        int mm = Calc.safeParseInt(m.group(2), -1);
        int yy = Calc.safeParseInt(m.group(3), -1);

        if (mm > 12) {
            return null;
        }

        dateFormat = MainWindow.DateFormats.DATE_FORMAT_DD_MM_YYYY;
        Calendar c = Calendar.getInstance();
        c.set(yy, mm - 1, dd);

        return c.getTime();
    }

    private Date formatMM_DD_YYYY(Object s) {
        Matcher m = ptXX_XX_YYYY.matcher(s.toString());
        m.matches();
        int mm = Calc.safeParseInt(m.group(1), -1);
        int dd = Calc.safeParseInt(m.group(2), -1);
        int yy = Calc.safeParseInt(m.group(3), -1);

        if (mm > 12) {
            return null;
        }

        dateFormat = MainWindow.DateFormats.DATE_FORMAT_MM_DD_YYYY;
        Calendar c = Calendar.getInstance();
        c.set(yy, mm - 1, dd);

        return c.getTime();
    }

    private boolean acceptYYYY_MM_DD(Object s) {
        Matcher m = ptYYYY_MM_DD.matcher(s.toString());
        if (m.matches()) {
            try {
                int mm = Calc.safeParseInt(m.group(2), -1);
                int dd = Calc.safeParseInt(m.group(3), -1);
                return mm >= 1 && mm <= 12 && dd >= 1 && dd <= 31;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private Date formatYYYY_MM_DD(Object s) {
        Matcher m = ptYYYY_MM_DD.matcher(s.toString());
        m.matches();
        int yy = Calc.safeParseInt(m.group(1), -1);
        int mm = Calc.safeParseInt(m.group(2), -1);
        int dd = Calc.safeParseInt(m.group(3), -1);

        dateFormat = MainWindow.DateFormats.DATE_FORMAT_YYYY_MM_DD;
        Calendar c = Calendar.getInstance();
        c.set(yy, mm - 1, dd);

        return c.getTime();
    }

}