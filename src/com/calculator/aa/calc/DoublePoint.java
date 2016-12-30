package com.calculator.aa.calc;

public class DoublePoint {
    private double x;
    private double y;

    public DoublePoint(double _x, double _y) {
        x = _x;
        y = _y;
    }

    DoublePoint(DoublePoint o) {
        x = o.x;
        y = o.y;
    }

    public double getX() {
        return x;
    }

    public void setX(double _x) {
        x = _x;
    }

    public double getY() {
        return y;
    }

    public void setY(double _y) {
        y = _y;
    }

    public boolean equals(Object o) {
        return o != null &&
                o instanceof DoublePoint &&
                Math.abs(x - ((DoublePoint)o).x) < Calc.epsilon &&
                Math.abs(y - ((DoublePoint)o).y) < Calc.epsilon;
    }
}
