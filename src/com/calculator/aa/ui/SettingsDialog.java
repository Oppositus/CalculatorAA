package com.calculator.aa.ui;

import com.calculator.aa.Main;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SettingsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton buttonApply;
    private JComboBox<String> comboBoxLook;
    private JCheckBox checkBoxUpdates;

    private UIManager.LookAndFeelInfo[] looksAndFeels;
    private LookAndFeel currentLookAndFeel;

    private SettingsDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        buttonApply.addActionListener(e -> onApply(null));

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

        checkBoxUpdates.setSelected("1".equals(Main.properties.getProperty("ui.updates_check", "1")));
    }

    private void onOK() {
        onApply(null);
        Main.properties.setProperty("ui.updates_check", checkBoxUpdates.isSelected() ? "1" : "0");
        dispose();
    }

    private void onApply(LookAndFeel lookAndFeel) {
        String name = (String)comboBoxLook.getSelectedItem();

        if (lookAndFeel != null) {
            name = lookAndFeel.getName();
        }

        for (UIManager.LookAndFeelInfo laf : looksAndFeels) {
            if (laf.getName().equals(name)) {
                try {
                    UIManager.setLookAndFeel(laf.getClassName());

                    SwingUtilities.updateComponentTreeUI(Main.getFrame());
                    Main.getMain().restoreFrameProperties();
                    SwingUtilities.updateComponentTreeUI(this);
                    pack();

                    Main.properties.setProperty("ui.theme", laf.getClassName());

                } catch (Exception ignored) {
                }
                break;
            }
        }
    }

    private void onCancel() {
        onApply(currentLookAndFeel);
        dispose();
    }

    static void showSettings() {
        SettingsDialog dialog = new SettingsDialog();
        dialog.setTitle(Main.resourceBundle.getString("text.settings"));
        dialog.setLocationRelativeTo(Main.getFrame());
        dialog.pack();
        dialog.setVisible(true);
    }

    private void createUIComponents() {
        looksAndFeels = UIManager.getInstalledLookAndFeels();
        String[] names = Arrays.stream(looksAndFeels).map(UIManager.LookAndFeelInfo::getName).collect(Collectors.toList()).toArray(new String[0]);
        comboBoxLook = new JComboBox<>(names);

        currentLookAndFeel = UIManager.getLookAndFeel();

        comboBoxLook.setSelectedItem(currentLookAndFeel.getName());
    }
}
