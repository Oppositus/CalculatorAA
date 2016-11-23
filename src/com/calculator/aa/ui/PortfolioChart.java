package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.Portfolio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
            int dividers = calculateDivision(Arrays.copyOf(minimals, minimals.length), Arrays.copyOf(maximals, maximals.length), false);
            List<Portfolio> portfolios = Calc.iteratePortfolios(corrTable, avYields, sdYields, minimals, maximals, trueInstr, dividers);

            ((CanvasPanel)chartPanel).setPortfolios(portfolios);
        });

        buttonBorderOnly.addChangeListener(e -> ((CanvasPanel)chartPanel).setBorderOnlyMode(buttonBorderOnly.isSelected()));
    }

    private int calculateDivision(int[] minimals, int[] maximals, boolean accuracy) {

        int[] variants = new int[] {100, 50, 25, 20, 10, 5, 4, 2, 1};
        int length = variants.length;
        int limit = 50000;
        int[] sum = new int[1];
        int nonEmpty = 0;

        for (int i = 0; i < length; i++) {
            if (maximals[i] - minimals[i] > 0) {
                nonEmpty += 1;
            }
        }

        for (int i = 0; i < length; i++) {
            sum[0] = 0;

            if (i == length - 2 && nonEmpty > 10) {
                return 100 / variants[length - 3];
            }

            if (i == length - 1 && nonEmpty > 5) {
                return 100 / variants[length - 2];
            }

            calculateDivisionHelper(
                    Arrays.copyOf(minimals, minimals.length),
                    Arrays.copyOf(maximals, maximals.length),
                    Arrays.copyOf(minimals, minimals.length),
                    0, variants[i], sum
            );

            if (sum[0] > limit) {
                return 100 / variants[accuracy ? i : i - 1];
            }
        }

        return 100;
    }

    // todo: optimize this shit!
    private void calculateDivisionHelper(int[] minimals, int[] maximals, int[] weights, int index, int step, int[] sum) {
        while (weights[index] <= maximals[index]) {

            // clear tail
            System.arraycopy(minimals, index + 1, weights, index + 1, weights.length - (index + 1));

            int testSum = Calc.sumIntArray(weights);

            if (testSum == 100) {
                sum[0] += 1;
            }

            if (index < weights.length - 1 && testSum < 100) {
                calculateDivisionHelper(minimals, maximals, weights, index + 1, step, sum);
            }

            weights[index] += step;
        }
    }

    private void onOK() {
        Properties properties = Main.getProperties();
        Rectangle bounds = getBounds();
        properties.setProperty("portfolio.x", String.valueOf((int)bounds.getX()));
        properties.setProperty("portfolio.y", String.valueOf((int)bounds.getY()));
        properties.setProperty("portfolio.w", String.valueOf((int)bounds.getWidth()));
        properties.setProperty("portfolio.h", String.valueOf((int)bounds.getHeight()));

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

        dialog.pack();

        Properties properties = Main.getProperties();
        int x = Integer.parseInt(properties.getProperty("portfolio.x", "-1"));
        int y = Integer.parseInt(properties.getProperty("portfolio.y", "-1"));
        int w = Integer.parseInt(properties.getProperty("portfolio.w", "-1"));
        int h = Integer.parseInt(properties.getProperty("portfolio.h", "-1"));

        if (x >= 0 && y >= 0 && w >= 0 && h >= 0) {
            Rectangle rec = new Rectangle(x, y, w, h);
            dialog.setBounds(rec);
        }

        dialog.updateLimitations();

        dialog.setVisible(true);
    }

    private void createUIComponents() {
        chartPanel = new CanvasPanel();
    }
}
