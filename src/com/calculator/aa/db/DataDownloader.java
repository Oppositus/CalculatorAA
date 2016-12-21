package com.calculator.aa.db;

import com.calculator.base.downloader.InstrumentHistory;

import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;

interface DataDownloader {
    void download(Instrument instrument, BiConsumer<Boolean, String> after);
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
