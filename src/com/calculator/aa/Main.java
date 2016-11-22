package com.calculator.aa;

import com.calculator.aa.ui.MainWindow;

import javax.swing.*;

public class Main {
    private static Main program;
    private final JFrame mainFrame;
    private final MainWindow mainWindow;

    private Main() {
        mainFrame = new JFrame("СПТ: калькулятор");
        mainWindow = new MainWindow();
        mainFrame.setContentPane(mainWindow.GetMainPanel());
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    public static JFrame getFrame() {
        return program.mainFrame;
    }

    public static void main(String[] args) {
        program = new Main();
    }
}
