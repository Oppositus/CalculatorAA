package com.calculator.aa.db;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MoexDownloader implements DataDownloader {

    private static final String DownloadURL = "http://www.micex.ru/iss/history/engines/stock/markets/shares/securities/{instrument}.csv?from={year_from}-{month_from}-{day_from}&start={cursor}";
    static final String Name = "Moscow Exchange";
    private static final String WebURL = "http://moex.com/";

    private static final Pattern headerPattern = Pattern.compile("^(BOARDID.*)$", Pattern.MULTILINE);
    private static final Pattern cursorPattern = Pattern.compile("^(INDEX.*)$", Pattern.MULTILINE);
    private static final Pattern cursorDataPattern = Pattern.compile("^(\\d+);(\\d+);(\\d+)$", Pattern.MULTILINE);
    private static final Pattern eolPattern = Pattern.compile("^\n$", Pattern.MULTILINE);

    private static int id = -1;

    private static Comparator<List<String>> dateComparator = (o1, o2) -> {
        Date d1 = parseDate(o1.get(0));
        Date d2 = parseDate(o2.get(0));

        return d1.compareTo(d2);
    };

    MoexDownloader() {
        if (id < 0) {
            id = Main.sqLite.getDownloaderId(this);
        }
    }

    @Override
    public void download(Instrument instrument, boolean reload, BiConsumer<Boolean, String> after) {

        Date lastUpdated = Main.sqLite.getLastUpdateDate(instrument, reload);

        if (SQLiteSupport.dateNow().equals(lastUpdated)) {
            after.accept(true, "");
            return;
        }

        Calendar calFrom = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calFrom.setTime(lastUpdated);

        String url = DownloadURL.replace("{instrument}", instrument.getTicker())
                .replace("{year_from}", String.valueOf(calFrom.get(Calendar.YEAR)))
                .replace("{month_from}", String.valueOf(calFrom.get(Calendar.MONTH) + 1))
                .replace("{day_from}", "1");

        StringBuilder totalResult = new StringBuilder();
        String result;
        int cursor = 0;

        try {

            while (cursor >= 0) {
                result = downloadPage(url, String.valueOf(cursor));

                if (result == null) {
                    after.accept(false, "");
                    return;
                }

                if (totalResult.length() == 0) {
                    totalResult.append(findHeader(result));
                    totalResult.append("\n");
                }
                String body = findData(result);
                if (body.isEmpty()) {
                    break;
                }
                totalResult.append(findData(result));

                cursor = getNextCursor(result);
            }

            after.accept(true, convertToMonthly(totalResult.toString()));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
            after.accept(false, e.getMessage());
        }

    }

    private String downloadPage(String partialUrl, String cursor) throws IOException {

        String url = partialUrl.replace("{cursor}", cursor);

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

            return result;

        } else {
            return null;
        }

    }

    private String findHeader(String source) {
        Matcher m = headerPattern.matcher(source);
        if (m.find()) {
            return m.group(1);
        }

        throw new IllegalArgumentException("Incorrect source data - does not match");
    }

    private String findData(String source) {
        Matcher h = headerPattern.matcher(source);
        if (h.find()) {
            int from = h.end();
            int to = source.indexOf("history.cursor");
            String dataString = source.substring(from + 1, to - 1);
            Matcher eol = eolPattern.matcher(dataString);
            return eol.replaceAll("");
        }

        throw new IllegalArgumentException("Incorrect source data - does not match");
    }

    private int getNextCursor(String source) {
        Matcher c = cursorPattern.matcher(source);
        if (c.find()) {
            int from = c.end();
            Matcher d = cursorDataPattern.matcher(source);
            if (d.find(from)) {
                int index = Calc.safeParseInt(d.group(1), -1);
                int total = Calc.safeParseInt(d.group(2), -1);
                int page = Calc.safeParseInt(d.group(3), -1);

                if (index < 0 || total < 0 || page < 0) {
                    throw new IllegalArgumentException("Incorrect source data - does not match cursor");
                }

                return index + page > total ? -1 : index + page;
            }
        }

        throw new IllegalArgumentException("Incorrect source data - does not match");
    }

    private String convertToMonthly(String source) {
        StringBuilder result = new StringBuilder();
        int eolIndex = source.indexOf("\n");
        result.append("DATE;OPEN;HIGH;LOW;CLOSE;CLOSEADJ;VOLUME");
        result.append("\n");

        SortedMap<Date, List<String>> months = new TreeMap<>();

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        BufferedReader reader = new BufferedReader(new StringReader(source.substring(eolIndex + 1)));
        reader.lines().forEach(l -> {

            if (l.isEmpty()) {
                return;
            }

            List<String> lineSplitted = Arrays.asList(l.split(";"));
            cal.setTime(parseDate(lineSplitted.get(1)));
            cal.set(Calendar.DATE, 1);
            Date dt = cal.getTime();

            if (months.containsKey(dt)) {
                List<String> thisMonth = months.get(dt);
                thisMonth.add(l);
            } else {
                List<String> thisMonth = new LinkedList<String>();
                thisMonth.add(l);
                months.put(dt, thisMonth);
            }
        });

        for(Date dt : months.keySet()) {
            List<String> data = months.get(dt);
            double open = -1;
            double high = -1;
            double low = Double.MAX_VALUE;
            double close = -1;
            double tmp;
            for (String str: data) {
                String[] splitted = correctLine(str).split(";");

                close = parseDouble(splitted[9]);
                if (close < 0) {
                    continue;
                }

                if (open < 0) {
                    if (splitted[6].isEmpty()) {
                        open = close;
                    } else {
                        open = parseDouble(splitted[6]);
                    }
                }

                if (splitted[7].isEmpty()) {
                    tmp = close;
                } else {
                    tmp = parseDouble(splitted[7]);
                }
                if (tmp < low) {
                    low = tmp;
                }

                if (splitted[8].isEmpty()) {
                    tmp = close;
                } else {
                    tmp = parseDouble(splitted[8]);
                }
                if (tmp > high) {
                    high = tmp;
                }
            }

            if (close > 0) {
                result.append(SQLiteSupport.printUnquotedDate(dt)).append(";")
                        .append(String.valueOf(open)).append(";")
                        .append(String.valueOf(high)).append(";")
                        .append(String.valueOf(low)).append(";")
                        .append(String.valueOf(close)).append(";")
                        .append(String.valueOf(close)).append(";0\n");
            }
        }

        return result.toString();
    }

    private String correctLine(String line) {
        StringBuilder sb = new StringBuilder();
        int length = line.length();
        for (int i = 0; i < length; i++) {
            char ch = line.charAt(i);
            sb.append(ch);

            if (i != length - 1) {
                if (ch == ';' && line.charAt(i + 1) == ';') {
                    sb.append("-1");
                }
            }
        }

        return sb.toString();
    }

    @Override
    public ReaderCSV createReader() {
        return new ReaderCSV("\"", ";", ".");
    }

    @Override
    public Comparator<List<String>> getDateComparator() {
        return dateComparator;
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
        return parseDouble(line.get(4));
    }

    @Override
    public double getVolume(List<String> line) {
        return parseDouble(line.get(5));
    }

    private static Date parseDate(String str) {
        int[] splitted = Arrays.stream(str.split("-")).mapToInt(s -> Calc.safeParseInt(s, 0)).toArray();

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(splitted[0], splitted[1] - 1, splitted[2], 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    private double parseDouble(String str) {
        return Double.valueOf(str);
    }
}
