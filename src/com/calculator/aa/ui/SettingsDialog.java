package com.calculator.aa.ui;

import com.calculator.aa.Main;

import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class SettingsDialog extends JDialog {

    private static final String updateUrl = "https://github.com/Oppositus/CalculatorAA/tree/master/builds/version.txt";

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton buttonApply;
    private JComboBox<String> comboBoxLook;
    private JCheckBox checkBoxUpdates;
    private JButton buttonCheckNow;

    private UIManager.LookAndFeelInfo[] looksAndFeels;
    private LookAndFeel currentLookAndFeel;
    private boolean hasUpdate;

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

        buttonCheckNow.addActionListener(actionEvent -> {
            try {
                hasUpdate = false;

                HttpURLConnection.setFollowRedirects(true);
                HttpURLConnection connection = (HttpURLConnection)new URL(updateUrl).openConnection();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                    int bufferLength = 1024;
                    byte[] responseBuffer = new byte[bufferLength];
                    List<Byte> bytesFromHTTP = new LinkedList<>();

                    InputStream isHTTP = connection.getInputStream();

                    while (true) {
                        int wasRead = isHTTP.read(responseBuffer);
                        if (wasRead < 0) {
                            break;
                        }
                        for (int i = 0; i < wasRead; i++) {
                            bytesFromHTTP.add(responseBuffer[i]);
                        }
                    }

                    int resLen = bytesFromHTTP.size();
                    byte[] res = new byte[resLen];
                    int index = 0;
                    for (byte b : bytesFromHTTP) {
                        res[index++] = b;
                    }
                    String result = new String(res, StandardCharsets.UTF_8);

                    isHTTP.close();
                    connection.disconnect();

                    String[] lines = result.split("[\\r\\n]+");
                    for (String line1 : lines) {
                        String[] line = line1.split("=");
                        if (line.length == 2) {
                            if ("application".equals(line[0]) && !Main.versionApp.equals(line[1])) {
                                hasUpdate = true;
                            }
                            if ("database".equals(line[0]) && !Main.versionBase.equals(line[1])) {
                                hasUpdate = true;
                            }
                        }
                    }

                    Main.getWindow().setUpdateAvailable(hasUpdate);
                    JOptionPane.showMessageDialog(Main.getFrame(),
                            Main.resourceBundle.getString(hasUpdate ? "text.update_has" : "text.update_no"),
                            Main.resourceBundle.getString("text.update"),
                            JOptionPane.INFORMATION_MESSAGE);

                } else {
                    JOptionPane.showMessageDialog(Main.getFrame(),
                            String.format(Main.resourceBundle.getString("text.update_error_http"), connection.getResponseCode()),
                            Main.resourceBundle.getString("text.error"),
                            JOptionPane.ERROR_MESSAGE);
                }

            } catch (IOException e) {
                JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
            }

        });
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
