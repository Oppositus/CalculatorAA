package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Properties;

class ShowTable extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JTable tableData;
    private JButton buttonCopy;

    private final String srcName;
    private final double[][] srcTable;
    private final String[] srcRowLabels;
    private final String[] srcColLabels;

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

            Properties prop = Main.getProperties();
            String delim = prop.getProperty("import.delimeter", ";");
            String mark = prop.getProperty("import.mark", "\"");
            String decimal = prop.getProperty("import.decimal", ".");

            StringBuilder sb = new StringBuilder();

            for (int row = 0; row < rowsLength; row++) {
                for (int col = 0; col < colsLength; col++) {

                    if (row == 0 && col == 0) {
                        sb.append(mark);
                        sb.append(srcName);
                        sb.append(mark);
                    } else if (col == 0) {
                        sb.append(mark);
                        sb.append(srcColLabels[row - 1]);
                        sb.append(mark);
                    } else if (row == 0) {
                        sb.append(mark);
                        sb.append(srcRowLabels[col - 1]);
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
    }

    private void onOK() {
        dispose();
    }

    static void show(String name, double[][] table, String[] rowLabels, String[] colLabels) {
        ShowTable dialog = new ShowTable(name, table, rowLabels, colLabels);

        int rowsLength = rowLabels.length;
        int colsLengthP1 = colLabels.length + 1;

        String[][] preparedRows = new String[rowsLength][colsLengthP1];
        String[] preparedCols = new String[colsLengthP1];

        for (int row = 0; row < rowsLength; row++) {
            for (int col = 0; col < colsLengthP1; col++) {
                preparedRows[row][col] = col == 0 ?
                        rowLabels[row] :
                        Calc.formatPercent2(table[row][col - 1]);
                if (row == 0) {
                    preparedCols[col] = col == 0 ? "" : colLabels[col - 1];
                }
            }
        }

        dialog.tableData.setModel(new DefaultTableModel(preparedRows, preparedCols));
        dialog.setTitle(name);
        dialog.setLocationRelativeTo(Main.getFrame());
        dialog.pack();
        dialog.setVisible(true);
    }
}
