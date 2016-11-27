package com.calculator.aa.calc;

import com.calculator.aa.Main;

import java.util.Arrays;

public class Portfolio implements Comparable<Portfolio> {
    private final DoublePoint parameters;
    private final double[] weights;
    private final String[] instruments;

    Portfolio(DoublePoint p, double[] w, String[] i) {
        parameters = p;
        weights = w;
        instruments = i;
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
}
