package com.calculator.aa.db;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;

interface DataDownloader {
    void download(Instrument instrument, boolean reload, BiConsumer<Boolean, String> after);
    ReaderCSV createReader();
    Comparator<List<String>> getDateComparator();
    int getId();
    String getName();
    String getWebUrl();
    Date getDate(List<String> line);
    double getOpen(List<String> line);
    double getHigh(List<String> line);
    double getLow(List<String> line);
    double getClose(List<String> line);
    double getCloseAdj(List<String> line);
    double getVolume(List<String> line);
}
