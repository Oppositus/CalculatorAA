package com.calculator.aa.db;

class DownloaderFactory {
    static DataDownloader getDownloader(String name) {
        switch (name) {
            case YahooDownloader.Name:
                return new YahooDownloader();
            default:
                return null;
        }
    }
}
