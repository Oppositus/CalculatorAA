package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.db.InstrumentsMeta;

import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterDB extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton buttonFilter;
    private JButton buttonClearFilter;
    private JList<String> listInstrumentTypes;
    private JList<String> listInstrumentProviders;
    private JSpinner spinnerMinHistory;
    private JList<String> listResults;

    private InstrumentsMeta meta;
    private String[] result;

    private FilterDB() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        spinnerMinHistory.addMouseWheelListener(new SpinnerWheelListener(spinnerMinHistory));

        result = null;
        meta = new InstrumentsMeta("db/meta/db_instruments.csv");

        prepareLists();
        buttonClearFilter.addActionListener(actionEvent -> {
            listInstrumentTypes.setSelectedIndex(0);
            listInstrumentProviders.setSelectedIndex(0);
            spinnerMinHistory.setValue(0);
            listResults.setModel(new DefaultListModel<>());
        });
        buttonFilter.addActionListener(actionEvent -> {
            int[] selectedTypes = listInstrumentTypes.getSelectedIndices();
            int[] selectedProviders = listInstrumentProviders.getSelectedIndices();

            Stream<List<String>> filteredTypes = null;
            if (!containsAllFilter(selectedTypes)) {
                for (int i: selectedTypes) {
                    if (filteredTypes == null) {
                        filteredTypes = meta.filterByType(listInstrumentTypes.getModel().getElementAt(i));
                    } else {
                        filteredTypes = Stream.concat(filteredTypes, meta.filterByType(listInstrumentTypes.getModel().getElementAt(i)));
                    }
                }
            } else {
                filteredTypes = meta.unfiltered();
            }
            List<List<String>> filteredTypesList = filteredTypes.collect(Collectors.toList());

            Stream<List<String>> filteredProviders = null;
            if (!containsAllFilter(selectedProviders)) {
                for (int i: selectedProviders) {
                    if (filteredProviders == null) {
                        filteredProviders = meta.filterByProvider(listInstrumentProviders.getModel().getElementAt(i));
                    } else {
                        filteredProviders = Stream.concat(filteredProviders, meta.filterByProvider(listInstrumentProviders.getModel().getElementAt(i)));
                    }
                }
            } else {
                filteredProviders = meta.unfiltered();
            }
            List<List<String>> filteredProvidersList = filteredProviders.collect(Collectors.toList());

            Stream<List<String>> filteredDates = null;
            int minMonths = (int)spinnerMinHistory.getModel().getValue();
            if (minMonths > 0) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, -minMonths);
                filteredDates = meta.filterByDate(cal.getTime());
            } else {
                filteredDates = meta.unfiltered();
            }
            List<List<String>> filteredDatesList = filteredDates.collect(Collectors.toList());

            Stream<List<String>> allFiltered = filteredTypesList.stream()
                    .filter(filteredProvidersList::contains)
                    .filter(filteredDatesList::contains);

            Map<String, String> result = meta.getNames(allFiltered);
            DefaultListModel<String> resultModel = new DefaultListModel<>();
            result.forEach((k, v) -> resultModel.addElement(k + "\t" + v));
            listResults.setModel(resultModel);
        });
    }

    private void prepareLists() {
        DefaultListModel<String> types = new DefaultListModel<>();
        types.addElement(Main.resourceBundle.getString("text.all"));
        meta.getTypes().forEach(types::addElement);
        listInstrumentTypes.setModel(types);
        listInstrumentTypes.setSelectedIndex(0);

        DefaultListModel<String> providers = new DefaultListModel<>();
        providers.addElement(Main.resourceBundle.getString("text.all"));
        meta.getProviders().keySet().forEach(providers::addElement);
        listInstrumentProviders.setModel(providers);
        listInstrumentProviders.setSelectedIndex(0);
    }

    private void onOK() {
        List<String> res = listResults.getSelectedValuesList();
        int length = res.size();
        if (length > 0) {
            result = new String[length];
            int index = 0;
            for (String s : res) {
                result[index++] = s;
            }
        } else {
            result = null;
        }

        dispose();
    }

    private void onCancel() {
        result = null;
        dispose();
    }

    private void createUIComponents() {
        listInstrumentTypes = new JList<>();
        listInstrumentProviders = new JList<>();
        listResults = new JList<>();

        spinnerMinHistory = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
    }

    private boolean containsAllFilter(int[] array) {
        for (int i: array) {
            if (i == 0) {
                return true;
            }
        }
        return false;
    }

    static String[] showFilter() {

        FilterDB dialog = new FilterDB();
        dialog.setTitle(Main.resourceBundle.getString("text.filter_db"));
        dialog.setLocationRelativeTo(Main.getFrame());

        dialog.pack();

        dialog.setVisible(true);

        return dialog.result;
    }
}
