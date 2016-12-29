package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ShowTable extends JDialog {

    public static String noPercentFormat = "x%";

    private JPanel contentPane;
    private JButton buttonOK;
    private JTable tableData;
    private JButton buttonCopy;
    private JCheckBox checkBoxColorize;

    private final String srcName;
    private final double[][] srcTable;
    private final String[] srcRowLabels;
    private final String[] srcColLabels;

    private static class GradientCellRenderer extends DefaultTableCellRenderer {

        private final double[][] source;
        private final double minValue;
        private final double maxValue;
        private boolean isActive;

        private GradientCellRenderer(double min, double max, double[][] src) {
            source = src;
            minValue = min;
            maxValue = max;
            isActive = true;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            if (isActive && col > 0) {
                double percent;
                double data = source[row][col - 1];

                if (data < 0) {
                    percent = 0.5 - data/minValue * 0.5;
                } else if (data > 0) {
                    percent = 0.5 + data / maxValue * 0.5;
                } else {
                    percent = 0.5;
                }

                cell.setBackground(
                        row == col - 1 ?
                                Color.LIGHT_GRAY :
                                Main.gradient.getColor(1.0 - percent)
                );
            } else {
                cell.setBackground(Color.WHITE);
            }

            return cell;
        }

        void setActive(boolean active) {
            isActive = active;
        }
    }

    private ShowTable(String name, double[][] table, String[] rowLabels, String[] colLabels) {

        srcName = name;
        srcTable = table;
        srcRowLabels = rowLabels;
        srcColLabels = colLabels;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onOK();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onOK(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        buttonCopy.addActionListener(actionEvent -> {
            int rowsLength = srcRowLabels.length + 1;
            int colsLength = srcColLabels.length + 1;

            String delim = Main.properties.getProperty("import.delimiter", ";");
            String mark = Main.properties.getProperty("import.mark", "\"");
            String decimal = Main.properties.getProperty("import.decimal", ".");

            StringBuilder sb = new StringBuilder();

            for (int row = 0; row < rowsLength; row++) {
                for (int col = 0; col < colsLength; col++) {

                    if (row == 0 && col == 0) {
                        sb.append(mark);
                        sb.append(srcName);
                        sb.append(mark);
                    } else if (col == 0) {
                        sb.append(mark);
                        sb.append(srcRowLabels[row - 1]);
                        sb.append(mark);
                    } else if (row == 0) {
                        sb.append(mark);
                        sb.append(srcColLabels[col - 1]);
                        sb.append(mark);
                    } else {
                        sb.append(String.valueOf(srcTable[row - 1][col - 1]).replace(".", decimal));
                    }

                    if (col != colsLength - 1) {
                        sb.append(delim);
                    }
                }
                if (row != rowsLength - 1) {
                    sb.append("\n");
                }

            }

            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
        });
        checkBoxColorize.addActionListener(actionEvent -> {
            boolean isSelected = checkBoxColorize.isSelected();
            int width = tableData.getColumnCount();
            for (int i = 0; i < width; i++) {
                TableCellRenderer renderer = tableData.getColumnModel().getColumn(i).getCellRenderer();

                if (renderer instanceof GradientCellRenderer) {
                    ((GradientCellRenderer)renderer).setActive(isSelected);
                }
            }

            ((DefaultTableModel)tableData.getModel()).fireTableDataChanged();

        });
    }

    private void onOK() {
        dispose();
    }

    static void show(String name, double[][] table, String[] rowLabels, String[] colLabels, boolean gradient) {
        ShowTable dialog = new ShowTable(name, table, rowLabels, colLabels);

        int rowsLength = rowLabels.length;
        int colsLengthP1 = colLabels.length + 1;

        String[][] preparedRows = new String[rowsLength][colsLengthP1];
        String[] preparedCols = new String[colsLengthP1];

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for (int row = 0; row < rowsLength; row++) {
            for (int col = 0; col < colsLengthP1; col++) {
                if (col == 0) {
                    preparedRows[row][col] = rowLabels[row].replace(noPercentFormat, "");
                } else {
                    preparedRows[row][col] = rowLabels[row].contains(noPercentFormat) ? Calc.formatDouble2(table[row][col - 1]) : Calc.formatPercent2(table[row][col - 1]);

                    if (table[row][col - 1] < min) {
                        min = table[row][col - 1];
                    }
                    if (table[row][col - 1] > max) {
                        max = table[row][col - 1];
                    }

                }
                if (row == 0) {
                    preparedCols[col] = col == 0 ? "" : colLabels[col - 1];
                }
            }
        }

        dialog.tableData.setModel(new DefaultTableModel(preparedRows, preparedCols));

        if (gradient) {
            int width = preparedCols.length;
            for (int i = 0; i < width; i++) {
                dialog.tableData.getColumnModel().getColumn(i).setCellRenderer(new GradientCellRenderer(min, max, table));
            }
        }
        dialog.checkBoxColorize.setEnabled(gradient);
        dialog.checkBoxColorize.setSelected(gradient);

        dialog.setTitle(name);
        dialog.setLocationRelativeTo(Main.getFrame());
        dialog.pack();
        dialog.setVisible(true);
    }
}
