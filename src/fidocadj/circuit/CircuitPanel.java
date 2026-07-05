package fidocadj.circuit;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import fidocadj.dialogs.controls.LayerInfo;
import fidocadj.dialogs.controls.TypedParameter;
import fidocadj.dialogs.controls.ParameterDescription;
import fidocadj.dialogs.DialogParameters;
import fidocadj.primitives.GraphicPrimitive;
import fidocadj.timer.MyTimer;
import fidocadj.toolbars.ChangeSelectionListener;
import fidocadj.toolbars.ChangeZoomListener;
import fidocadj.toolbars.ChangeGridState;
import fidocadj.toolbars.ChangeSelectedLayer;
import fidocadj.circuit.controllers.EditorActions;
import fidocadj.circuit.controllers.CopyPasteActions;
import fidocadj.circuit.controllers.ParserActions;
import fidocadj.circuit.controllers.UndoActions;
import fidocadj.circuit.controllers.ContinuosMoveActions;
import fidocadj.circuit.controllers.SelectionActions;
import fidocadj.circuit.controllers.PrimitivesParInterface;
import fidocadj.circuit.controllers.ElementsEdtActions;
import fidocadj.circuit.controllers.HandleActions;
import fidocadj.circuit.model.DrawingModel;
import fidocadj.circuit.views.Drawing;
import fidocadj.clipboard.TextTransfer;
import fidocadj.graphic.PointG;
import fidocadj.graphic.DimensionG;
import fidocadj.graphic.swing.Graphics2DSwing;
import fidocadj.graphic.swing.ColorSwing;
import fidocadj.geom.MapCoordinates;
import fidocadj.geom.DrawingSize;
import fidocadj.geom.ChangeCoordinatesListener;
import fidocadj.globals.Globals;
import fidocadj.graphic.FontG;
import fidocadj.primitives.PrimitiveAdvText;

/** Circuit panel: draw the circuit inside this panel. This is one of the most
 * important components, as it is responsible of all editing actions.
 * In many ways, this class contains the most important component of
 * FidoCadJ.
 * This class is able to perform its profiling, which is in particular
 * the measurement of the time needed to draw the circuit.
 *
 * <pre>
 * This file is part of FidoCadJ.
 *
 * FidoCadJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FidoCadJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FidoCadJ. If not,
 * @see <a href=http://www.gnu.org/licenses/>http://www.gnu.org/licenses/</a>.
 *
 * Copyright 2007-2026 by Davide Bucci
 * </pre>
 *
 * @author Davide Bucci
 */
public class CircuitPanel extends JPanel implements ChangeSelectedLayer,
                                                    ChangeGridState,
                                                    ChangeZoomListener,
                                                    ChangeSelectionListener,
                                                    PrimitivesParInterface
{
    private static final long serialVersionUID = 1L;

    // *********** DRAWING ***********
    Graphics2DSwing graphicSwing;

    // Coordinate system to be used.
    private transient MapCoordinates mapCoordinates;

    // Use anti alias in drawings
    private boolean antiAlias;

    // Draw the grid
    private boolean isGridVisible;

    // Selection direction
    private boolean isLeftToRight;

    // Default background color
    private Color backgroundColor;

    // Right-to-left selection rectangle color
    private Color rightToLeftColor;

    // Left-to-right selection rectangle color
    private Color leftToRightColor;

    // Default grid dots color
    private ColorSwing gridDotsColor;

    // Default grid lines color
    private ColorSwing gridLinesColor;

    // Use key for zoom
    private boolean enableKeyForZoom;

    public CircuitPanel()
    {
        this.ruler = null;
    }


    // Position of the rectangle used for the selection
    private Rectangle evidenceRect;

    // Margin size in pixels when calculating component sizes.
    public static final int MARGIN = 20;

    // Color of elements during editing
    static final ColorSwing editingColor = new ColorSwing(Color.green);

    private Color selectionColor = editingColor.getColorSwing();

    private transient DrawingModel drawingModel;

    // Scrolling pane data
    public JScrollPane father;
    private static final int MINSIZEX = 1000;
    private static final int MINSIZEY = 1000;

    // Views:
    public Drawing drawingAgent;

    // Controllers:
    private EditorActions editorActions;
    private CopyPasteActions copyPasteActions;
    private ParserActions parserActions;
    private UndoActions undoActions;
    private ContinuosMoveActions continuosMoveActions;
    private HandleActions handleActions;
    private SelectionActions selectionActions;

    // ********** PROFILING **********
    // Specify that the profiling mode should be activated.
    public boolean profileTime;
    private double average;

    // Record time for the redrawing.
    private double record;

    // Number of times the redraw has occourred.
    private double runs;

    // ********** INTERFACE **********
    // If this variable is different from null, the component will ensure that
    // the corresponding rectangle will be shown in a scroll panel during the
    // next redraw.
    private Rectangle scrollRectangle;

    // Strict FidoCAD compatibility
    public boolean extStrict;

    // ********** RULER **********
    private final Ruler ruler;  // Is it to be drawn?

    // ********** INTERFACE ELEMENTS **********
    PopUpMenu popup;                    // Popup menu
    MouseWheelHandler mwHandler;        // Wheel handler
    MouseMoveClickHandler mmcHandler;   // Mouse click handler

    // ********** LISTENERS **********
    private ChangeZoomListener zoomListener;
    private ChangeSelectionListener selectionListener;
    private ChangeSelectionListener scrollGestureSelectionListener;

    /** Standard constructor
     *
     * @param isEditable indicates whether the panel should be responsible
     * to keyboard and mouse inputs.
     */
    public CircuitPanel(boolean isEditable)
    {
        backgroundColor = Color.white;
        gridDotsColor = new ColorSwing(Color.BLACK);
        gridLinesColor = new ColorSwing(Color.LIGHT_GRAY);

        setDrawingModel(new DrawingModel());

        mwHandler = new MouseWheelHandler(this);
        mmcHandler = new MouseMoveClickHandler(this);

        graphicSwing = new Graphics2DSwing();

        ruler = new Ruler(Color.GREEN,
                editingColor.getColorSwing().darker().darker());

        isGridVisible = true;
        zoomListener = null;
        antiAlias = true;
        record = 1e100;
        evidenceRect = new Rectangle(0, 0, -1, -1);

        isLeftToRight = false;

        // Set up the standard view settings:
        // top left corner, 400% zoom.
        mapCoordinates = new MapCoordinates();
        mapCoordinates.setXCenter(0.0);
        mapCoordinates.setYCenter(0.0);
        mapCoordinates.setXMagnitude(4.0);
        mapCoordinates.setYMagnitude(4.0);
        mapCoordinates.setOrientation(0);
        setOpaque(true);
        runs = 0;
        average = 0;

        // This is not useful when preparing the applet: the circuit panel will
        // not be editable in this case.
        if (isEditable) {
            addMouseListener(mmcHandler);
            addMouseMotionListener(mmcHandler);
            addKeyListener(mwHandler);
            setFocusable(true);

            //Create the popup menu.
            popup = new PopUpMenu(this);
            popup.registerActiveKeys();

            // Patchwork for bug#54.
            // When mouse pointer enters into CircuitPanel with macro,
            // grab focus from macropicker.
            // NOTE: MouseListener.mouseEntered doesn't works stable.
            addMouseMotionListener(new MouseMotionAdapter()
            {
                @Override
                public void mouseMoved(MouseEvent e)
                {
                    if (continuosMoveActions.isEnteringMacro()
                        && !isFocusOwner())
                    {
                        requestFocus();
                    }
                }
            });
        }
    }

    /** By implementing writeObject method,
     * // we can prevent
     * // subclass from serialization
     */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        throw new NotSerializableException();
    }

    /* By implementing readObject method,
     * // we can prevent
     * // subclass from de-serialization
     */
    private void readObject(ObjectInputStream in) throws IOException
    {
        throw new NotSerializableException();
    }

    /** Show a popup menu representing the actions that can be done on the
     * selected context.
     *
     * @param x the x coordinate where the popup menu should be put
     * @param y the y coordinate where the popup menu should be put
     */
    public void showPopUpMenu(int x, int y)
    {
        popup.showPopUpMenu(this, x, y);
    }

    /** Makes sure the object gets focus.
     */
    @Override
    public void getFocus()
    {
        requestFocusInWindow();
    }

    /** ChangeSelectionListener interface implementation
     *
     * @param s the selection state
     * @param macro the macro key (if applies)
     */
    @Override
    public void setSelectionState(int s, String macro)
    {
        if (selectionListener != null &&
                s != continuosMoveActions.actionSelected)
        {
            selectionListener.setSelectionState(s, macro);
            selectionListener.setStrictCompatibility(extStrict);
        }

        if (scrollGestureSelectionListener != null) {
            scrollGestureSelectionListener.setSelectionState(s,
                    macro);
        }
        continuosMoveActions.setState(s, macro);
        mmcHandler.selectCursor();
    }

    /**
     Set whether to use anti-aliasing in drawings.

     @param antiAlias true if anti-aliasing should be used, false otherwise.
     */
    public void setAntiAlias(boolean antiAlias)
    {
        this.antiAlias = antiAlias;
    }

    /**
     Check if anti-aliasing is used in drawings.

     @return true if anti-aliasing is used, false otherwise.
     */
    public boolean getAntiAlias()
    {
        return antiAlias;
    }

    /** Set the rectangle which will be shown during the next redraw.
     *
     * @param r the rectangle to show.
     */
    public void setScrollRectangle(Rectangle r)
    {
        scrollRectangle = r;
        repaint();
    }

    /** Define the listener to be called when the zoom is changed
     *
     * @param c the new zoom listener.
     */
    public void addChangeZoomListener(ChangeZoomListener c)
    {
        zoomListener = c;
    }

    /** Define the listener to be called when the selected action is changed
     *
     * @param c the new selection listener.
     */
    public void addChangeSelectionListener(ChangeSelectionListener c)
    {
        selectionListener = c;
        continuosMoveActions.setChangeSelectionListener(c);
    }

    /** Define the listener to be called when the selected action is changed
     * (this is explicitly done for the ScrollGestureSelection).
     *
     * @param c the new selection listener.
     */
    public void addScrollGestureSelectionListener(ChangeSelectionListener c)
    {
        scrollGestureSelectionListener = c;
    }

    /** Define the listener to be called when the coordinates of the mouse
        cursor are changed
        @param c the new coordinates listener
    */
    public void addChangeCoordinatesListener(ChangeCoordinatesListener c)
    {
        continuosMoveActions.addChangeCoordinatesListener(c);
        handleActions.addChangeCoordinatesListener(c);

    }
    /** Return the current editing layer.
     *
     * @return the index of the layer.
     */
    public int getCurrentLayer()
    {
        return continuosMoveActions.currentLayer;
    }

    /** Set the current editing layer.
     *
     * @param cl the wanted layer.
     */
    public void setCurrentLayer(int cl)
    {
        int l = cl;
        /* two little checks... */
        if (l < 0) {
            l = 0;
        }
        if (l >= drawingModel.getLayers().size()) {
            l = drawingModel.getLayers().size() - 1;
        }

        continuosMoveActions.currentLayer = l;
    }

    /** Change the current layer state. Change the layer of all selected
     * primitives.
     *
     * @param s the layer to be selected.
     */
    @Override
    public void changeSelectedLayer(int s)
    {
        // Change the current layer
        continuosMoveActions.currentLayer = s;
        // Change also the layer of all selected primitives
        if (editorActions.setLayerForSelectedPrimitives(s)) {
            repaint();
        }
    }

    /** The method which is called when the current grid visibility
     * has to be changed.
     *
     * @param v is the wanted grid visibility state.
     */
    @Override
    public void setGridVisibility(boolean v)
    {
        isGridVisible = v;
        repaint();
    }

    /** Determines if the grid is visible or not.
     *
     * @return the grid visibility.
     */
    public boolean getGridVisibility()
    {
        return isGridVisible;
    }

    /** The method to be called when the current snap visibility
     * has changed.
     *
     * @param v is the wanted snap state.
     */
    @Override
    public void setSnapState(boolean v)
    {
        mapCoordinates.setSnap(v);
    }

    /** Determines if the grid is visible or not.
     *
     * @return the grid visibility.
     */
    public boolean getSnapState()
    {
        return mapCoordinates.getSnap();
    }

    /** Increase or decrease the zoom by a step of 33%.
     *
     * @param increase if true, increase the zoom, if false decrease.
     * @param x coordinate to which center the viewport (screen coordinates).
     * @param y coordinate to which center the viewport (screen coordinates).
     * @param rate the amount the zoom is multiplied (or divided). Should be
     * greater than 1.
     */
    @Override
    public void changeZoomByStep(boolean increase, int x, int y, double rate)
    {
        // Prevents possible exceptions in case it's null.
        if(father == null) {
            return;
        }

        float xpos = mapCoordinates.unmapXnosnap(x);
        float ypos = mapCoordinates.unmapYnosnap(y);
        double z = mapCoordinates.getXMagnitude();

        // Click+Meta reduces the zoom
        // Click raises the zoom
        double oldz = z;
        if (increase) {
            z = z * rate;
        } else {
            z = z / rate;
        }

        // Checking that reasonable limits are not exceeded.
        if (z > Globals.maxZoomFactor / 100) {
            z = Globals.maxZoomFactor / 100;
        }
        if (z < Globals.minZoomFactor / 100) {
            z = Globals.minZoomFactor / 100;
        }

        z = Math.round(z * 100.0) / 100.0;

        if (Math.abs(oldz - z) < 1e-5) {
            return;
        }

        mapCoordinates.setMagnitudes(z, z);

        // A little strong...
        int width = father.getViewport().getExtentSize().width;
        int height = father.getViewport().getExtentSize().height;

        Point rr = father.getViewport().getViewPosition();

        int corrx = x - rr.x;
        int corry = y - rr.y;

        Rectangle r = new Rectangle(
                mapCoordinates.mapXi(xpos, ypos, false) - corrx,
                mapCoordinates.mapYi(xpos, ypos, false) - corry,
                width, height);

        updateSizeOfScrollBars(r);
    }

    /** Calculate the size of the image and update the size of the
     * scroll bars, with the current zoom.
     *
     * @param r the Rectangle which will contain the new image size at the
     * end of this method.
     */
    public void updateSizeOfScrollBars(Rectangle r)
    {
        PointG origin = new PointG();
        DimensionG d = DrawingSize.getImageSize(drawingModel,
                mapCoordinates.getXMagnitude(),
                false, origin);

        int minx = mapCoordinates.mapXi(MINSIZEX, MINSIZEY, false);
        int miny = mapCoordinates.mapYi(MINSIZEX, MINSIZEY, false);

        Dimension dd = new Dimension(Math.max(d.width
                + MARGIN, minx), Math.max(d.height + MARGIN, miny));

        setPreferredSize(dd);
        if (r != null) {
            scrollRectangle = r;
        }
        revalidate();
        repaint();
    }

    /** Get the current editing action (see the constants defined in this
     * class).
     *
     * @return the current editing action.
     */
    @Override
    public int getSelectionState()
    {
        return continuosMoveActions.getSelectionState();
    }

    /** The zoom listener.
     *
     * @param tz the zoom factor to be used.
     */
    @Override
    public void changeZoom(double tz)
    {
        double z = Math.round(tz * 100.0) / 100.0;
        mapCoordinates.setMagnitudes(z, z);
        continuosMoveActions.successiveMove = false;
        requestFocusInWindow(); // #
        repaint();
    }

    /** Sets the background color.
     *
     * @param sfondo the background color to be used.
     */
    @Override
    public void setBackground(Color sfondo)
    {
        backgroundColor = sfondo;
    }

    /**
     Sets the dots grid color.

     @param color the the dots grid color to be used.
     */
    public void setDotsGridColor(Color color)
    {
        gridDotsColor = new ColorSwing(color);
    }

    /**
     Sets the current color of selected item

     @param color the color to be set.
     */
    public void setSelectedColor(Color color)
    {
        graphicSwing.setSelectedColor(color);
    }

    /**
     Sets Right-to-left selection rectangle color

     @param color the selection rectangle color to be used.
     */
    public void setRightToLeftColor(Color color)
    {
        rightToLeftColor = color;
    }

    /**
     Sets Left-to-right selection rectangle color

     @param color the selection rectangle color to be used.
     */
    public void setLeftToRightColor(Color color)
    {
        leftToRightColor = color;
    }

    /**
     Sets the lines grid color.

     @param color the lines grid colo to be used.
     */
    public void setLinesGridColor(Color color)
    {
        gridLinesColor = new ColorSwing(color);
    }

    /** Activate and sets an evidence rectangle which will be put on screen
     * at the next redraw. All sizes are given in pixel.
     *
     * @param lx the x coordinate of the left top corner.
     * @param ly the y coordinate of the left top corner.
     * @param w the width of the rectangle.
     * @param h the height of the rectangle.
     */
    @Override
    public void setEvidenceRect(int lx, int ly, int w, int h)
    {
        evidenceRect = new Rectangle();
        evidenceRect.x = lx;
        evidenceRect.y = ly;
        evidenceRect.height = h;
        evidenceRect.width = w;
    }

    /** Repaint the panel.
     * This method performs the following operations:<br>
     * 1. set the anti aliasing on (or off, depending on antiAlias).<br>
     * 2. paint in white the background, draw the bk. image and the grid.<br>
     * 3. call drawingAgent draw.<br>
     * 4. draw all active handles.<br>
     * 5. if needed, draw the primitive being edited.<br>
     * 6. draw the ruler, if needed.<br>
     * 7. if requested, print information about redraw speed.<br>
     *
     * @param g the graphic context on which perform the drawing operations.
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        MyTimer mt;
        mt = new MyTimer();

        Graphics2D g2 = (Graphics2D) g;
        graphicSwing.setGraphicContext(g2);
        activateDrawingSettings(g2);

        // Draw the background.
        g.setColor(backgroundColor);
        g.fillRect(0, 0, getWidth(), getHeight());

        drawingModel.getImgCanvas().drawCanvasImage(g2, mapCoordinates);
        // Draw the grid if necessary.
        if (isGridVisible) {
            graphicSwing.drawGrid(mapCoordinates, 0, 0,
                    getWidth(), getHeight(), gridDotsColor, gridLinesColor);
        }

        // The standard color is black.
        g.setColor(Color.black);
        // This is important for taking into account the dashing size
        graphicSwing.setZoom(mapCoordinates.getXMagnitude());

        // Draw all the elements of the drawing.
        drawingAgent.draw(graphicSwing, mapCoordinates);
        drawingModel.getImgCanvas().trackExtremePoints(mapCoordinates);

        if (zoomListener != null) {
            zoomListener.changeZoom(mapCoordinates.getXMagnitude());
        }

        // Draw the handles of all selected primitives.
        drawingAgent.drawSelectedHandles(graphicSwing, mapCoordinates);

        if (this.isLeftToRight) {
            this.selectionColor = leftToRightColor;
            float dash1[] = {3.0f};
            BasicStroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT,
                                                 BasicStroke.JOIN_MITER,
                                                 10.0f, dash1, 0.0f);
            g2.setStroke(dashed);
        } else {
            this.selectionColor = rightToLeftColor;
            g2.setStroke(new BasicStroke(1));
        }
        // If an evidence rectangle is active, draw it.
        if (evidenceRect != null
                && continuosMoveActions.actionSelected ==
                ElementsEdtActions.SELECTION)
        {
            g.setColor(selectionColor);
            g.drawRect(evidenceRect.x, evidenceRect.y,
                    evidenceRect.width, evidenceRect.height);
        } else {
            evidenceRect = null;
        }

        // If there is a primitive or a macro being edited, draws it.
        continuosMoveActions.drawPrimEdit(graphicSwing, mapCoordinates);

        // If a ruler.isActive() is active, draw it.
        ruler.drawRuler(g, mapCoordinates);

        setSizeIfNeeded();

        if (scrollRectangle != null) {
            Rectangle r = scrollRectangle;
            scrollRectangle = null;
            scrollRectToVisible(r);
        }
        // Since the redraw speed is a capital parameter which determines the
        // perceived speed, we monitor it very carefully if the program
        // profiling is active.

        if (profileTime) {
            double elapsed = mt.getElapsed();
            g2.drawString("Version: "
                    + Globals.version, 0, 100);
            g.drawString("Time elapsed: "
                    + elapsed + " ms", 0, 50);
            ++runs;
            average += elapsed;
            if (elapsed < record) {
                record = elapsed;
            }
            System.out.println("R: Time elapsed: "
                    + elapsed
                    + " averaging "
                    + average / runs
                    + "ms in " + runs
                    + " redraws; record: " + record + " ms");
        }
    }

    /** Activate or deactivate anti-aliasing if necessary.
     */
    private void activateDrawingSettings(Graphics2D g2)
    {
        if (antiAlias) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        } else {
            // Faster graphic (??? true??? I do not think so on modern systems)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_SPEED);
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_DITHERING,
                    RenderingHints.VALUE_DITHER_DISABLE);
        }
    }

    /** Set the new size of the drawing, if needed.
     */
    private void setSizeIfNeeded()
    {
        Dimension d = new Dimension(mapCoordinates.getXMax(),
                mapCoordinates.getYMax());

        if (d.width > 0 && d.height > 0) {
            int minx = mapCoordinates.mapXi(MINSIZEX, MINSIZEY, false);
            int miny = mapCoordinates.mapYi(MINSIZEX, MINSIZEY, false);

            Dimension dd = new Dimension(Math.max(d.width
                    + MARGIN, minx), Math.max(d.height + MARGIN, miny));
            Dimension nn = getPreferredSize();

            if (dd.width != nn.width || dd.height != nn.height) {
                setPreferredSize(dd);
                revalidate();
            }
        }
    }

    /** Update the current size of the object, given the current size of the
     * drawing.
     */
    @Override
    public void validate()
    {
        int minx = mapCoordinates.mapXi(MINSIZEX, MINSIZEY, false);
        int miny = mapCoordinates.mapYi(MINSIZEX, MINSIZEY, false);

        Dimension dd = new Dimension(Math.max(mapCoordinates.getXMax()
                + MARGIN, minx), Math.max(mapCoordinates.getYMax() + MARGIN,
                        miny));
        Dimension nn = getPreferredSize();

        if (dd.width != nn.width || dd.height != nn.height) {
            setPreferredSize(dd);
        }

        super.validate();
    }

    /** Get the current drawing model.
     *
     * @return the drawing model.
     */
    public final DrawingModel getDrawingModel()
    {
        return drawingModel;
    }

    /** Set the current drawing model. Changing it mean that all the
     * controllers and views associated to the panel will be updated.
     *
     * @param dm the drawing model.
     */
    public final void setDrawingModel(DrawingModel dm)
    {
        drawingModel = dm;
        selectionActions = new SelectionActions(drawingModel);
        parserActions = new ParserActions(drawingModel);
        undoActions = new UndoActions(parserActions);
        editorActions = new EditorActions(drawingModel, selectionActions,
                undoActions);
        continuosMoveActions = new ContinuosMoveActions(drawingModel,
                selectionActions, undoActions, editorActions);

        handleActions = new HandleActions(getDrawingModel(), getEditorActions(),
            getSelectionActions(), getUndoActions());
        drawingAgent = new Drawing(drawingModel);
        continuosMoveActions.setPrimitivesParListener(this);
        copyPasteActions = new CopyPasteActions(drawingModel, editorActions,
                selectionActions, parserActions,
                undoActions, new TextTransfer());
    }

    /** Get the current instance of SelectionActions controller class
     *
     * @return the class
     */
    public SelectionActions getSelectionActions()
    {
        return selectionActions;
    }

    /** Get the current instance of EditorActions controller class
     *
     * @return the class
     */
    public EditorActions getEditorActions()
    {
        return editorActions;
    }

    /** Get the current instance of CopyPasteActions controller class
     *
     * @return the class
     */
    public CopyPasteActions getCopyPasteActions()
    {
        return copyPasteActions;
    }

    /** Get the current instance of ParserActions controller class
     *
     * @return the class
     */
    public ParserActions getParserActions()
    {
        return parserActions;
    }

    /** Get the current instance of UndoActions controller class
     *
     * @return the class
     */
    public UndoActions getUndoActions()
    {
        return undoActions;
    }

    /** Get the current instance of ElementsEdtActions controller class
     *
     * @return the class
     */
    public ContinuosMoveActions getContinuosMoveActions()
    {
        return continuosMoveActions;
    }


    /** Get the current instance of HandleActions controller class
     *
     * @return the class
     */
    public HandleActions getHandleActions()
    {
        return handleActions;
    }

    /**
     NOTE: This method is not optimized and needs to be revised to work in a
     more dynamic manner. Achieving this will require a thorough review and
     potentially significant refactoring of the entire parameter management
     system.

     Shows a dialog which allows the user to modify the parameters of a given
     primitive. If only one primitive is selected, all its parameters can be
     modified. If multiple primitives are selected, only the common parameters
     such as the layer can be modified for all selected primitives. If multiple
     texts are selected, shared text-specific parameters can be edited
     collectively.
     */
    @Override
    public boolean setPropertiesForPrimitive()
    {
        GraphicPrimitive gp = selectionActions.getFirstSelectedPrimitive();
        if (gp == null) {
            return false;
        }

        java.util.List<PrimitiveAdvText> selectedAdvText = new ArrayList<>();
        java.util.List<ParameterDescription> v;
        boolean isMultipleTextSelected = false;

        String strParamMisc =
            "** " + Globals.messages.getString("param_misc") + " **";

        java.util.Map<String, TypedParameter> commonParameters
                                        = new java.util.LinkedHashMap<>();

        if (selectionActions.isUniquePrimitiveSelected()) {
            v = gp.getControls();
        } else {
            java.util.List<GraphicPrimitive> selectedPrimitives
                            = selectionActions.getSelectedPrimitives();

            for (GraphicPrimitive atxt : selectedPrimitives) {
                if (atxt instanceof PrimitiveAdvText) {
                    selectedAdvText.add((PrimitiveAdvText) atxt);
                }
            }

            v = new java.util.Vector<>(1);

            if (selectedPrimitives.size() == selectedAdvText.size()) {
                isMultipleTextSelected = true;
                boolean isFirst = true;

                for (PrimitiveAdvText t : selectedAdvText) {
                    java.util.List<ParameterDescription> controls
                                                         = t.getControls();

                    if (isFirst) {
                        for (ParameterDescription param : controls) {
                            commonParameters.put(param.description,
                                    new TypedParameter(param.parameter,
                                            param.parameter));
                        }
                        isFirst = false;
                    } else {
                        for (ParameterDescription param : controls) {
                            String key = param.description;
                            Object value = param.parameter;
                            TypedParameter existing
                                           = commonParameters.get(key);

                            if (existing != null) {
                                if (!(value instanceof LayerInfo)
                                        && !(value instanceof FontG)
                                        && !(value instanceof Boolean))
                                {
                                    if (!value.equals(
                                            existing.getOriginalValue()))
                                    {
                                        existing.setDisplayValue(strParamMisc);
                                    }
                                }
                            }
                        }
                    }
                }

                v = new java.util.Vector<>();
                for (ParameterDescription control
                     : selectedAdvText.get(0).getControls()) {
                    String key = control.description;
                    TypedParameter typedParam = commonParameters.get(key);

                    ParameterDescription paramDesc
                                         = new ParameterDescription();
                    paramDesc.description = key;
                    paramDesc.parameter = typedParam.getDisplayValue();
                    // Maintain the correct type

                    v.add(paramDesc);
                }
            } else {
                v = new Vector<>(1);
                ParameterDescription pd = new ParameterDescription();
                pd.parameter = new LayerInfo(gp.getLayer());
                pd.description = Globals.messages.getString("ctrl_layer");
                v.add(pd);
            }
        }

        DialogParameters dp = new DialogParameters(
                (JFrame) Globals.activeWindow,
                v, extStrict,
                drawingModel.getLayers());
        dp.setVisible(true);

        if (dp.active) {
            if (selectionActions.isUniquePrimitiveSelected()) {
                gp.setControls(dp.getCharacteristics());
            } else {
                if (isMultipleTextSelected) {
                    java.util.Map<String, Object> updatedValues
                                    = new java.util.LinkedHashMap<>();
                    for (ParameterDescription param : dp.getCharacteristics()) {
                        // Retrieve the TypedParameter to apply the correct
                        // values
                        TypedParameter originalParam
                            = commonParameters.get(param.description);

                        Object finalValue;
                        if (originalParam != null) {
                            if (strParamMisc.equals(param.parameter)) {
                                // Do not modify the value,
                                // leave it as "strParamMisc"
                                finalValue = strParamMisc;
                            } else {
                                // If the parameter has been modified,
                                // convert it to the original type
                                if (originalParam.getOriginalValue()
                                        instanceof Integer)
                                {
                                    try {
                                        finalValue = Integer.parseInt(
                                                param.parameter.toString());
                                        // Convert to Integer
                                    } catch (NumberFormatException e) {
                                        // Handle the error if conversion fails
                                        System.err.println(
                                            "Failed to convert the value to "
                                                + "Integer: " +
                                                 param.parameter);
                                        finalValue = originalParam
                                                .getOriginalValue();
                                        // Fallback to the original value
                                    }
                                } else {
                                    // Keep the new value if no specific
                                    // conversion is required
                                    finalValue = param.parameter;
                                }
                            }
                            updatedValues.put(param.description, finalValue);
                        }
                    }
                    // Apply the updated values to the selected PrimitiveAdvText
                    // objects
                    for (PrimitiveAdvText t : selectedAdvText) {
                        java.util.List<ParameterDescription> originalControls
                                                             = t.getControls();

                        java.util.List<ParameterDescription> newControls
                                       = new java.util.ArrayList<>();

                        for (ParameterDescription originalParam
                             : originalControls) {
                            String key = originalParam.description;
                            Object newValue = updatedValues.get(key);

                            ParameterDescription newParam
                                                 = new ParameterDescription();
                            newParam.description = key;

                            // Apply only if the new value is not "strParamMisc"
                            if (newValue != null
                                    && !strParamMisc.equals(newValue))
                            {
                                newParam.parameter = newValue;
                            } else {
                                // Keep the original value
                                newParam.parameter = originalParam.parameter;
                            }

                            newControls.add(newParam);
                        }
                        t.setControls(newControls);
                    }
                } else {
                    ParameterDescription pd
                                         = v.get(0);
                    if (pd.parameter instanceof LayerInfo layerInfo) {
                        int l = layerInfo.getLayer();
                        editorActions.setLayerForSelectedPrimitives(l);
                    } else {
                        System.out.println(
                                "Warning: unexpected parameter! (layer)");
                    }
                }
            }
            drawingModel.setChanged(true);

            // We need to check and sort the layers, since the user can
            // change the layer associated with a given primitive thanks to
            // the dialog window which has been shown.
            drawingModel.sortPrimitiveLayers();
            undoActions.saveUndoState();
            repaint();
            return true;
        }
        return false;
    }

    /** Selects the closest object to the given point (in logical coordinates)
     * and pops up a dialog for the editing of its Param_opt.
     *
     * @param x the x logical coordinate of the point used for the selection.
     * @param y the y logical coordinate of the point used for the selection.
     */
    @Override
    public void selectAndSetProperties(int x, int y)
    {
        selectionActions.setSelectionAll(false);
        editorActions.handleSelection(mapCoordinates, x, y, false);
        repaint();
        setPropertiesForPrimitive();
    }

    /** Checks if FidoCadJ should strictly comply with the FidoCAD
     * format (and limitations).
     *
     * @return the compliance mode.
     */
    public boolean getStrictCompatibility()
    {
        return extStrict;
    }

    /** Set if the strict FidoCAD compatibility mode is active.
     *
     * @param strict true if the compatibility with FidoCAD should be
     * obtained.
     */
    @Override
    public void setStrictCompatibility(boolean strict)
    {
        extStrict = strict;
    }

    /** Change the current coordinate mapping.
     *
     * @param m the new coordinate mapping to be adopted.
     */
    public void setMapCoordinates(MapCoordinates m)
    {
        mapCoordinates = m;
        // Force an in-depth redraw.
        drawingModel.setChanged(true);
    }

    /** Get the current coordinate mapping.
     *
     * @return the current coordinate mapping.
     */
    public MapCoordinates getMapCoordinates()
    {
        return mapCoordinates;
    }

    /** Force a repaint.
     */
    @Override
    public void forcesRepaint()
    {
        repaint();
    }

    /** Force a repaint.
     *
     * @param x the x leftmost corner of the dirty region to repaint.
     * @param y the y leftmost corner of the dirty region to repaint.
     * @param width the width of the dirty region.
     * @param height the height of the dirty region.
     */
    public void forcesRepaint(int x, int y, int width, int height)
    {
        repaint(x, y, width, height);
    }

    /** Get the Ruler object.
     *
     * @return the Ruler object.
     */
    public Ruler getRuler()
    {
        return ruler;
    }

    /** Check if the profiling is active.
     *
     * @return true if the profiling is active.
     */
    public boolean isProfiling()
    {
        return profileTime;
    }

    /** Get the attached image as background.
     *
     * @return the attached image object.
     */
    public ImageAsCanvas getAttachedImage()
    {
        return drawingModel.getImgCanvas();
    }

    /** Determine the direction of the selection.

        @param isLeftToRight True if the direction is from left to right..
                             False if it is from right to left.
     */
    @Override
    public void isLeftToRightSelection(boolean isLeftToRight)
    {
        this.isLeftToRight = isLeftToRight;
        // Force the redraw when the selection direction changes.
        this.repaint();
    }

    /**
     Checks if there are any primitives with negative coordinates in ..
     their virtual points.
     If such primitives are found, translates all primitives in the drawing ..
     so that the negative coordinates are shifted to positive,
     keeping their relative distances.

     @return true if a translation was necessary and performed, false otherwise.
     */
    public boolean normalizeCoordinates()
    {
        DrawingModel model = getDrawingModel();
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;

        final int gridStep = getMapCoordinates().getXGridStep();

        final int MARGIN_GRID_CELLS = 2;
        final int MARGIN = MARGIN_GRID_CELLS * gridStep;

        // Find the minimum (most negative) ..
        // x and y coordinates among all primitives
        for (GraphicPrimitive gp : model.getPrimitiveVector()) {
            for (PointG point : gp.virtualPoint) {
                if (point.x < minX) {
                    minX = point.x;
                }
                if (point.y < minY) {
                    minY = point.y;
                }
            }
        }

        // If minimum x or y coordinates are negative,
        // calculate the necessary translation
        if (minX < 0 || minY < 0) {
            float rawDeltaX = minX < 0 ? -minX + MARGIN : MARGIN;
            float rawDeltaY = minY < 0 ? -minY + MARGIN : MARGIN;

            float deltaX = ((rawDeltaX + gridStep - 1) / gridStep) * gridStep;
            float deltaY = ((rawDeltaY + gridStep - 1) / gridStep) * gridStep;

            // Apply the translation to all primitives
            for (GraphicPrimitive gp : model.getPrimitiveVector()) {
                gp.movePrimitive(deltaX, deltaY);
            }
            // Return true indicating that a translation was performed
            return true;
        }
        // No translation was necessary, return false
        return false;
    }

    /**
     Checks if there are any primitives where all virtual points ..
     have negative coordinates.

     @return true if there exists at least one primitive where all ..
     virtual points have negative coordinates, false otherwise.
     */
    public boolean checkGhostPrimitives()
    {
        DrawingModel model = getDrawingModel();

        // Iterate over all primitives in the model
        for (GraphicPrimitive gp : model.getPrimitiveVector()) {
            boolean allNegative = true;

            // Check each virtual point of the primitive
            for (PointG point : gp.virtualPoint) {
                // If any point is non-negative, set allNegative to false
                if (point.x >= 0 && point.y >= 0) {
                    allNegative = false;
                    break; // No need to check further, move to next primitive
                }
            }

            // If all points in this primitive are negative, return true
            if (allNegative) {
                return true;
            }
        }
        // No primitives found where all points are negative
        return false;
    }

    /**
     * @return the enableKeyForZoom
     */
    public boolean isEnabledKeyForZoom()
    {
        return enableKeyForZoom;
    }

    /**
     * Sets whether zoom with mouse wheel requires holding Ctrl key.
     * When this setting changes, the mouse wheel listener state is ..
     * updated accordingly.
     *
     * @param enableKeyForZoom true to enable wheel zoom without Ctrl key,
     *                         false to require Ctrl key for wheel zoom
     */
    public void setEnableKeyForZoom(boolean enableKeyForZoom)
    {
        this.enableKeyForZoom = enableKeyForZoom;

        // Notify the MouseWheelHandler about the configuration change
        // so it can add/remove the listener as appropriate
        if (mwHandler != null) {
            mwHandler.updateListenerState();
        }
    }
}
