package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;

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

    private String[] savedOptions;

    private class AATableCellRenderer extends DefaultTableCellRenderer {
        private final Color back = new Color(212, 212, 212);
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
                cell.setBackground(Color.WHITE);
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
                    instruments[wh] = String.format("%s %d", "Инструмент", wh);
                } else {
                    instruments[wh] = "";
                }

                for (int ht = 0; ht < height - 2; ht++) {
                    if (wh < width - 1) {
                        data[ht][wh] = 0.0f;
                    }
                    periods[ht] = String.format("%s %d", "Период", ht);
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
                    if (wh > 0) {
                        updateAverage(wh - 1);
                        updateStDev(wh - 1);
                    }
                    periods[ht] = l[ht];
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
                return col == 0 ? "Доходность" : (height < 4 ? "" : Calc.formatPercent2(averages[col - 1]));
            } else if (row == height - 1) {
                return col == 0 ? "Риск" : (height < 5 ? "" : Calc.formatPercent2(deviations[col - 1]));
            } else {
                return col == 0 ? periods[row] : data[row][col - 1];
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

            deviations[col] = Calc.stdevYields(getCol(col));
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
                    String extension = "";
                    String fileName = file.getName().toLowerCase();
                    int i = fileName.lastIndexOf('.');
                    if (i >= 0) {
                        extension = fileName.substring(i + 1);
                    }
                    return extension.equals("csv");
                }

                @Override
                public String getDescription() {
                    return "CSV data files (.*csv)";
                }
            });

            int result = fc.showOpenDialog(Main.getFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                if (f.exists()) {
                    askImportOptions(f);
                }
            }
        });
        buttonCorrelations.addActionListener(actionEvent -> {

            AATableModel model = (AATableModel)mainTable.getModel();
            double[][] corrTable = Calc.correlationTable(model.data);
            String[] cols = Arrays.copyOfRange(model.instruments, 1, model.instruments.length);

            ShowTable.show("Таблица корреляций", corrTable, cols, cols);
        });
        buttonCovariances.addActionListener(actionEvent -> {
            AATableModel model = (AATableModel)mainTable.getModel();
            double[][] covTable = Calc.covarianceTable(model.data);
            String[] cols = Arrays.copyOfRange(model.instruments, 1, model.instruments.length);

            ShowTable.show("Таблица ковариаций", covTable, cols, cols);
        });
        buttonPortfolio.addActionListener(actionEvent -> {
            AATableModel model = (AATableModel)mainTable.getModel();
            PortfolioChart.showChart(model.instruments, model.data);
        });
    }

    private void askImportOptions(File f) {

        if (savedOptions == null) {
            savedOptions = new String[] {";", "\"", "."};

            Properties prop = Main.getProperties();
            String s = prop.getProperty("import.delimeter");
            if (s != null) {
                savedOptions[0] = s;
            }

            s = prop.getProperty("import.mark");
            if (s != null) {
                savedOptions[1] = s;
            }

            s = prop.getProperty("import.decimal");
            if (s != null) {
                savedOptions[2] = s;
            }
        }

        String[] options = ImportOptions.showOptions(savedOptions);
        if (options != null) {
            savedOptions = options;
            parseCSVAndLoadData(f, options);
        }
    }

    private void parseCSVAndLoadData(File f, String[] options) {
        try {
            List<String> columns = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            List<List<Double>> data = new ArrayList<>();

            String delim = options[0];
            String mark = options[1];
            String decimal = options[2];

            Properties prop = Main.getProperties();
            prop.setProperty("import.delimeter", delim);
            prop.setProperty("import.mark", mark);
            prop.setProperty("import.decimal", decimal);

            BufferedReader is = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));

            is.lines().forEach(line -> {
                String[] splitted = processText(line, delim, mark);
                if (columns.isEmpty()) {
                    columns.addAll(Arrays.asList(Arrays.copyOfRange(splitted, 1, splitted.length)));
                } else {
                    if (!line.isEmpty()) {
                        labels.add(splitted[0]);
                        String[] numbers = Arrays.copyOfRange(splitted, 1, splitted.length);
                        data.add(Arrays.stream(numbers).map(s -> s.replace(decimal, ".")).map(Double::valueOf).collect(Collectors.toList()));
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

        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, "Ошибка", JOptionPane.ERROR_MESSAGE);
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
}
