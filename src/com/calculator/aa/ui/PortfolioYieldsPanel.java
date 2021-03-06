package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class PortfolioYieldsPanel extends JPanel {

    private static final Color backColor = Color.WHITE;
    private static final Color axisColor = Color.BLACK;
    private static final Color axisColor2 = Color.LIGHT_GRAY;
    private static final Color realColor = Color.BLUE;
    private static final Color modelColor = Color.RED;
    static final Color[] sigmaColor = new Color[] {new Color(0xFF, 0xDD, 0xDD), new Color(0xFF, 0xCC, 0xCC), new Color(0xFF, 0xBB, 0xBB)};
    private static final Color[] colorArray = new Color[] {
            new Color(0x99, 0x00, 0x00),
            new Color(0x00, 0x99, 0x00),
            new Color(0x00, 0x00, 0x99),

            new Color(0xCC, 0x00, 0x00),
            new Color(0x00, 0xCC, 0x00),
            new Color(0x00, 0x00, 0xCC),

            new Color(0x99, 0x66, 0x66),
            new Color(0x66, 0x99, 0x66),
            new Color(0x66, 0x66, 0x99),

            new Color(0xCC, 0x66, 0x66),
            new Color(0x66, 0xCC, 0x66),
            new Color(0x66, 0x66, 0xCC),

            new Color(0xCC, 0x99, 0x99),
            new Color(0x99, 0xCC, 0x99),
            new Color(0x99, 0x99, 0xCC)
    };

    private static final int safeZone = 10;
    private static final int safeTop = 5;
    private static final BasicStroke thick = new BasicStroke(2);
    private static final BasicStroke thin = new BasicStroke(1);

    private double minY;
    private double maxY;
    private double dYield;
    private double dPeriod;

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

    private BufferedImage labelCalculations;

    private final Rectangle labelArea;
    private boolean inLabelArea;
    private int mousePressedX;
    private int mousePressedY;
    private int labelPressedX;
    private int labelPressedY;

    private double[] realYields;
    private double[] modelYields;
    private double[][] instrumentsYields;
    private int periods;
    private double risk;
    private boolean[] sigmas;
    private String[] labels;
    private String[] instruments;
    private boolean isLog;

    private class mouseEnterExitListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            if (!inLabelArea) {
                mousePressedX = -1;
                return;
            }

            mousePressedX = mouseEvent.getX();
            mousePressedY = mouseEvent.getY();
            labelPressedX = (int)labelArea.getX();
            labelPressedY = (int)labelArea.getY();
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

            if (mousePressedX < 0) {
                return;
            }

            int dx = mouseEvent.getX() - mousePressedX;
            int dy = mouseEvent.getY() - mousePressedY;

            labelArea.setLocation(labelPressedX + dx, labelPressedY + dy);

            if (labelArea.getX() < 0) {
                labelArea.setLocation(0, (int)labelArea.getY());
            }
            if (labelArea.getX() + labelArea.getWidth() > drawingArea.getWidth()) {
                labelArea.setLocation((int)(drawingArea.getWidth() - labelArea.getWidth()), (int)labelArea.getY());
            }
            if (labelArea.getY() < 0) {
                labelArea.setLocation((int)labelArea.getX(), 0);
            }
            if (labelArea.getY() + labelArea.getHeight() > drawingArea.getHeight()) {
                labelArea.setLocation((int)labelArea.getX(), (int)(drawingArea.getHeight() - labelArea.getHeight()));
            }

            repaint();
        }

        @Override
        public void mouseMoved(MouseEvent mouseEvent) {
            int x = mouseEvent.getX();
            int y = mouseEvent.getY();

            if (drawingArea != null && labelArea.contains(x - drawingArea.getX(), y - drawingArea.getY())) {
                if (!inLabelArea) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                inLabelArea = true;
            } else {
                if (inLabelArea) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                }
                inLabelArea = false;
            }
            moveMouseCross(x, y);
        }
    }

    PortfolioYieldsPanel() {
        super();

        addMouseListener(new mouseEnterExitListener());
        addMouseMotionListener(new mouseMoveListener());

        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        labelArea = new Rectangle();
        inLabelArea = false;
    }

    void setData(String[] ls, double[] ry, double[] my, double r, boolean[] ss, boolean lg, double[][] iy, String[] is, double[] ws) {
        isLog = lg;
        labels = ls;
        realYields = ry;
        modelYields = my;
        instrumentsYields = iy;

        int len = is.length;
        instruments = new String[len];
        for (int i = 0; i < len; i++) {
            instruments[i] = String.format("%s (%s)", is[i], Calc.formatPercent0(ws[i]));
        }

        periods = realYields.length + (modelYields.length - realYields.length) - 1;
        risk = r;
        sigmas = ss;

        double minYield = Calc.minimum(realYields, modelYields);
        double maxYield = Calc.maximum(realYields, modelYields);

        int length = is.length;
        for (int i = 0; i < length; i++) {
            double[] col = Calc.column(iy, i);
            minYield = Math.min(minYield, Arrays.stream(col).min().orElse(Double.MAX_VALUE));
            maxYield = Math.max(maxYield, Arrays.stream(col).max().orElse(Double.NEGATIVE_INFINITY));
        }

        double dr = (maxYield - minYield) * 0.05;
        minY = minYield - dr;
        maxY = maxYield + dr;
        dYield = maxY - minY;

        minYieldStr = minY >= 0 ? Calc.formatDouble2(isLog ? Math.exp(minY) : minY) : "";
        maxYieldStr = Calc.formatDouble2(isLog ? Math.exp(maxY) : maxY);
        minPeriodStr = labels[0];
        maxPeriodStr = labels[labels.length - 1];
        if (modelYields.length - realYields.length > 0) {
            maxPeriodStr += "+" + (modelYields.length - realYields.length);
        }

        stringHeight = -1;
        stringWidth = -1;

        labelCalculations = null;

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

        if (labelCalculations == null) {
            createLabels(g);
        }

        calculateDrawingArea(w, h);

        for (int i = sigmas.length - 1; i >= 0; i--) {
            if (sigmas[i]) {
                g.setColor(sigmaColor[i]);
                drawSigma(g, i + 1);
            }
        }

        drawAxis(g);

        int length = instruments.length;
        int lengthCA = colorArray.length;
        for (int i = 0; i < length; i++) {
            double[] iy = Calc.column(instrumentsYields, i);
            g.setColor(colorArray[i % lengthCA]);
            drawYields(g, iy, false);
        }

        ((Graphics2D)g).setStroke(thick);
        g.setColor(realColor);
        drawYields(g, realYields, true);
        g.setColor(modelColor);
        drawYields(g, modelYields, true);
        ((Graphics2D)g).setStroke(thin);

        if (mouseCrossEnabled && mouseX >= 0 && mouseY >= 0) {
            g.setColor(axisColor);
            drawCross(g, w, h);
        }

        drawCrossYields(g);

        drawLabels(g);
    }

    private void drawAxis(Graphics g) {
        g.setColor(axisColor2);

        if (isLog) {
            double max = Math.exp(maxY);
            for (double y = 2; y < max; y *= 2) {
                int yy = mapY(Math.log(y));
                g.drawLine(drawingArea.x - safeTop, yy, drawingArea.x + drawingArea.width, yy);
                g.drawString(String.valueOf(y), drawingArea.x - stringWidth - safeTop, yy + safeTop);
            }
        } else {
            int step = maxY < 10 ? 1 : (maxY < 100 ? 10 : (maxY < 1000 ? 100 : 1000));
            for (double y = step; y < maxY; y += step) {
                if (y < minY || y == 1) {
                    continue;
                }
                int yy = mapY(y);
                g.drawLine(drawingArea.x - safeTop, yy, drawingArea.x + drawingArea.width, yy);
                g.drawString(String.valueOf(y), drawingArea.x - stringWidth - safeTop, yy + safeTop);
            }
        }

        int lastXPos = drawingArea.x + stringPeriodWidth;
        int length = labels.length;
        int lastPX = 0;
        for (int p = 1; p < length; p++) {
            int xx = mapX(p);
            if (xx - lastPX < 25) {
                continue;
            }
            lastPX = xx;
            g.drawLine(xx, drawingArea.y + drawingArea.height + safeTop, xx, safeZone);
            if (xx > lastXPos && xx + stringPeriodWidth < drawingArea.x + dPeriod * periods - stringPeriodWidth) {
                lastXPos = xx + stringPeriodWidth;
                g.drawString(labels[p], xx, drawingArea.y + drawingArea.height + stringHeight + safeTop);
            }
        }

        g.setColor(axisColor);

        g.drawRect(drawingArea.x, drawingArea.y, drawingArea.width, drawingArea.height);

        g.drawLine(drawingArea.x, drawingArea.y, drawingArea.x - safeTop, drawingArea.y);
        g.drawLine(drawingArea.x, drawingArea.y + drawingArea.height, drawingArea.x - safeTop, drawingArea.y + drawingArea.height);

        g.drawLine(drawingArea.x, drawingArea.y + drawingArea.height, drawingArea.x, drawingArea.y + drawingArea.height + safeTop);
        g.drawLine(drawingArea.x + drawingArea.width, drawingArea.y + drawingArea.height, drawingArea.x + drawingArea.width, drawingArea.y + drawingArea.height + safeTop);

        g.drawString(minYieldStr, drawingArea.x - stringWidth - safeTop, drawingArea.y + drawingArea.height);
        g.drawString(maxYieldStr, drawingArea.x - stringWidth - safeTop, drawingArea.y + stringHeight);

        g.drawString(minPeriodStr, drawingArea.x, drawingArea.y + drawingArea.height + stringHeight + safeTop);
        g.drawString(maxPeriodStr, drawingArea.x + (int)(dPeriod * periods) - stringPeriodWidth, drawingArea.y + drawingArea.height + stringHeight + safeTop);

        if (1.0 - minY > 0.05) {
            int pc100 = mapY(isLog ? Math.log(1.0) : 1.0);
            g.drawLine(drawingArea.x - safeTop, pc100, drawingArea.x + drawingArea.width, pc100);
            g.drawString(Calc.formatDouble2(1.0), drawingArea.x - stringWidth - safeTop, pc100 + stringHeight / 2);
        }
    }

    private void drawCross(Graphics g, int w, int h) {
        if (!drawingArea.contains(mouseX, mouseY)) {
            drawEmptyCross(g, w, h);
            return;
        }

        g.drawLine(drawingArea.x + safeTop, mouseY, drawingArea.x, mouseY);
        g.drawLine(mouseX, drawingArea.y + drawingArea.height, mouseX, drawingArea.y + drawingArea.height - safeTop);

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

    private void drawYields(Graphics g, double[] yields, boolean selectable) {
        int length = yields.length;
        int[] xs = new int[length];
        int[] ys = new int[length];
        int period = reMapX(mouseX) - 1;

        for (int i = 0; i < length; i ++) {
            xs[i] = mapX(i);
            ys[i] = mapY(yields[i]);

            g.drawRect(xs[i] - 1, ys[i] - 1, 2, 2);

            if(period == i && selectable) {
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

    private void drawLabels(Graphics g) {
        g.drawImage(labelCalculations, drawingArea.x + (int)labelArea.getX(), drawingArea.y + (int)labelArea.getY(), null);
    }

    private void calculateStringMetrics(Graphics g) {
        FontMetrics fm = g.getFontMetrics();

        calculateStringMetricsHelper(g, fm, minYieldStr);
        calculateStringMetricsHelper(g, fm, maxYieldStr);

        Rectangle2D bounds = fm.getStringBounds(maxPeriodStr, g);
        stringPeriodWidth = (int)Math.ceil(bounds.getWidth());
    }

    private void createLabels(Graphics g) {
        List<String> allLabels = new LinkedList<>();
        allLabels.add(Main.resourceBundle.getString("text.label_real_performance"));
        allLabels.add(Main.resourceBundle.getString("text.label_calculated_performance"));
        allLabels.addAll(Arrays.asList(instruments));
        int length = allLabels.size();

        FontMetrics fm = g.getFontMetrics();

        List<Rectangle2D> allBounds = allLabels.stream().map(l -> fm.getStringBounds(l, g)).collect(Collectors.toList());

        int lineWidth = 20;
        int labelCalculationsWidth = safeZone * 2 + lineWidth + safeTop +
                allBounds.stream()
                        .map(RectangularShape::getWidth)
                        .mapToInt(w -> (int)w.doubleValue())
                        .max()
                        .orElse(0);

        if (Main.osName.startsWith("windows")) {
            labelCalculationsWidth += safeZone * 2;
        }

        int lineHeight = (int)allBounds.get(0).getHeight();
        int lineHeightDiv2 = lineHeight / 2;
        int labelHeight = lineHeight * length;

        labelCalculations = new BufferedImage(labelCalculationsWidth, labelHeight + safeTop, BufferedImage.TYPE_INT_ARGB);

        Graphics2D glc = labelCalculations.createGraphics();

        labelArea.setBounds(safeZone, safeZone, labelCalculationsWidth, labelHeight + safeTop);

        glc.setColor(backColor);
        glc.fillRect(0, 0, labelCalculationsWidth, labelHeight + safeTop);

        glc.setColor(axisColor);
        glc.drawRect(0, 0, labelCalculationsWidth - 1, labelHeight + safeTop - 1);

        int lengthCA = colorArray.length;
        for (int i = 0; i < length; i++) {
            if (i == 0) {
                glc.setColor(realColor);
            } else if (i == 1) {
                glc.setColor(modelColor);
            } else {
                glc.setColor(colorArray[(i - 2) % lengthCA]);
            }

            glc.drawLine(safeZone, lineHeight * (i + 1) - lineHeightDiv2, safeTop + lineWidth, lineHeight * (i + 1) - lineHeightDiv2);
            if (i < 2) {
                glc.drawLine(safeZone, lineHeight * (i + 1) - lineHeightDiv2 + 1, safeTop + lineWidth, lineHeight * (i + 1) - lineHeightDiv2 + 1);
            }
            glc.drawString(allLabels.get(i), safeZone + lineWidth + safeTop, lineHeight * (i + 1));
        }

        glc.dispose();
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

        dPeriod = (double)drawingArea.width / periods;
    }

    private int mapX(int period) {
        return drawingArea.x + (int)(dPeriod * period);
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
