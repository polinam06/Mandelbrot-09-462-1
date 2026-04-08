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

    public void addSelectListener(SelectListener listener){
        selectHandlers.add(listener);
    }

    public void removeSelectListener(SelectListener listener){
        selectHandlers.remove(listener);
    }

    public SelectablePanel(Painter painter, Converter converter) {
        super(painter);
        this.converter = converter;
        g = getGraphics();

        // Обработчик для левой кнопки мыши (выделение)
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);

                if (e.getButton() == MouseEvent.BUTTON1) {
                    rect = new SelectedRect(e.getX(), e.getY());
                    paintSelectedRect();
                }

                else if (e.getButton() == MouseEvent.BUTTON3) {
                    rightButtonStartPos = new Point(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);

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
                }

                else if (e.getButton() == MouseEvent.BUTTON3 && rightButtonStartPos != null) {
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
                super.mouseDragged(e);

                if (SwingUtilities.isLeftMouseButton(e)) {
                    paintSelectedRect();
                    if (rect != null){
                        rect.setLastPoint(e.getX(), e.getY());
                    }
                    paintSelectedRect();
                }
                else if (SwingUtilities.isRightMouseButton(e) && rightButtonStartPos != null) {
                }
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                g = getGraphics();
            }
        });
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