package ru.gr0946x.ui;

import ru.gr0946x.Converter;
import ru.gr0946x.ui.fractals.Fractal;
import ru.gr0946x.ui.fractals.Mandelbrot;
import ru.gr0946x.ui.painting.FractalPainter;
import ru.gr0946x.ui.painting.Painter;
import ru.smak.math.Complex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.lang.Math.*;

public class MainWindow extends JFrame {

    private final SelectablePanel mainPanel;
    private final Painter painter;
    private final Fractal mandelbrot;
    private final Converter conv;
    private final MenuManager menuManager;

    private Point mousePressPoint = null;

    public MainWindow() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 650));
        setTitle("Фрактал Множество Мандельброта");

        mandelbrot = new Mandelbrot();
        conv = new Converter(-2.0, 1.0, -1.0, 1.0);
        painter = new FractalPainter(mandelbrot, conv, (value) -> {
            if (value == 1.0) return Color.BLACK;
            var r = (float) abs(sin(5 * value));
            var g = (float) abs(cos(8 * value) * sin(3 * value));
            var b = (float) abs((sin(7 * value) + cos(15 * value)) / 2f);
            return new Color(r, g, b);
        });

        mainPanel = new SelectablePanel(painter, conv);
        mainPanel.setBackground(Color.WHITE);

        mainPanel.addSelectListener((r) -> {
            var xMin = conv.xScr2Crt(r.x);
            var xMax = conv.xScr2Crt(r.x + r.width);
            var yMin = conv.yScr2Crt(r.y + r.height);
            var yMax = conv.yScr2Crt(r.y);
            mainPanel.applyZoom(xMin, xMax, yMin, yMax);
        });

        // 🔍 Отслеживание клика для открытия окна Жюлиа
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    mousePressPoint = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && mousePressPoint != null) {
                    double dist = e.getPoint().distance(mousePressPoint);
                    if (dist < 5) { // Клик (выделение не производилось)
                        double cX = conv.xScr2Crt(e.getX());
                        double cY = conv.yScr2Crt(e.getY());
                        Complex c = new Complex(cX, cY);

                        SwingUtilities.invokeLater(() -> {
                            JuliaWindow juliaWindow = new JuliaWindow(c, "Множество Жюлиа");
                            juliaWindow.setSize(800, 650);
                            juliaWindow.setLocationRelativeTo(MainWindow.this);
                            juliaWindow.setVisible(true);
                        });
                    }
                    mousePressPoint = null;
                }
            }
        });

        menuManager = new MenuManager((FractalPainter) painter);
        setJMenuBar(menuManager.createMenuBar());

        setContent();

        SwingUtilities.invokeLater(() -> mainPanel.repaint());
    }

    private void setContent() {
        var gl = new GroupLayout(getContentPane());
        setLayout(gl);
        gl.setVerticalGroup(gl.createSequentialGroup()
                .addGap(8)
                .addComponent(mainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
                .addGap(8)
        );
        gl.setHorizontalGroup(gl.createSequentialGroup()
                .addGap(8)
                .addComponent(mainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
                .addGap(8)
        );
    }
}