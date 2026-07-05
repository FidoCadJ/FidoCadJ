package fidocadj.circuit.controllers;

import fidocadj.circuit.model.DrawingModel;
import fidocadj.geom.MapCoordinates;
import fidocadj.layers.StandardLayers;
import fidocadj.primitives.GraphicPrimitive;
import fidocadj.primitives.PrimitiveComplexCurve;
import fidocadj.primitives.PrimitivePolygon;
import fidocadj.primitives.PrimitiveAdvText;
import fidocadj.primitives.PrimitiveMacro;
import fidocadj.graphic.GraphicsInterface;
import fidocadj.toolbars.ChangeSelectionListener;

/** ElementsEdtActions: contains a controller for adding/modifying elements
    to a drawing model.

    In the jargon of this file "editing primitive" means the one which is
    currently being entered if an editing action is in place. For example, if
    the user wants to introduce a new macro, it will be the new macro which
    is shown in green, following the mouse pointer.

<pre>
    This file is part of FidoCadJ.

    FidoCadJ is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FidoCadJ is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FidoCadJ. If not,
    @see <a href=http://www.gnu.org/licenses/>http://www.gnu.org/licenses/</a>.

    Copyright 2014-2026 by Davide Bucci
</pre>

    @author Davide Bucci
*/

public class ElementsEdtActions
{
    protected final DrawingModel dmp;
    protected final UndoActions ua;
    protected final EditorActions edt;
    final SelectionActions sa;
    final AddElements ae;
    private ChangeSelectionListener selectionListener;

    // The current layer being edited
    public int currentLayer;

    // Array used to keep track of the insertion of elements which require
    // more than one click (logical coordinates). Index begins at 1 to
    // clickNumber.
    public float[] xpoly;
    public float[] ypoly;

    // used when entering a macro
    public String macroKey;

    // Nuber of clicks done when entering an object.
    public int clickNumber;

    // The primitive being edited
    public transient GraphicPrimitive primEdit;

    // editing action being done
    public int actionSelected;

    // Track wether an editing action is being made.
    public boolean successiveMove;

    // TO IMPROVE: this must be synchronized with the value in PrimitivePolygon
    // Maximum number of polygon vertices
    public static final int NPOLY=256;

    // Selection states
    public static final int NONE = 0;
    public static final int SELECTION = 1;
    public static final int ZOOM = 2;
    public static final int HAND = 3;
    public static final int LINE = 4;
    public static final int TEXT = 5;
    public static final int BEZIER = 6;
    public static final int POLYGON = 7;
    public static final int ELLIPSE = 8;
    public static final int RECTANGLE = 9;
    public static final int CONNECTION = 10;
    public static final int PCB_LINE = 11;
    public static final int PCB_PAD = 12;
    public static final int MACRO = 13;
    public static final int COMPLEXCURVE = 14;
    public static final int MOVE = 12;

    protected PrimitivesParInterface primitivesParListener;

    /** Standard constructor: provide the database class.
        @param pp the Model containing the database.
        @param s the selection controller.
        @param u the Undo controller, to ease undo operations.
        @param e the Basic editing controller, for handling selection
            operations.
    */
    public ElementsEdtActions (DrawingModel pp, SelectionActions s,
        UndoActions u,
        EditorActions e)
    {
        dmp=pp;
        ua=u;
        ae=new AddElements(dmp,ua);
        edt=e;
        sa=s;
        xpoly = new float[NPOLY];
        ypoly = new float[NPOLY];
        currentLayer=0;

        primEdit = null;
        selectionListener=null;
        primitivesParListener=null;

        actionSelected = SELECTION;
    }

    /** Set the change selection listener. The selection listener is not called
        when the selection state is changed manually by means of the
        setActionSelected method, but it is instead when it is internally
        changed by the ElementEdtActions class (such as with a mouse
        operation).
        @param c the new selection listener.
    */
    public void setChangeSelectionListener(ChangeSelectionListener c)
    {
        selectionListener=c;
    }

    /** Get the current {@link AddElements} controller.
        @return the current controller.
    */
    public AddElements getAddElements()
    {
        return ae;
    }

    /** Set the listener for showing popups and editing actions which are
        platform-dependent.
        @param l the listener to be employed.
    */
    public void setPrimitivesParListener(PrimitivesParInterface l)
    {
        primitivesParListener=l;
    }

    /** Determine wether the current primitive being added is a macro.
        @return true if the current primitive (i.e. the one who is in green
        under the mouse cursor) is a macro.
    */
    public boolean isEnteringMacro()
    {
        return primEdit instanceof PrimitiveMacro;
    }

    /** Chooses the entering state.
        @param s the new state to be set.
        @param macro the current macro key if a applicable, which means that
            a macro is being entered.
    */
    public void setState(int s, String macro)
    {
        actionSelected=s;
        clickNumber=0;
        successiveMove=false;
        macroKey=macro;
    }

    /** Rotate the macro being edited around its first control point
        (90 degrees clockwise rotation).
    */
    public void rotateMacro()
    {
        if(primEdit instanceof PrimitiveMacro) {
            primEdit.rotatePrimitive(false,
                primEdit.getFirstPoint().x, primEdit.getFirstPoint().y);
        }
    }

    /** Mirror the macro being edited around the x coordinate of the first
        control point.
    */
    public void mirrorMacro()
    {
        if(primEdit instanceof PrimitiveMacro) {
            primEdit.mirrorPrimitive(primEdit.getFirstPoint().x);
        }
    }

    /** Here we analyze and handle the mouse click. The behaviour is
        different depending on which selection state we are.
        @param cs the current coordinate mapping
        @param x the x coordinate of the click (in screen coordinates)
        @param y the y coordinate of the click (in screen coordinates)
        @param button3 true if the alternate button has been pressed
        @param toggle if true, circle the selection state or activate alternate
            input method (i.e. ellipses are forced to be circles, rectangles
            squares and so on...)
        @param doubleClick true if a double click has to be processed

        @return true if a repaint is needed.
    */
    public boolean handleClick(MapCoordinates cs,
        int x, int y, boolean button3, boolean toggle,
        boolean doubleClick)
    {
        boolean repaint=false;

        if(clickNumber>NPOLY-1) {
            clickNumber=NPOLY-1;
        }

        // We need to differentiate this case since when we are entering a
        // macro, primEdit already contains some useful hints about the
        // orientation and the mirroring, so we need to keep it.
        if (actionSelected !=MACRO) {
            primEdit = null;
        }

        if(button3 && actionSelected==MACRO) {
            actionSelected=SELECTION;
            if(selectionListener!=null) {
                selectionListener.setSelectionState(actionSelected,"");
            }
            primEdit = null;
            return true;
        }

        // Right-click in certain cases shows the parameters dialog.
        if(button3 &&
            actionSelected!=NONE &&
            actionSelected!=SELECTION &&
            actionSelected!=ZOOM &&
            actionSelected!=TEXT &&
            primitivesParListener!=null)
        {
            primitivesParListener.selectAndSetProperties(x,y);
            return false;
        }

        switch(actionSelected) {
            // No action: ignore
            case NONE:
                clickNumber = 0;
                break;

            // Selection state
            case SELECTION:
                clickNumber = 0;
                // Double click shows the Parameters dialog.
                if(doubleClick&&primitivesParListener!=null) {
                    primitivesParListener.setPropertiesForPrimitive();
                    break;
                } else if(button3 && primitivesParListener!=null) {
                    // Show a pop up menu if the user does a right-click
                    primitivesParListener.showPopUpMenu(x,y);
                } else {
                    // Select elements
                    edt.handleSelection(cs, x, y, toggle);
                }
                break;

            // Zoom state
            case ZOOM:
                if(primitivesParListener!=null) {
                    primitivesParListener.changeZoomByStep(!button3, x,y,1.5);
                }
                break;

            // Put a connection (easy: just one click is needed)
            case CONNECTION:
                ae.addConnection(cs.unmapXsnap(x),cs.unmapXsnap(y),
                    currentLayer);
                repaint=true;
                break;

            // Put a PCB pad (easy: just one click is needed)
            case PCB_PAD:
                // Add a PCB pad primitive at the given point
                ae.addPCBPad(cs.unmapXsnap(x), cs.unmapYsnap(y), currentLayer);
                repaint=true;
                break;

            // Add a line: two clicks needed
            case LINE:
                if (doubleClick) {
                    clickNumber=0;
                } else {
                    successiveMove=false;
                    clickNumber=ae.addLine(cs.unmapXsnap(x),
                        cs.unmapYsnap(y),
                        xpoly,
                        ypoly,
                        currentLayer,
                        ++clickNumber,
                        button3);
                    repaint=true;
                }
                break;

            // Add a text line: just one click is needed
            case TEXT:
                if (doubleClick && primitivesParListener!=null) {
                    primitivesParListener.selectAndSetProperties(x,y);
                    break;
                }
                PrimitiveAdvText newtext =
                    new PrimitiveAdvText(cs.unmapXsnap(x),
                                        cs.unmapYsnap(y),
                                        3,4,dmp.getTextFont(),0,0,
                                        "String", currentLayer);
                sa.setSelectionAll(false);
                dmp.addPrimitive(newtext, true, ua);
                newtext.setSelected(true);
                repaint=true;
                if(primitivesParListener!=null) {
                    boolean ret =
                            primitivesParListener.setPropertiesForPrimitive();

                    if (ret == false) {
                        // remove the text if user don't confirm
                        dmp.getPrimitiveVector().remove(newtext);
                    }
                }
                break;

            // Add a Bézier polygonal curve: we need four clicks.
            case BEZIER:
                repaint=true;
                if(button3) {
                    clickNumber = 0;
                } else {
                    if(doubleClick) { successiveMove=false; }
                    clickNumber=ae.addBezier(cs.unmapXsnap(x),
                                cs.unmapYsnap(y), xpoly, ypoly,
                                currentLayer, ++clickNumber);
                }
                break;

            // Insert a polygon: continue until double click.
            case POLYGON:
                // a polygon definition is ended with a double click
                if (doubleClick) {
                    PrimitivePolygon poly=new PrimitivePolygon(false,
                                         currentLayer,0,
                                         dmp.getTextFont(),
                                         dmp.getTextFontSize());
                    for(int i=1; i<=clickNumber; ++i) {
                        poly.addPoint(xpoly[i],ypoly[i]);
                    }

                    dmp.addPrimitive(poly, true,ua);
                    clickNumber = 0;
                    repaint=true;
                    break;
                } else {
                    ++ clickNumber;
                    successiveMove=false;
                    // clickNumber == 0 means that no polygon is being drawn
                    // prevent that we exceed the number of allowed points
                    if (clickNumber==NPOLY) {
                        return false;
                    }
                    xpoly[clickNumber] = cs.unmapXsnap(x);
                    ypoly[clickNumber] = cs.unmapYsnap(y);
                }
                break;

            // Insert a complex curve: continue until double click.
            case COMPLEXCURVE:
                // a polygon definition is ended with a double click
                if (doubleClick) {
                    PrimitiveComplexCurve compc=new PrimitiveComplexCurve(false,
                                        false,
                                        currentLayer,
                                        false, false, 0, 3, 2, 0,
                                        dmp.getTextFont(),
                                        dmp.getTextFontSize());
                    for(int i=1; i<=clickNumber; ++i) {
                        compc.addPoint(xpoly[i],ypoly[i]);
                    }

                    dmp.addPrimitive(compc, true,ua);
                    clickNumber = 0;
                    repaint=true;
                } else {
                    ++ clickNumber;
                    successiveMove=false;
                    // prevent that we exceed the number of allowed points
                    if (clickNumber==NPOLY) {
                        return false;
                    }
                    // clickNumber == 0 means that no polygon is being drawn
                    xpoly[clickNumber] = cs.unmapXsnap(x);
                    ypoly[clickNumber] = cs.unmapYsnap(y);
                }
                break;

            // Enter an ellipse: two clicks needed
            case ELLIPSE:
                // If control is hold, trace a circle
                successiveMove=false;

                clickNumber=ae.addEllipse(cs.unmapXsnap(x), cs.unmapYsnap(y),
                    xpoly, ypoly, currentLayer,
                    ++clickNumber,
                    toggle&&clickNumber>0);
                repaint=true;
                break;

            // Enter a rectangle: two clicks needed
            case RECTANGLE:
                // If control is hold, trace a square
                successiveMove=false;
                clickNumber=ae.addRectangle(cs.unmapXsnap(x), cs.unmapYsnap(y),
                    xpoly, ypoly,
                    currentLayer,
                    ++clickNumber,
                    toggle&&clickNumber>0);
                repaint=true;
                break;

            // Insert a PCB line: two clicks needed.
            case PCB_LINE:
                if (doubleClick) {
                    clickNumber = 0;
                    break;
                }
                successiveMove=false;

                clickNumber = ae.addPCBLine(cs.unmapXsnap(x), cs.unmapYsnap(y),
                    xpoly, ypoly, currentLayer,
                    ++clickNumber,
                    button3,
                    ae.getPcbThickness());
                repaint=true;
                break;

            // Enter a macro: just one click is needed.
            case MACRO:
                successiveMove=false;
                primEdit=ae.addMacro(cs.unmapXsnap(x), cs.unmapYsnap(y),
                    sa, primEdit, macroKey);
                repaint=true;
                break;
            default:
                break;
        }

        return repaint;
    }

    /** Draws the current editing primitive.
        @param g the graphic context on which to draw.
        @param cs the current coordinate mapping system.
    */
    public void drawPrimEdit(GraphicsInterface g, MapCoordinates cs)
    {
        if(primEdit!=null) {
            primEdit.draw(g, cs, StandardLayers.createEditingLayerArray());
        }
    }

    /** Shows the clicks done by the user.
        @param g the graphic context where one should write.
        @param cs the current coordinate mapping.
    */
    public void showClicks(GraphicsInterface g, MapCoordinates cs)
    {
        int x;
        int y;
        g.setColor(g.getColor().red());
        // The data here begins at index 1, due to the internal construction.

        int mult=Math.round(g.getScreenDensity()/112);
        g.applyStroke(2.0f*mult,0);

        for(int i=1; i<=clickNumber; ++i) {
            x = cs.mapXi(xpoly[i], ypoly[i], false);
            y = cs.mapYi(xpoly[i], ypoly[i], false);
            g.drawLine(x-15*mult, y, x+15*mult, y);
            g.drawLine(x, y-15*mult, x, y+15*mult);
        }
    }

    /** Get the current editing action (see the constants defined in this
        class)

        @return the current editing action.
    */
    public int getSelectionState()
    {
        return actionSelected;
    }

    /** Set the current editing primitive.
        @param gp the current editing primitive.
    */
    public void setPrimEdit(GraphicPrimitive gp)
    {
        primEdit=gp;
    }

    /** Get the current editing primitive.
        @return the current editing primitive.
    */
    public GraphicPrimitive getPrimEdit()
    {
        return primEdit;
    }
}