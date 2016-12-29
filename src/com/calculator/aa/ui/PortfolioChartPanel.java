package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.DoublePoint;
import com.calculator.aa.calc.Portfolio;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class PortfolioChartPanel extends JPanel {

    private static final Color axisColor = Color.BLACK;
    private static final Color portfolioColor = Color.BLUE;
    private static final Color portfolioCompareColor = Color.BLACK;
    private static final Color frontierColor = Color.RED;
    private static final Color CALColor = new Color(0x00, 0xCC, 0x00);
    private static final Color backColor = Color.WHITE;
    private static final Color selectedColor = Color.RED;
    private static final Color zoomColor = new Color(0x00, 0x00, 0x00, 0x80);
    private static final int safeZone = 10;
    private static final int safeTop = 5;

    private static final BasicStroke thick = new BasicStroke(2);
    private static final BasicStroke thin = new BasicStroke(1);

    private JPopupMenu popupMenu = null;
    private Action setComparePortfolio;
    private Action showPortfolioComponents;

    private List<Portfolio> portfolios = new ArrayList<>();
    private List<Portfolio> portfoliosCompare = new ArrayList<>();
    private List<Portfolio> savedPortfolios;
    private List<Portfolio> frontierPortfolios = new ArrayList<>();
    private Portfolio bestCALPortfolio;

    private double minX;
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
    private int dragStart;
    private int dragEnd;
    private boolean horizontalDrag;
    private boolean bothDrag;
    private final Point dragStartPt = new Point();
    private final Point dragEndPt = new Point();

    private boolean frontierOnlyMode = false;
    private double riskFreeRate = -1;

    private Portfolio nearest = null;

    private double zoomMin;
    private double zoomMax;

    private boolean wasZoomed = false;

    private BufferedImage buffer;

    private PortfolioChartHelper helper;

    private class mouseEnterExitListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            if (nearest != null && SwingUtilities.isLeftMouseButton(mouseEvent)) {
                if (mouseEvent.isShiftDown()) {
                    showPortfolioComponents.actionPerformed(new ActionEvent(nearest, ActionEvent.ACTION_PERFORMED, null));
                } else if (mouseEvent.isControlDown()) {
                    setComparePortfolio.actionPerformed(new ActionEvent(nearest, ActionEvent.ACTION_PERFORMED, null));
                } else {
                    helper.showYields(nearest, riskFreeRate);
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            if (!SwingUtilities.isLeftMouseButton(mouseEvent)) {
                if (mouseEvent.isPopupTrigger()) {
                    showPopupMenu(mouseEvent.getX(), mouseEvent.getY());
                }
                return;
            }

            if ((portfolios == null || portfolios.isEmpty()) && (frontierPortfolios == null || frontierPortfolios.isEmpty())) {
                return;
            }

            if (!drawingArea.contains(mouseX, mouseY)) {
                bothDrag = false;
                if (mouseX > drawingArea.x && mouseY > drawingArea.y + drawingArea.height) {
                    dragStart = mouseEvent.getX();
                    dragEnd = mouseEvent.getX();
                    horizontalDrag = true;
                    dragMode = true;
                } else if (mouseX < drawingArea.x && mouseY < drawingArea.y + drawingArea.height) {
                    dragStart = mouseEvent.getY();
                    dragEnd = mouseEvent.getY();
                    horizontalDrag = false;
                    dragMode = true;
                }
            } else {
                dragMode = true;
                bothDrag = true;
                dragStartPt.setLocation(mouseEvent.getPoint());
                dragEndPt.setLocation(dragStartPt);
            }
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
            if (!SwingUtilities.isLeftMouseButton(mouseEvent)) {
                if (mouseEvent.isPopupTrigger()) {
                    showPopupMenu(mouseEvent.getX(), mouseEvent.getY());
                }
                return;
            }

            if (dragMode) {
                dragMode = false;
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();

                if (bothDrag) {
                    dragEndPt.setLocation(mouseEvent.getPoint());
                    setBothZoom();
                } else {
                    if (horizontalDrag) {
                        dragEnd = mouseEvent.getX();
                        int from = Math.min(dragStart, dragEnd);
                        int to = Math.max(dragStart, dragEnd);
                        zoomMin = reMapX(from);
                        zoomMax = reMapX(to);
                        setRiskZoom();
                    } else {
                        dragEnd = mouseEvent.getY();
                        int from = Math.max(dragStart, dragEnd);
                        int to = Math.min(dragStart, dragEnd);
                        zoomMin = reMapY(from);
                        zoomMax = reMapY(to);
                        setYieldZoom();
                    }
                }

            } else {
                moveMouseCross(mouseEvent.getX(), mouseEvent.getY());
            }
        }

        @Override
        public void mouseEntered(MouseEvent mouseEvent) {
            startMouseCross(mouseEvent.getX(), mouseEvent.getY());
        }

        @Override
        public void mouseExited(MouseEvent mouseEvent) {
            dragMode = false;
            stopMouseCross();
        }
    }

    private class mouseMoveListener implements MouseMotionListener {

        @Override
        public void mouseDragged(MouseEvent mouseEvent) {
            if (dragMode) {
                if (bothDrag) {
                    dragEndPt.setLocation(mouseEvent.getPoint());
                } else if (horizontalDrag) {
                    dragEnd = mouseEvent.getX();
                } else {
                    dragEnd = mouseEvent.getY();
                }
                repaint();
            } else {
                moveMouseCross(mouseEvent.getX(), mouseEvent.getY());
            }
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

        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        createPopupMenu();
    }

    void setHelper(PortfolioChartHelper h) {
        helper = h;
    }

    void setPortfolios(List<Portfolio> pfs, List<Portfolio> pfsComp) {
        if (pfs != null && pfs.isEmpty()) {
            return;
        }

        if (pfs != null) {
            portfolios = pfs;
            frontierPortfolios = Calc.getEfficientFrontier(portfolios);
        }

        if (frontierOnlyMode) {
            savedPortfolios = portfolios;
            portfolios = frontierPortfolios;
        }

        if (pfs == null && !frontierOnlyMode) {
            portfolios = savedPortfolios;
        }

        if (portfolios == null || portfolios.isEmpty()) {
            return;
        }

        buffer = null;

        portfoliosCompare = pfsComp;

        if (riskFreeRate >= 0) {
            bestCALPortfolio = Calc.findCAL(frontierPortfolios, riskFreeRate);
        } else {
            bestCALPortfolio = null;
        }

        double minRisk = Math.min(
                portfolios.get(0).risk(),
                portfoliosCompare != null ? portfoliosCompare.get(0).risk() : Double.MAX_VALUE);

        if (!wasZoomed && riskFreeRate >= 0) {
            minRisk = 0;
        }

        double maxRisk = Math.max(
                portfolios.get(portfolios.size() - 1).risk(),
                portfoliosCompare != null ? portfoliosCompare.get(portfoliosCompare.size() - 1).risk() : Double.NEGATIVE_INFINITY);

        double dr;
        if (portfolios != null && portfolios.size() == 1) {
            dr = 0.05;
        } else {
            dr = (maxRisk - minRisk) * 0.05;
        }

        minX = Math.max(minRisk - dr, 0);
        double maxX = maxRisk + dr;

        double minYield = Double.MAX_VALUE;
        double maxYield = Double.NEGATIVE_INFINITY;

        List<Portfolio> tmp;
        if (portfoliosCompare != null) {
            tmp = new ArrayList<>(portfolios.size() + portfoliosCompare.size());
            tmp.addAll(portfolios);
            tmp.addAll(portfoliosCompare);
        } else {
            tmp = portfolios;
        }

        for (Portfolio p : tmp) {
            if (p.yield() < minYield) {
                minYield = p.yield();
            }
            if (p.yield() > maxYield) {
                maxYield = p.yield();
            }

            double dy;
            if (portfolios != null && portfolios.size() == 1) {
                dy = 0.05;
            } else {
                dy = (maxYield - minYield) * 0.05;
            }
            minY = minYield - dy;
            maxY = maxYield + dy;
        }

        if (!wasZoomed && riskFreeRate >= 0) {
            minY = Math.min(minY, 0);
            maxY = Math.max(maxY, riskFreeRate * 1.05);
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

    void setFrontierOnlyMode(boolean mode) {
        if (frontierOnlyMode != mode) {
            frontierOnlyMode = mode;
            setPortfolios(null, portfoliosCompare);
        }
    }

    void setCAL(double rate) {
        buffer = null;
        riskFreeRate = rate;
    }

    List<Portfolio> getFrontierPortfolios() {
        return frontierPortfolios;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();

        if (portfolios.isEmpty()) {
            return;
        }

        if (stringHeight < 0 || stringWidth < 0) {
            calculateStringMetrics(g);
        }

        calculateDrawingArea(w, h);

        if (buffer == null || buffer.getWidth() != w || buffer.getHeight() != h) {

            buffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D gb = buffer.createGraphics();

            drawAxis(gb, w, h);

            if (frontierOnlyMode) {
                if (!frontierPortfolios.isEmpty() && frontierPortfolios.size() > 1) {
                    drawEfficientFrontier(gb);
                }
                frontierPortfolios.forEach(pf -> drawPortfolio(gb, pf));
            } else {
                portfolios.forEach(pf -> drawPortfolio(gb, pf));
                if (!frontierPortfolios.isEmpty() && frontierPortfolios.size() > 1) {
                    drawEfficientFrontier(gb);
                }
            }

            if (portfoliosCompare != null) {
                portfoliosCompare.forEach(pf -> drawLargePortfolio(gb, pf, portfolioCompareColor));
            }

            if (riskFreeRate >= 0 && frontierPortfolios != null && !frontierPortfolios.isEmpty()) {
                drawCAL(gb);
            }
        }

        drawBuffered(g);

        if ((mouseCrossEnabled && mouseX >= 0 && mouseY >= 0) || popupMenu.isVisible()) {
            drawNearest(g);
            drawCross(g, w);
        }

        if (dragMode) {
            drawZoom(g, w, h);
        }

    }

    private void drawBuffered(Graphics g) {
        g.drawImage(buffer, 0, 0, null);
    }

    private void drawZoom(Graphics g, int w, int h) {

        g.setColor(zoomColor);

        if (bothDrag) {
            int fromX = Math.min(dragStartPt.x, dragEndPt.x);
            int width = Math.max(dragStartPt.x, dragEndPt.x) - fromX;
            int fromY = Math.min(dragStartPt.y, dragEndPt.y);
            int height = Math.max(dragStartPt.y, dragEndPt.y) - fromY;
            g.fillRect(fromX, fromY, width, height);
        } else {
            int from = Math.min(dragStart, dragEnd);
            int to = Math.max(dragStart, dragEnd);

            if (horizontalDrag) {
                g.fillRect(from, 0, to - from, h);
            } else {
                g.fillRect(0, from, w, to - from);
            }
        }
    }

    private void drawAxis(Graphics g, int w, int h) {

        g.setColor(backColor);

        g.fillRect(0, 0, w, h);

        g.setColor(axisColor);

        g.drawRect(drawingArea.x, drawingArea.y, drawingArea.width, drawingArea.height);

        g.drawLine(drawingArea.x, drawingArea.y, drawingArea.x - safeTop, drawingArea.y);
        g.drawLine(drawingArea.x, drawingArea.y + drawingArea.height, drawingArea.x - safeTop, drawingArea.y + drawingArea.height);

        g.drawLine(drawingArea.x, drawingArea.y + drawingArea.height, drawingArea.x, drawingArea.y + drawingArea.height + safeTop);
        g.drawLine(drawingArea.x + drawingArea.width, drawingArea.y + drawingArea.height, drawingArea.x + drawingArea.width, drawingArea.y + drawingArea.height + safeTop);

        g.drawString(minRiskStr, drawingArea.x, drawingArea.y + drawingArea.height + stringHeight + safeTop);
        g.drawString(maxRiskStr, drawingArea.x + drawingArea.width - stringWidth, drawingArea.y + drawingArea.height + stringHeight + safeTop);

        g.drawString(minYieldStr, drawingArea.x - stringWidth - safeTop, drawingArea.y + drawingArea.height);
        g.drawString(maxYieldStr, drawingArea.x - stringWidth - safeTop, drawingArea.y + stringHeight);
    }

    private void drawPortfolio(Graphics g, Portfolio pf) {
        g.setColor(pf.hasCoefficient() ? Main.gradient.getColor(pf.getCoefficient()) : portfolioColor);
        g.drawRect(mapX(pf.risk()) - 1, mapY(pf.yield()) - 1, 2, 2);
    }

    private void drawLargePortfolio(Graphics g, Portfolio pf, Color color) {
        int risk = mapX(pf.risk());
        int yield = mapY(pf.yield());

        g.setColor(color);
        g.fillRect(risk - 2, yield - 2, 4, 4);
        g.drawRect(risk - 2, yield - 2, 4, 4);
        g.drawOval(risk - 10, yield - 10, 20, 20);
    }

    private void drawNearest(Graphics g) {
        if (!drawingArea.contains(mouseX, mouseY)) {
            nearest = null;
            return;
        }

        DoublePoint p = new DoublePoint(reMapX(mouseX), reMapY(mouseY));

        double distance = Double.MAX_VALUE;
        Portfolio nearestPortfolio = null;
        Portfolio nearestPortfolioCompare = null;

        List<Portfolio> pfs = frontierOnlyMode ? frontierPortfolios : portfolios;

        for (Portfolio pf : pfs) {
            double dst = Calc.distance(p, pf.performance());
            if (dst < distance) {
                distance = dst;
                nearestPortfolio = pf;
            }
        }

        if (portfoliosCompare != null) {
            for (Portfolio pf : portfoliosCompare) {
                double dst = Calc.distance(p, pf.performance());
                if (dst < distance) {
                    distance = dst;
                    nearestPortfolioCompare = pf;
                }
            }
        }

        if (nearestPortfolioCompare != null) {
            nearestPortfolio = nearestPortfolioCompare;
        }

        nearest = nearestPortfolio;
        if (nearest != null) {
            g.setColor(selectedColor);
            g.fillRect(mapX(nearest.risk()) - 4, mapY(nearest.yield()) - 4, 7, 7);
        }
    }

    private void drawEfficientFrontier(Graphics g) {
        int length = frontierPortfolios.size();
        if (length == 0) {
            return;
        }

        int[] xxs = new int[length];
        int[] yys = new int[length];
        int i = 0;

        for (Portfolio p : frontierPortfolios) {
            xxs[i] = mapX(p.risk());
            yys[i] = mapY(p.yield());
            i += 1;
        }

        ((Graphics2D) g).setStroke(thick);
        if (frontierPortfolios.get(0).hasCoefficient()) {
            int l1 = length - 1;
            for (i = 0; i < l1; i++) {
                g.setColor(Main.gradient.getColor(frontierPortfolios.get(i).getCoefficient()));
                g.drawLine(xxs[i], yys[i], xxs[i + 1], yys[i + 1]);
            }
        } else {
            g.setColor(frontierColor);
            g.drawPolyline(xxs, yys, length);
        }
        ((Graphics2D) g).setStroke(thin);

    }

    private void drawCAL(Graphics g) {
        int fromX = mapX(0);
        int fromY = mapY(riskFreeRate);
        int toX = mapX(bestCALPortfolio.risk());
        int toY = mapY(bestCALPortfolio.yield());

        // too much zoom
        if (fromX > -100000) {
            g.setColor(CALColor);
            g.setClip(drawingArea);
            g.drawLine(fromX, fromY, toX, toY);
            g.setClip(null);

            int freePos = mapY(riskFreeRate);
            if (wasZoomed) {
                if (riskFreeRate <= maxY && riskFreeRate >= minY) {
                    g.drawString(Calc.formatPercent1(riskFreeRate), drawingArea.x - stringWidth - safeTop, freePos);
                    g.drawLine(drawingArea.x, freePos, drawingArea.x - safeTop, freePos);
                }
            } else {
                g.drawString(Calc.formatPercent1(riskFreeRate), drawingArea.x - stringWidth - safeTop, Math.max(fromY, drawingArea.y + stringHeight));
                g.drawLine(drawingArea.x, freePos, drawingArea.x - safeTop, freePos);
            }
        }

        drawLargePortfolio(g, bestCALPortfolio, CALColor);
    }

    private void drawCross(Graphics g, int w) {
        g.setColor(axisColor);

        if (!drawingArea.contains(mouseX, mouseY)) {
            if (mouseX > drawingArea.x && mouseY > drawingArea.y + drawingArea.height) {
                setCursor(Main.weCursor);
                g.drawLine(mouseX, drawingArea.y, mouseX, drawingArea.y + drawingArea.height);
            } else if (mouseX < drawingArea.x && mouseY < drawingArea.y + drawingArea.height) {
                setCursor(Main.nsCursor);
                g.drawLine(drawingArea.x, mouseY, drawingArea.x + drawingArea.width, mouseY);
            } else {
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }

            return;
        }

        boolean popupVisible = popupMenu.isVisible();

        setCursor(popupVisible ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        g.drawLine(drawingArea.x + safeTop, mouseY, drawingArea.x, mouseY);
        g.drawLine(mouseX, drawingArea.y + drawingArea.height - safeTop, mouseX, drawingArea.y + drawingArea.height);

        if (nearest == null) {
            return;
        }

        double xPos = reMapX(mouseX);
        double yPos = reMapY(mouseY);

        String rString = Main.resourceBundle.getString("text.risk_short") + " " + Calc.formatPercent2(nearest.risk());
        String yString = Main.resourceBundle.getString("text.yield_short") + " " + Calc.formatPercent2(nearest.yield());

        FontMetrics fm = g.getFontMetrics();
        Rectangle2D boundsR = fm.getStringBounds(rString, g);
        Rectangle2D boundsY = fm.getStringBounds(yString, g);
        int max = (int)Math.ceil(Math.max(boundsR.getWidth(), boundsY.getWidth()));

        int nearestRiskX = mapX(nearest.risk());
        int nearestYieldY = mapY(nearest.yield());

        int rectX = nearestRiskX + safeZone;
        int rectY = nearestYieldY + safeZone;
        int textX = nearestRiskX + safeZone;
        int textY = nearestYieldY + stringHeight + safeZone;

        if (textX + max > drawingArea.x + drawingArea.width - safeZone || popupVisible) {
            rectX -= max + safeZone * 3;
            textX -= max + safeZone * 3;
        }

        if (textY + stringHeight * 2 > drawingArea.y + drawingArea.height || popupVisible) {
            rectY -= stringHeight * 4 - safeZone;
            textY -= stringHeight * 4 - safeZone;
        }

        g.setColor(frontierColor);
        g.drawLine(nearestRiskX, nearestYieldY, rectX, rectY);

        g.setColor(backColor);
        g.fillRect(rectX, rectY, max + safeZone, stringHeight * 2 + safeTop);

        g.setColor(frontierColor);

        g.drawRect(rectX, rectY, max + safeZone, stringHeight * 2 + safeTop);
        g.drawString(rString, textX + safeTop, textY);
        g.drawString(yString, textX + safeTop, textY + stringHeight);

        String rCrossString = Calc.formatPercent1(xPos);
        String yCrossString = Calc.formatPercent1(yPos);
        boundsR = fm.getStringBounds(rCrossString, g);
        int rWidth = (int)boundsR.getWidth();
        int rHeight = (int)boundsR.getHeight() + safeZone;
        boundsY = fm.getStringBounds(yCrossString, g);
        int yWidth = (int)boundsY.getWidth();
        int yHeight = (int)boundsY.getHeight();

        rectX = mouseX - 1 - safeZone;
        rectY = drawingArea.y + drawingArea.height + 1;
        textX = mouseX - 1;
        textY = drawingArea.y + drawingArea.height + stringHeight + safeTop;

        if (textX + rWidth + 1 > w - safeZone || popupVisible) {
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

        if (bestCALPortfolio == null || riskFreeRate < 0) {
            return;
        }

        double calPercent =  reMapX(mouseX) / bestCALPortfolio.risk();
        int calMouseX = mouseX;
        if (calPercent > 1) {
            if (bestCALPortfolio != nearest) {
                return;
            } else {
                calPercent = 1;
                calMouseX = mapX(bestCALPortfolio.risk());
            }
        }

        String calPercentStr = Calc.formatPercent1(calPercent);
        String calRiskStr = Main.resourceBundle.getString("text.risk_short") + " " + Calc.formatPercent2(bestCALPortfolio.risk() * calPercent);

        int calY = mapY((bestCALPortfolio.yield() - riskFreeRate) * calPercent + riskFreeRate);
        String calYieldStr = Main.resourceBundle.getString("text.yield_short") + " " +
                Calc.formatPercent2((bestCALPortfolio.yield() - riskFreeRate) * calPercent + riskFreeRate);

        Rectangle2D maxCalStrWidth;
        if (calRiskStr.length() > calYieldStr.length()) {
            maxCalStrWidth = fm.getStringBounds(calRiskStr, g);
        } else {
            maxCalStrWidth = fm.getStringBounds(calYieldStr, g);
        }

        int calStrWidth = (int)maxCalStrWidth.getWidth();
        int calStrHeight = (int)maxCalStrWidth.getHeight();
        int calTextX = calMouseX - safeZone - calStrWidth;

        if (calTextX - safeTop < drawingArea.x) {
            calTextX = drawingArea.x + safeTop;
        }

        int calYLine = calY;
        if (calY - calStrHeight * 3 - safeZone < drawingArea.y) {
            calY = drawingArea.y + calStrHeight * 3 + safeZone;
        }

        g.setColor(backColor);
        g.fillRect(calTextX - safeTop, calY - calStrHeight * 3 - safeZone, calStrWidth + safeZone, calStrHeight * 3 + safeTop);
        g.setColor(CALColor);
        g.drawRect(calTextX - safeTop, calY - calStrHeight * 3 - safeZone, calStrWidth + safeZone, calStrHeight * 3 + safeTop);
        g.drawString(calPercentStr, calTextX, calY - calStrHeight * 2 - safeZone);
        g.drawString(calRiskStr, calTextX, calY - calStrHeight - safeZone);
        g.drawString(calYieldStr, calTextX, calY - safeZone);

        if (calMouseX > calTextX + calStrWidth + safeTop) {
            g.drawLine(calMouseX, calYLine, calTextX + calStrWidth + safeTop, calY - safeTop);
        }
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

    void resetZoom() {
        buffer = null;
        wasZoomed = false;
    }

    boolean getZoom() {
        return wasZoomed;
    }

    void zoomAllToPortfolios() {
        wasZoomed = true;
        setPortfolios(portfolios, portfoliosCompare);
    }

    private void setRiskZoom() {
        if (portfolios == null || portfolios.isEmpty()) {
            return;
        }

        List<Portfolio> pfs = portfolios.stream()
                .filter(p -> p.risk() >= zoomMin && p.risk() <= zoomMax)
                .sorted(Portfolio::compareTo)
                .collect(Collectors.toList());

        List<Portfolio> pfsComp = null;

        if (portfoliosCompare != null) {
            pfsComp = portfoliosCompare.stream()
                    .filter(p -> p.risk() >= zoomMin && p.risk() <= zoomMax)
                    .sorted(Portfolio::compareTo)
                    .collect(Collectors.toList());
        }
        if (pfsComp != null && pfsComp.size() == 0) {
            pfsComp = null;
        }

        if (!pfs.isEmpty()) {
            wasZoomed = true;
            setPortfolios(pfs, pfsComp);
        } else {
            repaint();
        }
    }

    private void setBothZoom() {
        if (portfolios == null || portfolios.isEmpty()) {
            return;
        }

        double fromRisk = reMapX(Math.min(dragStartPt.x, dragEndPt.x));
        double toRisk = reMapX(Math.max(dragStartPt.x, dragEndPt.x));
        double fromYield = reMapY(Math.max(dragStartPt.y, dragEndPt.y));
        double toYield = reMapY(Math.min(dragStartPt.y, dragEndPt.y));

        List<Portfolio> pfs = portfolios.stream()
                .filter(p -> p.risk() >= fromRisk && p.risk() <= toRisk)
                .filter(p -> p.yield() >= fromYield && p.yield() <= toYield)
                .sorted(Portfolio::compareTo)
                .collect(Collectors.toList());

        List<Portfolio> pfsComp = null;

        if (portfoliosCompare != null) {
            pfsComp = portfoliosCompare.stream()
                    .filter(p -> p.risk() >= fromRisk && p.risk() <= toRisk)
                    .filter(p -> p.yield() >= fromYield && p.yield() <= toYield)
                    .sorted(Portfolio::compareTo)
                    .collect(Collectors.toList());
        }
        if (pfsComp != null && pfsComp.size() == 0) {
            pfsComp = null;
        }

        if (!pfs.isEmpty()) {
            wasZoomed = true;
            setPortfolios(pfs, pfsComp);
        } else {
            repaint();
        }
    }

    private void setYieldZoom() {
        if (portfolios == null || portfolios.isEmpty()) {
            return;
        }

        List<Portfolio> pfs = portfolios.stream()
                .filter(p -> p.yield() >= zoomMin && p.yield() <= zoomMax)
                .sorted(Portfolio::compareTo)
                .collect(Collectors.toList());

        List<Portfolio> pfsComp = null;

        if (portfoliosCompare != null) {
            pfsComp = portfoliosCompare.stream()
                    .filter(p -> p.yield() >= zoomMin && p.yield() <= zoomMax)
                    .sorted(Portfolio::compareTo)
                    .collect(Collectors.toList());
        }
        if (pfsComp != null && pfsComp.size() == 0) {
            pfsComp = null;
        }

        if (!pfs.isEmpty()) {
            wasZoomed = true;
            setPortfolios(pfs, pfsComp);
        } else {
            repaint();
        }
    }

    private void startMouseCross(int x, int y) {
        mouseCrossEnabled = true;
        moveMouseCross(x, y);
    }

    private void moveMouseCross(int x, int y) {
        if (!popupMenu.isVisible()) {
            mouseX = x;
            mouseY = y;
        }
        repaint();
    }

    private void stopMouseCross() {
        mouseCrossEnabled = false;
        repaint();
    }

    private void createPopupMenu() {
        setComparePortfolio = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Portfolio np;
                Object src = actionEvent.getSource();
                if (src instanceof Portfolio) {
                    np = (Portfolio)src;
                } else {
                    np = nearest;
                }

                if (np != null) {
                    PortfolioChart chart;
                    Component parent = getParent();
                    while (parent != null && !(parent instanceof PortfolioChart)) {
                        parent = parent.getParent();
                    }

                    if (parent == null) {
                        return;
                    }

                    chart = (PortfolioChart) parent;
                    chart.setPortfolioToCompare(
                            Arrays.stream(np.weights()).mapToInt(d -> (int) (d * 100)).toArray()
                    );
                }
            }
        };

        showPortfolioComponents = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Portfolio np;
                Object src = actionEvent.getSource();
                if (src instanceof Portfolio) {
                    np = (Portfolio)src;
                } else {
                    np = nearest;
                }

                if (np != null) {
                    ShowTable.show(
                            Main.resourceBundle.getString("text.portfolio"),
                            riskFreeRate >= 0 ? np.values(riskFreeRate) : np.values(),
                            riskFreeRate >= 0 ? np.labels(riskFreeRate) : np.labels(),
                            new String[]{Main.resourceBundle.getString("text.value")},
                            false
                    );
                }
            }
        };

        PopupMenuListener menuHideListener = new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {}

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }
        };

        JMenuItem menuItem;

        popupMenu = new JPopupMenu();

        menuItem = new JMenuItem(Main.resourceBundle.getString("ui.set_compare_portfolio"));
        menuItem.addActionListener(setComparePortfolio);
        popupMenu.add(menuItem);

        menuItem = new JMenuItem(Main.resourceBundle.getString("ui.portfolio_components_2"));
        menuItem.addActionListener(showPortfolioComponents);
        popupMenu.add(menuItem);

        popupMenu.addPopupMenuListener(menuHideListener);
    }

    private void showPopupMenu(int x, int y) {
        if (nearest != null) {
            setCursor(Cursor.getDefaultCursor());
            popupMenu.show(this, x, y);
        }
    }

    void repaintAll() {
        buffer = null;
        repaint();
    }
}
