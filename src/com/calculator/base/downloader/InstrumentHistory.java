package com.calculator.base.downloader;

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;

public class InstrumentHistory implements Comparable<InstrumentHistory> {

    private final Date date;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double closeAdj;
    private final double volume;

    public InstrumentHistory(Date d, double o, double h, double l, double c, double ca, double v) {
        date = d;
        open = o;
        high = h;
        low = l;
        close = c;
        closeAdj = ca;
        volume = v;
    }

    @Override
    public int compareTo(InstrumentHistory o) {
        return date.compareTo(o.date);
    }

    public Date getDate() {
        return date;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public double getCloseAdj() {
        return closeAdj;
    }

    public double getVolume() {
        return volume;
    }

    String valuesToInsert() {
        StringBuilder sb = new StringBuilder();

        sb.append(printDate(date));
        sb.append(", ");
        sb.append(printDouble(open));
        sb.append(", ");
        sb.append(printDouble(high));
        sb.append(", ");
        sb.append(printDouble(low));
        sb.append(", ");
        sb.append(printDouble(close));
        sb.append(", ");
        sb.append(printDouble(closeAdj));
        sb.append(", ");
        sb.append(printDouble(volume));

        return sb.toString();
    }

    private String printDate(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return String.format("'%04d-%02d-%02d'", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    private String printDouble(double d) {
        return String.valueOf(d);
    }
}
