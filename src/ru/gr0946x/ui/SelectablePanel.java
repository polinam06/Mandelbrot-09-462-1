package ru.gr0946x.ui;

import ru.gr0946x.Converter;
import ru.gr0946x.ui.painting.Painter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

public class SelectablePanel extends PaintPanel {
    private SelectedRect rect = null;
    private Graphics g;

    private Point rightButtonStartPos = null;
    private Point rightButtonCurrentPos = null;
    private boolean isRightDragging = false;

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

    private double currentWidth;
    private double currentHeight;

    private static final int MAX_HISTORY_STEPS = 100;

    private final Deque<ViewState> undoHistory = new ArrayDeque<>();
    private final Deque<ViewState> redoHistory = new ArrayDeque<>();

    private static class ViewState {
        double xMin;
        double xMax;
        double yMin;
        double yMax;
        double width;
        double height;

        ViewState(double xMin, double xMax, double yMin, double yMax, double width, double height) {
            this.xMin = xMin;
            this.xMax = xMax;
            this.yMin = yMin;
            this.yMax = yMax;
            this.width = width;
            this.height = height;
        }
    }

    public void addSelectListener(SelectListener listener) {
        selectHandlers.add(listener);
    }

    public void removeSelectListener(SelectListener listener) {
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
        this.currentWidth = currentXMax - currentXMin;
        this.currentHeight = currentYMax - currentYMin;

        g = getGraphics();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    rect = new SelectedRect(e.getX(), e.getY());
                    paintSelectedRect();
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    rightButtonStartPos = new Point(e.getX(), e.getY());
                    rightButtonCurrentPos = new Point(e.getX(), e.getY());
                    isRightDragging = true;
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
                    isRightDragging = false;

                    if (g != null && rightButtonStartPos != null && rightButtonCurrentPos != null) {
                        g.setXORMode(Color.BLACK);
                        g.setColor(Color.WHITE);
                        g.drawLine(
                                rightButtonStartPos.x,
                                rightButtonStartPos.y,
                                rightButtonCurrentPos.x,
                                rightButtonCurrentPos.y
                        );
                        g.setPaintMode();
                    }

                    int deltaX = e.getX() - rightButtonStartPos.x;
                    int deltaY = e.getY() - rightButtonStartPos.y;

                    if (deltaX != 0 || deltaY != 0) {
                        shiftFractal(deltaX, deltaY);
                    }

                    rightButtonStartPos = null;
                    rightButtonCurrentPos = null;
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    paintSelectedRect();
                    if (rect != null) {
                        rect.setLastPoint(e.getX(), e.getY());
                    }
                    paintSelectedRect();
                } else if (SwingUtilities.isRightMouseButton(e) && isRightDragging) {
                    if (g != null && rightButtonStartPos != null && rightButtonCurrentPos != null) {
                        g.setXORMode(Color.BLACK);
                        g.setColor(Color.WHITE);
                        g.drawLine(
                                rightButtonStartPos.x,
                                rightButtonStartPos.y,
                                rightButtonCurrentPos.x,
                                rightButtonCurrentPos.y
                        );
                        g.setPaintMode();
                    }

                    rightButtonCurrentPos = new Point(e.getX(), e.getY());

                    if (g != null && rightButtonStartPos != null) {
                        g.setXORMode(Color.BLACK);
                        g.setColor(Color.WHITE);
                        g.drawLine(
                                rightButtonStartPos.x,
                                rightButtonStartPos.y,
                                rightButtonCurrentPos.x,
                                rightButtonCurrentPos.y
                        );
                        g.setPaintMode();
                    }
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

    private ViewState createCurrentState() {
        return new ViewState(
                currentXMin,
                currentXMax,
                currentYMin,
                currentYMax,
                currentWidth,
                currentHeight
        );
    }

    private void pushWithLimit(Deque<ViewState> history, ViewState state) {
        if (history.size() >= MAX_HISTORY_STEPS) {
            history.removeFirst();
        }
        history.addLast(state);
    }

    private void saveStateForUndo() {
        pushWithLimit(undoHistory, createCurrentState());
        redoHistory.clear();
    }

    private void restoreState(ViewState state) {
        currentXMin = state.xMin;
        currentXMax = state.xMax;
        currentYMin = state.yMin;
        currentYMax = state.yMax;
        currentWidth = state.width;
        currentHeight = state.height;

        converter.setXShape(currentXMin, currentXMax);
        converter.setYShape(currentYMin, currentYMax);

        adjustBoundsForAspectRatio();
        repaint();
    }

    public boolean canUndo() {
        return !undoHistory.isEmpty();
    }

    public boolean canRedo() {
        return !redoHistory.isEmpty();
    }

    public void undo() {
        if (!canUndo()) {
            return;
        }

        pushWithLimit(redoHistory, createCurrentState());
        ViewState previousState = undoHistory.removeLast();
        restoreState(previousState);
    }

    public void redo() {
        if (!canRedo()) {
            return;
        }

        pushWithLimit(undoHistory, createCurrentState());
        ViewState nextState = redoHistory.removeLast();
        restoreState(nextState);
    }

    private void adjustBoundsForAspectRatio() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        double centerX = (currentXMin + currentXMax) / 2.0;
        double centerY = (currentYMin + currentYMax) / 2.0;

        double newWidth = currentWidth;
        double newHeight = currentHeight;

        double fractalRatio = newWidth / newHeight;
        double panelRatio = (double) width / height;

        double newXMin, newXMax, newYMin, newYMax;

        if (panelRatio > fractalRatio) {
            double scaledHeight = newHeight;
            double scaledWidth = scaledHeight * panelRatio;

            double halfScaledWidth = scaledWidth / 2.0;
            newXMin = centerX - halfScaledWidth;
            newXMax = centerX + halfScaledWidth;
            newYMin = centerY - newHeight / 2.0;
            newYMax = centerY + newHeight / 2.0;
        } else {
            double scaledWidth = newWidth;
            double scaledHeight = scaledWidth / panelRatio;

            double halfScaledHeight = scaledHeight / 2.0;
            newXMin = centerX - newWidth / 2.0;
            newXMax = centerX + newWidth / 2.0;
            newYMin = centerY - halfScaledHeight;
            newYMax = centerY + halfScaledHeight;
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
        saveStateForUndo();

        converter.setXShape(xMin, xMax);
        converter.setYShape(yMin, yMax);

        currentXMin = xMin;
        currentXMax = xMax;
        currentYMin = yMin;
        currentYMax = yMax;
        currentWidth = xMax - xMin;
        currentHeight = yMax - yMin;

        adjustBoundsForAspectRatio();
        repaint();
    }

    private void shiftFractal(int deltaX, int deltaY) {
        saveStateForUndo();

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

    private void paintSelectedRect() {
        if (g != null && rect != null) {
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
    public void setPainter(Painter p) {
        this.painter = p;

        if (p instanceof ru.gr0946x.ui.painting.FractalPainter) {
            p.setWidth(getWidth());
            p.setHeight(getHeight());
        }
        repaint();
    }


}