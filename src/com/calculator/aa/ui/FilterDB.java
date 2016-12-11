package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.db.Instrument;
import com.calculator.aa.db.SQLiteSupport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
    private JList<Instrument> listResults;
    private JTextField nameTextField;
    private JList<Instrument> listSelected;
    private JButton buttonRemoveSelected;
    private JButton buttonAddSelected;

    private AATableModel result;
    private boolean isAborted;
    private List<Instrument> filteredTickets;
    private List<Instrument> selectedTickets;

    private FilterDB(String[] instr) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonFilter);

        result = null;
        selectedTickets = new ArrayList<>();

        Stream.of(instr)
                .map(Main.sqLite::findInstrument)
                .filter(Objects::nonNull)
                .forEach(selectedTickets::add);

        prepareLists();

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        spinnerMinHistory.addMouseWheelListener(new SpinnerWheelListener(spinnerMinHistory));

        buttonClearFilter.addActionListener(actionEvent -> {
            listInstrumentTypes.setSelectedIndex(0);
            listInstrumentProviders.setSelectedIndex(0);
            spinnerMinHistory.setValue(0);
            nameTextField.setText("");
            listResults.setModel(new DefaultListModel<>());
        });

        buttonFilter.addActionListener(actionEvent -> {
            int selectedType = listInstrumentTypes.getSelectedIndex();
            int selectedProvider = listInstrumentProviders.getSelectedIndex();
            int minMonth = (int)spinnerMinHistory.getValue();
            String textFilter = nameTextField.getText();

            DefaultListModel<Instrument> resultModel = new DefaultListModel<>();
            filteredTickets = Main.sqLite.filterInstruments(
                    selectedType == 0 ? null : SQLiteSupport.getInstrumentType(listInstrumentTypes.getSelectedValue()),
                    selectedProvider == 0 ? null : listInstrumentProviders.getSelectedValue(),
                    minMonth,
                    textFilter
            );
            filteredTickets.forEach(resultModel::addElement);
            listResults.setModel(resultModel);
        });

        buttonAddSelected.addActionListener(actionEvent -> {
            int[] selectedIndexes = listResults.getSelectedIndices();
            if (selectedIndexes == null || selectedIndexes.length == 0) {
                return;
            }

            List<Instrument> results = Arrays.stream(selectedIndexes)
                    .boxed()
                    .map(filteredTickets::get)
                    .collect(Collectors.toList());

            List<Instrument> newSelected = Stream.concat(
                    results.stream().filter(r -> !selectedTickets.contains(r)),
                    selectedTickets.stream()
            ).collect(Collectors.toList());

            DefaultListModel<Instrument> newModel = new DefaultListModel<>();
            selectedTickets = new ArrayList<>(newSelected.size());
            newSelected.forEach(i -> {
                selectedTickets.add(i);
                newModel.addElement(i);
            });
            listSelected.setModel(newModel);
        });

        buttonRemoveSelected.addActionListener(actionEvent -> {
            int[] selectedIndexes = listSelected.getSelectedIndices();
            if (selectedIndexes == null || selectedIndexes.length == 0) {
                return;
            }

            List<Instrument> selected = Arrays.stream(selectedIndexes)
                    .boxed()
                    .map(selectedTickets::get)
                    .collect(Collectors.toList());

            List<Instrument> restTickets = selectedTickets.stream()
                    .filter(t -> !selected.contains(t))
                    .collect(Collectors.toList());

            DefaultListModel<Instrument> newModel = new DefaultListModel<>();
            selectedTickets = new ArrayList<>(restTickets.size());
            restTickets.forEach(i -> {
                selectedTickets.add(i);
                newModel.addElement(i);
            });
            listSelected.setModel(newModel);

        });
    }

    private void prepareLists() {
        DefaultListModel<String> types = new DefaultListModel<>();
        types.addElement(Main.resourceBundle.getString("text.all"));
        Main.sqLite.getTypes().forEach(types::addElement);
        listInstrumentTypes.setModel(types);
        listInstrumentTypes.setSelectedIndex(0);

        DefaultListModel<String> providers = new DefaultListModel<>();
        providers.addElement(Main.resourceBundle.getString("text.all"));
        Main.sqLite.getProviders().forEach(providers::addElement);
        listInstrumentProviders.setModel(providers);
        listInstrumentProviders.setSelectedIndex(0);

        DefaultListModel<Instrument> selected = new DefaultListModel<>();
        selectedTickets.forEach(selected::addElement);
        listSelected.setModel(selected);
    }

    private void onOK() {
        if (selectedTickets.size() > 0) {
            result = ConvertOptions.showOptions(selectedTickets);
        }
        isAborted = false;
        if (result != null) {

            Rectangle bounds = getBounds();
            Main.properties.setProperty("filter.x", String.valueOf((int)bounds.getX()));
            Main.properties.setProperty("filter.y", String.valueOf((int)bounds.getY()));
            Main.properties.setProperty("filter.w", String.valueOf((int)bounds.getWidth()));
            Main.properties.setProperty("filter.h", String.valueOf((int)bounds.getHeight()));

            dispose();
        }
    }

    private void onCancel() {
        result = null;
        isAborted = true;
        Rectangle bounds = getBounds();
        Main.properties.setProperty("filter.x", String.valueOf((int)bounds.getX()));
        Main.properties.setProperty("filter.y", String.valueOf((int)bounds.getY()));
        Main.properties.setProperty("filter.w", String.valueOf((int)bounds.getWidth()));
        Main.properties.setProperty("filter.h", String.valueOf((int)bounds.getHeight()));
        dispose();
    }

    private void createUIComponents() {
        listInstrumentTypes = new JList<>();
        listInstrumentProviders = new JList<>();
        listResults = new JList<>();
        listSelected = new JList<>();

        spinnerMinHistory = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
    }

    static AATableModel showFilter(String[] instr) {
        FilterDB dialog = new FilterDB(instr);
        dialog.setTitle(Main.resourceBundle.getString("text.filter_db"));
        dialog.setLocationRelativeTo(Main.getFrame());

        dialog.pack();

        int x = Calc.safeParseInt(Main.properties.getProperty("filter.x", "-1"), -1);
        int y = Calc.safeParseInt(Main.properties.getProperty("filter.y", "-1"), -1);
        int w = Calc.safeParseInt(Main.properties.getProperty("filter.w", "-1"), -1);
        int h = Calc.safeParseInt(Main.properties.getProperty("filter.h", "-1"), -1);

        if (x >= 0 && y >= 0 && w >= 0 && h >= 0) {
            Rectangle rec = new Rectangle(x, y, w, h);
            dialog.setBounds(rec);
        }

        while (dialog.result == null) {
            dialog.setVisible(true);
            if (dialog.isAborted) {
                return null;
            }
        }

        return dialog.result;
    }
}
