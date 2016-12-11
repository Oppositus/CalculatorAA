package com.calculator.aa.calc;

import com.calculator.aa.ui.AAModelComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Zipper {
    private final List<Object> keyList;
    private final List<List<Double>> valueList;
    private final List<String> labelList;

    public Zipper(List<Object> keys, List<Double> values, String label) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("Keys and values have different length");
        }

        if (keys.isEmpty()) {
            throw new IllegalArgumentException("Keys and values are empty");
        }

        keyList = new ArrayList<>(keys);
        valueList = new ArrayList<>();
        values.forEach(v -> {
            List<Double> l = new ArrayList<>();
            l.add(v);
            valueList.add(l);
        });
        labelList = new ArrayList<>();
        labelList.add(label);
    }

    public Zipper(List<Object> keys, List<List<Double>> values, List<String> labels) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("Keys and values have different length");
        }

        if (keys.isEmpty()) {
            throw new IllegalArgumentException("Keys and values are empty");
        }

        keyList = new ArrayList<>(keys);
        valueList = new ArrayList<>(values);
        labelList = new ArrayList<>(labels);
    }

    private Zipper(Zipper that) {
        this(that.keyList, that.valueList, that.labelList);
    }

    public List<Object> keys() {
        return keyList;
    }

    public List<List<Double>> values() {
        return valueList;
    }

    public List<String> labels() {
        return labelList;
    }

    public Zipper zip(Zipper other, AAModelComparator comparator, double dflt) throws NullPointerException, IllegalArgumentException {
        if (other == null) {
            throw new NullPointerException("Zipping with null");
        }
        int size = keyList.size();

        if (size < other.keyList.size()) {
            return other.zip(this, comparator, dflt);
        }

        List<Object> keyZippedList = new ArrayList<>();
        List<List<Double>> valueZippedList = new ArrayList<>();
        List<String> labelsZippedList = new ArrayList<>(labelList);
        labelsZippedList.addAll(other.labelList);

        int labelSize = labelsZippedList.size();

        int index = -1;
        for (int i = 0; i < size; i++) {
            Object key = keyList.get(i);
            if (index < 0 && comparator.equals(key, other.keyList.get(0))) {
                index = 0;
            }

            keyZippedList.add(key);

            List<Double> value = new ArrayList<>(valueList.get(i));

            if (index >= 0) {
                if (!comparator.equals(key, other.keyList.get(index))) {
                    throw new IllegalArgumentException("Zipper.zip: different keys: " + key.toString() + " - " + other.keyList.get(index).toString());
                }
                value.addAll(other.valueList.get(index));
            }
            while (value.size() < labelSize) {
                value.add(dflt);
            }

            valueZippedList.add(value);

            if (index >= 0) {
                index += 1;
            }
        }

        if (index != other.keyList.size()) {
            throw new IllegalArgumentException("Zipper.zip: unused keys");
        }

        return new Zipper(keyZippedList, valueZippedList, labelsZippedList);
    }

    public Zipper zipAll(List<Zipper> others, AAModelComparator comparator, double dflt) {
        Zipper result = new Zipper(this);
        for (Zipper z : others) {
            result = result.zip(z, comparator, dflt);
        }
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(" ");

        sb.append(String.join(
                " ",
                labelList.stream().map(Object::toString).collect(Collectors.toList())
        ));
        sb.append("\n");

        int size = keyList.size();
        for (int i = 0; i < size; i++) {
            sb.append(keyList.get(i));
            sb.append(" ");

            sb.append(String.join(
                    " ",
                    valueList.get(i).stream().map(Object::toString).collect(Collectors.toList())
            ));

            if (i != size - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
