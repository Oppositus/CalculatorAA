package com.calculator.aa.ui;

import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.DoublePoint;
import com.calculator.aa.calc.Portfolio;

import java.util.List;

class PortfolioChartHelper {

    private PortfolioChartPanel panel;

    private List<Portfolio> portfolios;
    private List<Portfolio> portfoliosCompare;
    private double[][] dataFiltered;
    private String[] periodsFiltered;

    private double calValue;

    private Portfolio nearest;

    PortfolioChartHelper() {
        nearest = null;
        calValue = -1;
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

        panel.resetZoom();
        panel.setCAL(calValue);
        panel.setPortfolios(portfolios, portfoliosCompare);
    }

    void showYields() {
        if (nearest != null) {
            YieldsChart.showYields(periodsFiltered, dataFiltered, new Portfolio(nearest));
        }
    }

    double[][] getDataFiltered() {
        return dataFiltered;
    }
}
