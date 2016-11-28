package com.calculator.aa.ui;

import com.calculator.aa.Main;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CSVOptions extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox comboTextMark;
    private JComboBox<String> comboBoxDelim;
    private JComboBox comboBoxDecimalPoint;

    private String[] result;

    private CSVOptions(String[] options) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        String[] delims = {",", ";", ":", Main.resourceBundle.getString("text.tabulation"), Main.resourceBundle.getString("text.space")};
        comboBoxDelim.setModel(new DefaultComboBoxModel<>(delims));

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
                result[0] = Main.resourceBundle.getString("text.tabulation");
            } else if (result[0].equals(" ")) {
                result[0] = Main.resourceBundle.getString("text.space");
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

    static String[] showOptions(String[] options) {
        CSVOptions dialog = new CSVOptions(options);
        dialog.setTitle(Main.resourceBundle.getString("text.import_data"));
        dialog.setLocationRelativeTo(Main.getFrame());
        dialog.pack();
        dialog.setVisible(true);

        if (dialog.result != null) {
            if (dialog.result[0].equals(Main.resourceBundle.getString("text.tabulation"))) {
                dialog.result[0] = "\t";
            } else if (dialog.result[0].equals(Main.resourceBundle.getString("text.space"))) {
                dialog.result[0] = " ";
            }
        }

        return dialog.result;
    }

    private void createUIComponents() {
        comboBoxDelim = new JComboBox<>();
    }
}
