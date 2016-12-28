package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SettingsDialog extends JDialog {

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton buttonApply;
    private JComboBox<String> comboBoxLook;
    private JCheckBox checkBoxUpdates;
    private JButton buttonCheckNow;
    private JPanel panel0Percent;
    private JPanel panelShowGradient;
    private JPanel panel50Percent;
    private JPanel panel100Percent;

    private UIManager.LookAndFeelInfo[] looksAndFeels;
    private LookAndFeel currentLookAndFeel;
    private boolean hasUpdate;
    private boolean changed;

    private SettingsDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        changed = false;

        panel0Percent.setBackground(parseColor("gradient.from", "255,0,0"));
        panel50Percent.setBackground(parseColor("gradient.middle", "255,255,0"));
        panel100Percent.setBackground(parseColor("gradient.to", "0,255,0"));

        ((GradientPanel)panelShowGradient).setColors(panel0Percent.getBackground(), panel50Percent.getBackground(), panel100Percent.getBackground());

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

        buttonCheckNow.addActionListener(actionEvent -> {
            hasUpdate = Main.getMain().checkHasUpdate(true);
            JOptionPane.showMessageDialog(Main.getFrame(),
                    Main.resourceBundle.getString(hasUpdate ? "text.update_has" : "text.update_no"),
                    Main.resourceBundle.getString("text.update"),
                    JOptionPane.INFORMATION_MESSAGE);
        });
        panel0Percent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Color begin = JColorChooser.showDialog(panel0Percent.getParent(), Main.resourceBundle.getString("text.gradient_start"), panel0Percent.getBackground());
                panel0Percent.setBackground(begin);
                ((GradientPanel)panelShowGradient).updateColor(GradientPainter.ColorName.Begin, begin);
            }
        });
        panel50Percent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Color middle = JColorChooser.showDialog(panel50Percent.getParent(), Main.resourceBundle.getString("text.gradient_middle"), panel50Percent.getBackground());
                panel50Percent.setBackground(middle);
                ((GradientPanel)panelShowGradient).updateColor(GradientPainter.ColorName.Middle, middle);
            }
        });
        panel100Percent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                Color end = JColorChooser.showDialog(panel100Percent.getParent(), Main.resourceBundle.getString("text.gradient_end"), panel100Percent.getBackground());
                panel100Percent.setBackground(end);
                ((GradientPanel)panelShowGradient).updateColor(GradientPainter.ColorName.End, end);
            }
        });
    }

    private void onOK() {
        onApply(null);
        Main.properties.setProperty("ui.updates_check", checkBoxUpdates.isSelected() ? "1" : "0");
        dispose();
    }

    private void onApply(LookAndFeel lookAndFeel) {

        changed = true;

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

        saveColor("gradient.from", panel0Percent.getBackground());
        saveColor("gradient.middle", panel50Percent.getBackground());
        saveColor("gradient.to", panel100Percent.getBackground());
    }

    private void onCancel() {
        if (changed) {
            onApply(currentLookAndFeel);
        }
        dispose();
    }

    static void showSettings() {
        SettingsDialog dialog = new SettingsDialog();
        dialog.setTitle(Main.resourceBundle.getString("text.settings"));
        dialog.setLocationRelativeTo(Main.getFrame());
        dialog.pack();
        dialog.setVisible(true);
    }

    static Color parseColor(String propertyName, String defaultValue) {
        String property = Main.properties.getProperty(propertyName, defaultValue);
        String[] splitted = property.split(",");
        return new Color(
                Calc.safeParseInt(splitted[0], 0),
                Calc.safeParseInt(splitted[1], 0),
                Calc.safeParseInt(splitted[2], 0)
        );
    }

    private void saveColor(String propertyName, Color color) {
        StringBuilder sb = new StringBuilder();
        sb.append(color.getRed());
        sb.append(",");
        sb.append(color.getGreen());
        sb.append(",");
        sb.append(color.getBlue());

        Main.properties.setProperty(propertyName, sb.toString());
    }

    private void createUIComponents() {
        looksAndFeels = UIManager.getInstalledLookAndFeels();
        String[] names = Arrays.stream(looksAndFeels).map(UIManager.LookAndFeelInfo::getName).collect(Collectors.toList()).toArray(new String[0]);
        comboBoxLook = new JComboBox<>(names);

        currentLookAndFeel = UIManager.getLookAndFeel();

        comboBoxLook.setSelectedItem(currentLookAndFeel.getName());

        panelShowGradient = new GradientPanel();
    }
}
