package com.calculator.base.downloader;

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

class Instrument {

    public enum Type {
        ETF,
        FUND,
        INDEX
    }

    final static Date BEGINNING;
    static {
        Calendar _cal = Calendar.getInstance();
        _cal.set(1900, 0, 1);
        BEGINNING = _cal.getTime();
    }

    private final String ticker;
    private final String fullName;
    private final Type type;
    private final Date fromDate;
    private final Date toDate;
    private final List<InstrumentHistory> history;

    private String providerName;
    private String providerUrl;

    Instrument(String t, String n, Type y, Date f, Date o) {
        ticker = t;
        fullName = n;
        type = y;
        fromDate = new Date(f.getTime());
        toDate = new Date(o.getTime());
        history = new LinkedList<>();

        providerName = "";
        providerUrl = "";
    }

    String getTicker() {
        return ticker;
    }

    void download(DataDownloader provider, Consumer<Boolean> after) {
        System.out.print("Downloading ");
        System.out.print(ticker);
        System.out.print(" (");
        System.out.print(fullName);
        System.out.print(")... ");

        providerName = provider.getName();
        providerUrl = provider.getWebUrl();

        provider.init(initDone -> {
            if (initDone) {
                provider.download(this, (downloadDone, s) -> {
                    if (downloadDone) {
                        ReaderCSV reader = new ReaderCSV("\"", ",", s);
                        reader.read()
                                .body()
                                .lines()
                                .forEach(line -> history.add(provider.parseLine(line)));

                        history.sort(InstrumentHistory::compareTo);
                        System.out.println("Downloaded " + reader.toList().size() + " lines");

                        fromDate.setTime(history.get(0).getDate().getTime());
                        toDate.setTime(history.get(history.size() - 1).getDate().getTime());
                    } else {
                        System.out.println("Download error");
                    }
                    after.accept(downloadDone);
                });
            } else {
                System.out.println("Init error");
                after.accept(false);
            }
        });
    }

    static void writeHead(PrintWriter os) {
        os.print("\"Ticker\"");
        os.print(";");
        os.print("\"Full name\"");
        os.print(";");
        os.print("\"Type\"");
        os.print(";");
        os.print("\"From date\"");
        os.print(";");
        os.print("\"To date\"");
        os.print(";");
        os.print("\"Provider name\"");
        os.print(";");
        os.print("\"Provider website\"");

        os.println();
    }

    void writeMeta(PrintWriter os) {
        os.print("\"");
        os.print(ticker);
        os.print("\";\"");
        os.print(fullName);
        os.print("\";\"");
        os.print(type.toString());
        os.print("\";\"");
        os.print(printDate(fromDate));
        os.print("\";\"");
        os.print(printDate(toDate));
        os.print("\";\"");
        os.print(providerName);
        os.print("\";\"");
        os.print(providerUrl);
        os.print("\"");

        os.println();
    }

    void write(PrintWriter os) {
        history.forEach(instr -> instr.write(os));
    }

    private String printDate(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
    }
}
