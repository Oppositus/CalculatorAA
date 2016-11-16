package com.calculator.aa.ui;

import javax.swing.*;
import java.awt.event.*;
import java.util.Arrays;

class InstrumentsEditor extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton buttonAdd;
    private JButton removeButton;
    private JList<String> listInstruments;

    private InstrumentsEditor(String[] current) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        Arrays.stream(current).forEach(listModel::addElement);
        listInstruments.setModel(listModel);

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
    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    static String[] showDialog(String[] current) {
        InstrumentsEditor dialog = new InstrumentsEditor(current);
        dialog.pack();
        dialog.setVisible(true);
        return current;
    }

    private void createUIComponents() {
        listInstruments = new JList<>();
    }
}
