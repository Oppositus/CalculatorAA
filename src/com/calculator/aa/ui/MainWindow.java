package com.calculator.aa.ui;

import com.calculator.aa.Main;
import com.calculator.aa.calc.Calc;
import com.calculator.aa.calc.Zipper;
import com.calculator.aa.db.InstrumentsMeta;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainWindow {
    private JTable mainTable;
    private JButton buttonOpen;
    private JPanel mainPanel;
    private JScrollPane scrollPane;
    private JButton buttonAddRow;
    private JButton buttonDeleteRow;
    private JButton buttonCorrelations;
    private JButton buttonCovariances;
    private JButton buttonPortfolio;
    private JButton buttonDeleteInvalid;
    private JButton buttonSave;
    private JButton buttonMerge;
    private JButton buttonRemoveColumn;
    private JButton buttonSettings;
    private JButton buttonDataBase;

    private String[] savedOptions;
    private String lastFileName;

    private class AATableCellRenderer extends DefaultTableCellRenderer {
        private final Color back = new Color(212, 212, 212);
        private final Color badBack = new Color(255, 224, 224);
        private final Color grayBack = new Color(240, 240, 240);
        private final int rows;

        AATableCellRenderer(int r) {
            rows = r;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            Font font = cell.getFont();

            if (row >= rows - 2) {
                font = font.deriveFont(Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD));
                cell.setBackground(back);
            } else {
                boolean isValidRow = ((AATableModel)table.getModel()).isRowValid(row);
                if (col > 0) {
                    double data = ((AATableModel)table.getModel()).getData()[row][col - 1];
                    if (data >= 0) {
                        cell.setBackground(isValidRow ? Color.WHITE : grayBack);
                    } else {
                        cell.setBackground(badBack);
                    }

                } else {
                    cell.setBackground(isValidRow ? Color.WHITE : grayBack);
                }
                font = font.deriveFont(Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR));
            }
            cell.setFont(font);
            return cell;
        }
    }

    enum DateFormats {
        DATE_FORMAT_NONE,
        DATE_FORMAT_YYYY,
        DATE_FORMAT_MM_YYYY,
        DATE_FORMAT_YYYY_MM,
        DATE_FORMAT_DD_MM_YYYY,
        DATE_FORMAT_MM_DD_YYYY,
        DATE_FORMAT_YYYY_MM_DD
    }

    public MainWindow() {
        buttonAddRow.addActionListener(actionEvent -> {
            AATableModel oldModel = (AATableModel)mainTable.getModel();
            AATableModel newModel = new AATableModel(oldModel.getWidth() - 1, oldModel.getHeight() - 1, oldModel, -1, -1);
            mainTable.setModel(newModel);

            for (int i = 0, width = newModel.getWidth(); i < width; i++) {
                mainTable.getColumnModel().getColumn(i).setCellRenderer(new AATableCellRenderer(newModel.getHeight()));
            }
        });
        buttonDeleteRow.addActionListener(actionEvent -> {
            AATableModel oldModel = (AATableModel)mainTable.getModel();
            if (oldModel.getHeight() > 4) {
                AATableModel newModel = new AATableModel(oldModel.getWidth() - 1,
                        oldModel.getHeight() - 3,
                        oldModel,
                        mainTable.getSelectedRow(), -1);
                mainTable.setModel(newModel);

                for (int i = 0, width = newModel.getWidth(); i < width; i++) {
                    mainTable.getColumnModel().getColumn(i).setCellRenderer(new AATableCellRenderer(newModel.getHeight()));
                }
            }
        });
        buttonOpen.addActionListener(actionEvent -> {
            File[] f = openExistingFile(true);
            if (f != null) {
                askCSVOptions(true, () -> {
                    try {
                        AATableModel newModel = parseCSVAndLoadData(f[0]);
                        setNewModel(newModel);

                        Stream.of(Arrays.copyOfRange(f, 1, f.length)).forEach(this::verboseParseCSVAndMergeData);

                        String files = String.join(
                                ";",
                                Arrays.stream(f).map(fl -> {
                                    try {
                                        return fl.getCanonicalPath();
                                    } catch (IOException e) {
                                        return "";
                                    }
                                }).collect(Collectors.toList()));

                        Main.properties.setProperty("files.last", files);
                        lastFileName = f[0].getCanonicalPath();

                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        });
        buttonCorrelations.addActionListener(actionEvent -> {

            AATableModel model = (AATableModel)mainTable.getModel();
            double[][] corrTable = Calc.correlationTable(model.getData());
            String[] cols = model.getInstrumentsOnly();

            ShowTable.show(Main.resourceBundle.getString("text.correlations_table"), corrTable, cols, cols);
        });
        buttonCovariances.addActionListener(actionEvent -> {
            AATableModel model = (AATableModel)mainTable.getModel();
            double[][] covTable = Calc.covarianceTable(model.getData());
            String[] cols = model.getInstrumentsOnly();

            ShowTable.show(Main.resourceBundle.getString("text.covariances_table"), covTable, cols, cols);
        });

        Action showPortfolioAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AATableModel model = (AATableModel)mainTable.getModel();
                PortfolioChart.showChart(model.getInstruments(), model.getData());
            }
        };
        KeyStroke keyShiftEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.CTRL_MASK);
        buttonPortfolio.getActionMap().put("showPortfolioAction", showPortfolioAction);
        buttonPortfolio.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyShiftEnter, "showPortfolioAction");
        buttonPortfolio.addActionListener(showPortfolioAction);

        buttonDeleteInvalid.addActionListener(e -> {
            AATableModel model = (AATableModel)mainTable.getModel();
            if (model.getHeight() < 5) {
                return;
            }

            boolean valid = false;
            while (!valid) {
                double[] row = model.getData()[0];
                for (double aRow : row) {
                    if (aRow < 0) {
                        model = new AATableModel(model.getWidth() - 1,
                                model.getHeight() - 3,
                                model,
                                0, -1);
                        valid = false;
                        break;
                    } else {
                        valid = true;
                    }
                }
            }

            if (model.getHeight() < 5) {
                return;
            }

            mainTable.setModel(model);

            for (int i = 0, width = model.getWidth(); i < width; i++) {
                mainTable.getColumnModel().getColumn(i).setCellRenderer(new AATableCellRenderer(model.getHeight()));
            }
        });

        buttonSave.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(lastFileName == null ? "." : lastFileName);
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(false);
            fc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory() || file.getName().toLowerCase().endsWith(".csv");
                }

                @Override
                public String getDescription() {
                    return Main.resourceBundle.getString("text.csv_extension");
                }
            });

            int result = fc.showSaveDialog(Main.getFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                String filePath = f.getAbsolutePath();
                if(!filePath.endsWith(".csv")) {
                    f = new File(filePath + ".csv");
                }

                if (f.exists()) {
                    int overwrite = JOptionPane.showConfirmDialog(
                            mainPanel,
                            String.format(Main.resourceBundle.getString("text.overwrite"), f.getName()),
                            Main.resourceBundle.getString("text.warning"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (overwrite != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                File ff = f;
                askCSVOptions(false, () -> parseCSVAndSaveData(ff));
            }
        });
        buttonMerge.addActionListener(actionEvent -> {
            File[] f = openExistingFile(false);
            if (f != null) {
                askCSVOptions(true, () -> {
                    verboseParseCSVAndMergeData(f[0]);

                    String names = Main.properties.getProperty("files.last", "");
                    try {
                        String path = f[0].getCanonicalPath();
                        if (names.isEmpty()) {
                            Main.properties.setProperty("files.last", path);
                        } else if (!names.contains(path)) {
                            Main.properties.setProperty("files.last", names + ";" + path);
                        }
                    } catch (Exception ignored) {

                    }
                });
            }
        });
        buttonRemoveColumn.addActionListener(actionEvent -> {
            AATableModel oldModel = (AATableModel)mainTable.getModel();
            if (oldModel.getWidth() > 2) {
                AATableModel newModel = new AATableModel(oldModel.getWidth() - 2,
                        oldModel.getHeight() - 2,
                        oldModel,
                        -1, mainTable.getSelectedColumn());

                mainTable.setModel(newModel);

                for (int i = 0, width = newModel.getWidth(); i < width; i++) {
                    mainTable.getColumnModel().getColumn(i).setCellRenderer(new AATableCellRenderer(newModel.getHeight()));
                }
            }
        });
        buttonSettings.addActionListener(e -> SettingsDialog.showSettings());
        buttonDataBase.addActionListener(actionEvent -> {
            String[] instr = ((AATableModel)mainTable.getModel()).getInstrumentsOnly();
            AATableModel newModel = FilterDB.showFilter(instr);
            if (newModel != null) {
                setNewModel(newModel);
                String[] newInstr = newModel.getInstrumentsOnly();
                Main.properties.setProperty("files.last", "base:" + String.join(";", newInstr));
            }
        });
    }

    private void askCSVOptions(boolean askDates, Runnable after) {
        String[] options = CSVOptions.showOptions(savedOptions, askDates);
        if (options != null) {
            savedOptions = options;
            after.run();
        }
    }

    public void silentParseCSVAndMergeData(File f) {
        parseCSVAndMergeData(f, false);
    }

    private void verboseParseCSVAndMergeData(File f) {
        parseCSVAndMergeData(f, true);
    }

    private void parseCSVAndMergeData(File f, boolean displayError) {
        try {
            AATableModel oldModel = (AATableModel)mainTable.getModel();
            AATableModel newModel = parseCSVAndLoadData(f);

            Zipper<String, Double, String> oldZ = oldModel.toZipper();
            Zipper<String, Double, String> newZ = newModel.toZipper();
            Zipper<String, Double, String> result = oldZ.zip(newZ, -1.0);

            AATableModel zippedModel = AATableModel.fromZipper(result, savedOptions);
            setNewModel(zippedModel);

        } catch (Exception e) {
            if (displayError) {
                JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void parseCSVAndLoadData(File f, String[] options) {
        savedOptions = options;
        try {
            AATableModel model = parseCSVAndLoadData(f);
            setNewModel(model);
        } catch (Exception ignored) {
        }
    }

    private AATableModel parseCSVAndLoadData(File f) throws Exception {
        List<String> columns = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<List<Double>> data = new ArrayList<>();

        String delim = savedOptions[0];
        String mark = savedOptions[1];
        String decimal = savedOptions[2];
        String dates = savedOptions[3];

        Main.properties.setProperty("import.delimiter", delim);
        Main.properties.setProperty("import.mark", mark);
        Main.properties.setProperty("import.decimal", decimal);
        Main.properties.setProperty("import.date", dates);

        BufferedReader is = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8));

        is.lines().forEach(line -> {
            String[] splitted = processText(line, delim, mark);
            if (columns.isEmpty()) {
                columns.addAll(Arrays.asList(Arrays.copyOfRange(splitted, 1, splitted.length)));
            } else {
                if (!line.isEmpty()) {
                    labels.add(splitted[0]);
                    String[] numbers = Arrays.copyOfRange(splitted, 1, splitted.length);
                    ArrayList<Double> parsed = new ArrayList<>(Arrays.stream(numbers).map(s -> s.replace(decimal, "."))
                            .map(s -> s.isEmpty() ? -1.0 : Double.valueOf(s)).collect(Collectors.toList()));

                    while (parsed.size() < columns.size()) {
                        parsed.add(-1.0);
                    }
                    data.add(parsed);
                }
            }
        });

        int htLength = data.size();
        int whLength = columns.size();
        double[][] rawData = new double[htLength][whLength];

        for (int ht = 0; ht < htLength; ht++) {
            for (int wh = 0; wh < whLength; wh++) {
                rawData[ht][wh] = data.get(ht).get(wh);
            }
        }

        return new AATableModel(whLength, htLength, rawData, labels.toArray(new String[0]), columns.toArray(new String[0]), "1".equals(dates));
    }

    private void setNewModel(AATableModel newModel) {
        mainTable.setModel(newModel);

        for (int i = 0, width = newModel.getWidth(); i < width; i++) {
            mainTable.getColumnModel().getColumn(i).setCellRenderer(new AATableCellRenderer(newModel.getHeight()));
        }
    }

    private void parseCSVAndSaveData(File f) {
        try {
            String delim = savedOptions[0];
            String mark = savedOptions[1];
            String decimal = savedOptions[2];
            String dates = savedOptions[3];

            Main.properties.setProperty("import.delimiter", delim);
            Main.properties.setProperty("import.mark", mark);
            Main.properties.setProperty("import.decimal", decimal);
            Main.properties.setProperty("import.date", dates);

            AATableModel model = (AATableModel)mainTable.getModel();

            BufferedWriter os = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8));

            os.write(
                    String.join(
                            delim,
                            Arrays.stream(model.getInstruments()).map(i -> mark + i + mark).collect(Collectors.toList())
                    )
            );
            os.newLine();

            int length = model.getPeriods().length;

            for (int i = 0; i < length; i++) {
                os.write(mark);
                os.write(model.formatPeriod(i));
                os.write(mark);
                os.write(delim);
                os.write(
                        String.join(
                                delim,
                                Arrays.stream(model.getData()[i])
                                        .boxed()
                                        .map(d -> d >= 0 ? String.valueOf(d) : "")
                                        .map(s -> s.replace(".", decimal))
                                        .collect(Collectors.toList())
                                )
                );
                os.newLine();
            }
            os.newLine();
            os.flush();
            os.close();

            Main.properties.setProperty("files.last", f.getCanonicalPath());
            lastFileName = f.getCanonicalPath();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(Main.getFrame(), e, Main.resourceBundle.getString("text.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private String[] processText(String text, String delim, String mark) {
        List<String> result = new LinkedList<>();
        boolean inText = false;
        int length = text.length();
        String mark2 = mark + mark;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < length) {
            String current = text.substring(i, i + 1);

            if (current.equals(mark)) {
                if (i < length - 1 && text.substring(i, i + 2).equals(mark2)) {
                    sb.append(mark);
                    i += 2;
                } else {
                    inText = !inText;
                    i += 1;
                }
                continue;
            }

            if (current.equals(delim)) {
                if (!inText) {
                    result.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(current);
                }
            } else {
                sb.append(current);
            }

            i += 1;
        }

        if (sb.length() > 0) {
            result.add(sb.toString());
        }

        return result.toArray(new String[0]);
    }

    public void getTickersAndLoadData(String[] tickers, String[] options) {
        savedOptions = options;
        setNewModel(ConvertOptions.notShowOptions(tickers, new InstrumentsMeta("db/meta/db_instruments.csv")));
    }

    private void createUIComponents() {
        mainTable = new JTable(new AATableModel());
        mainTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private File[] openExistingFile(boolean enableMultiSelect) {
        JFileChooser fc = new JFileChooser(".");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(enableMultiSelect);
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().toLowerCase().endsWith(".csv");
            }

            @Override
            public String getDescription() {
                return Main.resourceBundle.getString("text.csv_extension");
            }
        });

        int result = fc.showOpenDialog(Main.getFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] fs = fc.getSelectedFiles();
            if (fs.length == 0) {
                fs = new File[] { fc.getSelectedFile() };
            }
            return Arrays.stream(fs).allMatch(File::exists) ? fs : null;
        }

        return null;
    }

    public JPanel GetMainPanel() {
        return mainPanel;
    }

    public String[] getPeriods() {
        return ((AATableModel)mainTable.getModel()).formatPeriods();
    }
}
