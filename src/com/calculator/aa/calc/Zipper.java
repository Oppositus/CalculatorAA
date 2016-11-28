package com.calculator.aa.calc;

import com.sun.deploy.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class Zipper<KeyT, ValueT> {
    private final List<KeyT> keyList;
    private final List<List<ValueT>> valueList;

    public Zipper(List<KeyT> keys, List<ValueT> values, int i) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("Keys and values have different length");
        }

        if (keys.isEmpty()) {
            throw new IllegalArgumentException("Keys and values are empty");
        }

        keyList = new ArrayList<>(keys);
        valueList = new ArrayList<>();
        values.forEach(v -> {
            List<ValueT> l = new ArrayList<>();
            l.add(v);
            valueList.add(l);
        });
    }

    public Zipper(List<KeyT> keys, List<List<ValueT>> values) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("Keys and values have different length");
        }

        if (keys.isEmpty()) {
            throw new IllegalArgumentException("Keys and values are empty");
        }

        keyList = new ArrayList<>(keys);
        valueList = new ArrayList<>(values);
    }

    public List<KeyT> keys() {
        return keyList;
    }

    public List<List<ValueT>> values() {
        return valueList;
    }

    public Zipper<KeyT, ValueT> zip(Zipper<KeyT, ValueT> o) {
        if (o == null) {
            throw new NullPointerException("Zipping with null");
        }
        int size = keyList.size();

        if (size < o.keyList.size()) {
            return o.zip(this);
        }

        List<KeyT> keyZippedList = new ArrayList<>();
        List<List<ValueT>> valueZippedList = new ArrayList<List<ValueT>>();

        int index = -1;
        for (int i = 0; i < size; i++) {
            KeyT key = keyList.get(i);
            if (index < 0 && key.equals(o.keyList.get(0))) {
                index = 0;
            }

            keyZippedList.add(key);

            List<ValueT> value = valueList.get(i);

            if (index >= 0) {
                if (key != o.keyList.get(index)) {
                    throw new IllegalArgumentException("Zipper.zip: different keys");
                }
                value.addAll(o.valueList.get(index));
            }

            valueZippedList.add(value);

            if (index >= 0) {
                index += 1;
            }
        }

        if (index != o.keyList.size()) {
            throw new IllegalArgumentException("Zipper.zip: unused keys");
        }

        return new Zipper<KeyT, ValueT>(keyZippedList, valueZippedList);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        int size = keyList.size();
        for (int i = 0; i < size; i++) {
            sb.append(keyList.get(i).toString());
            sb.append("\t");

            String joined = StringUtils.join(
                    valueList.get(i).stream().map(Object::toString).collect(Collectors.toList()),
                    "\t"
            );

            sb.append(joined);

            if (i != size - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        List<String> k1 = new ArrayList<>(Arrays.asList(new String[] {"k1", "k2", "k3", "k4", "k5", "k6"}));
        List<String> k2 = new ArrayList<>(Arrays.asList(new String[] {"k2", "k3", "k4", "k5", "k6"}));
        List<String> k3 = new ArrayList<>(Arrays.asList(new String[] {"k5", "k6"}));
        List<String> k4 = new ArrayList<>(Arrays.asList(new String[] {"k0", "k00", "k000", "k1", "k2", "k3", "k4", "k5", "k6"}));

        List<Integer> v1 = new ArrayList<>(Arrays.asList(new Integer[] {1, 2, 3, 4, 5, 6}));
        List<Integer> v2 = new ArrayList<>(Arrays.asList(new Integer[] {22, 23, 24, 25, 26}));
        List<Integer> v3 = new ArrayList<>(Arrays.asList(new Integer[] {35, 36}));
        List<Integer> v4 = new ArrayList<>(Arrays.asList(new Integer[] {40, 400, 4000, 41, 42, 43, 44, 45, 46}));

        Zipper<String, Integer> r;
        Zipper<String, Integer> z = new Zipper<>(k1, v1, 0);
        System.out.println(z.toString());
        System.out.println("=================");

        r = z.zip(new Zipper<>(k2, v2, 0));
        System.out.println(r.toString());
        System.out.println("=================");

        r = z.zip(new Zipper<>(k3, v3, 0));
        System.out.println(r.toString());
        System.out.println("=================");

        r = z.zip(new Zipper<>(k4, v4, 0));
        System.out.println(r.toString());
        System.out.println("=================");
    }
}
