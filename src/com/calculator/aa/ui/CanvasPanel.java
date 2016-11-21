package com.calculator.aa.ui;

import com.calculator.aa.calc.DoublePoint;

import javax.swing.*;
import java.awt.*;

class CanvasPanel extends JPanel {

    private final Color axisColor = Color.BLACK;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawAxis(g, new DoublePoint(0.1, 0.05), new DoublePoint(0.5, 0.22));
    }

    private void drawAxis(Graphics g, DoublePoint min, DoublePoint max) {
        DoublePoint minAjusted = min.multiply(0.95);
        DoublePoint maxAjusted = min.multiply(1.05);

        int border = 10;
        int halfBorder = border / 2;
        int w = getWidth();
        int h = getHeight();

        g.setColor(axisColor);

        g.drawLine(border - halfBorder, h - border, w - border, h - border); // horz
        g.drawLine(border, h - halfBorder, border, border); // vert
    }

}
