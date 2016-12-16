package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

public class UpdateDownloader extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JProgressBar progressBarApp;
    private JProgressBar progressBarBase;

    private boolean stopFlag;

    public UpdateDownloader() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        stopFlag = false;

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
        downloadFile(Main.newVersionUrl, progressBarApp, "bin/update/");
    }

    private void onCancel() {
        stopFlag = true;
        dispose();
    }

    private void downloadFile(String url, JProgressBar progress, String folderToUnzip) {

        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) new URL(url).openConnection();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                long size = connection.getHeaderFieldLong("Content-Length", -1);
                InputStream is = connection.getInputStream();
                OutputStream os = Files.newOutputStream(Files.createTempFile("calcaa", ".zip"), StandardOpenOption.CREATE);

                downloadPart(connection, is, os, size, 0, progress, folderToUnzip, s -> {
                    System.out.println("After: " + s);
                });

            } else {
                JOptionPane.showMessageDialog(Main.getFrame(),
                        String.format(Main.resourceBundle.getString("text.update_error_http"), connection.getResponseCode()),
                        Main.resourceBundle.getString("text.error"),
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void downloadPart(HttpURLConnection conn, InputStream is, OutputStream os, long size, long downloaded, JProgressBar progress, String folderToUnzip, Consumer<String> after) {

        if (stopFlag) {
            try {
                is.close();
                os.close();
                conn.disconnect();
            } catch (IOException ignored) {
            }
            after.accept(null);
        }

        int bufferLength = 16 * 1024;
        byte[] responseBuffer = new byte[bufferLength];

        try {
            int wasRead = is.read(responseBuffer);
            if (wasRead >= 0) {
                os.write(responseBuffer, 0, wasRead);
                downloaded += wasRead;
            } else {
                if (downloaded != size) {
                    try {
                        is.close();
                        os.close();
                        conn.disconnect();
                    } catch (IOException ignored) {
                    }
                    after.accept(null);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);

            try {
                is.close();
                os.close();
                conn.disconnect();
            } catch (IOException ignored) {
            }
            after.accept(null);

        }

        int percentI = (int)(((double)downloaded) / ((double)size));
        String percentS = Calc.formatPercent0(((double)downloaded) / ((double)size));
        progress.setValue(percentI);
        progress.setString(percentS);

        if (downloaded == size) {
            after.accept(folderToUnzip);
        } else {
            long dn = downloaded;
            SwingUtilities.invokeLater(() -> downloadPart(conn, is, os, size, dn, progress, folderToUnzip, after));
        }
    }

    static void showDownloader() {
        UpdateDownloader dialog = new UpdateDownloader();
        dialog.setTitle(Main.resourceBundle.getString("text.update_title"));
        dialog.setLocationRelativeTo(Main.getFrame());
        dialog.pack();
        dialog.setVisible(true);
    }
}
