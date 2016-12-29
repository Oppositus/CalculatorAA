package com.calculator.aa.ui;

import com.calculator.aa.calc.Calc;
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

    private Portfolio nearest;

    PortfolioChartHelper() {
        nearest = null;
        calValue = -1;
        minCoefficient = 0;
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

        panel.resetZoom();
        panel.setCAL(calValue);
        panel.setPortfolios(portfolios, portfoliosCompare);
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
        if (portfolios != null && portfolios.size() > 0 && portfolios.get(0).hasCoefficient()) {
            return portfolios.stream()
                    .filter(p -> p.getCoefficient() > minCoefficient)
                    .collect(Collectors.toList());
        }

        return portfolios;
    }

    List<Portfolio> getFrontierPortfolios() {
        if (frontierPortfolios != null && frontierPortfolios.size() > 0 && frontierPortfolios.get(0).hasCoefficient()) {
            return frontierPortfolios.stream()
                    .filter(p -> p.getCoefficient() > minCoefficient)
                    .collect(Collectors.toList());
        }

        return frontierPortfolios;
    }

    List<Portfolio> getPortfoliosCompare() {
        return portfoliosCompare;
    }

    void setMinCoefficient(double coef) {
        minCoefficient = coef;
    }
}
