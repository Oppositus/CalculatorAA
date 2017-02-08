package com.calculator.aa.ui;

import com.calculator.aa.calc.Calc;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

public class GradientSliderPanel extends GradientPanel {

    private double position;
    private double minimum;
    private double maximum;
    private int mousePosition;
    private final ActionListener listener;

    GradientSliderPanel(boolean enable, ActionListener listen) {
        super(enable);

        listener = listen;
        position = 0;
        minimum = 0;
        maximum = 1;

        mousePosition = -1;

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);

                if (isPanelEnabled) {
                    setFilterPosition(mouseEvent.getX());
                }
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                super.mouseExited(mouseEvent);
                mousePosition = -1;
                repaint();
            }
        });

        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent mouseEvent) {
                super.mouseMoved(mouseEvent);
                mousePosition = mouseEvent.getX();
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent mouseEvent) {
                super.mouseDragged(mouseEvent);
                mousePosition = mouseEvent.getX();
                if (isPanelEnabled) {
                    setFilterPosition(mousePosition);
                }
            }
        });
    }

    private void setFilterPosition(int mouseX) {
        double pos = Math.min((double)mouseX / (double)getWidth(), 0.99);

        if (pos >= 0.0 && pos <= 1.0) {
            position = pos;
            applyFilter();
        }
    }

    private void applyFilter() {
        repaint();
        listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();

        if (isPanelEnabled) {
            if (mousePosition >= 0) {
                g.setColor(Color.BLACK);
                g.drawLine(mousePosition, 0, mousePosition, h);
            }
        }

        g.setColor(isPanelEnabled ? Color.BLACK : Color.DARK_GRAY);

        Rectangle clip = new Rectangle(0, 0, w, h);
        g.setClip(clip);

        int pos = (int)(position * w);

        final int triangle = 6;
        int[] xs = new int[] {
                pos - triangle,
                pos,
                pos + triangle,
        };
        int[] ytop = new int[] {
                0,
                triangle,
                0
        };

        int[] ybot = new int[] {
                h - 1,
                h - 1 - triangle,
                h - 1
        };

        g.fillPolygon(xs, ytop, 3);
        g.fillPolygon(xs, ybot, 3);
        g.drawLine(pos, 0, pos, h);

        if (isPanelEnabled) {
            g.setColor(Color.BLACK);
            String currentSlider = Calc.formatDouble2((maximum - minimum) * position + minimum);
            FontMetrics fm = g.getFontMetrics();
            Rectangle2D bounds = fm.getStringBounds(currentSlider, g);
            int posTextX;
            if (position < 0.5) {
                posTextX = pos + 5;
            } else {
                posTextX = pos - (int)bounds.getWidth() - 5;
            }
            g.drawString(currentSlider, posTextX, h / 2 + (int)(bounds.getHeight() / 2));
        }

        g.setClip(null);
    }

    void resetPosition() {
        position = 0;
        applyFilter();
    }

    void setPosition(double pos) {
        position = pos;
        applyFilter();
    }

    double getPosition() {
        return position;
    }

    void setGradientBounds(double min, double max) {
        minimum = min;
        maximum = max;
        repaint();
    }
}
