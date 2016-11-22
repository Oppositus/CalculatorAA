package com.calculator.aa.ui;

import com.calculator.aa.calc.Portfolio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

class CanvasPanel extends JPanel {

    private static final Color axisColor = Color.BLACK;
    private static final Color portfolioColor = Color.BLUE;
    private static final int safeZone = 10;

    private ArrayList<Portfolio> portfolios = new ArrayList<>();
    private double minRisk;
    private double maxRisk;
    private double minYield;
    private double maxYield;

    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private double dx;
    private double dy;

    private Rectangle drawingArea;

    private String minRiskStr;
    private String maxRiskStr;
    private String minYieldStr;
    private String maxYieldStr;

    private int stringHeight;
    private int stringWidth;

    private boolean mouseCrossEnabled = false;
    private int mouseX = 0;
    private int mouseY = 0;

    private class mouseEnterExitListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {}

        @Override
        public void mousePressed(MouseEvent mouseEvent) {}

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {}

        @Override
        public void mouseEntered(MouseEvent mouseEvent) {
            startMouseCross(mouseEvent.getX(), mouseEvent.getY());
        }

        @Override
        public void mouseExited(MouseEvent mouseEvent) {
            stopMouseCross();
        }
    }

    private class mouseMoveListener implements MouseMotionListener {

        @Override
        public void mouseDragged(MouseEvent mouseEvent) {

        }

        @Override
        public void mouseMoved(MouseEvent mouseEvent) {
            moveMouseCross(mouseEvent.getX(), mouseEvent.getY());
        }
    }

    CanvasPanel() {
        super();

        addMouseListener(new mouseEnterExitListener());
        addMouseMotionListener(new mouseMoveListener());
    }

    public void setPortfolios(ArrayList<Portfolio> pfs) {
        portfolios = pfs;

        minRisk = portfolios.get(0).risk();
        minX = minRisk * 0.9;
        maxRisk = portfolios.get(portfolios.size() - 1).risk();
        maxX = maxRisk * 1.1;

        minYield = Double.MAX_VALUE;
        maxYield = Double.MIN_VALUE;

        for (Portfolio p : portfolios) {
            if (p.yield() < minYield) {
                minYield = p.yield();
                minY = minYield * 0.95;
            }
            if (p.yield() > maxYield) {
                maxYield = p.yield();
                maxY = maxYield * 1.1;
            }
        }

        dx = maxX - minX;
        dy = maxY - minY;

        minRiskStr = String.format("%.1f%%", minRisk * 100);
        maxRiskStr = String.format("%.1f%%", maxRisk * 100);
        minYieldStr = String.format("%.1f%%", minYield * 100);
        maxYieldStr = String.format("%.1f%%", maxYield * 100);

        stringHeight = -1;
        stringWidth = -1;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (portfolios.isEmpty()) {
            return;
        }

        int w = getWidth();
        int h = getHeight();

        if (stringHeight < 0 || stringWidth < 0) {
            calculateStringMetrics(g);
        }

        calculateDrawingArea(w, h);

        g.setColor(axisColor);
        drawAxis(g);

        g.setColor(portfolioColor);
        portfolios.forEach(pf -> drawPortfolio(g, pf, w, h));

        if (mouseCrossEnabled && mouseX >= 0 && mouseY >= 0) {
            g.setColor(axisColor);
            drawCross(g);
        }
    }

    private void drawAxis(Graphics g) {
        g.drawRect(drawingArea.x, drawingArea.y, drawingArea.width, drawingArea.height);

        g.drawLine(drawingArea.x, drawingArea.y, drawingArea.x - safeZone / 2, drawingArea.y);
        g.drawLine(drawingArea.x, drawingArea.y + drawingArea.height, drawingArea.x - safeZone / 2, drawingArea.y + drawingArea.height);

        g.drawLine(drawingArea.x, drawingArea.y + drawingArea.height, drawingArea.x, drawingArea.y + drawingArea.height + safeZone / 2);
        g.drawLine(drawingArea.x + drawingArea.width, drawingArea.y + drawingArea.height, drawingArea.x + drawingArea.width, drawingArea.y + drawingArea.height + safeZone / 2);

        g.drawString(minRiskStr, drawingArea.x, drawingArea.y + drawingArea.height + stringHeight);
        g.drawString(maxRiskStr, drawingArea.x + drawingArea.width - stringWidth, drawingArea.y + drawingArea.height + stringHeight);
        g.drawString(minYieldStr, drawingArea.x - stringWidth, drawingArea.y + drawingArea.height);
        g.drawString(maxYieldStr, drawingArea.x - stringWidth, drawingArea.y + stringHeight);
    }

    private void drawPortfolio(Graphics g, Portfolio pf, int width, int height) {
        g.drawRect(mapX(pf.risk()) - 1, mapY(pf.yield()) - 1, 2, 2);
    }

    private void drawCross(Graphics g) {
        g.drawLine(drawingArea.x - safeZone / 2, mouseY, drawingArea.x + drawingArea.width + safeZone / 2, mouseY);
        g.drawLine(mouseX, drawingArea.y - safeZone / 2, mouseX, drawingArea.y + drawingArea.height + safeZone / 2);
    }

    private void calculateStringMetrics(Graphics g) {
        FontMetrics fm = g.getFontMetrics();

        calculateStringMetricsHelper(g, fm, minRiskStr);
        calculateStringMetricsHelper(g, fm, maxRiskStr);
        calculateStringMetricsHelper(g, fm, minYieldStr);
        calculateStringMetricsHelper(g, fm, maxYieldStr);
    }

    private void calculateStringMetricsHelper(Graphics g, FontMetrics fm, String test) {
        Rectangle2D bounds;

        bounds = fm.getStringBounds(test, g);
        if (stringHeight < bounds.getHeight()) {
            stringHeight = (int)Math.ceil(bounds.getHeight());
        }

        if (stringWidth < bounds.getWidth()) {
            stringWidth = (int)Math.ceil(bounds.getWidth());
        }
    }

    private void calculateDrawingArea(int w, int h) {
        drawingArea = new Rectangle(
                stringWidth + safeZone,
                safeZone,
                w - stringWidth - safeZone * 2,
                h - stringHeight - safeZone * 2);
    }

    private int mapX(double x) {
        double posX = (x - minX) / dx;
        return (int)(posX * drawingArea.width + drawingArea.x);
    }

    private int mapY(double y) {
        double posY = (y - minY) / dy;
        return drawingArea.height + drawingArea.y - (int)(posY * drawingArea.height + drawingArea.y);
    }

    private void startMouseCross(int x, int y) {
        mouseCrossEnabled = true;

        if (portfolios.isEmpty()) {
            return;
        }

        moveMouseCross(x, y);
    }

    private void moveMouseCross(int x, int y) {
        if (portfolios.isEmpty()) {
            return;
        }

        if (x >= drawingArea.x && x <= drawingArea.width + drawingArea.x) {
            mouseX = x;
        } else {
            mouseX = -1;
        }

        if (y >= drawingArea.y && y <= drawingArea.height + drawingArea.y) {
            mouseY = y;
        } else {
            mouseY = -1;
        }

        repaint();
    }

    private void stopMouseCross() {
        mouseCrossEnabled = false;
        repaint();
    }
}
