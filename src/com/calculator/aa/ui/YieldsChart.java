package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.Portfolio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Properties;

public class YieldsChart extends JDialog {

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
    private JButton buttonDraw;
    private JSpinner spinnerPeriods;
    private JButton buttonWeights;
    private JCheckBox checkBoxSigma1;
    private JCheckBox checkBoxSigma2;
    private JCheckBox checkBoxSigma3;

    private final String[] labels;
    private final double[][] data;
    private final Portfolio portfolio;
    private double[] realYields;
    private double[] portfolioYields;

    private final int completePeriods;

    private YieldsChart(String[] ls, double[][] sourceData, Portfolio p) {
        data = Calc.filterValidData(sourceData, p.weights());
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

        buttonWeights.addActionListener(e -> ShowTable.show("Портфель", portfolio.values(), portfolio.labels(), new String[] {"Значение"}));
        buttonDraw.addActionListener(e -> {
            boolean isLog = buttonLogScale.isSelected();
            realYields = calculateRealYields(isLog);
            portfolioYields = calculateModelYields(isLog);
            boolean[] sigmas = new boolean[] {
                    checkBoxSigma1.isSelected(),
                    checkBoxSigma2.isSelected(),
                    checkBoxSigma3.isSelected()
            };
            ((PortfolioYieldsPanel)yieldsPanel).setData(labels, realYields, portfolioYields, portfolio.risk(), sigmas, isLog);
        });
        buttonLogScale.addActionListener(e -> {
            for(ActionListener a: buttonDraw.getActionListeners()) {
                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {});
            }
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
        dispose();
    }

    private void createUIComponents() {
        spinnerPeriods = new JSpinner(new SpinnerNumberModel(10, 0, 100, 1));
        yieldsPanel = new PortfolioYieldsPanel();
    }

    private double[] calculateRealYields(boolean isLog) {
        double[] weights = portfolio.weights();
        int cols = weights.length;

        double[][] dataWeighted = new double[completePeriods][cols];

        for (int row = 0; row < completePeriods; row++) {
            for (int col = 0; col < cols; col++) {
                dataWeighted[row][col] = data[row][col] * weights[col];
            }
        }

        double denom = Arrays.stream(dataWeighted[0]).sum();

        double[] result = new double[completePeriods];

        for (int row = 0; row < completePeriods; row++) {
            result[row] = Arrays.stream(dataWeighted[row]).sum() / denom;
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

    static void showYields(String[] labels, double[][] data, Portfolio portfolio) {
        YieldsChart dialog = new YieldsChart(labels, data, portfolio);
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

        dialog.setTitle("Доходность портфеля");

        SwingUtilities.invokeLater(() -> {
            for(ActionListener a: dialog.buttonDraw.getActionListeners()) {
                a.actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, null) {});
            }
        });

        dialog.setVisible(true);
    }
}
