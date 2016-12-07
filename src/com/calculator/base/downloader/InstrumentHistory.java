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

    public static void writeMeta(PrintWriter os) {
        os.println("\"Date\";\"Open\";\"High\";\"Low\";\"Close\";\"Volume\";\"Close adj.\"");
    }

    public void write(PrintWriter os) {
        os.print(printDate(date));
        os.print(";");
        os.print(printDouble(open));
        os.print(";");
        os.print(printDouble(high));
        os.print(";");
        os.print(printDouble(low));
        os.print(";");
        os.print(printDouble(close));
        os.print(";");
        os.print(printDouble(volume));
        os.print(";");
        os.println(printDouble(closeAdj));
    }

    private String printDate(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
    }

    private String printDouble(double d) {
        return String.format("%.6f", d);
    }
}
