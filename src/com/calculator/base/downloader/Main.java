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

    private final Map<String, String> instruments;

    private Main() {
        instruments = new HashMap<>();

        readInstruments();

        processInstruments();
    }

    private void readInstruments() {
        System.out.println("Read ETFs...");
        readInstrument("input.instruments.etf");
    }

    private void readInstrument(String property) {
        new ReaderCSV("\"", ",")
                .read(properties.getProperty(property))
                .skip(1)
                .toList()
                .forEach(line -> instruments.put(line.get(0), line.get(1)));
    }

    private void processInstruments() {
        instruments.forEach(this::processInstrument);
    }

    private void processInstrument(String key, String value) {

        System.out.print("Downloading ");
        System.out.print(key);
        System.out.print(" (");
        System.out.print(value);
        System.out.println(")...");

        String url = properties.getProperty("input.url");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        url = url.replace("{instrument}", key)
                .replace("{month_from}", "0")
                .replace("{day_from}", "1")
                .replace("{year_from}", "1900")
                .replace("{month_to}", String.valueOf(cal.get(Calendar.MONTH)))
                .replace("{day_to}", String.valueOf(cal.get(Calendar.DAY_OF_MONTH)))
                .replace("{year_to}", String.valueOf(cal.get(Calendar.YEAR)))
                .replace("{period}", "m");

        try {
            HttpURLConnection.setFollowRedirects(true);
            HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();

            // todo: report error
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                int length = 16 * 1024;
                byte[] buffer = new byte[length];
                List<Byte> bytes = new ArrayList<>();

                InputStream is = connection.getInputStream();

                while (true) {
                    int wasRead = is.read(buffer);

                    // todo: report error
                    if (wasRead != length) {
                        if (wasRead < 0) {
                            break;
                        }

                        for (int i = 0; i < wasRead; i++){
                            bytes.add(buffer[i]);
                        }

                    } else {
                        for (byte b : bytes){
                            bytes.add(b);
                        }
                    }
                }

                int resLen = bytes.size();
                byte[] res = new byte[resLen];
                for (int i = 0; i < resLen; i++) {
                    res[i] = bytes.get(i);
                }
                String result = new String(res, StandardCharsets.UTF_8);
                System.out.println(result);

                connection.disconnect();
                is.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            properties.load(new FileInputStream("downloader.properties"));
        } catch (IOException e) {

            System.err.println("Can't read properties. Use default!");
            e.printStackTrace();

            properties.setProperty("input.instruments.etf", "nasdaq_etf_full.csv");
            properties.setProperty("input.url", "http://chart.finance.yahoo.com/table.csv?s={instrument}&a={month_from}&b={day_from}&c={year_from}&d={month_to}&e={day_to}&f={year_to}&g={period}&ignore=.csv");
            properties.setProperty("input.delay", "5");
            properties.setProperty("output.file", "{instrument}.txt");
            properties.setProperty("output.quote", "\"");
            properties.setProperty("output.decimal", ",");
            properties.setProperty("output.separator", ";");
        }

        new Main();
    }
}
