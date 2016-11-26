package com.calculator.aa.ui;

import com.calculator.aa.Main;
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

class PortfolioYieldsPanel extends JPanel {

    private static final Color backColor = Color.WHITE;
    private static final Color axisColor = Color.BLACK;
    private static final Color realColor = Color.BLUE;
    private static final Color modelColor = Color.RED;
    static final Color[] sigmaColor = new Color[] {new Color(255, 224, 224), new Color(255, 200, 200), new Color(255, 176, 176)};
    private static final int safeZone = 10;
    private static final int safeTop = 5;

    private double minY;
    private double maxY;
    private double dYield;
    private int dPeriod;

    private Rectangle drawingArea;

    private String minPeriodStr;
    private String maxPeriodStr;
    private String minYieldStr;
    private String maxYieldStr;

    private int stringHeight;
    private int stringWidth;
    private int stringPeriodWidth;

    private boolean mouseCrossEnabled = false;
    private int mouseX = 0;
    private int mouseY = 0;

    private double[] realYields;
    private double[] modelYields;
    private int periods;
    private double risk;
    private boolean[] sigmas;
    private String[] labels;
    private boolean isLog;

    private class mouseEnterExitListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
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

        }

        @Override
        public void mouseMoved(MouseEvent mouseEvent) {
            moveMouseCross(mouseEvent.getX(), mouseEvent.getY());
        }
    }

    PortfolioYieldsPanel() {
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

    void setData(String[] ls, double[] ry, double[] my, double r, boolean[] ss, boolean lg) {
        isLog = lg;
        labels = ls;
        realYields = ry;
        modelYields = my;
        periods = realYields.length + (modelYields.length - realYields.length);
        risk = r;
        sigmas = ss;

        double minYield = Calc.minimum(realYields, modelYields);
        double maxYield = Calc.maximum(realYields, modelYields);

        double dr = (maxYield - minYield) * 0.05;
        minY = minYield - dr;
        maxY = maxYield + dr;
        dYield = maxY - minY;

        minYieldStr = Calc.formatDouble2(isLog ? Math.exp(minY) : minY);
        maxYieldStr = Calc.formatDouble2(isLog ? Math.exp(maxY) : maxY);
        minPeriodStr = labels[0];
        maxPeriodStr = labels[labels.length - 1] + "+" + (modelYields.length - realYields.length);

        stringHeight = -1;
        stringWidth = -1;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();

        if (realYields == null || modelYields == null) {
            drawEmptyCross(g, w, h);
            return;
        }

        if (stringHeight < 0 || stringWidth < 0) {
            calculateStringMetrics(g);
        }

        calculateDrawingArea(w, h);

        for (int i = sigmas.length - 1; i >= 0; i--) {
            if (sigmas[i]) {
                g.setColor(sigmaColor[i]);
                drawSigma(g, i + 1);
            }
        }

        g.setColor(axisColor);
        drawAxis(g);

        g.setColor(realColor);
        drawYields(g, realYields);

        g.setColor(modelColor);
        drawYields(g, modelYields);

        if (mouseCrossEnabled && mouseX >= 0 && mouseY >= 0) {
            g.setColor(axisColor);
            drawCross(g, w, h);
        }

        drawCrossYields(g);
    }

    private void drawAxis(Graphics g) {
        g.drawRect(drawingArea.x, drawingArea.y, drawingArea.width, drawingArea.height);

        g.drawLine(drawingArea.x, drawingArea.y, drawingArea.x - safeZone / 2, drawingArea.y);
        g.drawLine(drawingArea.x, drawingArea.y + drawingArea.height, drawingArea.x - safeZone / 2, drawingArea.y + drawingArea.height);

        g.drawLine(drawingArea.x, drawingArea.y + drawingArea.height, drawingArea.x, drawingArea.y + drawingArea.height + safeZone / 2);
        g.drawLine(drawingArea.x + drawingArea.width, drawingArea.y + drawingArea.height, drawingArea.x + drawingArea.width, drawingArea.y + drawingArea.height + safeZone / 2);

        g.drawString(minYieldStr, drawingArea.x - stringWidth - safeTop, drawingArea.y + drawingArea.height);
        g.drawString(maxYieldStr, drawingArea.x - stringWidth - safeTop, drawingArea.y + stringHeight);

        g.drawString(minPeriodStr, drawingArea.x, drawingArea.y + drawingArea.height + stringHeight + safeTop);
        g.drawString(maxPeriodStr, drawingArea.x + dPeriod * periods - stringPeriodWidth, drawingArea.y + drawingArea.height + stringHeight + safeTop);

        if (1.0 - minY > 0.05) {
            int pc100 = mapY(isLog ? Math.log(1.0) : 1.0);
            g.drawLine(drawingArea.x - safeZone / 2, pc100, drawingArea.x + drawingArea.width, pc100);
            g.drawString(Calc.formatDouble2(1.0), drawingArea.x - stringWidth - safeTop, pc100 + stringHeight / 2);
        }
    }

    private void drawCross(Graphics g, int w, int h) {
        if (!drawingArea.contains(mouseX, mouseY)) {
            drawEmptyCross(g, w, h);
            return;
        }

        g.drawLine(drawingArea.x - safeZone / 2, mouseY, drawingArea.x + drawingArea.width + safeZone / 2, mouseY);
        g.drawLine(mouseX, drawingArea.y - safeZone / 2, mouseX, drawingArea.y + drawingArea.height + safeZone / 2);

        int labelX = mouseX;
        if (labelX + stringPeriodWidth > drawingArea.x + drawingArea.width) {
            labelX = drawingArea.x + drawingArea.width - stringPeriodWidth;
        }

        int period = reMapX(mouseX) - 1;
        String periodString;
        if (period < 0) {
            periodString = " ";
        } else if (period < labels.length) {
            periodString = labels[period];
        } else {
            periodString = labels[labels.length - 1] + "+" + (period - labels.length + 1);
        }

        g.setColor(backColor);
        g.fillRect(
                labelX - safeTop,
                drawingArea.y + drawingArea.height + safeTop + 1,
                stringPeriodWidth + safeTop * 3,
                stringHeight + safeTop);
        g.setColor(axisColor);
        g.drawString(periodString, labelX, drawingArea.y + drawingArea.height + stringHeight + safeTop);

        g.setColor(backColor);
        g.fillRect(
                drawingArea.x - stringWidth - safeTop,
                mouseY - stringHeight / 2,
                stringWidth,
                stringHeight + safeTop);
        g.setColor(axisColor);
        String yield = Calc.formatDouble2(isLog ? Math.exp(reMapY(mouseY)) : reMapY(mouseY));
        g.drawString(yield, drawingArea.x - stringWidth - safeTop, mouseY + stringHeight / 2);
    }

    private void drawEmptyCross(Graphics g, int w, int h) {
        g.drawLine(0, mouseY, w, mouseY);
        g.drawLine(mouseX, 0, mouseX, h);
    }

    private void drawYields(Graphics g, double[] yields) {
        int length = yields.length;
        int[] xs = new int[length];
        int[] ys = new int[length];
        int period = reMapX(mouseX) - 1;

        for (int i = 0; i < length; i ++) {
            xs[i] = mapX(i);
            ys[i] = mapY(yields[i]);

            g.drawRect(xs[i] - 1, ys[i] - 1, 2, 2);

            if(period == i) {
                g.drawRect(xs[i] - 3, ys[i] - 3, 6, 6);
            }
        }

        g.drawPolyline(xs, ys, length);
    }

    private void drawCrossYields(Graphics g) {
        int period = reMapX(mouseX) - 1;

        if (period < 0) {
            return;
        }

        int yr = 0;
        int ym = 0;

        String sr = "";
        String sm = "";

        if (period < realYields.length) {
            g.setColor(realColor);
            double y = realYields[period];
            yr = mapY(y);
            sr = Calc.formatDouble2(isLog ? Math.exp(y) : y);
            g.drawLine(mapX(period), yr, drawingArea.x - safeTop, yr);
        }
        if (period < modelYields.length) {
            g.setColor(modelColor);
            double y = modelYields[period];
            ym = mapY(y);
            sm = Calc.formatDouble2(isLog ? Math.exp(y) : y);
            g.drawLine(mapX(period), ym, drawingArea.x - safeTop, ym);
        }

        if (!sr.isEmpty()) {
            int add = yr < ym ? stringHeight : 0;
            g.setColor(realColor);
            g.fillRect(
                    drawingArea.x - stringWidth - safeTop,
                    yr - add,
                    stringWidth,
                    stringHeight + safeTop);
            g.setColor(backColor);
            g.drawString(sr, drawingArea.x - stringWidth - safeTop, yr - add + stringHeight);
        }

        if (!sm.isEmpty()) {
            int add = ym > yr ? stringHeight : 0;
            g.setColor(modelColor);
            g.fillRect(
                    drawingArea.x - stringWidth - safeTop,
                    ym - (stringHeight - add),
                    stringWidth,
                    stringHeight + safeTop);
            g.setColor(backColor);
            g.drawString(sm, drawingArea.x - stringWidth - safeTop, ym + add);
        }
    }

    private void drawSigma(Graphics g, int sigma) {
        Polygon p = new Polygon();
        int length = modelYields.length;

        g.setClip(drawingArea);

        for (int i = 0; i < length; i++) {
            double y = isLog ? Math.exp(modelYields[i]) : modelYields[i];
            double s = y + y * risk * sigma;
            p.addPoint(mapX(i), mapY(isLog ? Math.log(s) : s));
        }

        for (int i = length - 1; i >= 0; i--) {
            double y = isLog ? Math.exp(modelYields[i]) : modelYields[i];
            double s = y - y * risk * sigma;
            p.addPoint(mapX(i), mapY(isLog ? Math.log(s) : s));
        }

        g.fillPolygon(p);

        g.setClip(null);
    }

    private void calculateStringMetrics(Graphics g) {
        FontMetrics fm = g.getFontMetrics();

        calculateStringMetricsHelper(g, fm, minYieldStr);
        calculateStringMetricsHelper(g, fm, maxYieldStr);

        Rectangle2D bounds = fm.getStringBounds(maxPeriodStr, g);
        stringPeriodWidth = (int)Math.ceil(bounds.getWidth());
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

        dPeriod = drawingArea.width / periods;

        drawingArea.width = Math.min(dPeriod * periods, drawingArea.width);
    }

    private int mapX(int period) {
        return drawingArea.x + dPeriod * period;
    }

    private int mapY(double y) {
        double posY = (y - minY) / dYield;
        return drawingArea.height + drawingArea.y - (int)(posY * drawingArea.height);
    }

    private int reMapX(int x) {
        int xx = x - drawingArea.x;
        if (xx >= 0 && xx <= drawingArea.width) {
            return (int)Math.round((double)xx / dPeriod) + 1;
        }
        return 0;
    }

    private double reMapY(int y) {
        int yy = y - drawingArea.y;
        if (yy >= 0 && yy <= drawingArea.height) {
            double pos = 1 - (double)yy / drawingArea.height;
            return dYield * pos + minY;
        }
        return -1;
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
