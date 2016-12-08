package com.calculator.aa.db;

import com.calculator.aa.Main;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InstrumentsMeta {

    private static final String fieldTicker = "Ticker";
    private static final String fieldFullName = "Full name";
    private static final String fieldType = "Type";
    private static final String fieldDateFrom = "From date";
    private static final String fieldDateTo = "To date";
    private static final String fieldProviderName = "Provider name";
    private static final String fieldProviderWeb = "Provider website";

    public enum InstrumentType {
        ETF,
        FUND,
        INDEX
    }

    public enum ValueType {
        OPEN,
        HIGH,
        LOW,
        CLOSE,
        CLOSE_ADJ
    }

    public enum PeriodType {
        MONTH,
        YEAR
    }

    public static final Map<ValueType, String> ValueName = new HashMap<>();
    static {
        ValueName.put(ValueType.OPEN, "Open");
        ValueName.put(ValueType.HIGH, "High");
        ValueName.put(ValueType.LOW, "Low");
        ValueName.put(ValueType.CLOSE, "Close");
        ValueName.put(ValueType.CLOSE_ADJ, "Close adj.");
    }

    private final List<String> header;
    private final List<List<String>> instruments;

    private final List<String> types;
    private final Map<String, String> providers;

    public InstrumentsMeta(String fileName) {
        header = new ArrayList<>();
        instruments = new ArrayList<>();

        types = new ArrayList<>();
        providers = new HashMap<>();

        try {
            ReaderCSV reader = new ReaderCSV(ReaderCSV.dbMark, ReaderCSV.dbDelim).readFromFile(fileName);
            header.addAll(reader.head().toList().get(0));
            reader.body().lines().forEach(instruments::add);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public List<String> getTypes() {

        if (types.isEmpty()) {
            int indexOfType = header.indexOf(fieldType);
            types.addAll(instruments.stream().map(i -> i.get(indexOfType)).distinct().collect(Collectors.toList()));
        }

        return types;
    }

    public Map<String, String> getProviders() {

        if (providers.isEmpty()) {
            int indexOfProvider = header.indexOf(fieldProviderName);
            int indexOfWeb = header.indexOf(fieldProviderWeb);
            instruments.forEach(i -> {
                String key = i.get(indexOfProvider);
                if (!providers.containsKey(key)) {
                    providers.put(key, i.get(indexOfWeb));
                }
            });
        }

        return providers;
    }

    public Stream<List<String>> filterByDate(Date minDate) {
        int indexOfFrom = header.indexOf(fieldDateFrom);
        Calendar cal = Calendar.getInstance();

        return instruments.stream().filter(i -> {
            String[] dt = i.get(indexOfFrom).split(ReaderCSV.dbDateSeparator);
            cal.set(Integer.parseInt(dt[0]), Integer.parseInt(dt[1]) - 1, 1);
            return minDate.compareTo(cal.getTime()) >= 0;
        });
    }

    public Stream<List<String>> filterByText(String text) {
        int indexOfTicker = header.indexOf(fieldTicker);
        int indexOfName = header.indexOf(fieldFullName);
        boolean allowTextSearch = text.length() >= 3;
        Locale loc = Locale.getDefault();
        String textUp = text.toUpperCase(loc);

        return instruments.stream().filter(i ->
                i.get(indexOfTicker).toUpperCase(loc).contains(textUp) ||
                        (allowTextSearch && i.get(indexOfName).toUpperCase(loc).contains(textUp))
        );
    }

    public Stream<List<String>> filterByType(String type) {
        return filterByStringField(type, header.indexOf(fieldType));
    }

    public Stream<List<String>> filterByProvider(String name) {
        return filterByStringField(name, header.indexOf(fieldProviderName));
    }

    private Stream<List<String>> filterByStringField(String text, int field) {
        return instruments.stream().filter(i -> i.get(field).equals(text));
    }

    public Stream<List<String>> unfiltered() {
        return instruments.stream();
    }

    public Map<String, String> getNames(Stream<List<String>> filtered) {
        int indexOfTicker = header.indexOf(fieldTicker);
        int indexOfName = header.indexOf(fieldFullName);

        return filtered.collect(Collectors.toMap(i -> i.get(indexOfTicker), i -> i.get(indexOfName)));
    }

    public InstrumentType getTypeFromTicker(String ticker) {
        int indexOfTicker = header.indexOf(fieldTicker);
        int indexOfType = header.indexOf(fieldType);
        Optional<String> descr = instruments.stream()
                .filter(i -> i.get(indexOfTicker).equals(ticker))
                .map(i -> i.get(indexOfType))
                .findFirst();

        if (descr.isPresent()) {
            String type = descr.get().toUpperCase();
            switch (type) {
                case "ETF":
                    return InstrumentType.ETF;
                case "FUND":
                    return InstrumentType.FUND;
                case "INDEX":
                    return InstrumentType.INDEX;

                default:
                    return null;
            }
        } else {
            return null;
        }
    }
}
