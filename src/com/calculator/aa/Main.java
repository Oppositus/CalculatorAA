package com.calculator.aa;

import com.calculator.aa.calc.Calc;
import com.calculator.aa.db.SQLiteSupport;
import com.calculator.aa.ui.MainWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class Main {
    private static Main program;
    private final JFrame mainFrame;
    private final MainWindow mainWindow;
    private static final String propertiesFile = "calcaa.properties";

    public static Properties properties;
    public static ResourceBundle resourceBundle;
    public static Cursor voidCursor;
    public static SQLiteSupport sqLite;

    private Main() {
        mainFrame = new JFrame(resourceBundle.getString("text.program_name"));
        mainWindow = new MainWindow();
        mainFrame.setContentPane(mainWindow.GetMainPanel());
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setLocationRelativeTo(null);

        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener( new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                try {
                    Rectangle bounds = mainFrame.getBounds();
                    properties.setProperty("frame.x", String.valueOf((int)bounds.getX()));
                    properties.setProperty("frame.y", String.valueOf((int)bounds.getY()));
                    properties.setProperty("frame.w", String.valueOf((int)bounds.getWidth()));
                    properties.setProperty("frame.h", String.valueOf((int)bounds.getHeight()));

                    int maximized = (mainFrame.getExtendedState() & JFrame.MAXIMIZED_BOTH) > 0 ? 1 : 0;
                    properties.setProperty("frame.z", String.valueOf(maximized));

                    properties.store(new BufferedOutputStream(new FileOutputStream(propertiesFile)), "CalculatorAA");

                } catch(Exception ignored) {}

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

    public static void main(String[] args) {
        resourceBundle = ResourceBundle.getBundle("com.calculator.aa.messages", Locale.getDefault());

        voidCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB),
                new Point(0, 0),
                "null");

        properties = new Properties();
        try {
            if (Files.exists(new File(propertiesFile).toPath())) {
                properties.load(new BufferedInputStream(new FileInputStream(propertiesFile)));
            }
        } catch (IOException ignored) {}

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
        } catch (Exception ignored) {}

        String[] savedOptions = new String[] {";", "\"", ".", "1"};

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

        sqLite = new SQLiteSupport();

        SwingUtilities.invokeLater(() -> {
            program = new Main();

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
        });
    }
}
