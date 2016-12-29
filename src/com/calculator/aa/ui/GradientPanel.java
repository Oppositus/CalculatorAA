package com.calculator.aa.ui;

import javax.swing.*;
import java.awt.*;

class GradientPanel extends JPanel {

    private Color color0;
    private Color color50;
    private Color color100;
    private GradientPainter painter;

    boolean isPanelEnabled;

    GradientPanel(boolean enable) {
        isPanelEnabled = enable;
    }

    void setColors(Color c0, Color c50, Color c100) {
        color0 = c0;
        color50 = c50;
        color100 = c100;

        painter = new GradientPainter(color0, color50, color100);
    }

    void updateColor(GradientPainter.ColorName name, Color c) {
        painter.updateColor(name, c);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (isPanelEnabled) {
            painter.paintBackground(this, g, null);
        }
    }

    void setGradientEnabled(boolean enable) {
        isPanelEnabled = enable;
        repaint();
    }

}
