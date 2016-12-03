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
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class PortfolioChartPanel extends JPanel {

    private static final Color axisColor = Color.BLACK;
    private static final Color portfolioColor = Color.BLUE;
    private static final Color portfolioCompareColor = Color.BLACK;
    private static final Color backColor = Color.WHITE;
    private static final Color selectedColor = Color.RED;
    private static final Color zoomColor = Color.GRAY;
    private static final int safeZone = 10;
    private static final int safeTop = 5;
    private BufferedImage rebalancesGradient = null;

    private JPopupMenu popupMenu = null;
    private Action setComparePortfolio;
    private Action showPortfolioComponents;

    private List<Portfolio> portfolios = new ArrayList<>();
    private List<Portfolio> portfoliosCompare = new ArrayList<>();
    private List<Portfolio> savedPortfolios;
    private List<Portfolio> frontierPortfolios = new ArrayList<>();

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
    private int dragStartX = 0;
    private int dragEndX = 0;

    private boolean frontierOnlyMode = false;
    private boolean showRebalancesMode;
    private double minYieldRebalance;
    private double maxYieldRebalance;
    private double dYieldRebalance;

    private Portfolio nearest = null;

    private double[][] dataFiltered;
    private String[] periodsFiltered;

    private double zoomMin;
    private double zoomMax;

    private class mouseEnterExitListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            if (nearest != null && SwingUtilities.isLeftMouseButton(mouseEvent)) {
                if (mouseEvent.isShiftDown()) {
                    showPortfolioComponents.actionPerformed(new ActionEvent(nearest, ActionEvent.ACTION_PERFORMED, null));
                } else if (mouseEvent.isControlDown()) {
                    setComparePortfolio.actionPerformed(new ActionEvent(nearest, ActionEvent.ACTION_PERFORMED, null));
                } else {
                    YieldsChart.showYields(periodsFiltered, dataFiltered, nearest);
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

            dragMode = true;
            dragStartX = mouseEvent.getX();
            dragEndX = mouseEvent.getX();
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
            if (!SwingUtilities.isLeftMouseButton(mouseEvent)) {
                if (mouseEvent.isPopupTrigger()) {
                    showPopupMenu(mouseEvent.getX(), mouseEvent.getY());
                }
                return;
            }

            dragEndX = mouseEvent.getX();
            if (dragMode) {
                dragMode = false;
                int from = Math.min(dragStartX, dragEndX);
                int to = Math.max(dragStartX, dragEndX);

                zoomMin = reMapX(from);
                zoomMax = reMapX(to);

                setZoom();
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
            stopMouseCross();
        }
    }

    private class mouseMoveListener implements MouseMotionListener {

        @Override
        public void mouseDragged(MouseEvent mouseEvent) {
            if (dragMode) {
                dragEndX = mouseEvent.getX();
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

        setCursor(Main.voidCursor);

        createPopupMenu();
    }

    void setPortfolios(List<Portfolio> pfs, List<Portfolio> pfsComp, double[][] df, String[] pf) {
        if (pfs != null && pfs.isEmpty()) {
            return;
        }

        if (pfs != null && df != null && pf != null) {
            portfolios = pfs;
            dataFiltered = df;
            periodsFiltered = pf;
        }

        if (pfs != null) {
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

        portfoliosCompare = pfsComp;
        showRebalancesMode = false;
        minYieldRebalance = Double.MAX_VALUE;
        maxYieldRebalance = Double.MIN_VALUE;
        dYieldRebalance = 1;

        double minRisk = Math.min(
                portfolios.get(0).risk(),
                portfoliosCompare != null ? portfoliosCompare.get(0).risk() : Double.MAX_VALUE);

        double maxRisk = Math.max(
                portfolios.get(portfolios.size() - 1).risk(),
                portfoliosCompare != null ? portfoliosCompare.get(portfoliosCompare.size() - 1).risk() : Double.MIN_VALUE);

        double dr;
        if (portfolios != null && portfolios.size() == 1) {
            dr = 0.05;
        } else {
            dr = (maxRisk - minRisk) * 0.05;
        }

        minX = minRisk - dr;
        double maxX = maxRisk + dr;

        double minYield = Double.MAX_VALUE;
        double maxYield = Double.MIN_VALUE;

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
            boolean wasRebalanceMode = showRebalancesMode;
            setPortfolios(null, portfoliosCompare, null, null);
            setRebalancesMode(wasRebalanceMode);
        }
    }

    void setRebalancesMode(boolean mode) {
        if (showRebalancesMode != mode) {
            showRebalancesMode = mode;

            if (showRebalancesMode) {
                List<Portfolio> totalList = new LinkedList<>();

                if (portfolios != null) {
                    totalList.addAll(portfolios);
                }

                if (frontierPortfolios != null) {
                    totalList.addAll(frontierPortfolios);
                }

                if (portfoliosCompare != null) {
                    totalList.addAll(portfoliosCompare);
                }

                for (Portfolio p : totalList) {
                    p.calculateRebalances(dataFiltered);
                    double y = p.yieldRebalances();

                    if (y > maxYieldRebalance) {
                        maxYieldRebalance = y;
                    }
                    if (y < minYieldRebalance) {
                        minYieldRebalance = y;
                    }
                }

                dYieldRebalance = maxYieldRebalance - minYieldRebalance;
                if (dYieldRebalance < Calc.epsilon) {
                    dYieldRebalance = 1;
                }
            }

            repaint();
        }
    }

    List<Portfolio> getFrontierPortfolios() {
        return frontierPortfolios;
    }

    double[][] getDataFiltered() {
        return dataFiltered;
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
            drawZoom(g, h);
        }

        g.setColor(axisColor);
        drawAxis(g);

        if (showRebalancesMode) {
            drawRebalancedYields(g);
        }

        if (!frontierPortfolios.isEmpty() && frontierPortfolios.size() > 1) {
            g.setColor(portfolioColor);
            drawEfficientFrontier(g);
        }

        if (frontierOnlyMode) {
            frontierPortfolios.forEach(pf -> drawPortfolio(g, pf));
        } else {
            portfolios.forEach(pf -> drawPortfolio(g, pf));
        }

        g.setColor(portfolioCompareColor);
        if (portfoliosCompare != null) {
            portfoliosCompare.forEach(pf -> drawLargePortfolio(g, pf));
        }

        if ((mouseCrossEnabled && mouseX >= 0 && mouseY >= 0) || popupMenu.isVisible()) {
            drawNearest(g);
            g.setColor(axisColor);
            drawCross(g, w, h);
        }
    }

    private void drawZoom(Graphics g, int h) {
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

    private void drawRebalancedYields(Graphics g) {

        if (rebalancesGradient == null) {
            createRebalancesGradient();
        }

        String minRebY = Calc.formatDouble2(minYieldRebalance);
        String maxRebY = Calc.formatDouble2(maxYieldRebalance);

        FontMetrics fm = g.getFontMetrics();
        Rectangle2D boundsMin = fm.getStringBounds(minRebY, g);
        Rectangle2D boundsMax = fm.getStringBounds(maxRebY, g);

        int addWidth = (int)Math.max(boundsMin.getWidth(), boundsMax.getWidth());
        int rebWidth = addWidth * 2 + safeZone * 2 + 200;

        int center = drawingArea.x + drawingArea.width / 2 - rebWidth / 2;

        g.drawString(minRebY, center, drawingArea.y + drawingArea.height + stringHeight + safeTop);
        g.drawImage(rebalancesGradient, center + addWidth + safeZone, drawingArea.y + drawingArea.height + safeTop, null);
        g.drawString(maxRebY, center + addWidth + 200 + safeZone * 2, drawingArea.y + drawingArea.height + stringHeight + safeTop);
    }

    private void createRebalancesGradient() {
        int h = stringHeight;
        rebalancesGradient = new BufferedImage(200, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = rebalancesGradient.createGraphics();
        for (int i = 0; i < 200; i++) {
            Color c = getGradientColor(((i / 2) * 2) / 200.0);
            g.setColor(c);
            g.drawLine(i, 0, i, h);
        }
        g.dispose();
    }

    private Color getGradientColor(double percent){
        int red = percent < 0.5 ? 255 : (int)Math.round(255 - (percent - 0.5) * 5.1 * 100);
        int green = percent > 0.5 ? 255 : (int)Math.round(percent * 5.1 * 100);

        if (red < 0) {
            red = 0;
        }

        return new Color(red, green, 0);
    }

    private void drawPortfolio(Graphics g, Portfolio pf) {

        if (showRebalancesMode) {
            double reb = pf.yieldRebalances();
            double percent = (reb - minYieldRebalance) / dYieldRebalance;
            Color cl = getGradientColor(percent);
            g.setColor(cl);
        }

        g.fillRect(mapX(pf.risk()) - 1, mapY(pf.yield()) - 1, 2, 2);
        g.drawRect(mapX(pf.risk()) - 1, mapY(pf.yield()) - 1, 2, 2);
    }

    private void drawLargePortfolio(Graphics g, Portfolio pf) {
        g.fillRect(mapX(pf.risk()) - 2, mapY(pf.yield()) - 2, 4, 4);
        g.drawRect(mapX(pf.risk()) - 2, mapY(pf.yield()) - 2, 4, 4);
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

        if (showRebalancesMode) {
            length -= 1;
            for (int i = 0; i < length; i++) {
                Portfolio p1 = frontierPortfolios.get(i);
                Portfolio p2 = frontierPortfolios.get(i + 1);
                double reb1 = p1.yieldRebalances();
                double percent1 = (reb1 - minYieldRebalance) / dYieldRebalance;
                double reb2 = p2.yieldRebalances();
                double percent2 = (reb2 - minYieldRebalance) / dYieldRebalance;
                double percent = (percent1 + percent2) / 2;
                Color cl = getGradientColor(percent);
                g.setColor(cl);

                int x1 = mapX(p1.risk());
                int y1 = mapY(p1.yield());
                int x2 = mapX(p2.risk());
                int y2 = mapY(p2.yield());

                g.drawLine(x1, y1, x2, y2);
            }
        } else {
            int[] xxs = new int[length];
            int[] yys = new int[length];
            int i = 0;

            for (Portfolio p : frontierPortfolios) {
                xxs[i] = mapX(p.risk());
                yys[i] = mapY(p.yield());
                i += 1;
            }

            g.drawPolyline(xxs, yys, length);
        }
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

        String rString = Main.resourceBundle.getString("text.risk_short") + " " + Calc.formatPercent2(nearest.risk());
        String yString = Main.resourceBundle.getString("text.yield_short") + " " + Calc.formatPercent2(nearest.yield());

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

        boolean popupVisible = popupMenu.isVisible();

        if (textX + max > drawingArea.x + drawingArea.width - safeZone || popupVisible) {
            rectX -= max + safeZone * 2 + plus;
            textX -= max + safeZone * 2 + plus;
        }

        if (textY + stringHeight * 2 > drawingArea.y + drawingArea.height || popupVisible) {
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

        boolean wasRebalanceMode = showRebalancesMode;
        setPortfolios(pfs, pfsComp, dataFiltered, periodsFiltered);
        setRebalancesMode(wasRebalanceMode);
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
                            np.values(),
                            np.labels(),
                            new String[]{Main.resourceBundle.getString("text.value")});
                }
            }
        };

        PopupMenuListener menuHideListener = new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {}

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                setCursor(Main.voidCursor);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                setCursor(Main.voidCursor);
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
}
