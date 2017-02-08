package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.Portfolio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

class YieldsChart extends JDialog {

    private JPanel contentPane;
    private JButton buttonOK;
    private JPanel yieldsPanel;
    private JToggleButton buttonLogScale;
    private JButton buttonDraw;
    private JSpinner spinnerPeriods;
    private JButton buttonWeights;
    private JCheckBox checkBoxSigma1;
    private JCheckBox checkBoxSigma2;
    private JCheckBox checkBoxSigma3;
    private JCheckBox checkBoxRebalance;

    private final String[] labels;
    private final double[][] data;
    private final Portfolio portfolio;
    private double[] realYields;
    private double[] portfolioYields;
    private final double riskFreeRate;
    private final Calc.RebalanceMode rebalanceMode;
    private final int rebalalanceThreshold;

    private final int completePeriods;

    private YieldsChart(String[] ls, double[][] sourceData, Portfolio p, double freeRate, Calc.RebalanceMode mode, int threshold) {
        data = sourceData;
        labels = Arrays.copyOfRange(ls, sourceData.length - data.length, ls.length);
        portfolio = p;
        completePeriods = data.length;
        riskFreeRate = freeRate;
        rebalanceMode = mode;
        rebalalanceThreshold = threshold;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        checkBoxSigma1.setBackground(PortfolioYieldsPanel.sigmaColor[0]);
        checkBoxSigma2.setBackground(PortfolioYieldsPanel.sigmaColor[1]);
        checkBoxSigma3.setBackground(PortfolioYieldsPanel.sigmaColor[2]);

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

        spinnerPeriods.addMouseWheelListener(new SpinnerWheelListener(spinnerPeriods));

        buttonWeights.addActionListener(e -> ShowTable.show(
                Main.resourceBundle.getString("text.portfolio"),
                riskFreeRate >= 0 ? portfolio.values(riskFreeRate) : portfolio.values(),
                riskFreeRate >= 0 ? portfolio.labels(riskFreeRate) : portfolio.labels(),
                new String[] {Main.resourceBundle.getString("text.value")},
                false)
        );

        buttonDraw.addActionListener(e -> {
            boolean isSelected = checkBoxRebalance.isSelected();
            portfolio.setRebalancedMode(isSelected);
            boolean isLog = buttonLogScale.isSelected();
            realYields = isSelected ? Calc.calculateRebalances(portfolio, isLog, false, rebalanceMode, rebalalanceThreshold) : Calc.calculateRealYields(portfolio, isLog, false);
            portfolioYields = calculateModelYields(isLog);
            boolean[] sigmas = new boolean[] {
                    checkBoxSigma1.isSelected(),
                    checkBoxSigma2.isSelected(),
                    checkBoxSigma3.isSelected()
            };
            double[][] instrumentsYields = calculateInstrumentYields(isLog);
            String[] instrumentsFiltered = filterInstruments();
            double[] instrumentWeights = filterWeights();
            ((PortfolioYieldsPanel)yieldsPanel).setData(labels, realYields, portfolioYields, portfolio.risk(), sigmas, isLog, instrumentsYields, instrumentsFiltered, instrumentWeights);
        });
        buttonLogScale.addActionListener(e -> {
            for(ActionListener a: buttonDraw.getActionListeners()) {
                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
        });
        checkBoxRebalance.addActionListener(e -> {
            for(ActionListener a: buttonDraw.getActionListeners()) {
                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
        });
    }

    private void onOK() {
        Rectangle bounds = getBounds();
        Main.properties.setProperty("yields.x", String.valueOf((int)bounds.getX()));
        Main.properties.setProperty("yields.y", String.valueOf((int)bounds.getY()));
        Main.properties.setProperty("yields.w", String.valueOf((int)bounds.getWidth()));
        Main.properties.setProperty("yields.h", String.valueOf((int)bounds.getHeight()));
        Main.properties.setProperty("sigma.1", checkBoxSigma1.isSelected() ? "1" : "0");
        Main.properties.setProperty("sigma.2", checkBoxSigma2.isSelected() ? "1" : "0");
        Main.properties.setProperty("sigma.3", checkBoxSigma3.isSelected() ? "1" : "0");
        Main.properties.setProperty("forecast.p", spinnerPeriods.getValue().toString());
        dispose();
    }

    private void createUIComponents() {
        spinnerPeriods = new JSpinner(new SpinnerNumberModel(10, 0, 100, 1));
        yieldsPanel = new PortfolioYieldsPanel();
    }

    private double[] calculateModelYields(boolean isLog) {
        int add = (int)spinnerPeriods.getValue();
        int length = completePeriods + add;
        double[] result = new double[length];
        double yield = portfolio.yield() + 1;

        result[0] = 1;
        for (int row = 1; row < length; row++) {
            result[row] = result[row - 1] * yield;
        }

        if (isLog) {
            return Arrays.stream(result).map(Math::log).toArray();
        } else {
            return result;
        }
    }


    private double[][] calculateInstrumentYields(boolean isLog) {
        double[] weights = portfolio.weights();
        int cols = weights.length;
        int nonNullCols = 0;
        for (double w : weights) {
            if (w > 0) {
                nonNullCols += 1;
            }
        }

        int rows = data.length;
        double[][] result = new double[rows][nonNullCols];
        int indexCol = 0;
        for (int col = 0; col < cols; col++) {
            if (weights[col] > 0) {
                double divider = data[0][col];
                for (int row = 0; row < rows; row++) {
                    double divided = data[row][col] / divider;
                    result[row][indexCol] = isLog ? Math.log(divided) : divided;
                }
                indexCol += 1;
            }
        }
        return result;
    }

    private String[] filterInstruments() {
        List<String> result = new LinkedList<>();
        double[] weights = portfolio.weights();
        String[] instr = portfolio.getInstruments();
        int index = 0;

        for (double w : weights) {
            if (w > 0) {
                result.add(instr[index]);
            }
            index += 1;
        }

        return result.toArray(new String[0]);
    }

    private double[] filterWeights() {
        List<Double> result = new LinkedList<>();
        double[] weights = portfolio.weights();

        for (double w : weights) {
            if (w > 0) {
                result.add(w);
            }
        }

        int length = result.size();
        double[] doubleResult = new double[length];
        for (int i = 0; i < length; i++) {
            doubleResult[i] = result.get(i);
        }

        return doubleResult;
    }

    static void showYields(String[] labels, double[][] data, Portfolio portfolio, double riskFreeRate, Calc.RebalanceMode mode, int threshold) {
        double[][] filtered = Calc.filterValidData(data, portfolio.weights(), new int[] {0}, new int[] {data.length - 1});
        if (filtered == null) {
            return;
        }

        YieldsChart dialog = new YieldsChart(labels, filtered, portfolio, riskFreeRate, mode, threshold);
        dialog.pack();

        int x = Calc.safeParseInt(Main.properties.getProperty("yields.x", "-1"), -1);
        int y = Calc.safeParseInt(Main.properties.getProperty("yields.y", "-1"), -1);
        int w = Calc.safeParseInt(Main.properties.getProperty("yields.w", "-1"), -1);
        int h = Calc.safeParseInt(Main.properties.getProperty("yields.h", "-1"), -1);

        if (x >= 0 && y >= 0 && w >= 0 && h >= 0) {
            Rectangle rec = new Rectangle(x, y, w, h);
            dialog.setBounds(rec);
        }

        boolean s1 = "1".equals(Main.properties.getProperty("sigma.1", "0"));
        boolean s2 = "1".equals(Main.properties.getProperty("sigma.2", "0"));
        boolean s3 = "1".equals(Main.properties.getProperty("sigma.3", "0"));

        dialog.checkBoxSigma1.setSelected(s1);
        dialog.checkBoxSigma2.setSelected(s2);
        dialog.checkBoxSigma3.setSelected(s3);
        dialog.checkBoxRebalance.setSelected(portfolio.getRebalancedMode());

        int forecast = Calc.safeParseInt(Main.properties.getProperty("forecast.p", "10"), 10);
        dialog.spinnerPeriods.setValue(forecast);

        dialog.setTitle(Main.resourceBundle.getString("text.portfolio_yield"));

        SwingUtilities.invokeLater(() -> {
            for(ActionListener a: dialog.buttonDraw.getActionListeners()) {
                a.actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, null));
            }
        });

        dialog.setVisible(true);
    }
}
