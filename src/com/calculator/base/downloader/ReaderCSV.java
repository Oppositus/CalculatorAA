package com.calculator.base.downloader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class ReaderCSV {

    private final String mark;
    private final String delim;

    private List<List<String>> result;

    ReaderCSV(String m, String d) {
        mark = m;
        delim = d;

        result = new LinkedList<>();
    }

    ReaderCSV read(String fileName) {
        BufferedReader is;

        try {
            is = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8));
            result.addAll(is.lines().map(this::processText).collect(Collectors.toList()));
        } catch (FileNotFoundException e) {

            System.err.println("Can't read file.");
            e.printStackTrace();
        }

        return this;
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

    ReaderCSV skip(int count) {
        result = result.subList(count, result.size());
        return this;
    }

    List<List<String>> toList() {
        return result;
    }
}
