package com.calculator.base.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class YahooDownloader implements DataDownloader {

    private static final String DownloadURL = "http://chart.finance.yahoo.com/table.csv?s={instrument}&a={month_from}&b={day_from}&c={year_from}&d={month_to}&e={day_to}&f={year_to}&g={period}&ignore=.csv";
    private static final String Name = "Yahoo Finance";
    private static final String WebURL = "https://finance.yahoo.com/";

    @Override
    public void init(Consumer<Boolean> after) {
        after.accept(true);
    }

    @Override
    public void download(Instrument instrument, BiConsumer<Boolean, String> after) {

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        String url = DownloadURL.replace("{instrument}", instrument.getTicker())
                .replace("{month_from}", "0")
                .replace("{day_from}", "1")
                .replace("{year_from}", "1900")
                .replace("{month_to}", String.valueOf(cal.get(Calendar.MONTH)))
                .replace("{day_to}", String.valueOf(cal.get(Calendar.DAY_OF_MONTH)))
                .replace("{year_to}", String.valueOf(cal.get(Calendar.YEAR)))
                .replace("{period}", "m");


        try {
            HttpURLConnection.setFollowRedirects(true);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                int bufferLength = 16 * 1024;
                byte[] responseBuffer = new byte[bufferLength];
                List<Byte> bytesFromHTTP = new LinkedList<>();

                InputStream isHTTP = connection.getInputStream();

                while (true) {
                    int wasRead = isHTTP.read(responseBuffer);
                    if (wasRead < 0) {
                        break;
                    }
                    for (int i = 0; i < wasRead; i++) {
                        bytesFromHTTP.add(responseBuffer[i]);
                    }
                }

                int resLen = bytesFromHTTP.size();
                byte[] res = new byte[resLen];
                int index = 0;
                for (byte b : bytesFromHTTP) {
                    res[index++] = b;
                }
                String result = new String(res, StandardCharsets.UTF_8);

                isHTTP.close();
                connection.disconnect();

                after.accept(true, result);

            } else {
                after.accept(false, null);
            }

        } catch (IOException e) {
            e.printStackTrace();
            after.accept(false, null);
        }
    }

    @Override
    public InstrumentHistory parseLine(List<String> line) {
        return new InstrumentHistory(
                parseDate(line.get(0)),
                parseDouble(line.get(1)),
                parseDouble(line.get(2)),
                parseDouble(line.get(3)),
                parseDouble(line.get(4)),
                parseDouble(line.get(6)),
                parseDouble(line.get(5))
        );
    }

    @Override
    public String getName() {
        return Name;
    }

    @Override
    public String getWebUrl() {
        return WebURL;
    }

    private Date parseDate(String str) {
        int[] splitted = Arrays.stream(str.split("-")).mapToInt(Integer::parseInt).toArray();

        Calendar cal = Calendar.getInstance();
        cal.set(splitted[0], splitted[1] - 1, splitted[2]);
        return cal.getTime();
    }

    private double parseDouble(String str) {
        return Double.valueOf(str);
    }
}
