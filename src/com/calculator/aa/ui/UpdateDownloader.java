package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UpdateDownloader extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JProgressBar progressBarApp;
    private JProgressBar progressBarBase;

    private boolean stopFlag;

    private UpdateDownloader() {
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
        downloadFile(Main.newVersionUrl, progressBarApp, "bin" + File.separator + "update" + File.separator);
    }

    private void onCancel() {
        stopFlag = true;
        dispose();
    }

    private void downloadFile(String url, JProgressBar progress, String folderToUnzip) {
        buttonOK.setEnabled(false);

        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) new URL(url).openConnection();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                long size = connection.getHeaderFieldLong("Content-Length", -1);
                InputStream is = connection.getInputStream();

                String fileName = url.substring(url.lastIndexOf("/") + 1, url.length());
                String fullName = folderToUnzip + fileName;

                Path fullPath = Paths.get(fullName);
                Files.createDirectories(fullPath.getParent());

                File f = fullPath.toFile();
                OutputStream os = new BufferedOutputStream(new FileOutputStream(f));

                downloadPart(connection, is, os, size, 0, progress, f, this::unZip);

            } else {
                connection.disconnect();
                JOptionPane.showMessageDialog(Main.getFrame(),
                        String.format(Main.resourceBundle.getString("text.update_error_http"), connection.getResponseCode()),
                        Main.resourceBundle.getString("text.error"),
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void downloadPart(HttpURLConnection conn, InputStream is, OutputStream os, long size, long downloaded, JProgressBar progress, File file, Consumer<File> after) {

        if (stopFlag) {
            closeStreams(conn, is, os);
            after.accept(null);
            return;
        }

        byte[] responseBuffer = new byte[16 * 1024];

        try {
            int wasRead = is.read(responseBuffer);
            if (wasRead >= 0) {
                os.write(responseBuffer, 0, wasRead);
                downloaded += wasRead;
            } else {
                if (downloaded != size) {
                    closeStreams(conn, is, os);
                    after.accept(null);
                    return;
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
            closeStreams(conn, is, os);
            after.accept(null);
            return;
        }

        int percentI = (int)(((double)downloaded) / ((double)size) * 100);
        String percentS = Calc.formatPercent0(((double)downloaded) / ((double)size));
        progress.setValue(percentI);
        progress.setString(percentS);

        if (downloaded == size) {
            try {
                os.flush();
                closeStreams(conn, is, os);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            after.accept(file);
        } else {
            long dn = downloaded;
            SwingUtilities.invokeLater(() -> downloadPart(conn, is, os, size, dn, progress, file, after));
        }
    }

    private void closeStreams(HttpURLConnection conn, InputStream is, OutputStream os) {
        try {
            is.close();
            os.close();
            conn.disconnect();
        } catch (IOException ignored) {
        }
    }

    private void unZip(File zip) {
        if (zip == null) {
            return;
        }

        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zip));
            ZipEntry ze = zis.getNextEntry();
            String outputFolder = zip.getParent();
            byte[] buffer = new byte[16 * 1024];

            while(ze != null){
                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);

                if (ze.isDirectory()) {
                    Files.createDirectories(newFile.toPath());
                } else {

                    if (!Files.exists(newFile.toPath().getParent())) {
                        Files.createDirectories(newFile.toPath().getParent());
                    }

                    BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(newFile));
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }

                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }

        buttonOK.setEnabled(true);
    }

    static void showDownloader() {
        UpdateDownloader dialog = new UpdateDownloader();
        dialog.setTitle(Main.resourceBundle.getString("text.update_title"));
        dialog.setLocationRelativeTo(Main.getFrame());
        dialog.pack();
        dialog.setVisible(true);
    }
}
