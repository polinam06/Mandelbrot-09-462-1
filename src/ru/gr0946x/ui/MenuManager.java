package ru.gr0946x.ui;
import ru.gr0946x.ui.painting.FractalPainter;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Color;
import ru.gr0946x.ui.fractals.FractalConfig;

public class MenuManager {
    private final FractalPainter painter;
    private final SelectablePanel panel;

    public MenuManager(FractalPainter painter, SelectablePanel panel) {
        this.painter = painter;
        this.panel = panel;
    }
    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Файл");

        JMenuItem saveItem = new JMenuItem("Сохранить как...");
        saveItem.addActionListener(e -> saveImageWithChoice());
        fileMenu.add(saveItem);

        JMenuItem saveFracItem = new JMenuItem("Сохранить как FRAC...");
        saveFracItem.addActionListener(this::showNotImplementedMessage);
        fileMenu.add(saveFracItem);

        fileMenu.addSeparator();

        JMenuItem openFracItem = new JMenuItem("Открыть FRAC...");
        openFracItem.addActionListener(this::showNotImplementedMessage);
        fileMenu.add(openFracItem);

        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Правка");

        JMenuItem undoItem = new JMenuItem("Отменить");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undoItem.addActionListener(e -> panel.undo());
        editMenu.add(undoItem);

        JMenuItem redoItem = new JMenuItem("Повторить");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        redoItem.addActionListener(e -> panel.redo());
        editMenu.add(redoItem);

        menuBar.add(editMenu);

        JMenu viewMenu = new JMenu("Вид");
        JMenu formulaMenu = new JMenu("Формулы для построения");
        ButtonGroup formulaGroup = new ButtonGroup();

        for (int i = 0; i < FractalConfig.FRACTAL_NAMES.size(); i++) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(FractalConfig.FRACTAL_NAMES.get(i));
            final int idx = i;

            item.addActionListener(e -> {
                if (item.isSelected()) {
                    mainWindow.applySettings(idx, mainWindow.getCurrentColorIdx());
                }
            });

            formulaGroup.add(item);
            formulaMenu.add(item);
        }
        if (formulaGroup.getButtonCount() > 0) {
            formulaGroup.getElements().nextElement().setSelected(true);
        }

        JMenu colorSchemeMenu = new JMenu("Цветовая схема");
        ButtonGroup schemeGroup = new ButtonGroup();

        for (int i = 0; i < FractalConfig.COLOR_NAMES.size(); i++) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(FractalConfig.COLOR_NAMES.get(i));
            final int idx = i;

            item.addActionListener(e -> {
                if (item.isSelected()) {
                    mainWindow.applySettings(mainWindow.getCurrentFractalIdx(), idx);
                }
            });

            schemeGroup.add(item);
            colorSchemeMenu.add(item);
        }
        if (schemeGroup.getButtonCount() > 0) {
            schemeGroup.getElements().nextElement().setSelected(true);
        }

        viewMenu.add(formulaMenu);
        viewMenu.add(colorSchemeMenu);
        menuBar.add(viewMenu);

        JMenu fractalMenu = new JMenu("Фрактал");

        JMenuItem tourItem = new JMenuItem("Экскурсия по фракталу");
        tourItem.addActionListener(this::showNotImplementedMessage);
        fractalMenu.add(tourItem);

        menuBar.add(fractalMenu);

        return menuBar;
    }

    private void showNotImplementedMessage(ActionEvent e) {
        JMenuItem source = (JMenuItem) e.getSource();
        JOptionPane.showMessageDialog(
                null,
                "Функция \"" + source.getText() + "\" будет реализована позже",
                "Информация",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
    private void saveImage(String format) {
        System.out.println("SAVE CLICKED " + format);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Сохранить изображение");

        FileNameExtensionFilter jpgFilter = new FileNameExtensionFilter("JPEG Image (*.jpg, *.jpeg)", "jpg", "jpeg");
        FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG Image (*.png)", "png");

        chooser.addChoosableFileFilter(jpgFilter);
        chooser.addChoosableFileFilter(pngFilter);

        if ("jpg".equals(format)) {
            chooser.setFileFilter(jpgFilter);
        } else if ("png".equals(format)) {
            chooser.setFileFilter(pngFilter);
        }

        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();

            String selectedFormat;
            if (chooser.getFileFilter() == jpgFilter) {
                selectedFormat = "jpg";
            } else if (chooser.getFileFilter() == pngFilter) {
                selectedFormat = "png";
            } else {
                selectedFormat = format;
            }

            String filePath = file.getPath();

            String lowerPath = filePath.toLowerCase();
            if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
                filePath = filePath.substring(0, lowerPath.lastIndexOf('.'));
            } else if (lowerPath.endsWith(".png")) {
                filePath = filePath.substring(0, lowerPath.lastIndexOf('.'));
            }

            file = new File(filePath + "." + selectedFormat);

            if (file.exists()) {
                int result = JOptionPane.showConfirmDialog(
                        null,
                        "Файл \"" + file.getName() + "\" уже существует.\nЗаменить его?",
                        "Подтверждение перезаписи",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try {
                BufferedImage img = painter.createImage();

                Graphics2D g = img.createGraphics();
                g.setColor(Color.WHITE);
                g.drawString(
                        String.format("Re: [%.3f; %.3f], Im: [%.3f; %.3f]",
                                painter.getConverter().getXMin(),
                                painter.getConverter().getXMax(),
                                painter.getConverter().getYMin(),
                                painter.getConverter().getYMax()
                        ),
                        10,
                        img.getHeight() - 10
                );
                g.dispose();

                ImageIO.write(img, selectedFormat, file);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Ошибка при сохранении файла:\n" + ex.getMessage(),
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    private void saveImageWithChoice() {
        saveImage(null);
    }
}