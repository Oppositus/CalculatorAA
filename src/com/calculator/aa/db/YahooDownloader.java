package com.calculator.aa.db;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;

public class YahooDownloader implements DataDownloader {

    private static final String DownloadURL = "http://chart.finance.yahoo.com/table.csv?s={instrument}&a={month_from}&b={day_from}&c={year_from}&d={month_to}&e={day_to}&f={year_to}&g={period}&ignore=.csv";
    private static final String Name = "Yahoo Finance";
    private static final String WebURL = "https://finance.yahoo.com/";

    private final int id;

    YahooDownloader() {
        id = Main.sqLite.getDownloaderId(this);
    }

    @Override
    public void download(Instrument instrument, BiConsumer<Boolean, String> after) {

        Date lastUpdated = Main.sqLite.getLastUpdateDate(instrument);

        Calendar calFrom = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calFrom.setTime(lastUpdated);
        Calendar calTo = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        String url = DownloadURL.replace("{instrument}", instrument.getTicker())
                .replace("{month_from}", String.valueOf(calFrom.get(Calendar.MONTH)))
                .replace("{day_from}", "1")
                .replace("{year_from}", String.valueOf(calFrom.get(Calendar.YEAR)))
                .replace("{month_to}", String.valueOf(calTo.get(Calendar.MONTH)))
                .replace("{day_to}", String.valueOf(calTo.get(Calendar.DAY_OF_MONTH)))
                .replace("{year_to}", String.valueOf(calTo.get(Calendar.YEAR)))
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
                after.accept(false, "HTTP status: " + connection.getResponseCode());
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
            after.accept(false, e.getMessage());
        }
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return Name;
    }

    @Override
    public String getWebUrl() {
        return WebURL;
    }

    @Override
    public Date getDate(List<String> line) {
        return parseDate(line.get(0));
    }

    @Override
    public double getOpen(List<String> line) {
        return parseDouble(line.get(1));
    }

    @Override
    public double getHigh(List<String> line) {
        return parseDouble(line.get(2));
    }

    @Override
    public double getLow(List<String> line) {
        return parseDouble(line.get(3));
    }

    @Override
    public double getClose(List<String> line) {
        return parseDouble(line.get(4));
    }

    @Override
    public double getCloseAdj(List<String> line) {
        return parseDouble(line.get(6));
    }

    @Override
    public double getVolume(List<String> line) {
        return parseDouble(line.get(5));
    }

    private Date parseDate(String str) {
        int[] splitted = Arrays.stream(str.split("-")).mapToInt(s -> Calc.safeParseInt(s, 0)).toArray();

        Calendar cal = Calendar.getInstance();
        cal.set(splitted[0], splitted[1] - 1, splitted[2]);
        return cal.getTime();
    }

    private double parseDouble(String str) {
        return Double.valueOf(str);
    }
}
