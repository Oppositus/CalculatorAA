package com.calculator.aa.ui;

import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.DoublePoint;
import com.calculator.aa.calc.Portfolio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class PortfolioChartPanel extends JPanel {

    private static final Color axisColor = Color.BLACK;
    private static final Color portfolioColor = Color.BLUE;
    private static final Color backColor = Color.WHITE;
    private static final Color selectedColor = Color.RED;
    private static final Color zoomColor = Color.GRAY;
    private static final int safeZone = 10;
    private static final int safeTop = 5;

    private List<Portfolio> portfolios = new ArrayList<>();
    private List<Portfolio> savedPortfolios;
    private List<Portfolio> optimalPortfolios = new ArrayList<>();

    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private double dRisk;
    private double dYield;

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

    private boolean dragMode = false;
    private int dragStartX = 0;
    private int dragEndX = 0;

    private boolean borderOnlyMode = false;

    private Portfolio nearest = null;

    private double[][] dataFiltered;
    private String[] periodsFiltered;
    private int dividers;

    private double zoomMin;
    private double zoomMax;

    private class mouseEnterExitListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            if (nearest == null) {
                return;
            }

            if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
                YieldsChart.showYields(periodsFiltered, dataFiltered, nearest);
            } else if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                ShowTable.show("Портфель", nearest.values(), nearest.labels(), new String[] {"Значение"});
            }
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            dragMode = true;
            dragStartX = mouseEvent.getX();
            dragEndX = mouseEvent.getX();
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
            dragEndX = mouseEvent.getX();
            if (dragMode) {
                int from = Math.min(dragStartX, dragEndX);
                int to = Math.max(dragStartX, dragEndX);

                zoomMin = reMapX(from);
                zoomMax = reMapX(to);

                setZoom();
            }
            dragMode = false;
        }

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
            dragEndX = mouseEvent.getX();
            repaint();
        }

        @Override
        public void mouseMoved(MouseEvent mouseEvent) {
            dragMode = false;
            moveMouseCross(mouseEvent.getX(), mouseEvent.getY());
        }
    }

    PortfolioChartPanel() {
        super();

        addMouseListener(new mouseEnterExitListener());
        addMouseMotionListener(new mouseMoveListener());

        setCursor(
                getToolkit().createCustomCursor(
                        new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB),
                        new Point(0, 0),
                        "null")
        );
    }

    void setPortfolios(List<Portfolio> pfs, double[][] df, String[] pf, int dv) {

        dividers = dv;

        if (pfs != null && pfs.isEmpty()) {
            return;
        }

        if (pfs != null && df != null && pf != null) {
            portfolios = pfs;
            dataFiltered = df;
            periodsFiltered = pf;
        }

        if (pfs != null) {
            optimalPortfolios = Calc.getOptimalBorder(portfolios);
        }

        if (borderOnlyMode) {
            savedPortfolios = portfolios;
            portfolios = optimalPortfolios;
        }

        if (pfs == null && !borderOnlyMode) {
            portfolios = savedPortfolios;
        }

        if (portfolios == null || portfolios.isEmpty()) {
            return;
        }

        double minRisk = portfolios.get(0).risk();
        double maxRisk = portfolios.get(portfolios.size() - 1).risk();
        double dr;
        if (pfs != null && pfs.size() == 1) {
            dr = 0.05;
        } else {
            dr = (maxRisk - minRisk) * 0.05;
        }

        minX = minRisk - dr;
        maxX = maxRisk + dr;

        double minYield = Double.MAX_VALUE;
        double maxYield = Double.MIN_VALUE;

        for (Portfolio p : portfolios) {
            if (p.yield() < minYield) {
                minYield = p.yield();
            }
            if (p.yield() > maxYield) {
                maxYield = p.yield();
            }

            double dy;
            if (pfs != null && pfs.size() == 1) {
                dy = 0.05;
            } else {
                dy = (maxYield - minYield) * 0.05;
            }
            minY = minYield - dy;
            maxY = maxYield + dy;
        }

        dRisk = maxX - minX;
        dYield = maxY - minY;

        minRiskStr = Calc.formatPercent1(minX);
        maxRiskStr = Calc.formatPercent1(maxX);
        minYieldStr = Calc.formatPercent1(minY);
        maxYieldStr = Calc.formatPercent1(maxY);

        stringHeight = -1;
        stringWidth = -1;

        repaint();
    }

    void setBorderOnlyMode(boolean mode) {
        if (borderOnlyMode != mode) {
            borderOnlyMode = mode;
            setPortfolios(null, null, null, dividers);
        }
    }

    List<Portfolio> getBorderPortfolios() {
        return optimalPortfolios;
    }

    double[][] getDataFiltered() {
        return dataFiltered;
    }

    int getDividers() {
        return dividers;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();

        if (portfolios.isEmpty()) {
            if (mouseCrossEnabled && mouseX >= 0 && mouseY >= 0) {
                g.setColor(axisColor);
                drawEmptyCross(g, w, h);
            }
            return;
        }

        if (stringHeight < 0 || stringWidth < 0) {
            calculateStringMetrics(g);
        }

        calculateDrawingArea(w, h);

        if (dragMode) {
            g.setColor(zoomColor);
            drawZoom(g, w, h);
        }

        g.setColor(axisColor);
        drawAxis(g);

        g.setColor(portfolioColor);

        if (borderOnlyMode) {
            optimalPortfolios.forEach(pf -> drawPortfolio(g, pf));
        } else {
            portfolios.forEach(pf -> drawPortfolio(g, pf));
        }

        if (!optimalPortfolios.isEmpty() && optimalPortfolios.size() > 1) {
            drawOptimalBorder(g);
        }

        if (mouseCrossEnabled && mouseX >= 0 && mouseY >= 0) {
            drawNearest(g);
            g.setColor(axisColor);
            drawCross(g, w, h);
        }
    }

    private void drawZoom(Graphics g, int w, int h) {
        int from = Math.min(dragStartX, dragEndX);
        int to = Math.max(dragStartX, dragEndX);
        g.fillRect(from, 0, to - from, h);
    }

    private void drawAxis(Graphics g) {
        g.drawRect(drawingArea.x, drawingArea.y, drawingArea.width, drawingArea.height);

        g.drawLine(drawingArea.x, drawingArea.y, drawingArea.x - safeZone / 2, drawingArea.y);
        g.drawLine(drawingArea.x, drawingArea.y + drawingArea.height, drawingArea.x - safeZone / 2, drawingArea.y + drawingArea.height);

        g.drawLine(drawingArea.x, drawingArea.y + drawingArea.height, drawingArea.x, drawingArea.y + drawingArea.height + safeZone / 2);
        g.drawLine(drawingArea.x + drawingArea.width, drawingArea.y + drawingArea.height, drawingArea.x + drawingArea.width, drawingArea.y + drawingArea.height + safeZone / 2);

        g.drawString(minRiskStr, drawingArea.x, drawingArea.y + drawingArea.height + stringHeight + safeTop);
        g.drawString(maxRiskStr, drawingArea.x + drawingArea.width - stringWidth, drawingArea.y + drawingArea.height + stringHeight + safeTop);

        g.drawString(minYieldStr, drawingArea.x - stringWidth - safeTop, drawingArea.y + drawingArea.height);
        g.drawString(maxYieldStr, drawingArea.x - stringWidth - safeTop, drawingArea.y + stringHeight);
    }

    private void drawPortfolio(Graphics g, Portfolio pf) {
        g.drawRect(mapX(pf.risk()) - 1, mapY(pf.yield()) - 1, 2, 2);
    }

    private void drawNearest(Graphics g) {

        if (!drawingArea.contains(mouseX, mouseY)) {
            nearest = null;
            return;
        }

        DoublePoint p = new DoublePoint(reMapX(mouseX), reMapY(mouseY));

        double distance = Double.MAX_VALUE;
        Portfolio nearestPortfolio = null;

        List<Portfolio> pfs = borderOnlyMode ? optimalPortfolios : portfolios;

        for (Portfolio pf : pfs) {
            double dst = Calc.distance(p, pf.performance());
            if (dst < distance) {
                distance = dst;
                nearestPortfolio = pf;
            }
        }

        nearest = nearestPortfolio;
        if (nearest != null) {
            g.setColor(selectedColor);
            g.fillRect(mapX(nearest.risk()) - 4, mapY(nearest.yield()) - 4, 7, 7);
        }
    }

    private void drawOptimalBorder(Graphics g) {

        int length = optimalPortfolios.size();
        int[] xxs = new int[length];
        int[] yys = new int[length];
        int i = 0;

        for (Portfolio p : optimalPortfolios) {
            xxs[i] = mapX(p.risk());
            yys[i] = mapY(p.yield());
            i += 1;
        }

        g.drawPolyline(xxs, yys, length);
    }

    private void drawCross(Graphics g, int w, int h) {
        if (!drawingArea.contains(mouseX, mouseY)) {
            drawEmptyCross(g, w, h);
            return;
        }

        double xPos = reMapX(mouseX);
        double yPos = reMapY(mouseY);

        g.drawLine(drawingArea.x - safeZone / 2, mouseY, drawingArea.x + drawingArea.width + safeZone / 2, mouseY);
        g.drawLine(mouseX, drawingArea.y - safeZone / 2, mouseX, drawingArea.y + drawingArea.height + safeZone / 2);

        if (nearest == null) {
            return;
        }

        String rString = "Р: " + Calc.formatPercent2(nearest.risk());
        String yString = "Д: " + Calc.formatPercent2(nearest.yield());

        FontMetrics fm = g.getFontMetrics();
        Rectangle2D boundsR = fm.getStringBounds(rString, g);
        Rectangle2D boundsY = fm.getStringBounds(yString, g);
        int max = (int)Math.ceil(Math.max(boundsR.getWidth(), boundsY.getWidth()));

        int plus = 10;
        int plus2 = 5;

        int rectX = mouseX + safeZone / 2 + plus2;
        int rectY = mouseY + plus;
        int textX = mouseX + safeZone + plus2;
        int textY = mouseY + stringHeight + plus;

        if (textX + max > drawingArea.x + drawingArea.width - safeZone) {
            rectX -= max + safeZone * 2 + plus;
            textX -= max + safeZone * 2 + plus;
        }

        if (textY + stringHeight * 2 > drawingArea.y + drawingArea.height) {
            rectY -= stringHeight * 4 - plus;
            textY -= stringHeight * 4 - plus;
        }

        g.drawRect(rectX, rectY, max + safeZone, stringHeight * 2 + safeZone / 2);

        g.setColor(backColor);
        g.fillRect(rectX + 1, rectY + 1, max + safeZone - 1, stringHeight * 2 + safeZone / 2 - 1);

        g.setColor(axisColor);
        g.drawString(rString, textX, textY);
        g.drawString(yString, textX, textY + stringHeight);

        String rCrossString = Calc.formatPercent1(xPos);
        String yCrossString = Calc.formatPercent1(yPos);
        boundsR = fm.getStringBounds(rCrossString, g);
        int rWidth = (int)boundsR.getWidth();
        int rHeight = (int)boundsR.getHeight();
        boundsY = fm.getStringBounds(yCrossString, g);
        int yWidth = (int)boundsY.getWidth();
        int yHeight = (int)boundsY.getHeight();

        rectX = mouseX - 1 - safeZone;
        rectY = drawingArea.y + drawingArea.height + stringHeight - safeTop;
        textX = mouseX - 1;
        textY = drawingArea.y + drawingArea.height + stringHeight + safeTop;

        if (textX + rWidth + 1 > w - safeZone) {
            textX = w - safeZone - rWidth + 1;
            rectX = w - safeZone - rWidth + 1;
        }

        g.setColor(backColor);
        g.fillRect(rectX, rectY, rWidth + safeZone + 1, rHeight + 1);
        g.setColor(axisColor);
        g.drawString(rCrossString, textX, textY);

        rectX = drawingArea.x - yWidth - safeZone - safeTop;
        rectY = mouseY - 1 - safeZone;
        textX = drawingArea.x - yWidth - safeZone;
        textY = mouseY - 1 + safeTop;

        g.setColor(backColor);
        g.fillRect(rectX, rectY, yWidth + safeZone, yHeight + safeTop + 1);
        g.setColor(axisColor);
        g.drawString(yCrossString, textX, textY);
    }

    private void drawEmptyCross(Graphics g, int w, int h) {
        g.drawLine(0, mouseY, w, mouseY);
        g.drawLine(mouseX, 0, mouseX, h);
    }

    private void calculateStringMetrics(Graphics g) {
        FontMetrics fm = g.getFontMetrics();

        calculateStringMetricsHelper(g, fm, minRiskStr);
        calculateStringMetricsHelper(g, fm, maxRiskStr);
        calculateStringMetricsHelper(g, fm, minYieldStr);
        calculateStringMetricsHelper(g, fm, maxYieldStr);
    }

    private void calculateStringMetricsHelper(Graphics g, FontMetrics fm, String test) {
        Rectangle2D bounds = fm.getStringBounds(test, g);
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
        double posX = (x - minX) / dRisk;
        return drawingArea.x + (int)(posX * drawingArea.width);
    }

    private int mapY(double y) {
        double posY = (y - minY) / dYield;
        return drawingArea.height + drawingArea.y - (int)(posY * drawingArea.height);
    }

    private double reMapX(int x) {
        int xx = x - drawingArea.x;
        if (xx >= 0 && xx <= drawingArea.width) {
            double pos = (double)xx / drawingArea.width;
            return dRisk * pos + minX;
        }
        return -1;
    }

    private double reMapY(int y) {
        int yy = y - drawingArea.y;
        if (yy >= 0 && yy <= drawingArea.height) {
            double pos = 1 - (double)yy / drawingArea.height;
            return dYield * pos + minY;
        }
        return -1;
    }

    private void setZoom() {
        if (portfolios == null || portfolios.isEmpty()) {
            return;
        }

        List<Portfolio> pfs = portfolios.stream()
                .filter(p -> p.risk() >= zoomMin && p.risk() <= zoomMax)
                .sorted(Portfolio::compareTo)
                .collect(Collectors.toList());

        setPortfolios(pfs, dataFiltered, periodsFiltered, dividers);
    }

    private void startMouseCross(int x, int y) {
        mouseCrossEnabled = true;
        moveMouseCross(x, y);
    }

    private void moveMouseCross(int x, int y) {
        mouseX = x;
        mouseY = y;
        repaint();
    }

    private void stopMouseCross() {
        mouseCrossEnabled = false;
        repaint();
    }
}
