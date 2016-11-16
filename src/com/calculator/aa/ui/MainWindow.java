package com.calculator.aa.ui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class MainWindow {
    private JTable mainTable;
    private JButton buttonInstruments;
    private JPanel mainPanel;
    private JScrollPane scrollPane;
    private JButton buttonAddRow;
    private JButton buttonDeleteRow;

    private class AATableModel extends AbstractTableModel {

        private final int width;
        private final int height;
        private double[][]data;
        private String[] instruments;
        private String[] periods;
        private double[] averages;
        private double[] deviations;

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
                    periods[ht] = String.format("%s %d", "Период", ht);;
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
                return col == 0 ? "Доходность" : (height < 4 ? "" : formatPercent(averages[col - 1] - 1));
            } else if (row == height - 1) {
                return col == 0 ? "Риск" : (height < 5 ? "" : formatPercent(deviations[col - 1]));
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

        private double[] Yields(int col) {
            double[] yields = new double[height - 3];

            for (int ht = 1; ht < height - 2; ht++) {
                double prev = data[ht - 1][col];
                double curr = data[ht][col];

                if (prev <= 0.0f || curr <= 0.0f) {
                    return yields;
                }

                double divided = curr / prev;
                if (divided == Double.POSITIVE_INFINITY) {
                    return yields;
                }

                yields[ht - 1] = divided;
            }

            return yields;
        }

        private void updateAverage(int col) {
            if (height < 4) {
                return;
            }

            double[] yields = Yields(col);

            double sum = 0.0;
            for (int ht = 0; ht < height - 3; ht++) {
                sum += yields[ht];
            }
            averages[col] = sum / (height - 3);
        }

        private void updateStDev(int col) {

            if (height < 5) {
                return;
            }

            double[] yields = Yields(col);

            double sum2 = 0.0;

            for (int ht = 0; ht < height - 3; ht++) {
                double difference = (yields[ht] - averages[col]);
                sum2 += difference * difference;
            }

            deviations[col] = Math.sqrt(1.0 / (height - 4) * sum2);
        }

        private String formatPercent(double f) {
            return String.format("%.2f%%", f * 100);
        }
    }

    public MainWindow() {
        buttonAddRow.addActionListener(actionEvent -> {
            AATableModel oldModel = (AATableModel)mainTable.getModel();
            AATableModel newModel = new AATableModel(oldModel.width - 1, oldModel.height - 1, oldModel, -1);
            mainTable.setModel(newModel);
        });
        buttonDeleteRow.addActionListener(actionEvent -> {
            AATableModel oldModel = (AATableModel)mainTable.getModel();
            if (oldModel.height > 4) {
                AATableModel newModel = new AATableModel(oldModel.width - 1,
                        oldModel.height - 3,
                        oldModel,
                        mainTable.getSelectedRow());
                mainTable.setModel(newModel);
            }
        });
    }

    private void createUIComponents() {
        mainTable = new JTable(new AATableModel());
        mainTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    public JPanel GetMainPanel() {
        return mainPanel;
    }
}
