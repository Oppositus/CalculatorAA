package com.calculator.aa;

import com.calculator.aa.ui.MainWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

public class Main {
    private static Main program;
    private final JFrame mainFrame;
    private final MainWindow mainWindow;
    private final Properties properties;
    private static final String propertiesFile = "calcaa.properties";

    public static ResourceBundle resourceBundle;

    private Main() {

        properties = new Properties();
        try {
            if (Files.exists(new File(propertiesFile).toPath())) {
                properties.load(new BufferedInputStream(new FileInputStream(propertiesFile)));
            }
        } catch (IOException ignored) {}

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
                System.exit(0);
            }
        });

        mainFrame.pack();

        int x = Integer.parseInt(properties.getProperty("frame.x", "-1"));
        int y = Integer.parseInt(properties.getProperty("frame.y", "-1"));
        int w = Integer.parseInt(properties.getProperty("frame.w", "-1"));
        int h = Integer.parseInt(properties.getProperty("frame.h", "-1"));
        int z = Integer.parseInt(properties.getProperty("frame.z", "0"));

        if (z == 1) {
            mainFrame.setExtendedState(mainFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        } else if (x >= 0 && y >= 0 && w >= 0 && h >= 0) {
            Rectangle rec = new Rectangle(x, y, w, h);
            mainFrame.setBounds(rec);
        }

        mainFrame.setVisible(true);
    }

    public static JFrame getFrame() {
        return program.mainFrame;
    }

    public static Properties getProperties() {
        return program.properties;
    }

    public static String[] getPeriods(int last) {
        String[] periods = program.mainWindow.getPeriods();
        return Arrays.copyOfRange(periods, periods.length - last, periods.length);
    }

    public static void main(String[] args) {
        resourceBundle = ResourceBundle.getBundle("com.calculator.aa.messages", Locale.getDefault());

        program = new Main();

        Properties prop = getProperties();
        String[] savedOptions = new String[] {";", "\"", "."};

        String s = prop.getProperty("import.delimeter");
        if (s != null) {
            savedOptions[0] = s;
        }

        s = prop.getProperty("import.mark");
        if (s != null) {
            savedOptions[1] = s;
        }

        s = prop.getProperty("import.decimal");
        if (s != null) {
            savedOptions[2] = s;
        }

        String file = prop.getProperty("file", "");

        if (!file.isEmpty()) {
            SwingUtilities.invokeLater(() -> program.mainWindow.parseCSVAndLoadData(new File(file), savedOptions));
        }
    }
}
