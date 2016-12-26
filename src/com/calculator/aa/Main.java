package com.calculator.aa;

import com.calculator.aa.calc.Calc;
import com.calculator.aa.db.SQLiteSupport;
import com.calculator.aa.ui.MainWindow;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

public class Main {
    private static final String versionApp = "2.0";
    private static final String versionBase = "1.0";
    private static final String updateUrl = "https://raw.githubusercontent.com/Oppositus/CalculatorAA/master/builds/version.txt";
    public static String newVersionUrl = null;
    public static String newDatabaseUrl = null;

    public static Cursor weCursor;
    public static Cursor nsCursor;

    private static Main program;
    private final JFrame mainFrame;
    private final MainWindow mainWindow;
    private static final String propertiesFile = "calcaa.properties";

    public static Properties properties;
    public static ResourceBundle resourceBundle;
    public static SQLiteSupport sqLite;

    private Main() {
        mainFrame = new JFrame(resourceBundle.getString("text.program_name"));
        mainWindow = new MainWindow();
        mainFrame.setContentPane(mainWindow.GetMainPanel());
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setLocationRelativeTo(null);

        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                try {
                    Rectangle bounds = mainFrame.getBounds();
                    properties.setProperty("frame.x", String.valueOf((int) bounds.getX()));
                    properties.setProperty("frame.y", String.valueOf((int) bounds.getY()));
                    properties.setProperty("frame.w", String.valueOf((int) bounds.getWidth()));
                    properties.setProperty("frame.h", String.valueOf((int) bounds.getHeight()));

                    int maximized = (mainFrame.getExtendedState() & JFrame.MAXIMIZED_BOTH) > 0 ? 1 : 0;
                    properties.setProperty("frame.z", String.valueOf(maximized));

                    properties.store(new BufferedOutputStream(new FileOutputStream(propertiesFile)), "CalculatorAA");

                } catch (Exception ignored) {
                }

                sqLite.dispose();

                System.exit(0);
            }
        });

        restoreFrameProperties();

        mainFrame.setVisible(true);
    }

    public static Main getMain() {
        return program;
    }

    public static JFrame getFrame() {
        return program.mainFrame;
    }

    public void restoreFrameProperties() {
        mainFrame.pack();

        int x = Calc.safeParseInt(properties.getProperty("frame.x", "-1"), -1);
        int y = Calc.safeParseInt(properties.getProperty("frame.y", "-1"), -1);
        int w = Calc.safeParseInt(properties.getProperty("frame.w", "-1"), -1);
        int h = Calc.safeParseInt(properties.getProperty("frame.h", "-1"), -1);
        int z = Calc.safeParseInt(properties.getProperty("frame.z", "0"), -1);

        if (z == 1) {
            mainFrame.setExtendedState(mainFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        } else if (x >= 0 && y >= 0 && w >= 0 && h >= 0) {
            Rectangle rec = new Rectangle(x, y, w, h);
            mainFrame.setBounds(rec);
        }
    }

    public static String[] getPeriods(int fromIndex, int toIndex) {
        String[] periods = program.mainWindow.getPeriods();
        int max = Math.min(toIndex, periods.length - 1) + 1;
        return Arrays.copyOfRange(periods, fromIndex, max);
    }

    public boolean checkHasUpdate(boolean verbose) {

        try {
            HttpURLConnection.setFollowRedirects(true);
            HttpURLConnection connection = (HttpURLConnection) new URL(updateUrl).openConnection();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                int bufferLength = 1024;
                byte[] responseBuffer = new byte[bufferLength];
                java.util.List<Byte> bytesFromHTTP = new LinkedList<>();

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
                boolean hasAppUpdate = false;
                boolean hasBaseUpdate = false;

                for (String line1 : lines) {
                    String[] line = line1.split("=");
                    if (line.length == 2) {
                        if ("application".equals(line[0]) && !versionApp.equals(line[1])) {
                            hasAppUpdate = true;
                        }
                        if ("database".equals(line[0]) && !versionBase.equals(line[1])) {
                            hasBaseUpdate = true;
                        }

                        if ("aurl".equals(line[0])) {
                            newVersionUrl = line[1];
                        }
                        if ("durl".equals(line[0])) {
                            newDatabaseUrl = line[1];
                        }
                    }
                }

                if (!hasAppUpdate) {
                    newVersionUrl = null;
                }
                if (!hasBaseUpdate) {
                    newDatabaseUrl = null;
                }

                mainWindow.setUpdateAvailable(hasAppUpdate);

                return hasAppUpdate || hasBaseUpdate;
            } else {
                if (verbose) {
                    JOptionPane.showMessageDialog(mainFrame,
                            String.format(resourceBundle.getString("text.update_error_http"), connection.getResponseCode()),
                            resourceBundle.getString("text.error"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (IOException e) {
            if (verbose) {
                JOptionPane.showMessageDialog(mainFrame, e, resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
            }
        }

        return false;
    }

    public static void reconnectDatabase(Runnable middle) {
        sqLite.dispose();
        middle.run();
        sqLite = new SQLiteSupport();
    }

    public static void main(String[] args) {
        resourceBundle = ResourceBundle.getBundle("com.calculator.aa.messages", Locale.getDefault());

        properties = new Properties();
        try {
            if (Files.exists(new File(propertiesFile).toPath())) {
                properties.load(new BufferedInputStream(new FileInputStream(propertiesFile)));
            }
        } catch (IOException ignored) {
        }

        String laf = properties.getProperty("ui.theme");
        try {
            if (laf != null) {
                try {
                    UIManager.setLookAndFeel(laf);
                } catch (Exception ignored) {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception ignored) {
        }

        String[] savedOptions = readImportOptions();

        sqLite = new SQLiteSupport();

        SwingUtilities.invokeLater(() -> {
            program = new Main();
            createCursors();

            String file = properties.getProperty("files.last", "");
            if (!file.isEmpty()) {

                if (file.startsWith("base:")) {
                    SwingUtilities.invokeLater(() -> {
                        String[] files = file.replace("base:", "").split(";");
                        if (files.length > 0) {
                            program.mainWindow.getTickersAndLoadData(files, savedOptions);
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        String[] files = file.split(";");
                        if (files.length > 0) {
                            program.mainWindow.parseCSVAndLoadData(new File(files[0]), savedOptions);
                        }
                        Stream.of(Arrays.copyOfRange(files, 1, files.length)).map(File::new).forEach(program.mainWindow::silentParseCSVAndMergeData);
                    });
                }

            }

            if ("1".equals(Main.properties.getProperty("ui.updates_check", "1"))) {
                Timer tm = new Timer(10000, actionEvent -> program.checkHasUpdate(false));
                tm.setRepeats(false);
                tm.start();
            }
        });
    }

    private static String[] readImportOptions() {
        String[] savedOptions = new String[]{";", "\"", ".", "1"};

        String s = properties.getProperty("import.delimiter");
        if (s != null) {
            savedOptions[0] = s;
        }

        s = properties.getProperty("import.mark");
        if (s != null) {
            savedOptions[1] = s;
        }

        s = properties.getProperty("import.decimal");
        if (s != null) {
            savedOptions[2] = s;
        }

        s = properties.getProperty("import.date");
        if (s != null) {
            savedOptions[3] = s;
        }

        return savedOptions;
    }

    private static void createCursors() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();

        Image we = toolkit.getImage(program.getClass().getResource("ui/icons/we-cursor.png"));
        weCursor = toolkit.createCustomCursor(we, new Point(7, 4), "WE-CURSOR");
        Image ns = toolkit.getImage(program.getClass().getResource("ui/icons/ns-cursor.png"));
        nsCursor = toolkit.createCustomCursor(ns, new Point(4, 7), "NS-CURSOR");
    }
}
