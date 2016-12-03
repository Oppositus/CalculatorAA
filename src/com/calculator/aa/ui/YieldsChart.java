package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.Portfolio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Properties;

class YieldsChart extends JDialog {

    final class SpinnerWheelListener implements MouseWheelListener {

        private final JSpinner Spinner;

        SpinnerWheelListener(JSpinner spinner) {
            Spinner = spinner;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (!Spinner.isEnabled() || e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                return;
            }

            SpinnerModel sm = Spinner.getModel();
            Object next;

            next = e.getWheelRotation() < 0 ? sm.getNextValue() : sm.getPreviousValue();

            if (next != null) {
                Spinner.setValue(next);
            }
        }
    }

    private JPanel contentPane;
    private JButton buttonOK;
    private JPanel yieldsPanel;
    private JToggleButton buttonLogScale;
    private JToggleButton buttonDraw;
    private JSpinner spinnerPeriods;
    private JButton buttonWeights;
    private JCheckBox checkBoxSigma1;
    private JCheckBox checkBoxSigma2;
    private JCheckBox checkBoxSigma3;
    private JToggleButton buttonRebalance;

    private final String[] labels;
    private final double[][] data;
    private final Portfolio portfolio;
    private double[] realYields;
    private double[] portfolioYields;

    private final int completePeriods;

    private JToggleButton lastModeButton;

    private YieldsChart(String[] ls, double[][] sourceData, Portfolio p) {
        data = sourceData;
        labels = Arrays.copyOfRange(ls, sourceData.length - data.length, ls.length);
        portfolio = p;
        completePeriods = data.length;

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
                portfolio.values(),
                portfolio.labels(),
                new String[] {Main.resourceBundle.getString("text.value")})
        );

        buttonDraw.addActionListener(e -> {
            lastModeButton = buttonDraw;
            buttonDraw.setSelected(true);
            buttonRebalance.setSelected(false);
            boolean isLog = buttonLogScale.isSelected();
            realYields = calculateRealYields(isLog);
            portfolioYields = calculateModelYields(isLog);
            boolean[] sigmas = new boolean[] {
                    checkBoxSigma1.isSelected(),
                    checkBoxSigma2.isSelected(),
                    checkBoxSigma3.isSelected()
            };
            ((PortfolioYieldsPanel)yieldsPanel).setData(labels, realYields, portfolioYields, portfolio.risk(), sigmas, isLog, PortfolioYieldsPanel.PortfolioPerformanceMode.MODE_CALCULATION);
        });
        buttonLogScale.addActionListener(e -> {
            if (lastModeButton == null) {
                lastModeButton = buttonDraw;
            }
            for(ActionListener a: lastModeButton.getActionListeners()) {
                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {});
            }
        });
        buttonRebalance.addActionListener(e -> {
            lastModeButton = buttonRebalance;
            buttonDraw.setSelected(false);
            buttonRebalance.setSelected(true);
            boolean isLog = buttonLogScale.isSelected();
            realYields = calculateRealYields(isLog);
            portfolioYields = calculateRebalances(isLog);
            boolean[] sigmas = new boolean[3];
            Arrays.fill(sigmas, false);
            ((PortfolioYieldsPanel)yieldsPanel).setData(labels, realYields, portfolioYields, portfolio.risk(), sigmas, isLog, PortfolioYieldsPanel.PortfolioPerformanceMode.MODE_REBALANCES);
        });
    }

    private void onOK() {
        Properties properties = Main.getProperties();
        Rectangle bounds = getBounds();
        properties.setProperty("yields.x", String.valueOf((int)bounds.getX()));
        properties.setProperty("yields.y", String.valueOf((int)bounds.getY()));
        properties.setProperty("yields.w", String.valueOf((int)bounds.getWidth()));
        properties.setProperty("yields.h", String.valueOf((int)bounds.getHeight()));
        properties.setProperty("sigma.1", checkBoxSigma1.isSelected() ? "1" : "0");
        properties.setProperty("sigma.2", checkBoxSigma2.isSelected() ? "1" : "0");
        properties.setProperty("sigma.3", checkBoxSigma3.isSelected() ? "1" : "0");
        properties.setProperty("forecast.p", spinnerPeriods.getValue().toString());
        dispose();
    }

    private void createUIComponents() {
        spinnerPeriods = new JSpinner(new SpinnerNumberModel(10, 0, 100, 1));
        yieldsPanel = new PortfolioYieldsPanel();
    }

    private double[] calculateRealYields(boolean isLog) {
        double[] weights = portfolio.weights();
        int cols = weights.length;
        double[] result = new double[completePeriods];

        for (int row = 0; row < completePeriods; row++) {
            double sum = 0;
            for (int col = 0; col < cols; col++) {
                sum += data[row][col] / data[0][col] * weights[col];
            }
            result[row] = sum;
        }

        if (isLog) {
            return Arrays.stream(result).map(Math::log).toArray();
        } else {
            return result;
        }
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

    private double[] calculateRebalances(boolean isLog) {
        double[] weights = portfolio.weights();
        int cols = weights.length;

        double[][] dataWeighted = new double[completePeriods][cols];

        for (int row = 0; row < completePeriods; row++) {
            for (int col = 0; col < cols; col++) {
                dataWeighted[row][col] = data[row][col] * weights[col];
            }
        }

        double[] result = new double[completePeriods];
        double[] prev = new double[cols];
        double multiplier = 1.0;

        System.arraycopy(dataWeighted[0], 0, prev, 0, cols);
        result[0] = 1.0;

        for (int row = 1; row < completePeriods; row++) {
            double sum = 0;
            for (int col = 0; col < cols; col++) {
                if (weights[col] > 0) {
                    sum += dataWeighted[row][col] / prev[col] * weights[col];
                }
            }
            multiplier *= sum;
            result[row] = multiplier;
            System.arraycopy(dataWeighted[row], 0, prev, 0, cols);
        }

        if (isLog) {
            return Arrays.stream(result).map(Math::log).toArray();
        } else {
            return result;
        }
    }

    static void showYields(String[] labels, double[][] data, Portfolio portfolio) {
        double[][] filtered = Calc.filterValidData(data, portfolio.weights(), new int[] {0}, new int[] {data.length - 1});
        if (filtered == null) {
            return;
        }

        YieldsChart dialog = new YieldsChart(labels, filtered, portfolio);
        dialog.pack();

        Properties properties = Main.getProperties();
        int x = Integer.parseInt(properties.getProperty("yields.x", "-1"));
        int y = Integer.parseInt(properties.getProperty("yields.y", "-1"));
        int w = Integer.parseInt(properties.getProperty("yields.w", "-1"));
        int h = Integer.parseInt(properties.getProperty("yields.h", "-1"));

        if (x >= 0 && y >= 0 && w >= 0 && h >= 0) {
            Rectangle rec = new Rectangle(x, y, w, h);
            dialog.setBounds(rec);
        }

        boolean s1 = "1".equals(properties.getProperty("sigma.1", "0"));
        boolean s2 = "1".equals(properties.getProperty("sigma.2", "0"));
        boolean s3 = "1".equals(properties.getProperty("sigma.3", "0"));

        dialog.checkBoxSigma1.setSelected(s1);
        dialog.checkBoxSigma2.setSelected(s2);
        dialog.checkBoxSigma3.setSelected(s3);

        int forcast = 10;
        try {
            forcast = Integer.parseInt(properties.getProperty("forecast.p", "10"));
        } catch (NumberFormatException ignored) {}
        dialog.spinnerPeriods.setValue(forcast);

        dialog.setTitle(Main.resourceBundle.getString("text.portfolio_yield"));

        SwingUtilities.invokeLater(() -> {
            if (dialog.lastModeButton == null) {
                dialog.lastModeButton = dialog.buttonDraw;
                dialog.buttonDraw.setSelected(true);
                dialog.buttonRebalance.setSelected(false);
            }
            for(ActionListener a: dialog.lastModeButton.getActionListeners()) {
                a.actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, null) {});
            }
        });

        dialog.setVisible(true);
    }
}
