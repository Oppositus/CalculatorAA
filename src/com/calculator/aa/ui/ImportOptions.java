package com.calculator.aa.ui;

import com.calculator.aa.Main;

import javax.swing.*;
import java.awt.event.*;

public class ImportOptions extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox comboTextMark;
    private JComboBox comboBoxDelim;
    private JComboBox comboBoxDecimalPoint;

    private String[] result;

    public ImportOptions(String[] options) {
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

        result = options;

        if (result != null) {

            if (result[0].equals("\t")) {
                result[0] = "табуляция";
            } else if (result[0].equals(" ")) {
                result[0] = "пробел";
            }

            comboBoxDelim.getModel().setSelectedItem(result[0]);
            comboTextMark.getModel().setSelectedItem(result[1]);
            comboBoxDecimalPoint.getModel().setSelectedItem(result[2]);
        } else {
            result = new String[] {
                    (String)comboBoxDelim.getModel().getSelectedItem(),
                    (String)comboTextMark.getModel().getSelectedItem(),
                    (String)comboBoxDecimalPoint.getModel().getSelectedItem()
            };
        }

        comboBoxDelim.addActionListener(actionEvent -> result[0] = (String)comboBoxDelim.getModel().getSelectedItem());
        comboTextMark.addActionListener(actionEvent -> result[1] = (String)comboTextMark.getModel().getSelectedItem());
        comboBoxDecimalPoint.addActionListener(actionEvent -> result[2] = (String)comboBoxDecimalPoint.getModel().getSelectedItem());
    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        result = null;
        dispose();
    }

    public static String[] showOptions(String[] options) {
        ImportOptions dialog = new ImportOptions(options);
        dialog.setTitle("Импорт данных");
        dialog.setLocationRelativeTo(Main.getFrame());
        dialog.pack();
        dialog.setVisible(true);

        if (dialog.result != null) {
            if (dialog.result[0].equals("табуляция")) {
                dialog.result[0] = "\t";
            } else if (dialog.result[0].equals("пробел")) {
                dialog.result[0] = " ";
            }
        }

        return dialog.result;
    }
}
