package com.calculator.aa.calc;

import java.awt.Point;

public class DoublePoint {
    private final double x;
    private final double y;

    public DoublePoint(double _x, double _y) {
        x = _x;
        y = _y;
    }

    public DoublePoint() {
        this(0, 0);
    }

    public DoublePoint(DoublePoint other) {
        this(other.x, other.y);
    }

    public int mapX(int max) {
        return (int)(x / max);
    }

    public int mapY(int max) {
        return (int)(y / max);
    }

    public Point map(int maxX, int maxY) {
        return new Point(mapX(maxX), mapY(maxY));
    }
}
