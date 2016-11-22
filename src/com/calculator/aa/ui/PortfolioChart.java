package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.Portfolio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;

class PortfolioChart extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JPanel chartPanel;
    private JTable tableLimitations;
    private JButton buttonCompute;
    private JToggleButton buttonBorderOnly;

    private final String[] instruments;
    private final double[][] data;

    private PortfolioChart(String[] i, double[][] d) {

        instruments = i;
        data = d;

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

        buttonCompute.addActionListener(e -> {
            int length = instruments.length - 1;

            int[] minimals = new int[length];
            int[] maximals = new int[length];

            DefaultTableModel model = (DefaultTableModel)tableLimitations.getModel();

            double[][] corrTable = Calc.correlationTable(data);
            double[] avYields = new double[length];
            double[] sdYields = new double[length];

            for (int col = 0; col < length; col++) {
                minimals[col] = Integer.valueOf((String) model.getValueAt(0, col + 1));
                maximals[col] = Integer.valueOf((String) model.getValueAt(1, col + 1));
                avYields[col] = Calc.averageYields(Calc.column(data, col));
                sdYields[col] = Calc.stdevYields(Calc.column(data, col));
            }

            String[] trueInstr = Arrays.copyOfRange(instruments, 1, instruments.length);
            List<Portfolio> portfolios = Calc.iteratePortfolios(corrTable, avYields, sdYields, minimals, maximals, trueInstr, 20);

            //portfolios.forEach(Portfolio::print);
            ((CanvasPanel)chartPanel).setPortfolios(portfolios);
        });

        buttonBorderOnly.addChangeListener(e -> ((CanvasPanel)chartPanel).setBorderOnlyMode(buttonBorderOnly.isSelected()));
    }

    private void onOK() {
        dispose();
    }

    private void updateLimitations() {
        int length = instruments.length;
        String[][] limits = new String[2][length];
        for (int col = 0; col < instruments.length; col++) {
            limits[0][col] = col == 0 ? "Мин" : "0";
            limits[1][col] = col == 0 ? "Макс" : "100";
        }

        tableLimitations.setModel(new DefaultTableModel(limits, instruments));
    }

    static void showChart(String[] instruments, double[][] data) {
        PortfolioChart dialog = new PortfolioChart(instruments, data);
        dialog.setTitle("Портфели");
        dialog.setLocationRelativeTo(Main.getFrame());

        dialog.updateLimitations();

        dialog.pack();
        dialog.setVisible(true);
    }

    private void createUIComponents() {
        chartPanel = new CanvasPanel();
    }
}
