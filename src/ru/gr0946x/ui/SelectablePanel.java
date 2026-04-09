package ru.gr0946x.ui;

import ru.gr0946x.Converter;
import ru.gr0946x.ui.painting.Painter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class SelectablePanel extends PaintPanel{
    private SelectedRect rect = null;
    private Graphics g;

    private Point rightButtonStartPos = null;
    private final Converter converter;

    private final ArrayList<SelectListener> selectHandlers = new ArrayList<>();

    private final double origXMin;
    private final double origXMax;
    private final double origYMin;
    private final double origYMax;

    private double currentXMin;
    private double currentXMax;
    private double currentYMin;
    private double currentYMax;

    public void addSelectListener(SelectListener listener){
        selectHandlers.add(listener);
    }

    public void removeSelectListener(SelectListener listener){
        selectHandlers.remove(listener);
    }

    public SelectablePanel(Painter painter, Converter converter) {
        super(painter);
        this.converter = converter;
        this.origXMin = converter.getXMin();
        this.origXMax = converter.getXMax();
        this.origYMin = converter.getYMin();
        this.origYMax = converter.getYMax();

        this.currentXMin = origXMin;
        this.currentXMax = origXMax;
        this.currentYMin = origYMin;
        this.currentYMax = origYMax;

        g = getGraphics();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    rect = new SelectedRect(e.getX(), e.getY());
                    paintSelectedRect();
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    rightButtonStartPos = new Point(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    paintSelectedRect();
                    if (rect != null) {
                        for (var handler : selectHandlers) {
                            handler.onSelect(new Rectangle(
                                    rect.getUpperLeft().x,
                                    rect.getUpperLeft().y,
                                    rect.getWidth(),
                                    rect.getHeight()
                            ));
                        }
                        rect = null;
                    }
                } else if (e.getButton() == MouseEvent.BUTTON3 && rightButtonStartPos != null) {
                    int deltaX = e.getX() - rightButtonStartPos.x;
                    int deltaY = e.getY() - rightButtonStartPos.y;
                    if (deltaX != 0 || deltaY != 0) {
                        shiftFractal(deltaX, deltaY);
                    }
                    rightButtonStartPos = null;
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    paintSelectedRect();
                    if (rect != null){
                        rect.setLastPoint(e.getX(), e.getY());
                    }
                    paintSelectedRect();
                }
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                adjustBoundsForAspectRatio();
                g = getGraphics();
            }
        });
    }

    private void adjustBoundsForAspectRatio() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        double currentW = currentXMax - currentXMin;
        double currentH = currentYMax - currentYMin;
        double currentRatio = currentW / currentH;

        double panelRatio = (double) width / height;

        double newXMin, newXMax, newYMin, newYMax;

        if (panelRatio > currentRatio) {
            double newH = currentH;
            double newW = newH * panelRatio;
            double offsetX = (newW - currentW) / 2.0;
            newXMin = currentXMin - offsetX;
            newXMax = currentXMax + offsetX;
            newYMin = currentYMin;
            newYMax = currentYMax;
        } else {
            double newW = currentW;
            double newH = newW / panelRatio;
            double offsetY = (newH - currentH) / 2.0;
            newXMin = currentXMin;
            newXMax = currentXMax;
            newYMin = currentYMin - offsetY;
            newYMax = currentYMax + offsetY;
        }

        converter.setXShape(newXMin, newXMax);
        converter.setYShape(newYMin, newYMax);
        converter.setWidth(width);
        converter.setHeight(height);

        currentXMin = newXMin;
        currentXMax = newXMax;
        currentYMin = newYMin;
        currentYMax = newYMax;
    }

    public void applyZoom(double xMin, double xMax, double yMin, double yMax) {
        converter.setXShape(xMin, xMax);
        converter.setYShape(yMin, yMax);
        currentXMin = xMin;
        currentXMax = xMax;
        currentYMin = yMin;
        currentYMax = yMax;
        adjustBoundsForAspectRatio();
        repaint();
    }

    private void shiftFractal(int deltaX, int deltaY) {
        double xMin = converter.xScr2Crt(0);
        double xMax = converter.xScr2Crt(getWidth());
        double yMin = converter.yScr2Crt(getHeight());
        double yMax = converter.yScr2Crt(0);
        double width = xMax - xMin;
        double height = yMax - yMin;

        double shiftX = (deltaX * width) / getWidth();
        double shiftY = (deltaY * height) / getHeight();
        converter.setXShape(xMin - shiftX, xMax - shiftX);
        converter.setYShape(yMin + shiftY, yMax + shiftY);

        currentXMin = xMin - shiftX;
        currentXMax = xMax - shiftX;
        currentYMin = yMin + shiftY;
        currentYMax = yMax + shiftY;

        repaint();
    }

    private void paintSelectedRect(){
        if (g != null && rect != null){
            g.setXORMode(Color.WHITE);
            g.setColor(Color.BLACK);
            g.drawRect(
                    rect.getUpperLeft().x,
                    rect.getUpperLeft().y,
                    rect.getWidth(),
                    rect.getHeight()
            );
            g.setPaintMode();
        }
    }
}