package com.calculator.aa.db;

import com.calculator.aa.Main;

public class ReadCSVDecimalException extends ReadCSVException {
    public ReadCSVDecimalException() {
        super();
    }

    public String getMessage() {
        return Main.resourceBundle.getString("exception.csv_decimal");
    }
}
