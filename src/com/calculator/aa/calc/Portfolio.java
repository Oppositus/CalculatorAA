package com.calculator.aa.calc;

public class Portfolio implements Comparable<Portfolio> {
    private final DoublePoint parameters;
    private final double[] weights;

    Portfolio(DoublePoint p, double[] w) {
        parameters = p;
        weights = w;
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

    public void print() {
        for (double weight : weights) {
            System.out.print(Calc.formatPercent(weight));
            System.out.print("\t");
        }
        System.out.print(Calc.formatPercent(parameters.getX()));
        System.out.print("\t");
        System.out.println(Calc.formatPercent(parameters.getY()));
    }
}
