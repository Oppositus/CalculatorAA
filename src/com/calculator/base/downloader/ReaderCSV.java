package com.calculator.base.downloader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ReaderCSV {

    private final String mark;
    private final String delim;
    private final String source;
    private List<List<String>> result;

    ReaderCSV(String m, String d, String s) {
        mark = m;
        delim = d;
        source = s;
        result = new LinkedList<>();
    }

    private ReaderCSV(ReaderCSV o, List<List<String>> res) {
        mark = o.mark;
        delim = o.delim;
        source = o.source;
        result = res;
    }

    ReaderCSV read() {
        read(new BufferedReader(new StringReader(source)));
        return this;
    }

    ReaderCSV read(String fileName) {
        try {
            read(new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8)));
            return this;
        } catch (FileNotFoundException e) {
            System.err.println("Can't read file.");
            e.printStackTrace();
            return this;
        }
    }

    private void read(BufferedReader is) {
        result.addAll(is.lines().map(this::processText).collect(Collectors.toList()));
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

        return result;
    }

    ReaderCSV head() {
        return new ReaderCSV(this, result.subList(0, 1));
    }

    ReaderCSV body() {
        return new ReaderCSV(this, result.subList(1, result.size()));
    }

    List<List<String>> toList() {
        return result;
    }

    Stream<List<String>> lines() {
        return result.stream();
    }
}
