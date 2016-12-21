package com.calculator.aa.db;

public class DownloaderFactory {
    public static DataDownloader getDownloader(String name) {
        switch (name) {
            case "YahooDownloader":
                return new YahooDownloader();
            default:
                return null;
        }
    }
}
