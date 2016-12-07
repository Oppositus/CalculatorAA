package com.calculator.base.downloader;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface DataDownloader {
    void init(Consumer<Boolean> after);
    void download(Instrument instrument, BiConsumer<Boolean, String> after);
    InstrumentHistory parseLine(List<String> line);
}
