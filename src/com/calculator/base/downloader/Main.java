package com.calculator.base.downloader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class Main {
    private static final Properties properties = new Properties();

    private final List<Instrument> instruments;
    private final SQLiteSupport sqlLite;

    private Main() throws IOException {
        instruments = new LinkedList<>();
        sqlLite = new SQLiteSupport();
        sqlLite.createTables();

        readInstruments();
        processInstruments();

        sqlLite.dispose();
    }

    private void readInstruments() {
        System.out.print("Read ETFs... ");
        readInstrument("input.instruments.etf", Instrument.Type.ETF);

        /*System.out.print("Read Funds... ");
        readInstrument("input.instruments.funds", Instrument.Type.FUND);*/
    }

    private void readInstrument(String property, Instrument.Type type) {
        new ReaderCSV("\"", ",", null)
                .read(properties.getProperty(property))
                .body()
                .lines()
                .forEach(line -> {
                    if (instruments.stream().noneMatch(i -> i.getTicker().equals(line.get(0)))) {
                        instruments.add(
                                new Instrument(
                                        line.get(0),
                                        line.get(1),
                                        type,
                                        Instrument.BEGINNING,
                                        Calendar.getInstance().getTime()
                                )
                        );
                    }
                });

        System.out.println("Got " + instruments.size() + ".");
    }

    private void processInstruments() {
        System.out.println("Start download process...");
        instruments.forEach(this::processInstrument);
        System.out.println("Download complete...");
    }

    private void processInstrument(Instrument instr) {
        instr.download(new YahooDownloader(), allOk -> {
            if (allOk) {
                sqlLite.saveInstrument(instr);
            } else {
                System.out.println("Skip instrument: " + instr.getTicker());
            }
        });
    }

    public static void main(String[] args) throws IOException {
        try {
            properties.load(new FileInputStream("downloader.properties"));
        } catch (IOException e) {

            System.err.println("Can't read properties. Use default!");
            e.printStackTrace();
            System.exit(0);
        }

        new Main();
    }
}
