package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.Zipper;
import com.calculator.aa.db.Instrument;
import com.calculator.aa.db.ReaderCSV;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConvertOptions extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox<String> comboBoxValue;
    private JComboBox<String> comboBoxMonth;
    private JCheckBox checkBoxAnnual;
    private JLabel labelMonth;
    private JCheckBox checkBoxReload;
    private JLabel progressLabel;

    private final List<Instrument> instruments;
    private AATableModel result;
    private boolean dialogAborted;
    private boolean downloadingInProgress;

    private ConvertOptions(List<Instrument> instrs) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        dialogAborted = false;
        downloadingInProgress = false;

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

        checkBoxAnnual.addActionListener(actionEvent -> {
            boolean isSelected = checkBoxAnnual.isSelected();
            labelMonth.setEnabled(isSelected);
            comboBoxMonth.setEnabled(isSelected);
        });

        instruments = instrs;

        comboBoxValue.setSelectedIndex(Calc.safeParseInt(Main.properties.getProperty("convert.value", "0"), 0));
        comboBoxMonth.setSelectedIndex(Calc.safeParseInt(Main.properties.getProperty("convert.month", "0"), 0));
        boolean isSelected = "1".equals(Main.properties.getProperty("convert.annual", "0"));
        checkBoxAnnual.setSelected(isSelected);
        if (isSelected) {
            labelMonth.setEnabled(isSelected);
            comboBoxMonth.setEnabled(isSelected);
        }
    }

    private void onOK() {
        downloadingInProgress = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingUtilities.invokeLater(() -> updateInstruments(instruments, new LinkedList<>()));
    }

    private void afterOk(List<Instrument> downloaded) {
        downloadingInProgress = false;

        setCursor(Cursor.getDefaultCursor());

        if (downloaded != null && !downloaded.isEmpty()) {
            result = processInstruments(downloaded, comboBoxValue.getSelectedIndex(), comboBoxMonth.getSelectedIndex(), checkBoxAnnual.isSelected());
        } else {
            result = null;
        }

        dispose();
    }

    private void updateInstruments(List<Instrument> instrs, List<Instrument> result) {
        if (instrs.isEmpty()) {
            afterOk(result);
        } else {

            if (dialogAborted) {
                afterOk(null);
                return;
            }

            Instrument ins = instrs.get(0);
            instrs.remove(0);

            SwingUtilities.invokeLater(() -> Main.sqLite.updateInstrumentHistory(ins, checkBoxReload.isSelected(), progressLabel, res -> {
                if (!res) {
                    JOptionPane.showMessageDialog(Main.getFrame(),
                            String.format(Main.resourceBundle.getString("text.error_downloading"), ins.toString()),
                            Main.resourceBundle.getString("text.error"),
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    result.add(ins);
                }

                SwingUtilities.invokeLater(() -> updateInstruments(instrs, result));
            }));
        }
    }

    private void onCancel() {
        dialogAborted = true;
        if (!downloadingInProgress) {
            result = null;
            dispose();
        }
    }

    private void createUIComponents() {
        String[] cbValues = new String[] {
                Main.resourceBundle.getString("text.value_close_adj"),
                Main.resourceBundle.getString("text.value_close"),
                Main.resourceBundle.getString("text.value_open"),
                Main.resourceBundle.getString("text.value_high"),
                Main.resourceBundle.getString("text.value_low")
        };
        comboBoxValue = new JComboBox<>(cbValues);

        String[] cbMonths = new String[12];
        for (int i = 0; i < 12; i++) {
            cbMonths[i] = Main.resourceBundle.getString("text.month." + i);
        }
        comboBoxMonth = new JComboBox<>(cbMonths);

    }

    private static Instrument.ValueType typeFromInt(int type) {
        switch (type) {
            case 0:
                return Instrument.ValueType.CLOSEADJ;
            case 1:
                return Instrument.ValueType.CLOSE;
            case 2:
                return Instrument.ValueType.OPEN;
            case 3:
                return Instrument.ValueType.HIGH;
            case 4:
                return Instrument.ValueType.LOW;
            default:
                return null;
        }
    }

    private static AATableModel processInstruments(List<Instrument> instrs, int valIndex, int month, boolean annualize) {
        Instrument.ValueType vt = typeFromInt(valIndex);

        List<Zipper> zippers = instrs.stream()
                .map(i -> {
                    Main.sqLite.setInstrumentHistory(i, vt);
                    if (annualize) {
                        i.makeAnnual(month);
                    }
                    return i;
                })
                .map(i -> {
                    AATableModel model = i.getTableModelForInstrument();
                    model.setDateFormat(MainWindow.DateFormats.DATE_FORMAT_YYYY_MM_DD);
                    return model;
                })
                .map(AATableModel::toZipper)
                .collect(Collectors.toList());

        if (zippers.size() == 0) {
            return null;
        } else {
            Zipper zipper = zippers.get(0);
            int length = zippers.size();
            for (int i = 1; i < length; i++) {
                try {
                    zipper = zipper.zip(zippers.get(i), new AAModelComparator(MainWindow.DateFormats.DATE_FORMAT_YYYY_MM), -1.0);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
                    break;
                }
            }

            AATableModel model = AATableModel.fromZipper(zipper, new String[]{ReaderCSV.dbDelim, ReaderCSV.dbMark, ReaderCSV.dbDecimal, "1"});
            model.setDateFormat(MainWindow.DateFormats.DATE_FORMAT_YYYY_MM);
            return model;
        }
    }

    static AATableModel showOptions(List<Instrument> instruments, String title) {

        ConvertOptions dialog = new ConvertOptions(instruments);
        dialog.setTitle(title == null ? Main.resourceBundle.getString("ui.convert_options") : title);
        dialog.setLocationRelativeTo(Main.getFrame());

        dialog.pack();
        dialog.setResizable(false);

        dialog.setVisible(true);

        Main.properties.setProperty("convert.value", String.valueOf(dialog.comboBoxValue.getSelectedIndex()));
        Main.properties.setProperty("convert.annual", dialog.checkBoxAnnual.isSelected() ? "1" : "0");
        Main.properties.setProperty("convert.month", String.valueOf(dialog.comboBoxMonth.getSelectedIndex()));

        return dialog.result;
    }

    static AATableModel notShowOptions(String[] tickers) {
        int val = Calc.safeParseInt(Main.properties.getProperty("convert.value", "0"), 0);
        int month = Calc.safeParseInt(Main.properties.getProperty("convert.month", "0"), 0);
        boolean annual = "1".equals(Main.properties.getProperty("convert.annual", "0"));

        List<Instrument> instruments = Stream.of(tickers)
                .map(Main.sqLite::findInstrument)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return processInstruments(instruments, val, month, annual);
    }
}
