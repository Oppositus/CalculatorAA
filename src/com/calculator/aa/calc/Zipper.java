package com.calculator.aa.calc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Zipper<K, V, L> {
    private final List<K> keyList;
    private final List<List<V>> valueList;
    private final List<L> labelList;

    public Zipper(List<K> keys, List<V> values, L label) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("Keys and values have different length");
        }

        if (keys.isEmpty()) {
            throw new IllegalArgumentException("Keys and values are empty");
        }

        keyList = new ArrayList<>(keys);
        valueList = new ArrayList<>();
        values.forEach(v -> {
            List<V> l = new ArrayList<>();
            l.add(v);
            valueList.add(l);
        });
        labelList = new ArrayList<>();
        labelList.add(label);
    }

    public Zipper(List<K> keys, List<List<V>> values, List<L> labels) {
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

    private Zipper(Zipper<K, V, L> that) {
        this(that.keyList, that.valueList, that.labelList);
    }

    public List<K> keys() {
        return keyList;
    }

    public List<List<V>> values() {
        return valueList;
    }

    public List<L> labels() {
        return labelList;
    }

    public Zipper<K, V, L> zip(Zipper<K, V, L> other, V dflt) {
        if (other == null) {
            throw new NullPointerException("Zipping with null");
        }
        int size = keyList.size();

        if (size < other.keyList.size()) {
            return other.zip(this, dflt);
        }

        List<K> keyZippedList = new ArrayList<>();
        List<List<V>> valueZippedList = new ArrayList<>();
        List<L> labelsZippedList = new ArrayList<>(labelList);
        labelsZippedList.addAll(other.labelList);

        int labelSize = labelsZippedList.size();

        int index = -1;
        for (int i = 0; i < size; i++) {
            K key = keyList.get(i);
            if (index < 0 && key.equals(other.keyList.get(0))) {
                index = 0;
            }

            keyZippedList.add(key);

            List<V> value = new ArrayList<>(valueList.get(i));

            if (index >= 0) {
                if (!key.equals(other.keyList.get(index))) {
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

        return new Zipper<>(keyZippedList, valueZippedList, labelsZippedList);
    }

    public Zipper<K, V, L> zipAll(List<Zipper<K, V, L>> others, V dflt) {
        Zipper<K, V, L> result = new Zipper<>(this);
        for (Zipper<K, V, L> z : others) {
            result = result.zip(z, dflt);
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
            sb.append(keyList.get(i).toString());
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
