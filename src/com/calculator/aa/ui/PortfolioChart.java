package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.Portfolio;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class PortfolioChart extends JDialog {

    enum Coefficients {
        NONE,
        SHARPE,
        SORTINO
    }

    private JPanel contentPane;
    private JButton buttonOK;
    private JPanel chartPanel;
    private JTable tableLimitations;
    private JButton buttonCompute;
    private JCheckBox cbFrontierOnly;
    private JButton buttonAccuracy;
    private JButton buttonAccuracyMax;
    private JComboBox<String> comboBoxFrom;
    private JComboBox<String> comboBoxTo;
    private JCheckBox cbShowRebalances;
    private JCheckBox checkBoxCAL;
    private JSpinner spinnerCAL;
    private JButton buttonZoomPortfolios;
    private JButton buttonConvertRate;
    private JLabel labelRatePeriod;
    private JRadioButton radioButtonNone;
    private JRadioButton radioButtonSharpe;
    private JButton buttonCoefMin;
    private JPanel panelCoefGradient;
    private JButton buttonCoefMax;
    private JRadioButton radioButtonSortino;
    private JComboBox<String> comboBoxRebalanceMethod;
    private JSpinner spinnerThreshold;
    private JLabel labelThreshold;
    private ButtonGroup coefficientGroup;

    private final String[] instruments;
    private final double[][] data;

    private int indexFrom;
    private int indexTo;

    private final PortfolioChartHelper helper;

    private class LimitationTableCellRenderer extends DefaultTableCellRenderer {
        private final Color back = new Color(212, 212, 212);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            cell.setBackground(row == 3 ? back : Color.WHITE);
            cell.setForeground(Color.BLACK);

            return cell;
        }
    }

    private class LimitationTableModel extends DefaultTableModel {

        LimitationTableModel(Object[][] body, Object[] header) {
            super(body, header);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return row != 3;
        }
    }

    private PortfolioChart(String[] i, double[][] d) {
        instruments = i;
        data = d;
        helper = new PortfolioChartHelper();
        helper.setPanel((PortfolioChartPanel)chartPanel);
        ((PortfolioChartPanel)chartPanel).setHelper(helper);
        buttonConvertRate.setText(Main.resourceBundle.getString("ui.convert_y_m"));
        buttonConvertRate.setToolTipText(Main.resourceBundle.getString("ui.convert_y_m_help"));
        labelRatePeriod.setText(Main.resourceBundle.getString("ui.label_y"));

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonCompute);

        buttonOK.addActionListener(e -> onOK());

        String[] periods = Main.getPeriods(0, Integer.MAX_VALUE);
        comboBoxFrom.setModel(new DefaultComboBoxModel<>(Arrays.copyOfRange(periods, 0, periods.length - 2)));
        comboBoxFrom.setSelectedIndex(0);
        comboBoxTo.setModel(new DefaultComboBoxModel<>(Arrays.copyOfRange(periods, 2, periods.length)));
        comboBoxTo.setSelectedIndex(periods.length - 3);
        spinnerCAL.addMouseWheelListener(new SpinnerWheelListener(spinnerCAL));
        spinnerThreshold.addMouseWheelListener(new SpinnerWheelListener(spinnerThreshold));

        comboBoxRebalanceMethod.setModel(new DefaultComboBoxModel<>(new String[] {
                Main.resourceBundle.getString("text.periodical"),
                Main.resourceBundle.getString("text.threshold"),

        }));

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

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            SwingUtilities.invokeLater(() -> {

                int length = instruments.length - 1;

                LimitationTableModel model = (LimitationTableModel) tableLimitations.getModel();

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
                fromIndex[0] = periodsList.indexOf((String) comboBoxFrom.getSelectedItem());
                if (fromIndex[0] < 0) {
                    fromIndex[0] = 0;
                }

                int[] toIndex = new int[1];
                toIndex[0] = periodsList.indexOf((String) comboBoxTo.getSelectedItem());
                if (toIndex[0] < 0) {
                    toIndex[0] = data.length - 1;
                }

                if (toIndex[0] - fromIndex[0] < 2) {
                    setCursor(Cursor.getDefaultCursor());
                    return;
                }

                double[][] dataFiltered = Calc.filterValidData(data, maximals, fromIndex, toIndex);

                if (dataFiltered == null) {
                    return;
                }

                indexFrom = fromIndex[0];
                indexTo = toIndex[0];

                double[][] corrTable = Calc.correlationTable(dataFiltered);
                double[] avYields = new double[length];
                double[] sdYields = new double[length];

                for (int col = 0; col < length; col++) {
                    double[] column = Calc.column(dataFiltered, col);
                    avYields[col] = Calc.averagePercentYields(column);
                    sdYields[col] = Calc.stdevYields(column);
                }

                String[] trueInstr = Arrays.copyOfRange(instruments, 1, instruments.length);
                int dividers = calculateDivision(Arrays.copyOf(minimals, minimals.length), Arrays.copyOf(maximals, maximals.length), false, 0);
                if (dividers < 0) {
                    int force = JOptionPane.showConfirmDialog(
                            Main.getFrame(),
                            Main.resourceBundle.getString("text.too_many_computations"),
                            Main.resourceBundle.getString("text.warning"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (force == JOptionPane.YES_OPTION) {
                        dividers = calculateDivision(Arrays.copyOf(minimals, minimals.length), Arrays.copyOf(maximals, maximals.length), true, -dividers);
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                        return;
                    }

                }

                Calc.RebalanceMode mode = getRebalaceMode();
                int threshold = getRebalaceThreshold();

                List<Portfolio> portfolios = Calc.iteratePortfolios(corrTable, avYields, sdYields, minimals, maximals, trueInstr, dataFiltered, dividers, mode, threshold);

                List<Portfolio> portfoliosCompare = null;
                if (Arrays.stream(compares).sum() == 100) {
                    portfoliosCompare = Calc.iteratePortfolios(corrTable, avYields, sdYields, compares, compares, trueInstr, dataFiltered, 100, mode, threshold);
                }

                boolean isSelected = cbShowRebalances.isSelected();
                portfolios.forEach(p -> p.setRebalancedMode(isSelected));
                if (portfoliosCompare != null) {
                    portfoliosCompare.forEach(p -> p.setRebalancedMode(isSelected));
                    portfoliosCompare.sort(Portfolio::compareTo);
                }

                if (isSelected) {
                    portfolios.sort(Portfolio::compareTo);
                }

                updatePortfolios(portfolios, portfoliosCompare, dataFiltered);

                SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
            });
        });

        cbFrontierOnly.addActionListener(e -> ((PortfolioChartPanel) chartPanel).setFrontierOnlyMode(cbFrontierOnly.isSelected()));

        buttonAccuracy.addActionListener(e -> {
            List<Portfolio> frontier = helper.getFrontierPortfoliosNoFilter();
            if (frontier == null || frontier.isEmpty()) {
                return;
            }

            List<Portfolio> accuracyPortfolios = addAccuracy();

            if (accuracyPortfolios.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    for(ActionListener a: buttonAccuracy.getActionListeners()) {
                        a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
                    }
                });
            }
        });
        buttonAccuracyMax.addActionListener(e -> {
            List<Portfolio> frontier = helper.getFrontierPortfoliosNoFilter();
            if (frontier == null || frontier.isEmpty()) {
                return;
            }

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            List<Portfolio> accuracyPortfolios = addAccuracy();
            List<Portfolio> accuracyPortfolios2 = addAccuracy();

            if (!accuracyPortfolios.equals(accuracyPortfolios2) || (accuracyPortfolios.size() == 0 && accuracyPortfolios2.size() == 0)) {
                SwingUtilities.invokeLater(() -> {
                    for(ActionListener a: buttonAccuracyMax.getActionListeners()) {
                        a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
                    }
                });
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        });
        cbShowRebalances.addActionListener(e -> {
            if (cbShowRebalances.isSelected()) {
                comboBoxRebalanceMethod.setEnabled(true);
                boolean isThreshold = comboBoxRebalanceMethod.getSelectedIndex() == 1;
                labelThreshold.setEnabled(isThreshold);
                spinnerThreshold.setEnabled(isThreshold);
            } else {
                comboBoxRebalanceMethod.setEnabled(false);
                labelThreshold.setEnabled(false);
                spinnerThreshold.setEnabled(false);
            }

            for(ActionListener a: buttonCompute.getActionListeners()) {
                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
        });
        checkBoxCAL.addActionListener(actionEvent -> {
            boolean isSelected = checkBoxCAL.isSelected();

            Enumeration<AbstractButton> radios = coefficientGroup.getElements();
            while (radios.hasMoreElements()) {
                AbstractButton radio = radios.nextElement();
                radio.setEnabled(isSelected);
            }

            buttonZoomPortfolios.setEnabled(isSelected);
            for(ActionListener a: buttonCompute.getActionListeners()) {
                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
        });
        buttonZoomPortfolios.addActionListener(actionEvent -> ((PortfolioChartPanel) chartPanel).zoomAllToPortfolios());
        buttonConvertRate.addActionListener(e -> {

            double rate = (double)spinnerCAL.getValue() / 100.0 + 1;

            if (buttonConvertRate.getText().equals(Main.resourceBundle.getString("ui.convert_y_m"))) {
                buttonConvertRate.setText(Main.resourceBundle.getString("ui.convert_m_y"));
                buttonConvertRate.setToolTipText(Main.resourceBundle.getString("ui.convert_m_y_help"));
                labelRatePeriod.setText(Main.resourceBundle.getString("ui.label_m"));
                rate = Math.pow(rate, 1.0 / 12.0);
            } else {
                buttonConvertRate.setText(Main.resourceBundle.getString("ui.convert_y_m"));
                buttonConvertRate.setToolTipText(Main.resourceBundle.getString("ui.convert_y_m_help"));
                labelRatePeriod.setText(Main.resourceBundle.getString("ui.label_y"));
                rate = Math.pow(rate, 12);
            }

            spinnerCAL.setValue((rate - 1) * 100);

            if (checkBoxCAL.isSelected()) {
                for (ActionListener a : buttonCompute.getActionListeners()) {
                    a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
                }
            }
        });
        radioButtonNone.addActionListener(e -> {
            if (radioButtonNone.isSelected()) {
                ((GradientSliderPanel)panelCoefGradient).resetPosition();
                setCoefficientVisible(helper.getPortfolios());
            }
        });
        radioButtonSharpe.addActionListener(e -> {
            if (radioButtonSharpe.isSelected()) {
                ((GradientSliderPanel)panelCoefGradient).resetPosition();
                setCoefficientVisible(helper.getPortfolios());
            }
        });
        radioButtonSortino.addActionListener(actionEvent -> {
            if (radioButtonSortino.isSelected()) {
                ((GradientSliderPanel)panelCoefGradient).resetPosition();
                setCoefficientVisible(helper.getPortfolios());
            }
        });

        buttonCoefMin.addActionListener(actionEvent -> ((GradientSliderPanel)panelCoefGradient).setPosition(0));
        buttonCoefMax.addActionListener(actionEvent -> ((GradientSliderPanel)panelCoefGradient).setPosition(0.99));
        comboBoxRebalanceMethod.addActionListener(actionEvent -> {
            int selected = comboBoxRebalanceMethod.getSelectedIndex();
            boolean isThreshold = selected == 1;
            labelThreshold.setEnabled(isThreshold);
            spinnerThreshold.setEnabled(isThreshold);
        });
    }

    private int calculateDivision(int[] minimals, int[] maximals, boolean force, int start) {

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

        int lastNotZero = start;
        int result;

        if (nonEmpty >= 10) {
            result = 100 / variants[3];
            if (checkModulo(minimals, maximals, result)) {
                return result;
            } else if (!force) {
                return -1;
            }
        }

        for (int i = start; i < length; i++) {
            sum[0] = 0;

            if (i == length - 1 && nonEmpty > 5) {
                result = 100 / variants[lastNotZero];
                if (checkModulo(minimals, maximals, result)) {
                    return result;
                } else if (!force) {
                    return -(lastNotZero + 1);
                }
            }

            calculateDivisionHelper(
                    Arrays.copyOf(minimals, minimals.length),
                    Arrays.copyOf(maximals, maximals.length),
                    Arrays.copyOf(minimals, minimals.length),
                    0, variants[i], sum
            );

            if (sum[0] > limit) {
                int index = lastNotZero >= 0 ? lastNotZero : i;
                result = 100 / variants[index];
                if (checkModulo(minimals, maximals, result)) {
                    return result;
                } else if (!force) {
                    return -(index + 1);
                }
            }

            if (sum[0] != 0) {
                lastNotZero = i;
            }
        }

        return 100;
    }

    // todo: optimize this shit!
    private void calculateDivisionHelper(int[] minimals, int[] maximals, int[] weights, int index, int step, int[] sum) {

        int l1 = weights.length - 1;

        while (weights[index] <= maximals[index]) {

            // clear tail
            System.arraycopy(minimals, index + 1, weights, index + 1, weights.length - (index + 1));

            int testSum = Calc.sumIntArray(weights);

            if (testSum > 100) {
                break;
            }

            if (testSum == 100) {
                sum[0] += 1;
            }

            if (index < l1 && testSum < 100) {
                calculateDivisionHelper(minimals, maximals, weights, index + 1, step, sum);
            }

            weights[index] += step;
        }
    }

    private boolean checkModulo(int[] minimals, int[] maximals, int divider) {
        int step = 100 / divider;
        return Arrays.stream(minimals).allMatch(i -> i % step == 0) &&
                Arrays.stream(maximals).allMatch(i -> i % step == 0);
    }

    private void onOK() {
        Rectangle bounds = getBounds();
        Main.properties.setProperty("portfolio.x", String.valueOf((int)bounds.getX()));
        Main.properties.setProperty("portfolio.y", String.valueOf((int)bounds.getY()));
        Main.properties.setProperty("portfolio.w", String.valueOf((int)bounds.getWidth()));
        Main.properties.setProperty("portfolio.h", String.valueOf((int)bounds.getHeight()));

        dispose();
    }

    private void updateLimitations() {
        int length = instruments.length;
        String[][] limits = new String[4][length];
        for (int col = 0; col < length; col++) {
            limits[0][col] = col == 0 ? Main.resourceBundle.getString("text.min") : "0";
            limits[1][col] = col == 0 ? Main.resourceBundle.getString("text.max") : "100";
            limits[2][col] = col == 0 ? Main.resourceBundle.getString("text.compare") : "0";
            limits[3][col] = col == 0 ? Main.resourceBundle.getString("text.nearest") : getNearestComponent(col - 1);
        }

        tableLimitations.setModel(new LimitationTableModel(limits, instruments));
        for (int i = 0; i < length; i++) {
            tableLimitations.getColumnModel().getColumn(i).setCellRenderer(new LimitationTableCellRenderer());
        }

    }

    void updateNearestWeights() {
        int length = instruments.length;
        LimitationTableModel model = (LimitationTableModel)tableLimitations.getModel();
        for (int col = 1; col < length; col++) {
            model.setValueAt(getNearestComponent(col -1), 3, col);
        }
    }

    private String getNearestComponent(int component) {
        Portfolio pf = helper.getNearest();
        if (pf == null) {
            return "0";
        }

        return Integer.toString((int)(pf.weights()[component] * 100));
    }

    private void createUIComponents() {
        chartPanel = new PortfolioChartPanel();
        comboBoxFrom = new JComboBox<>();
        comboBoxTo = new JComboBox<>();
        spinnerCAL = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 100.0, 0.1));
        spinnerThreshold = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));

        panelCoefGradient = new GradientSliderPanel(false, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                double filtered = ((GradientSliderPanel)panelCoefGradient).getPosition();
                helper.setMinCoefficient(filtered);
                ((PortfolioChartPanel) chartPanel).repaintAll();
            }
        });
        ((GradientSliderPanel)panelCoefGradient).setColors(
                Main.gradient.getPointColor(GradientPainter.ColorName.Begin),
                Main.gradient.getPointColor(GradientPainter.ColorName.Middle),
                Main.gradient.getPointColor(GradientPainter.ColorName.End)
        );
    }

    private List<Portfolio> addAccuracy() {
        List<Portfolio> frontier = helper.getFrontierPortfoliosNoFilter();
        List<Portfolio> accuracyPortfolios = new LinkedList<>();

        if (frontier == null || frontier.isEmpty()) {
            return accuracyPortfolios;
        }

        int length = instruments.length - 1;
        double[][] dataFiltered = helper.getDataFiltered();
        double[][] corrTable = Calc.correlationTable(dataFiltered);
        double[] avYields = new double[length];
        double[] sdYields = new double[length];
        for (int col = 0; col < length; col++) {
            double[] column = Calc.column(dataFiltered, col);
            avYields[col] = Calc.averagePercentYields(column);
            sdYields[col] = Calc.stdevYields(column);
        }
        String[] trueInstr = Arrays.copyOfRange(instruments, 1, instruments.length);
        int dividers = 100;

        LimitationTableModel model = (LimitationTableModel)tableLimitations.getModel();
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

        Calc.RebalanceMode mode = getRebalaceMode();
        int threshold = getRebalaceThreshold();

        int index = 0;
        int size = frontier.size();
        while (index < size) {
            Portfolio first = frontier.get(index);
            Portfolio next = index < size - 1 ? frontier.get(index + 1) : first;

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
                    Calc.iteratePortfolios(corrTable, avYields, sdYields, minimals, maximals, trueInstr, dataFiltered, dividers, mode, threshold)
            );

            index += 1;
        }

        List<Portfolio> portfoliosCompare = null;
        if (Arrays.stream(compares).sum() == 100) {
            portfoliosCompare = Calc.iteratePortfolios(corrTable, avYields, sdYields, compares, compares, trueInstr, dataFiltered, 100, mode, threshold);
        }

        boolean isSelected = cbShowRebalances.isSelected();
        accuracyPortfolios.forEach(p -> p.setRebalancedMode(isSelected));
        if (portfoliosCompare != null) {
            portfoliosCompare.forEach(p -> p.setRebalancedMode(isSelected));
        }

        accuracyPortfolios.sort(Portfolio::compareTo);
        updatePortfolios(accuracyPortfolios, portfoliosCompare, dataFiltered);
        return accuracyPortfolios;
    }

    private void updatePortfolios(List<Portfolio> pfs, List<Portfolio> pfsComp, double[][] df) {
        setCoefficientVisible(pfs);
        helper.setRebalanceMode(getRebalaceMode(), getRebalaceThreshold());
        helper.setPortfolios(pfs,
                pfsComp,
                df,
                Main.getPeriods(indexFrom, indexTo),
                checkBoxCAL.isSelected() ? (double)spinnerCAL.getValue() / 100.0 : -1
        );
    }

    void setPortfolioToCompare(int[] weights) {
        LimitationTableModel model = (LimitationTableModel)tableLimitations.getModel();

        int length = weights.length;
        for (int i = 0; i < length; i++) {
            model.setValueAt(String.valueOf(weights[i]), 2, i + 1);
        }
    }

    private void setCoefficientVisible(List<Portfolio> pfs) {

        Coefficients coefficient = Coefficients.NONE;

        if (radioButtonSharpe.isEnabled() && radioButtonSharpe.isSelected()) {
            coefficient = Coefficients.SHARPE;
        }
        if (radioButtonSortino.isEnabled() && radioButtonSortino.isSelected()) {
            coefficient = Coefficients.SORTINO;
        }

        if (coefficient == Coefficients.NONE) {
            pfs.forEach(p -> p.setCoefficient(Double.NaN));
            buttonCoefMin.setText("0");
            buttonCoefMin.setEnabled(false);
            buttonCoefMax.setText("0");
            buttonCoefMax.setEnabled(false);
            ((GradientPanel)panelCoefGradient).setGradientEnabled(false);
        }

        double minCoef;
        double maxCoef;
        double dCoef;
        double rate = ((double)spinnerCAL.getValue()) / 100.0;
        Calc.RebalanceMode mode = getRebalaceMode();
        int threshold = getRebalaceThreshold();

        List<Double> coefList = null;

        if (coefficient == Coefficients.SHARPE) {
            coefList = pfs.stream()
                    .map(p -> Calc.ratioSharpe(p, rate, mode, threshold))
                    .collect(Collectors.toList());

        } else if (coefficient == Coefficients.SORTINO) {
            coefList = pfs.stream()
                    .map(p -> Calc.ratioSortino(p, rate, mode, threshold))
                    .collect(Collectors.toList());
        }

        if (coefList != null) {
            minCoef = coefList.stream().mapToDouble(d -> d).min().orElse(0);
            maxCoef = coefList.stream().mapToDouble(d -> d).max().orElse(1);
            dCoef = maxCoef - minCoef;

            if (Math.abs(dCoef) < Calc.epsilon) {
                dCoef = Calc.epsilon;
            }

            int length = pfs.size();
            for (int i = 0; i < length; i++) {
                pfs.get(i).setCoefficient((coefList.get(i) - minCoef) / dCoef);
            }

            buttonCoefMin.setText(Calc.formatDouble2(minCoef));
            buttonCoefMin.setEnabled(true);
            buttonCoefMax.setText(Calc.formatDouble2(maxCoef));
            buttonCoefMax.setEnabled(true);
            ((GradientPanel) panelCoefGradient).setGradientEnabled(true);
            ((GradientSliderPanel) panelCoefGradient).setGradientBounds(minCoef, maxCoef);
        }

        ((PortfolioChartPanel) chartPanel).repaintAll();
    }

    private Calc.RebalanceMode getRebalaceMode() {
        return comboBoxRebalanceMethod.getSelectedIndex() == 0 ? Calc.RebalanceMode.PERIODIC : Calc.RebalanceMode.THRESHOLD;
    }

    private int getRebalaceThreshold() {
        return (int)spinnerThreshold.getValue();
    }

    static void showChart(String[] instruments, double[][] data) {
        PortfolioChart dialog = new PortfolioChart(instruments, data);
        dialog.updateLimitations();
        dialog.setTitle(Main.resourceBundle.getString("text.portfolios"));
        dialog.setLocationRelativeTo(Main.getFrame());

        dialog.pack();

        int x = Calc.safeParseInt(Main.properties.getProperty("portfolio.x", "-1"), -1);
        int y = Calc.safeParseInt(Main.properties.getProperty("portfolio.y", "-1"), -1);
        int w = Calc.safeParseInt(Main.properties.getProperty("portfolio.w", "-1"), -1);
        int h = Calc.safeParseInt(Main.properties.getProperty("portfolio.h", "-1"), -1);

        if (x >= 0 && y >= 0 && w >= 0 && h >= 0) {
            Rectangle rec = new Rectangle(x, y, w, h);
            dialog.setBounds(rec);
        }

        SwingUtilities.invokeLater(() -> {
            for(ActionListener a: dialog.buttonCompute.getActionListeners()) {
                a.actionPerformed(new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, null));
            }
        });

        dialog.setVisible(true);
    }
}
