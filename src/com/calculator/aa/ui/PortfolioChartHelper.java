package com.calculator.aa.ui;

import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.DoublePoint;
import com.calculator.aa.calc.Portfolio;

import java.util.List;
import java.util.stream.Collectors;

class PortfolioChartHelper {

    private PortfolioChartPanel panel;

    private List<Portfolio> portfolios;
    private List<Portfolio> frontierPortfolios;
    private List<Portfolio> portfoliosCompare;
    private double[][] dataFiltered;
    private String[] periodsFiltered;

    private double calValue;
    private double minCoefficient;

    private final DoublePoint zoomFrom;
    private final DoublePoint zoomTo;

    private Portfolio nearest;

    PortfolioChartHelper() {
        nearest = null;
        calValue = -1;
        minCoefficient = 0;
        zoomFrom = new DoublePoint(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        zoomTo = new DoublePoint(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    void setPanel(PortfolioChartPanel p) {
        panel = p;
    }

    void setPortfolios(List<Portfolio> pfs, List<Portfolio> pfsComp, double[][] df, String[] pf, double cal) {
        dataFiltered = df;
        periodsFiltered = pf;
        calValue = cal;
        setPortfolios(pfs, pfsComp);
    }

    private void setPortfolios(List<Portfolio> pfs, List<Portfolio> pfsComp) {
        portfolios = pfs;
        portfoliosCompare = pfsComp;

        if (portfolios != null && portfolios.size() > 0) {
            frontierPortfolios = Calc.getEfficientFrontier(portfolios);
        } else {
            frontierPortfolios = null;
        }

        resetZoom();
        panel.resetZoom();
        panel.setCAL(calValue);
        panel.setPortfolios();
    }

    void showYields(Portfolio p, double riskFreeRate) {
        //if (nearest != null) {
        YieldsChart.showYields(periodsFiltered, dataFiltered, new Portfolio(p), riskFreeRate);
        //}
    }

    double[][] getDataFiltered() {
        return dataFiltered;
    }

    List<Portfolio> getPortfolios() {
        List<Portfolio> tmp = portfolios;

        if (portfolios != null && portfolios.size() > 0 && portfolios.get(0).hasCoefficient()) {
            tmp = filterByMinCoefficient(portfolios);
        }

        return filterByZoom(tmp);
    }

    List<Portfolio> getFrontierPortfolios() {
        List<Portfolio> tmp = frontierPortfolios;

        if (frontierPortfolios != null && frontierPortfolios.size() > 0 && frontierPortfolios.get(0).hasCoefficient()) {
            tmp = filterByMinCoefficient(frontierPortfolios);
        }

        return filterByZoom(tmp);
    }

    List<Portfolio> getFrontierPortfoliosNoFilter() {
        return frontierPortfolios;
    }

    List<Portfolio> getPortfoliosCompare() {
        List<Portfolio> tmp = portfoliosCompare;

        return filterByZoom(tmp);
    }

    void setMinCoefficient(double coef) {
        minCoefficient = coef;
    }

    private List<Portfolio> filterByMinCoefficient(List<Portfolio> list) {
        return list.stream()
                .filter(p -> p.getCoefficient() >= minCoefficient)
                .collect(Collectors.toList());
    }

    private List<Portfolio> filterByZoom(List<Portfolio> list) {
        if (list == null) {
            return null;
        }

        return list.stream()
                .filter(p -> {
                    double risk = p.risk();
                    double yield = p.yield();
                    return risk >= zoomFrom.getX() && risk <= zoomTo.getX() && yield >= zoomFrom.getY() && yield <= zoomTo.getY();
                })
                .collect(Collectors.toList());
    }

    void resetZoom() {
        zoomFrom.setX(Double.NEGATIVE_INFINITY);
        zoomFrom.setY(Double.NEGATIVE_INFINITY);
        zoomTo.setX(Double.POSITIVE_INFINITY);
        zoomTo.setY(Double.POSITIVE_INFINITY);
    }

    void setZoom(double fromRisk, double toRisk, double fromYield, double toYield) {
        zoomFrom.setX(fromRisk);
        zoomFrom.setY(fromYield);
        zoomTo.setX(toRisk);
        zoomTo.setY(toYield);
    }
}
