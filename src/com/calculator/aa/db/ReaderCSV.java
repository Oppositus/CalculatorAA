package com.calculator.aa.db;

import com.calculator.aa.Main;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class ReaderCSV {

    public static final String dbMark = "\"";
    public static final String dbDelim = ";";
    public static final String dbDecimal = ".";
    public static final String dbDateSeparator = "-";

    private final String mark;
    private final String delim;
    private final String decimal;
    private final List<List<String>> result;
    private int headCount;

    public ReaderCSV(String m, String d, String e) {
        mark = m == null || m.isEmpty() ? dbMark : m;
        delim = d == null || d.isEmpty() ? dbDelim : d;
        decimal = e == null || e.isEmpty() ? dbDecimal : e;
        headCount = -1;
        result = new LinkedList<>();
    }

    private ReaderCSV(ReaderCSV o, List<List<String>> res) {
        mark = o.mark;
        delim = o.delim;
        decimal = o.decimal;
        headCount = o.headCount;
        result = res;
    }

    ReaderCSV readFromString(String source) {
        read(new BufferedReader(new StringReader(source)));
        return this;
    }

    public void readFromFile(File file) {
        try {
            read(new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)));
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public ReaderCSV head() {
        return new ReaderCSV(this, result.subList(0, 1));
    }

    public ReaderCSV body() {
        return new ReaderCSV(this, result.subList(1, result.size()));
    }

    public List<List<String>> toList() {
        return result;
    }

    public Stream<List<String>> lines() {
        return result.stream();
    }

    private double parse(String text, double def) {
        if (text == null || text.isEmpty()) {
            return def;
        }
        return Double.valueOf(text.replace(decimal, "."));
    }

    public DoubleStream parse(List<String> texts, double def) {
        return texts.stream().mapToDouble(s -> this.parse(s, def));
    }

    private void read(BufferedReader is) {
        result.addAll(is.lines().filter(l -> !l.isEmpty()).map(this::processText).collect(Collectors.toList()));
    }

    private List<String> processText(String text) {

        List<String> result = new LinkedList<>();
        boolean inText = false;
        int length = text.length();
        String mark2 = mark + mark;
        StringBuilder sb = new StringBuilder();
        int i = 0;

        while (i < length) {
            String current = text.substring(i, i + 1);

            if (current.equals(mark)) {
                if (i < length - 1 && text.substring(i, i + 2).equals(mark2)) {
                    sb.append(mark);
                    i += 2;
                } else {
                    inText = !inText;
                    i += 1;
                }
                continue;
            }

            if (current.equals(delim)) {
                if (!inText) {
                    result.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(current);
                }
            } else {
                sb.append(current);
            }

            i += 1;
        }

        if (sb.length() > 0) {
            result.add(sb.toString());
        }

        if (headCount < 0) {
            headCount = result.size();
        } else {
            while (result.size() < headCount) {
                result.add("");
            }
        }

        return result;
    }
}
