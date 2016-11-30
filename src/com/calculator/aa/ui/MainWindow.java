package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.sun.deploy.util.StringUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

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

    private class AATableModel extends AbstractTableModel {

        private final int width;
        private final int height;
        private final double[][]data;
        private final String[] instruments;
        private final String[] periods;
        private final double[] averages;
        private final double[] deviations;

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
            periods = new String[height - 2];
            averages = new double[width - 1];
            deviations = new double[width - 1];

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
                }
            }
        }

        // Create new table w*h and copy data from previous model
        private AATableModel(int w, int h, AATableModel prev, int ignoredRow) {
            this(w, h);

            String[] prevPeriods = prev.periods;
            double[][] prevData = prev.data;
            int length = prevPeriods.length;

            if (ignoredRow >= 0) {
                for (int ht = ignoredRow; ht < length - 1; ht++) {
                    prevPeriods[ht] = prevPeriods[ht + 1];
                    prevData[ht] = prevData[ht + 1];
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
                }

                if (wh > 0) {
                    updateAverage(wh - 1);
                    updateStDev(wh - 1);
                }
            }
        }

        // Create new table w*h and copy data from data
        private AATableModel(int w, int h, double[][] d, String[] l, String[] i) {
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
                }

                if (wh > 0) {
                    updateAverage(wh - 1);
                    updateStDev(wh - 1);
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
                    return periods[row];
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

        private double[] getCol(int col) {
            int length = height - 2;
            double[] values = new double[length];
            for (int i = 0; i < length; i++) {
                values[i] = data[i][col];
            }
            return values;
        }

        private void updateAverage(int col) {
            averages[col] = Calc.averageYields(getCol(col));
        }

        private void updateStDev(int col) {

            if (height < 5) {
                return;
            }

            deviations[col] = Calc.stdevYields(getCol(col), averages[col]);
        }
    }

    public MainWindow() {
        buttonAddRow.addActionListener(actionEvent -> {
            AATableModel oldModel = (AATableModel)mainTable.getModel();
            AATableModel newModel = new AATableModel(oldModel.width - 1, oldModel.height - 1, oldModel, -1);
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
                        mainTable.getSelectedRow());
                mainTable.setModel(newModel);

                for (int i = 0; i < newModel.width; i++) {
                    mainTable.getColumnModel().getColumn(i).setCellRenderer(new AATableCellRenderer(newModel.height));
                }
            }
        });
        buttonOpen.addActionListener(actionEvent -> {
            JFileChooser fc = new JFileChooser(".");
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

            int result = fc.showOpenDialog(Main.getFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                if (f.exists()) {
                    askCSVOptions(() -> {
                        try {
                            parseCSVAndLoadData(f);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }
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
        buttonPortfolio.addActionListener(actionEvent -> {
            AATableModel model = (AATableModel)mainTable.getModel();
            PortfolioChart.showChart(model.instruments, model.data);
        });
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
                                0);
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

            int result = fc.showOpenDialog(Main.getFrame());
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
                askCSVOptions(() -> parseCSVAndSaveData(ff));
            }
        });
    }

    private void askCSVOptions(Runnable after) {
        String[] options = CSVOptions.showOptions(savedOptions);
        if (options != null) {
            savedOptions = options;
            after.run();
        }
    }

    public void parseCSVAndLoadData(File f, String[] options) {
        savedOptions = options;
        try {
            parseCSVAndLoadData(f);
        } catch (Exception ignored) {
        }
    }

    private void parseCSVAndLoadData(File f) throws Exception {
        List<String> columns = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<List<Double>> data = new ArrayList<>();

        String delim = savedOptions[0];
        String mark = savedOptions[1];
        String decimal = savedOptions[2];
        String dates = savedOptions[3];

        Properties prop = Main.getProperties();
        prop.setProperty("import.delimeter", delim);
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

        double[][] rawData = new double[data.size()][columns.size()];
        int htLength = data.size();
        int whLength = columns.size();

        for (int ht = 0; ht < htLength; ht++) {
            for (int wh = 0; wh < whLength; wh++) {
                rawData[ht][wh] = data.get(ht).get(wh);
            }
        }

        AATableModel newModel = new AATableModel(whLength, htLength, rawData, labels.toArray(new String[0]), columns.toArray(new String[0]));
        mainTable.setModel(newModel);

        for (int i = 0; i < newModel.width; i++) {
            mainTable.getColumnModel().getColumn(i).setCellRenderer(new AATableCellRenderer(newModel.height));
        }

        prop.setProperty("file", f.getCanonicalPath());
        lastFileName = f.getCanonicalPath();
    }

    private void parseCSVAndSaveData(File f) {
        try {
            String delim = savedOptions[0];
            String mark = savedOptions[1];
            String decimal = savedOptions[2];
            String dates = savedOptions[3];

            Properties prop = Main.getProperties();
            prop.setProperty("import.delimeter", delim);
            prop.setProperty("import.mark", mark);
            prop.setProperty("import.decimal", decimal);
            prop.setProperty("import.date", dates);

            AATableModel model = (AATableModel)mainTable.getModel();

            BufferedWriter os = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));

            os.write(
                    StringUtils.join(
                            Arrays.stream(model.instruments).map(i -> mark + i + mark).collect(Collectors.toList()),
                            delim)
            );
            os.newLine();

            int length = model.periods.length;

            for (int i = 0; i < length; i++) {
                os.write(mark);
                os.write(model.periods[i]);
                os.write(mark);
                os.write(delim);
                os.write(
                        StringUtils.join(
                            Arrays.stream(model.data[i])
                                    .boxed()
                                    .map(d -> d >= 0 ? String.valueOf(d) : "")
                                    .map(s -> s.replace(".", decimal))
                                    .collect(Collectors.toList()),
                            delim)
                );
                os.newLine();
            }
            os.newLine();
            os.flush();
            os.close();

            prop.setProperty("file", f.getCanonicalPath());
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

    public JPanel GetMainPanel() {
        return mainPanel;
    }

    public double[][] getData() {
        return ((AATableModel)mainTable.getModel()).data;
    }

    public String[] getPeriods() {
        return ((AATableModel)mainTable.getModel()).periods;
    }
}
