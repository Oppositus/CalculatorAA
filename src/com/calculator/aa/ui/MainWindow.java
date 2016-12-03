package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.Zipper;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainWindow {
    private JTable mainTable;
    private JButton buttonOpen;
    private JPanel mainPanel;
    private JScrollPane scrollPane;
    private JButton buttonAddRow;
    private JButton buttonDeleteRow;
    private JButton buttonCorrelations;
    private JButton buttonCovariances;
    private JButton buttonPortfolio;
    private JButton buttonDeleteInvalid;
    private JButton buttonSave;
    private JButton buttonMerge;
    private JButton buttonRemoveColumn;

    private String[] savedOptions;
    private String lastFileName;

    private class AATableCellRenderer extends DefaultTableCellRenderer {
        private final Color back = new Color(212, 212, 212);
        private final Color badBack = new Color(255, 224, 224);
        private final int rows;

        AATableCellRenderer(int r) {
            rows = r;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            Font font = cell.getFont();

            if (row >= rows - 2) {
                font = font.deriveFont(Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD));
                cell.setBackground(back);
            } else {
                if (col > 0) {
                    double data = ((AATableModel)table.getModel()).data[row][col - 1];
                    cell.setBackground(data >= 0 ? Color.WHITE : badBack);
                } else {
                    cell.setBackground(Color.WHITE);
                }
                font = font.deriveFont(Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR));
            }
            cell.setFont(font);
            return cell;
        }
    }

    enum DateFormats {
        DATE_FORMAT_NONE,
        DATE_FORMAT_YYYY,
        DATE_FORMAT_MM_YYYY,
        DATE_FORMAT_YYYY_MM,
        DATE_FORMAT_DD_MM_YYYY,
        DATE_FORMAT_MM_DD_YYYY,
        DATE_FORMAT_YYYY_MM_DD
    }

    private static class AATableModel extends AbstractTableModel {

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

        private DateFormats dateFormat;

        // Default constructor
        private AATableModel() {
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
            dateFormat = DateFormats.DATE_FORMAT_NONE;

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

        // Create new table w*h and copy data from previous model
        private AATableModel(int w, int h, AATableModel prev, int ignoredRow, int ignoredColumn) {
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

        // Create new table w*h and copy data from data
        private AATableModel(int w, int h, double[][] d, String[] l, String[] i, boolean useDates) {
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
            String dates = options != null ? options[3] : Main.getProperties().getProperty("import.date", "1");

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

        private double[] getCol(int col) {
            int length = height - 2;
            double[] values = new double[length];
            for (int i = 0; i < length; i++) {
                values[i] = data[i][col];
            }
            return values;
        }

        private void updateAverage(int col) {
            averages[col] = Calc.averageRealYields(getCol(col));
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
            dateFormat = DateFormats.DATE_FORMAT_YYYY;
            Calendar c = Calendar.getInstance();
            c.set(Integer.parseInt(s.toString()), 0, 1);

            return c.getTime();
        }

        private boolean acceptMM_YYYY(Object s) {
            Matcher m = ptMM_YYYY.matcher(s.toString());
            if (m.matches()) {
                try {
                    int mm = Integer.parseInt(m.group(1));
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
            int mm = Integer.parseInt(m.group(1));
            int yy = Integer.parseInt(m.group(2));

            dateFormat = DateFormats.DATE_FORMAT_MM_YYYY;
            Calendar c = Calendar.getInstance();
            c.set(yy, mm - 1, 1);

            return c.getTime();
        }

        private boolean acceptYYYY_MM(Object s) {
            Matcher m = ptYYYY_MM.matcher(s.toString());
            if (m.matches()) {
                try {
                    int mm = Integer.parseInt(m.group(2));
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
            int yy = Integer.parseInt(m.group(1));
            int mm = Integer.parseInt(m.group(2));

            dateFormat = DateFormats.DATE_FORMAT_YYYY_MM;
            Calendar c = Calendar.getInstance();
            c.set(yy, mm - 1, 1);

            return c.getTime();
        }

        private boolean acceptXX_XX_YYYY(Object s) {
            Matcher m = ptXX_XX_YYYY.matcher(s.toString());
            if (m.matches()) {
                try {
                    int p1 = Integer.parseInt(m.group(1));
                    int p2 = Integer.parseInt(m.group(2));
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
            int dd = Integer.parseInt(m.group(1));
            int mm = Integer.parseInt(m.group(2));
            int yy = Integer.parseInt(m.group(3));

            if (mm > 12) {
                return null;
            }

            dateFormat = DateFormats.DATE_FORMAT_DD_MM_YYYY;
            Calendar c = Calendar.getInstance();
            c.set(yy, mm - 1, dd);

            return c.getTime();
        }

        private Date formatMM_DD_YYYY(Object s) {
            Matcher m = ptXX_XX_YYYY.matcher(s.toString());
            m.matches();
            int mm = Integer.parseInt(m.group(1));
            int dd = Integer.parseInt(m.group(2));
            int yy = Integer.parseInt(m.group(3));

            if (mm > 12) {
                return null;
            }

            dateFormat = DateFormats.DATE_FORMAT_MM_DD_YYYY;
            Calendar c = Calendar.getInstance();
            c.set(yy, mm - 1, dd);

            return c.getTime();
        }

        private boolean acceptYYYY_MM_DD(Object s) {
            Matcher m = ptYYYY_MM_DD.matcher(s.toString());
            if (m.matches()) {
                try {
                    int mm = Integer.parseInt(m.group(2));
                    int dd = Integer.parseInt(m.group(3));
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
            int yy = Integer.parseInt(m.group(1));
            int mm = Integer.parseInt(m.group(2));
            int dd = Integer.parseInt(m.group(3));

            dateFormat = DateFormats.DATE_FORMAT_YYYY_MM_DD;
            Calendar c = Calendar.getInstance();
            c.set(yy, mm - 1, dd);

            return c.getTime();
        }

    }

    public MainWindow() {
        buttonAddRow.addActionListener(actionEvent -> {
            AATableModel oldModel = (AATableModel)mainTable.getModel();
            AATableModel newModel = new AATableModel(oldModel.width - 1, oldModel.height - 1, oldModel, -1, -1);
            mainTable.setModel(newModel);

            for (int i = 0; i < newModel.width; i++) {
                mainTable.getColumnModel().getColumn(i).setCellRenderer(new AATableCellRenderer(newModel.height));
            }
        });
        buttonDeleteRow.addActionListener(actionEvent -> {
            AATableModel oldModel = (AATableModel)mainTable.getModel();
            if (oldModel.height > 4) {
                AATableModel newModel = new AATableModel(oldModel.width - 1,
                        oldModel.height - 3,
                        oldModel,
                        mainTable.getSelectedRow(), -1);
                mainTable.setModel(newModel);

                for (int i = 0; i < newModel.width; i++) {
                    mainTable.getColumnModel().getColumn(i).setCellRenderer(new AATableCellRenderer(newModel.height));
                }
            }
        });
        buttonOpen.addActionListener(actionEvent -> {
            File[] f = openExistingFile(true);
            if (f != null) {
                askCSVOptions(true, () -> {
                    try {
                        AATableModel newModel = parseCSVAndLoadData(f[0]);
                        setNewModel(newModel);

                        Stream.of(Arrays.copyOfRange(f, 1, f.length)).forEach(this::verboseParseCSVAndMergeData);

                        Properties prop = Main.getProperties();

                        String files = String.join(
                                ";",
                                Arrays.stream(f).map(fl -> {
                                    try {
                                        return fl.getCanonicalPath();
                                    } catch (IOException e) {
                                        return "";
                                    }
                                }).collect(Collectors.toList()));

                        prop.setProperty("files.last", files);
                        lastFileName = f[0].getCanonicalPath();

                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        });
        buttonCorrelations.addActionListener(actionEvent -> {

            AATableModel model = (AATableModel)mainTable.getModel();
            double[][] corrTable = Calc.correlationTable(model.data);
            String[] cols = Arrays.copyOfRange(model.instruments, 1, model.instruments.length);

            ShowTable.show(Main.resourceBundle.getString("text.correlations_table"), corrTable, cols, cols);
        });
        buttonCovariances.addActionListener(actionEvent -> {
            AATableModel model = (AATableModel)mainTable.getModel();
            double[][] covTable = Calc.covarianceTable(model.data);
            String[] cols = Arrays.copyOfRange(model.instruments, 1, model.instruments.length);

            ShowTable.show(Main.resourceBundle.getString("text.covariances_table"), covTable, cols, cols);
        });

        Action showPortfolioAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AATableModel model = (AATableModel)mainTable.getModel();
                PortfolioChart.showChart(model.instruments, model.data);
            }
        };
        KeyStroke keyShiftEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.CTRL_MASK);
        buttonPortfolio.getActionMap().put("showPortfolioAction", showPortfolioAction);
        buttonPortfolio.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyShiftEnter, "showPortfolioAction");
        buttonPortfolio.addActionListener(showPortfolioAction);

        buttonDeleteInvalid.addActionListener(e -> {
            AATableModel model = (AATableModel)mainTable.getModel();
            if (model.height < 5) {
                return;
            }

            boolean valid = false;
            while (!valid) {
                double[] row = model.data[0];
                for (double aRow : row) {
                    if (aRow < 0) {
                        model = new AATableModel(model.width - 1,
                                model.height - 3,
                                model,
                                0, -1);
                        valid = false;
                        break;
                    } else {
                        valid = true;
                    }
                }
            }

            if (model.height < 5) {
                return;
            }

            mainTable.setModel(model);

            for (int i = 0; i < model.width; i++) {
                mainTable.getColumnModel().getColumn(i).setCellRenderer(new AATableCellRenderer(model.height));
            }
        });

        buttonSave.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(lastFileName == null ? "." : lastFileName);
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(false);
            fc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory() || file.getName().toLowerCase().endsWith(".csv");
                }

                @Override
                public String getDescription() {
                    return Main.resourceBundle.getString("text.csv_extension");
                }
            });

            int result = fc.showSaveDialog(Main.getFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                String filePath = f.getAbsolutePath();
                if(!filePath.endsWith(".csv")) {
                    f = new File(filePath + ".csv");
                }

                if (f.exists()) {
                    int overwrite = JOptionPane.showConfirmDialog(
                            mainPanel,
                            String.format(Main.resourceBundle.getString("text.overwrite"), f.getName()),
                            Main.resourceBundle.getString("text.warning"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (overwrite != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                File ff = f;
                askCSVOptions(false, () -> parseCSVAndSaveData(ff));
            }
        });
        buttonMerge.addActionListener(actionEvent -> {
            File[] f = openExistingFile(false);
            if (f != null) {
                askCSVOptions(true, () -> {
                    verboseParseCSVAndMergeData(f[0]);

                    Properties prop = Main.getProperties();
                    String names = prop.getProperty("files.last", "");
                    try {
                        String path = f[0].getCanonicalPath();
                        if (names.isEmpty()) {
                            prop.setProperty("files.last", path);
                        } else if (!names.contains(path)) {
                            prop.setProperty("files.last", names + ";" + path);
                        }
                    } catch (Exception ignored) {

                    }
                });
            }
        });
        buttonRemoveColumn.addActionListener(actionEvent -> {
            AATableModel oldModel = (AATableModel)mainTable.getModel();
            if (oldModel.width > 2) {
                AATableModel newModel = new AATableModel(oldModel.width - 2,
                        oldModel.height - 2,
                        oldModel,
                        -1, mainTable.getSelectedColumn());

                mainTable.setModel(newModel);

                for (int i = 0; i < newModel.width; i++) {
                    mainTable.getColumnModel().getColumn(i).setCellRenderer(new AATableCellRenderer(newModel.height));
                }
            }
        });
    }

    private void askCSVOptions(boolean askDates, Runnable after) {
        String[] options = CSVOptions.showOptions(savedOptions, askDates);
        if (options != null) {
            savedOptions = options;
            after.run();
        }
    }

    public void silentParseCSVAndMergeData(File f) {
        parseCSVAndMergeData(f, false);
    }

    private void verboseParseCSVAndMergeData(File f) {
        parseCSVAndMergeData(f, true);
    }

    private void parseCSVAndMergeData(File f, boolean displayError) {
        try {
            AATableModel oldModel = (AATableModel)mainTable.getModel();
            AATableModel newModel = parseCSVAndLoadData(f);

            Zipper<String, Double, String> oldZ = oldModel.toZipper();
            Zipper<String, Double, String> newZ = newModel.toZipper();
            Zipper<String, Double, String> result = oldZ.zip(newZ, -1.0);

            AATableModel zippedModel = AATableModel.fromZipper(result, savedOptions);
            setNewModel(zippedModel);

        } catch (Exception e) {
            if (displayError) {
                JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void parseCSVAndLoadData(File f, String[] options) {
        savedOptions = options;
        try {
            AATableModel model = parseCSVAndLoadData(f);
            setNewModel(model);
        } catch (Exception ignored) {
        }
    }

    private AATableModel parseCSVAndLoadData(File f) throws Exception {
        List<String> columns = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<List<Double>> data = new ArrayList<>();

        String delim = savedOptions[0];
        String mark = savedOptions[1];
        String decimal = savedOptions[2];
        String dates = savedOptions[3];

        Properties prop = Main.getProperties();
        prop.setProperty("import.delimiter", delim);
        prop.setProperty("import.mark", mark);
        prop.setProperty("import.decimal", decimal);
        prop.setProperty("import.date", dates);

        BufferedReader is = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));

        is.lines().forEach(line -> {
            String[] splitted = processText(line, delim, mark);
            if (columns.isEmpty()) {
                columns.addAll(Arrays.asList(Arrays.copyOfRange(splitted, 1, splitted.length)));
            } else {
                if (!line.isEmpty()) {
                    labels.add(splitted[0]);
                    String[] numbers = Arrays.copyOfRange(splitted, 1, splitted.length);
                    ArrayList<Double> parsed = new ArrayList<>(Arrays.stream(numbers).map(s -> s.replace(decimal, "."))
                            .map(s -> s.isEmpty() ? -1.0 : Double.valueOf(s)).collect(Collectors.toList()));

                    while (parsed.size() < columns.size()) {
                        parsed.add(-1.0);
                    }
                    data.add(parsed);
                }
            }
        });

        int htLength = data.size();
        int whLength = columns.size();
        double[][] rawData = new double[htLength][whLength];

        for (int ht = 0; ht < htLength; ht++) {
            for (int wh = 0; wh < whLength; wh++) {
                rawData[ht][wh] = data.get(ht).get(wh);
            }
        }

        return new AATableModel(whLength, htLength, rawData, labels.toArray(new String[0]), columns.toArray(new String[0]), "1".equals(dates));
    }

    private void setNewModel(AATableModel newModel) {
        mainTable.setModel(newModel);

        for (int i = 0; i < newModel.width; i++) {
            mainTable.getColumnModel().getColumn(i).setCellRenderer(new AATableCellRenderer(newModel.height));
        }
    }

    private void parseCSVAndSaveData(File f) {
        try {
            String delim = savedOptions[0];
            String mark = savedOptions[1];
            String decimal = savedOptions[2];
            String dates = savedOptions[3];

            Properties prop = Main.getProperties();
            prop.setProperty("import.delimiter", delim);
            prop.setProperty("import.mark", mark);
            prop.setProperty("import.decimal", decimal);
            prop.setProperty("import.date", dates);

            AATableModel model = (AATableModel)mainTable.getModel();

            BufferedWriter os = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));

            os.write(
                    String.join(
                            delim,
                            Arrays.stream(model.instruments).map(i -> mark + i + mark).collect(Collectors.toList())
                    )
            );
            os.newLine();

            int length = model.periods.length;

            for (int i = 0; i < length; i++) {
                os.write(mark);
                os.write(model.formatPeriod(i));
                os.write(mark);
                os.write(delim);
                os.write(
                        String.join(
                                delim,
                                Arrays.stream(model.data[i])
                                        .boxed()
                                        .map(d -> d >= 0 ? String.valueOf(d) : "")
                                        .map(s -> s.replace(".", decimal))
                                        .collect(Collectors.toList())
                                )
                );
                os.newLine();
            }
            os.newLine();
            os.flush();
            os.close();

            prop.setProperty("files.last", f.getCanonicalPath());
            lastFileName = f.getCanonicalPath();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private String[] processText(String text, String delim, String mark) {
        List<String> result = new LinkedList<>();
        boolean inText = false;
        int length = text.length();
        String mark2 = mark + mark;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < length) {
            String current = text.substring(i, i + 1);

            if (current.equals(mark)) {
                if (i < length - 1 && text.substring(i, i + 2).equals(mark2)) {
                    sb.append(mark);
                    i += 2;
                } else {
                    inText = !inText;
                    i += 1;
                }
                continue;
            }

            if (current.equals(delim)) {
                if (!inText) {
                    result.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(current);
                }
            } else {
                sb.append(current);
            }

            i += 1;
        }

        if (sb.length() > 0) {
            result.add(sb.toString());
        }

        return result.toArray(new String[0]);
    }

    private void createUIComponents() {
        mainTable = new JTable(new AATableModel());
        mainTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private File[] openExistingFile(boolean enableMultiSelect) {
        JFileChooser fc = new JFileChooser(".");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(enableMultiSelect);
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().toLowerCase().endsWith(".csv");
            }

            @Override
            public String getDescription() {
                return Main.resourceBundle.getString("text.csv_extension");
            }
        });

        int result = fc.showOpenDialog(Main.getFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] fs = fc.getSelectedFiles();
            if (fs.length == 0) {
                fs = new File[] { fc.getSelectedFile() };
            }
            return Arrays.stream(fs).allMatch(File::exists) ? fs : null;
        }

        return null;
    }

    public JPanel GetMainPanel() {
        return mainPanel;
    }

    public String[] getPeriods() {
        return ((AATableModel)mainTable.getModel()).formatPeriods();
    }
}
