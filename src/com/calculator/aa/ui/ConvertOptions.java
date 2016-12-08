package com.calculator.aa.ui;

import com.calculator.aa.Main;

import javax.swing.*;
import java.awt.event.*;

public class ConvertOptions extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox<String> comboBoxValue;
    private JComboBox<String> comboBoxMonth;
    private JCheckBox checkBoxAnnual;
    private JLabel labelMonth;

    private String[] tickers;
    private AATableModel result;

    private ConvertOptions(String[] ts) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

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

        checkBoxAnnual.addActionListener(actionEvent -> {
            boolean isSelected = checkBoxAnnual.isSelected();
            labelMonth.setEnabled(isSelected);
            comboBoxMonth.setEnabled(isSelected);
        });

        tickers = ts;
    }

    private void onOK() {
        result = null;
        dispose();
    }

    private void onCancel() {
        result = null;
        dispose();
    }

    static AATableModel showOptions(String[] tickers) {

        ConvertOptions dialog = new ConvertOptions(tickers);
        dialog.setTitle(Main.resourceBundle.getString("ui.convert_options"));
        dialog.setLocationRelativeTo(Main.getFrame());

        dialog.pack();

        dialog.setVisible(true);

        return dialog.result;
    }

    private void createUIComponents() {
        String[] cbValues = new String[] {
                Main.resourceBundle.getString("text.value_close_adj"),
                Main.resourceBundle.getString("text.value_close"),
                Main.resourceBundle.getString("text.value_open"),
                Main.resourceBundle.getString("text.value_high"),
                Main.resourceBundle.getString("text.value_low")
        };
        comboBoxValue = new JComboBox<>(cbValues);

        String[] cbMonths = new String[12];
        for (int i = 0; i < 12; i++) {
            cbMonths[i] = Main.resourceBundle.getString("text.month." + i);
        }
        comboBoxMonth = new JComboBox<>(cbMonths);

    }
}
