package com.calculator.aa;

import com.calculator.aa.ui.MainWindow;

import javax.swing.*;

class Main {
    private static Main program;
    private final JFrame mainFrame;
    private final MainWindow mainWindow;

    private Main() {
        mainFrame = new JFrame("todo: name");
        mainWindow = new MainWindow();
        mainFrame.setContentPane(mainWindow.GetMainPanel());
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }

    public static void main(String[] args) {
        program = new Main();
    }
}
