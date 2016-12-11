package com.calculator.base.downloader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

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
        System.out.println("Read ETFs... ");
        readInstrument("input.instruments.etf", Instrument.Type.ETF);

        System.out.println("Read Funds... ");
        readInstrument("input.instruments.funds", Instrument.Type.FUND);
    }

    private void readInstrument(String property, Instrument.Type type) {

        String[] toRead = properties.getProperty(property).split(";");

        Arrays.asList(toRead)
                .forEach(tr -> {
                    String[] trProps = tr.split("!");

                    System.out.println("Read " + trProps[0] + " with " + trProps[1]);

                    new ReaderCSV("\"", ",", null)
                            .read(trProps[0])
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
                                                    Calendar.getInstance().getTime(),
                                                    trProps[1]
                                            )
                                    );
                                }
                            });

                });

        System.out.println("Got " + instruments.size() + ".");
    }

    private void processInstruments() {
        System.out.println("Start download process...");
        instruments.forEach(this::processInstrument);
        System.out.println("Download complete...");
    }

    private void processInstrument(Instrument instr) {
        instr.download(allOk -> {
            if (allOk) {
                sqlLite.saveInstrument(instr);
            } else {
                System.out.println("Skip instrument: " + instr.getTicker());
            }
        });
    }

    static DataDownloader getDownloader(String name) {
        switch (name) {
            case "YahooDownloader":
                return new YahooDownloader();

            default:
                return null;
        }
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
