package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.Portfolio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

class PortfolioChart extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JPanel chartPanel;
    private JTable tableLimitations;
    private JButton buttonCompute;
    private JToggleButton buttonBorderOnly;
    private JButton buttonAccuracy;
    private JButton buttonAccuracyMax;
    private JComboBox<String> comboBoxFrom;
    private JComboBox<String> comboBoxTo;

    private final String[] instruments;
    private final double[][] data;

    private int indexFrom;
    private int indexTo;

    private PortfolioChart(String[] i, double[][] d) {

        instruments = i;
        data = d;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        String[] periods = Main.getPeriods(0, Integer.MAX_VALUE);
        comboBoxFrom.setModel(new DefaultComboBoxModel<>(Arrays.copyOfRange(periods, 0, periods.length - 2)));
        comboBoxFrom.setSelectedIndex(0);
        comboBoxTo.setModel(new DefaultComboBoxModel<>(Arrays.copyOfRange(periods, 2, periods.length)));
        comboBoxTo.setSelectedIndex(periods.length - 3);

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

            DefaultTableModel model = (DefaultTableModel)tableLimitations.getModel();

            int[] minimals = new int[length];
            int[] maximals = new int[length];
            int[] compares = new int[length];

            for (int col = 0; col < length; col++) {
                int minValue = Integer.valueOf((String) model.getValueAt(0, col + 1));
                int maxValue = Integer.valueOf((String) model.getValueAt(1, col + 1));

                if (minValue < 0 || maxValue > 100 || minValue > maxValue) {
                    return;
                }

                minimals[col] = minValue;
                maximals[col] = maxValue;
                compares[col] = Integer.valueOf((String) model.getValueAt(2, col + 1));
            }

            List<String> periodsList = Arrays.asList(periods);

            int[] fromIndex = new int[1];
            fromIndex[0]= periodsList.indexOf((String)comboBoxFrom.getSelectedItem());
            if (fromIndex[0] < 0) {
                fromIndex[0] = 0;
            }

            int[] toIndex = new int[1];
            toIndex[0] = periodsList.indexOf((String)comboBoxTo.getSelectedItem());
            if (toIndex[0] < 0) {
                toIndex[0] = data.length - 1;
            }

            double[][] dataFiltered = Calc.filterValidData(data, maximals, fromIndex, toIndex);
            indexFrom = fromIndex[0];
            indexTo = toIndex[0];

            double[][] corrTable = Calc.correlationTable(dataFiltered);
            double[] avYields = new double[length];
            double[] sdYields = new double[length];

            for (int col = 0; col < length; col++) {
                double[] column = Calc.column(dataFiltered, col);
                avYields[col] = Calc.averageYields(column);
                sdYields[col] = Calc.stdevYields(column);
            }

            String[] trueInstr = Arrays.copyOfRange(instruments, 1, instruments.length);
            int dividers = calculateDivision(Arrays.copyOf(minimals, minimals.length), Arrays.copyOf(maximals, maximals.length));
            List<Portfolio> portfolios = Calc.iteratePortfolios(corrTable, avYields, sdYields, minimals, maximals, trueInstr, dividers);

            List<Portfolio> portfoliosCompare = null;
            if (Arrays.stream(compares).sum() == 100) {
                portfoliosCompare = Calc.iteratePortfolios(corrTable, avYields, sdYields, compares, compares, trueInstr, 100);
            }

            ((PortfolioChartPanel)chartPanel).setPortfolios(portfolios, portfoliosCompare, dataFiltered, Main.getPeriods(indexFrom, indexTo));
        });

        buttonBorderOnly.addChangeListener(e -> ((PortfolioChartPanel) chartPanel).setBorderOnlyMode(buttonBorderOnly.isSelected()));

        buttonAccuracy.addActionListener(e -> {
            List<Portfolio> border = ((PortfolioChartPanel) chartPanel).getBorderPortfolios();
            if (border == null || border.isEmpty()) {
                return;
            }

            List<Portfolio> accuracyPortfolios = addAccuracy();

            if (accuracyPortfolios.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    for(ActionListener a: buttonAccuracy.getActionListeners()) {
                        a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {});
                    }
                });
            }
        });
        buttonAccuracyMax.addActionListener(e -> {
            List<Portfolio> border = ((PortfolioChartPanel) chartPanel).getBorderPortfolios();
            if (border == null || border.isEmpty()) {
                return;
            }

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            List<Portfolio> accuracyPortfolios = addAccuracy();
            List<Portfolio> accuracyPortfolios2 = addAccuracy();

            if (!accuracyPortfolios.equals(accuracyPortfolios2) || (accuracyPortfolios.size() == 0 && accuracyPortfolios2.size() == 0)) {
                SwingUtilities.invokeLater(() -> {
                    for(ActionListener a: buttonAccuracyMax.getActionListeners()) {
                        a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {});
                    }
                });
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    private int calculateDivision(int[] minimals, int[] maximals) {

        int[] variants = new int[] {100, 50, 25, 20, 10, 5, 4, 2, 1};
        int length = variants.length;
        int checkLen = minimals.length;
        int limit = 50000;
        int[] sum = new int[1];
        int nonEmpty = 0;

        for (int i = 0; i < checkLen; i++) {
            if (maximals[i] - minimals[i] > 0) {
                nonEmpty += 1;
            }
        }

        int lastNotZero = -1;

        for (int i = 0; i < length; i++) {
            sum[0] = 0;

            if (i == length - 2 && nonEmpty > 10) {
                return 100 / variants[lastNotZero];
            }

            if (i == length - 1 && nonEmpty > 5) {
                return 100 / variants[lastNotZero];
            }

            calculateDivisionHelper(
                    Arrays.copyOf(minimals, minimals.length),
                    Arrays.copyOf(maximals, maximals.length),
                    Arrays.copyOf(minimals, minimals.length),
                    0, variants[i], sum
            );

            if (sum[0] > limit) {
                return 100 / variants[lastNotZero >= 0 ? lastNotZero : i];
            }

            if (sum[0] != 0) {
                lastNotZero = i;
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
        String[][] limits = new String[3][length];
        for (int col = 0; col < instruments.length; col++) {
            limits[0][col] = col == 0 ? Main.resourceBundle.getString("text.min") : "0";
            limits[1][col] = col == 0 ? Main.resourceBundle.getString("text.max") : "100";
            limits[2][col] = col == 0 ? Main.resourceBundle.getString("text.compare") : "0";
        }

        tableLimitations.setModel(new DefaultTableModel(limits, instruments));
    }

    private void createUIComponents() {
        chartPanel = new PortfolioChartPanel();
        comboBoxFrom = new JComboBox<>();
        comboBoxTo = new JComboBox<>();
    }

    private List<Portfolio> addAccuracy() {
        List<Portfolio> border = ((PortfolioChartPanel) chartPanel).getBorderPortfolios();
        List<Portfolio> accuracyPortfolios = new LinkedList<>();

        if (border == null || border.isEmpty()) {
            return accuracyPortfolios;
        }

        int length = instruments.length - 1;
        double[][] dataFiltered = ((PortfolioChartPanel)chartPanel).getDataFiltered();
        double[][] corrTable = Calc.correlationTable(dataFiltered);
        double[] avYields = new double[length];
        double[] sdYields = new double[length];
        for (int col = 0; col < length; col++) {
            double[] column = Calc.column(dataFiltered, col);
            avYields[col] = Calc.averageYields(column);
            sdYields[col] = Calc.stdevYields(column);
        }
        String[] trueInstr = Arrays.copyOfRange(instruments, 1, instruments.length);
        int dividers = 100;

        DefaultTableModel model = (DefaultTableModel)tableLimitations.getModel();
        int[] userMinimals = new int[length];
        int[] userMaximals = new int[length];
        int[] compares = new int[length];

        for (int col = 0; col < length; col++) {
            int minValue = Integer.valueOf((String) model.getValueAt(0, col + 1));
            int maxValue = Integer.valueOf((String) model.getValueAt(1, col + 1));

            if (minValue < 0 || maxValue > 100 || minValue > maxValue) {
                return accuracyPortfolios;
            }

            userMinimals[col] = minValue;
            userMaximals[col] = maxValue;
            compares[col] = Integer.valueOf((String) model.getValueAt(2, col + 1));
        }

        int index = 0;
        int size = border.size();
        while (index < size) {
            Portfolio first = border.get(index);
            Portfolio next = index < size - 1 ? border.get(index + 1) : first;

            int[] minimals = new int[length];
            int[] maximals = new int[length];

            for (int col = 0; col < length; col++) {
                minimals[col] = Math.min((int)(first.weights()[col] * 100), (int)(next.weights()[col] * 100)) - 100 / dividers;
                if (minimals[col] < 0) {
                    minimals[col] = 0;
                }

                maximals[col] = Math.max((int)(first.weights()[col] * 100), (int)(next.weights()[col] * 100)) + 100 / dividers;
                if (maximals[col] > 100) {
                    maximals[col] = 100;
                }

                if (minimals[col] < userMinimals[col]) {
                    minimals[col] = userMinimals[col];
                }

                if (maximals[col] > userMaximals[col]) {
                    maximals[col] = userMaximals[col];
                }
            }

            accuracyPortfolios.addAll(
                    Calc.iteratePortfolios(corrTable, avYields, sdYields, minimals, maximals, trueInstr, dividers)
            );

            index += 1;
        }

        accuracyPortfolios.sort(Portfolio::compareTo);

        List<Portfolio> portfoliosCompare = null;
        if (Arrays.stream(compares).sum() == 100) {
            portfoliosCompare = Calc.iteratePortfolios(corrTable, avYields, sdYields, compares, compares, trueInstr, 100);
        }

        ((PortfolioChartPanel) chartPanel).setPortfolios(accuracyPortfolios, portfoliosCompare, dataFiltered, Main.getPeriods(indexFrom, indexTo));

        return accuracyPortfolios;
    }

    static void showChart(String[] instruments, double[][] data) {
        PortfolioChart dialog = new PortfolioChart(instruments, data);
        dialog.setTitle(Main.resourceBundle.getString("text.portfolios"));
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
}
