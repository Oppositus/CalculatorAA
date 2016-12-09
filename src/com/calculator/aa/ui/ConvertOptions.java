package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.Zipper;
import com.calculator.aa.db.Instrument;
import com.calculator.aa.db.InstrumentsMeta;
import com.calculator.aa.db.ReaderCSV;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConvertOptions extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox<String> comboBoxValue;
    private JComboBox<String> comboBoxMonth;
    private JCheckBox checkBoxAnnual;
    private JLabel labelMonth;

    private final InstrumentsMeta meta;
    private final String[] tickers;
    private AATableModel result;

    private ConvertOptions(String[] ts, InstrumentsMeta m) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

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

        tickers = ts;
        meta = m;

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
        result = processTickers(tickers, meta, comboBoxValue.getSelectedIndex(), comboBoxMonth.getSelectedIndex(), checkBoxAnnual.isSelected());
        dispose();
    }

    private void onCancel() {
        result = null;
        dispose();
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

    private static AATableModel processTickers(String[] tickers, InstrumentsMeta meta, int valIndex, int month, boolean annualize) {
        InstrumentsMeta.ValueType vt;
        InstrumentsMeta.PeriodType pt;

        switch (valIndex) {
            case 0:
                vt = InstrumentsMeta.ValueType.CLOSE_ADJ;
                break;
            case 1:
                vt = InstrumentsMeta.ValueType.CLOSE;
                break;
            case 2:
                vt = InstrumentsMeta.ValueType.OPEN;
                break;
            case 3:
                vt = InstrumentsMeta.ValueType.HIGH;
                break;
            case 4:
                vt = InstrumentsMeta.ValueType.LOW;
                break;
            default:
                vt = null;
        }

        pt = annualize ? InstrumentsMeta.PeriodType.YEAR : InstrumentsMeta.PeriodType.MONTH;

        List<Zipper<String, Double, String>> zippers = Arrays.stream(tickers)
                .map(t -> {
                    InstrumentsMeta.InstrumentType type = meta.getTypeFromTicker(t);
                    return new Instrument(t, type);
                })
                .filter(Instrument::isValid)
                .map(i -> {
                    i.applyFilter(vt, pt, month);
                    return i.getModel().toZipper();
                })
                .collect(Collectors.toList());

        if (zippers.size() == 0) {
            return null;
        } else {
            Zipper<String, Double, String> zipper = zippers.get(0);
            int length = zippers.size();
            for (int i = 1; i < length; i++) {
                zipper = zipper.zip(zippers.get(i), -1.0);
            }

            return AATableModel.fromZipper(zipper, new String[]{ReaderCSV.dbDelim, ReaderCSV.dbMark, ReaderCSV.dbDecimal, "1"});
        }
    }

    static AATableModel showOptions(String[] tickers, InstrumentsMeta meta) {

        ConvertOptions dialog = new ConvertOptions(tickers, meta);
        dialog.setTitle(Main.resourceBundle.getString("ui.convert_options"));
        dialog.setLocationRelativeTo(Main.getFrame());

        dialog.pack();

        dialog.setVisible(true);

        Main.properties.setProperty("convert.value", String.valueOf(dialog.comboBoxValue.getSelectedIndex()));
        Main.properties.setProperty("convert.annual", dialog.checkBoxAnnual.isSelected() ? "1" : "0");
        Main.properties.setProperty("convert.month", String.valueOf(dialog.comboBoxMonth.getSelectedIndex()));

        return dialog.result;
    }

    static AATableModel notShowOptions(String[] tickers, InstrumentsMeta meta) {
        int val = Calc.safeParseInt(Main.properties.getProperty("convert.value", "0"), 0);
        int month = Calc.safeParseInt(Main.properties.getProperty("convert.month", "0"), 0);
        boolean annual = "1".equals(Main.properties.getProperty("convert.annual", "0"));

        return processTickers(tickers, meta, val, month, annual);
    }
}
