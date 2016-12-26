package com.calculator.aa.db;

class DownloaderFactory {
    static DataDownloader getDownloader(String name) {
        switch (name) {
            case YahooDownloader.Name:
                return new YahooDownloader();
            case MoexDownloader.Name:
                return new MoexDownloader();
            default:
                return null;
        }
    }
}
