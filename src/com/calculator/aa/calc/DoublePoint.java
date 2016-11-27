package com.calculator.aa.calc;

public class DoublePoint {
    private final double x;
    private final double y;

    public DoublePoint(double _x, double _y) {
        x = _x;
        y = _y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public boolean equals(Object o) {
        return o != null &&
                o instanceof DoublePoint &&
                Math.abs(x - ((DoublePoint)o).x) < Calc.epsilon &&
                Math.abs(y - ((DoublePoint)o).y) < Calc.epsilon;
    }
}
