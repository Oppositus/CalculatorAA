package com.calculator.base.downloader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

public class Main {
    private static final Properties properties = new Properties();

    private final List<Instrument> instruments;
    private PrintWriter mainWriter;

    private Main() throws IOException {
        instruments = new LinkedList<>();

        createMainFile();

        readInstruments();

        processInstruments();

        Instrument.writeHead(mainWriter);
        instruments.forEach(instr -> instr.writeMeta(mainWriter));

        closeMainFile();
    }

    private void createMainFile() throws IOException {
        mainWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream("data/db_instruments.csv"), StandardCharsets.UTF_8), true);
    }

    private void closeMainFile() {
        mainWriter.close();
    }

    private void readInstruments() {
        System.out.print("Read ETFs... ");
        readInstrument("input.instruments.etf");
    }

    private void readInstrument(String property) {
        new ReaderCSV("\"", ",", null)
                .read(properties.getProperty(property))
                .body()
                .lines()
                .forEach(line ->
                        instruments.add(
                                new Instrument(
                                        line.get(0),
                                        line.get(1),
                                        Instrument.Type.ETF,
                                        Instrument.BEGINNING,
                                        Calendar.getInstance().getTime()
                                )));

        System.out.println("Got " + instruments.size() + ".");
    }

    private void processInstruments() {
        System.out.print("Start download process...");
        instruments.forEach(this::processInstrument);
        System.out.print("Download complete...");
    }

    private void processInstrument(Instrument instr) {
        instr.download(new YahooDownloader(), allOk -> {
            if (allOk) {
                try {
                    PrintWriter iWriter = new PrintWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(String.format("data/%s_etf.csv", instr.getTicker().toLowerCase())),
                                    StandardCharsets.UTF_8),
                            true);

                    InstrumentHistory.writeMeta(iWriter);
                    instr.write(iWriter);
                    iWriter.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) throws IOException {
        try {
            properties.load(new FileInputStream("downloader.properties"));
        } catch (IOException e) {

            System.err.println("Can't read properties. Use default!");
            e.printStackTrace();

            properties.setProperty("input.instruments.etf", "sources/nasdaq_etf_full.csv");
        }

        new Main();
    }
}
