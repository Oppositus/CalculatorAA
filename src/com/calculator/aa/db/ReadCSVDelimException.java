package com.calculator.aa.db;

import com.calculator.aa.Main;

public class ReadCSVDelimException extends ReadCSVException {
    public ReadCSVDelimException() {
        super();
    }

    @Override
    public String getMessage() {
        return Main.resourceBundle.getString("exception.csv_delim");
    }
}
