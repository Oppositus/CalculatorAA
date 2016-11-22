package com.calculator.aa.calc;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Calc {

    private static Object lock = new Object();

    private static double[] yields(double[] values) {
        int length = values.length;
        double[] yields = new double[length - 1];

        for (int i = 1; i < length; i++) {
            double prev = values[i - 1];
            double curr = values[i];

            if (prev <= 0.0f || curr <= 0.0f) {
                return yields;
            }

            double divided = curr / prev;
            if (divided == Double.POSITIVE_INFINITY) {
                return yields;
            }

            yields[i - 1] = Math.log(divided);
        }

        return yields;
    }

    private static double correlation(double[] y1, double[] y2) {
        double avy1 = Arrays.stream(y1).average().orElse(0.0);
        double avy2 = Arrays.stream(y2).average().orElse(0.0);
        int length = y1.length;

        double sum1 = 0.0;
        double sum21 = 0.0;
        double sum22 = 0.0;

        for (int i = 0; i < length; i++) {
            double yv1d = y1[i] - avy1;
            double yv2d = y2[i] - avy2;

            sum1 += yv1d * yv2d;
            sum21 += yv1d * yv1d;
            sum22 += yv2d * yv2d;
        }

        return sum1 / (Math.sqrt(sum21 * sum22));
    }

    private static double covariance(double[] y1, double[] y2) {
        double avy1 = Arrays.stream(y1).average().orElse(0.0);
        double avy2 = Arrays.stream(y2).average().orElse(0.0);
        int length = y1.length;

        double sum = 0.0;

        for (int i = 0; i < length; i++) {
            double yv1d = y1[i] - avy1;
            double yv2d = y2[i] - avy2;

            sum += yv1d * yv2d;
        }

        return sum / length;
    }

    private static double sumProduct(double[] v1, double[] v2) {

        double sum = 0.0;
        int length = v1.length;
        for (int i = 0; i < length; i++) {
            sum += v1[i] * v2[i];
        }

        return sum;
    }

    public static double[] column(double[][] values, int col) {
        int length = values.length;
        double[] result = new double[length];
        for (int i = 0; i < length; i++) {
            result[i] = values[i][col];
        }
        return result;
    }

    public static double averageYields(double[] values) {
        return Arrays.stream(yields(values)).average().orElse(0.0);
    }

    public static double stdevYields(double[] values) {

        double sum2 = 0.0;
        double[] yields = yields(values);
        double average = averageYields(values);
        int length = yields.length;

        for (double yield : yields) {
            double difference = (yield - average);
            sum2 += difference * difference;
        }

        return Math.sqrt(1.0 / (length - 1) * sum2);
    }

    public static double[][] correlationTable(double[][] values) {

        int cols = values[0].length;
        double[][] corrTable = new double[cols][cols];

        for (int col1 = 0; col1 < cols; col1++) {
            for (int col2 = col1; col2 < cols; col2++) {
                double[] valuesC1 = column(values, col1);
                double[] valuesC2 = column(values, col2);

                double[] y1 = yields(valuesC1);
                double[] y2 = yields(valuesC2);

                corrTable[col1][col2] = correlation(y1, y2);
                corrTable[col2][col1] = corrTable[col1][col2];
            }
        }

        return corrTable;
    }

    public static double[][] covarianceTable(double[][] values) {

        int cols = values[0].length;
        double[][] covTable = new double[cols][cols];

        for (int col1 = 0; col1 < cols; col1++) {
            for (int col2 = col1; col2 < cols; col2++) {
                double[] valuesC1 = column(values, col1);
                double[] valuesC2 = column(values, col2);

                double[] y1 = yields(valuesC1);
                double[] y2 = yields(valuesC2);

                covTable[col1][col2] = covariance(y1, y2);
                covTable[col2][col1] = covTable[col1][col2];
            }
        }

        return covTable;
    }

    private static double portfolioYield(double[] averageYields, double[] weights) {
        return sumProduct(averageYields, weights);
    }

    private static double portfolioRisk(double[][] correlations, double[] stdevYields, double[] weights) {

        int length = weights.length;
        double sum = 0;

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                sum += weights[i] * weights[j] * stdevYields[i] * stdevYields[j] * correlations[i][j];
            }
        }

        return Math.sqrt(sum);
    }

    public static Portfolio portfolio(double[][] correlations, double[] averageYields, double[] stdevYields, double[] weights) {
        return new Portfolio(
                new DoublePoint(
                        portfolioRisk(correlations, stdevYields, weights),
                        portfolioYield(averageYields, weights)),
                weights);
    }

    private static double[] createWeights(double[] stdevYields, boolean minimal) {
        int length = stdevYields.length;
        int minIndex = 0;
        int maxIndex = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (int i = 0; i < length; i++) {
            if (stdevYields[i] < min) {
                min = stdevYields[i];
                minIndex = i;
            }
            if (stdevYields[i] > max) {
                max = stdevYields[i];
                maxIndex = i;
            }
        }

        double[] result = new double[length];
        result[minimal ? minIndex : maxIndex] = 1;

        return result;
    }

    public static ArrayList<Portfolio> iteratePortfolios(double[][] correlations, double[] averageYields,
                                                         double[] stdevYields, int[] minimals, int[] maximals,
                                                         int divStep, boolean effectiveOnly) {
        ArrayList<Portfolio> result = new ArrayList<>();
        int length = averageYields.length;

        for (int i = 0; i < length; i++) {
            int min = minimals[i];
            int max = maximals[i];

            if (min < 0 || min > max || max > 100) {
                return result;
            }
        }

        int[] weights = new int[length];
        System.arraycopy(minimals, 0, weights, 0, length);

        iteratePortfolioHelper(correlations, averageYields, stdevYields, minimals, maximals, 100 / divStep, weights, 0, result);

        result.sort(Portfolio::compareTo);

        if (effectiveOnly) {

            int effectiveCount = 1000;

            double[] border = new double[effectiveCount + 1];
            Portfolio[] effecive = new Portfolio[effectiveCount + 1];
            double minRisk = result.get(0).risk();
            double maxRisk = result.get(result.size() - 1).risk();

            for (Portfolio pf : result) {
                double diff = maxRisk - minRisk;
                double pos = pf.risk() - minRisk;
                int percent = (int)(pos / diff * effectiveCount);
                if (pf.yield() > border[percent]) {
                    border[percent] = pf.yield();
                    effecive[percent] = pf;
                }
            }

            List<Portfolio> effeciveResult = Arrays.stream(effecive).filter(p -> p != null).collect(Collectors.toList());
            result = new ArrayList<Portfolio>(effeciveResult);
        }

        return result;
    }

    private static int sumIntArray(int[] array) {
        int sum = 0;
        for (int i : array) {
            sum += i;
        }
        return sum;
    }

    private static void iteratePortfolioHelper(double[][] correlations, double[] averageYields, double[] stdevYields,
                                               int[] minimals, int[] maximals, int step,
                                               int[] weights, int index, ArrayList<Portfolio> acc) {

        if (sumIntArray(weights) > 100) {
            return;
        }

        while (weights[index] <= maximals[index]) {

            // clear tail
            System.arraycopy(minimals, index + 1, weights, index + 1, weights.length - (index + 1));

            int sum = sumIntArray(weights);

            // todo: check intervals!
            if (sum == 100) {
                acc.add(
                        portfolio(correlations, averageYields, stdevYields,
                                Arrays.stream(weights).mapToDouble(d -> d / 100.0).toArray())
                );
            }

            if (index < weights.length - 1 && sum < 100) {
                iteratePortfolioHelper(correlations, averageYields, stdevYields, minimals, maximals, step, weights, index + 1, acc);
            }

            weights[index] += step;
        }
    }

    public static String formatPercent(double f) {
        return String.format("%.2f%%", f * 100);
    }

}
