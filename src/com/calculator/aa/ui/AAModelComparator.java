package com.calculator.aa.ui;

import java.util.Calendar;
import java.util.Date;

public class AAModelComparator {

    private final MainWindow.DateFormats format;

    AAModelComparator(MainWindow.DateFormats f) {
        format = f;
    }

    public boolean equals(Object o1, Object o2) {

        if (format == MainWindow.DateFormats.DATE_FORMAT_NONE) {
            return o1.equals(o2);
        }

        if (o1 instanceof Date && o2 instanceof Date) {
            Calendar c1 = Calendar.getInstance();
            c1.setTime((Date)o1);
            Calendar c2 = Calendar.getInstance();
            c2.setTime((Date)o2);

            switch (format) {
                case DATE_FORMAT_YYYY:
                    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);

                case DATE_FORMAT_MM_YYYY:
                case DATE_FORMAT_YYYY_MM:
                    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                            c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH);

                case DATE_FORMAT_DD_MM_YYYY:
                case DATE_FORMAT_MM_DD_YYYY:
                case DATE_FORMAT_YYYY_MM_DD:
                    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                            c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) &&
                            c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);

                default:
                    return false;
            }
        } else {
            return false;
        }
    }

}
