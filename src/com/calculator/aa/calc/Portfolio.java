package com.calculator.aa.calc;

import com.calculator.aa.Main;

import java.util.Arrays;

public class Portfolio implements Comparable<Portfolio> {
    private final DoublePoint parameters;
    private final double[] weights;
    private final String[] instruments;
    private double yieldWithRebalances;
    private boolean rebalancesDone;

    Portfolio(DoublePoint p, double[] w, String[] i) {
        parameters = p;
        weights = w;
        instruments = i;
        yieldWithRebalances = 0;
        rebalancesDone = false;
    }

    @Override
    public int compareTo(Portfolio o) {
        double myRisk = parameters.getX();
        double otherRisk = o.parameters.getX();

        if (myRisk < otherRisk) {
            return -1;
        } else if (myRisk > otherRisk) {
            return 1;
        } else {
            return 0;
        }
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof Portfolio)) {
            return false;
        }

        Portfolio oo = (Portfolio)o;

        if (!parameters.equals(oo.parameters)) {
            return false;
        }

        if (!Arrays.equals(instruments, oo.instruments)) {
            return false;
        }

        double[] ws = oo.weights;
        int length = weights.length;
        if (length != ws.length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (Math.abs(weights[i] - ws[i]) > Calc.epsilon) {
                return false;
            }
        }

        return true;
    }

    int compareToYield(Portfolio o) {
        double myYield = parameters.getY();
        double otherYield = o.parameters.getY();

        if (myYield < otherYield) {
            return -1;
        } else if (myYield > otherYield) {
            return 1;
        } else {
            return 0;
        }
    }

    public String[] labels() {
        int length = weights.length;
        String[] result = new String[length + 2];

        System.arraycopy(instruments, 0, result, 0, length);

        result[length] = Main.resourceBundle.getString("text.risk");
        result[length + 1] = Main.resourceBundle.getString("text.yield");

        return result;
    }

    public double[][] values() {
        int length = weights.length;
        double[][] result = new double[length + 2][1];

        for (int i = 0; i < length; i++) {
            result[i][0] = weights[i];
        }

        result[length][0] = risk();
        result[length + 1][0] = yield();

        return result;
    }

    public double[] weights() {
        return weights;
    }

    public double yield() {
        return parameters.getY();
    }

    public double risk() {
        return parameters.getX();
    }

    public DoublePoint performance() {
        return parameters;
    }

    public double yieldRebalances() {
        return yieldWithRebalances;
    }

    public void calculateRebalances(double[][] data) {

        if (rebalancesDone) {
            return;
        }

        int rows = data.length;
        int cols = weights.length;
        double[][] dataWeighted = new double[rows][cols];

        for (int row = 0; row < rows; row++) {
            System.arraycopy(data[row], 0, dataWeighted[row], 0, cols);
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                dataWeighted[row][col] = data[row][col] * weights[col];
            }
        }

        double[] prev = new double[cols];
        double multiplier = 1.0;

        System.arraycopy(dataWeighted[0], 0, prev, 0, cols);

        for (int row = 1; row < rows; row++) {
            double sum = 0;
            for (int col = 0; col < cols; col++) {
                if (weights[col] > 0) {
                    sum += dataWeighted[row][col] / prev[col] * weights[col];
                }
            }
            multiplier *= sum;
            System.arraycopy(dataWeighted[row], 0, prev, 0, cols);
        }

        yieldWithRebalances = multiplier;
        rebalancesDone = true;
    }
}
