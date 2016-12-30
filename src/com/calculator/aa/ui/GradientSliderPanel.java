package com.calculator.aa.ui;

import java.awt.*;
import java.awt.event.*;

public class GradientSliderPanel extends GradientPanel {

    private static int triangle = 6;

    private double position;
    private int mousePosition;
    private ActionListener listener;

    GradientSliderPanel(boolean enable, double pos, ActionListener listen) {
        super(enable);

        position = pos;
        listener = listen;

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
        position = ((double) mouseX) / ((double) getWidth());
        applyFilter();
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
}
