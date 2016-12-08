package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.db.InstrumentsMeta;

import javax.swing.*;
import java.awt.event.*;
import java.util.List;

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
        listInstrumentProviders.setModel(types);
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

    static String[] showFilter() {

        FilterDB dialog = new FilterDB();
        dialog.setTitle(Main.resourceBundle.getString("text.filter_db"));
        dialog.setLocationRelativeTo(Main.getFrame());

        dialog.pack();

        dialog.setVisible(true);

        return dialog.result;
    }
}
