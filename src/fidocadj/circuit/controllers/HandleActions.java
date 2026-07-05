package fidocadj.circuit.controllers;

import java.util.*;

import fidocadj.circuit.model.DrawingModel;
import fidocadj.geom.MapCoordinates;
import fidocadj.geom.ChangeCoordinatesListener;
import fidocadj.graphic.RectangleG;
import fidocadj.layers.LayerDesc;
import fidocadj.primitives.GraphicPrimitive;
import fidocadj.primitives.PrimitiveRectangle;
import fidocadj.primitives.PrimitiveMacro;
import fidocadj.primitives.PrimitiveOval;
import fidocadj.globals.Globals;



/** CopyPasteActions: contains a controller which can perform handle drag and
    move actions on a primitive database

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

public class HandleActions
{
    private final DrawingModel drawingModel;
    private final UndoActions undoActions;
    private final SelectionActions selectionActions;

    // A coordinates listener (to show messages)
    private ChangeCoordinatesListener coordinatesListener=null;


    // ******** DRAG & INTERFACE *********

    // True if we are at the beginning of a dragging operation.
    private boolean firstDrag;

    // The graphic primitive being treated.
    private GraphicPrimitive primBeingDragged=null;
    // The handle of the active graphic primitive being treated.
    private int handleBeingDragged;

    // Old cursor position for handle drag.
    private int opx;
    private int opy;

    // True if the primitive has moved while dragging.
    private boolean hasMoved;

    // Other old cursor position for handle drag...
    private float oldpx;
    private float oldpy;

    private boolean isLeftToRightSelection;


    /** Standard constructor: provide the database class.
        @param pp the drawing model
        @param e the editor controller
        @param s the selection controller
        @param u the undo controller
    */
    public HandleActions (DrawingModel pp, EditorActions e,
        SelectionActions s,
        UndoActions u)
    {
        drawingModel=pp;
        undoActions=u;
        selectionActions=s;
        firstDrag=false;
        handleBeingDragged=GraphicPrimitive.NO_DRAG;
        isLeftToRightSelection = false;
    }
    /** Define the listener to be called to write messages (selection states,
        etc...).
        @param c the new coordinates listener
    */
    public void addChangeCoordinatesListener(ChangeCoordinatesListener c)
    {
        coordinatesListener=c;
    }

    /** Drag all the selected primitives during a drag operation.
        Position the primitives in the given (screen) position

        @param cc the element containing the drawing, which can receive
            repaint callbacks.
        @param px the x position (screen coordinates).
        @param py the y position (screen coordinates).
        @param cs the coordinate mapping.
    */
    public void dragPrimitives(PrimitivesParInterface cc, int px, int py,
            MapCoordinates cs)
    {
        // Check if we are effectively dragging the whole primitive...
        if (handleBeingDragged != GraphicPrimitive.DRAG_PRIMITIVE) {
            return;
        }

        firstDrag = false;

        float dx = cs.unmapXsnap(px) - oldpx;
        float dy = cs.unmapYsnap(py) - oldpy;

        oldpx = cs.unmapXsnap(px);
        oldpy = cs.unmapXsnap(py);

        if (dx == 0 && dy == 0) {
            return;
        }

        // First, check if all primitives can move without going out of bounds
        boolean canMoveAll = true;
        for (GraphicPrimitive g : drawingModel.getPrimitiveVector()) {
            if (g.isSelected()) {
                for (int j = 0; j < g.getControlPointNumber(); ++j) {
                    float newX = g.virtualPoint[j].x + dx;
                    float newY = g.virtualPoint[j].y + dy;

                    // If any point goes out of bounds,
                    // prevent the move for all
                    if (newX < 0 || newY < 0) {
                        canMoveAll = false;
                        break;
                    }
                }
                if (!canMoveAll) {
                    break;
                }
            }
        }

        // If all primitives can move, apply the move to all of them
        if (canMoveAll) {
            for (GraphicPrimitive g : drawingModel.getPrimitiveVector()) {
                if (g.isSelected()) {
                    for (int j = 0; j < g.getControlPointNumber(); ++j) {
                        g.virtualPoint[j].x += dx;
                        g.virtualPoint[j].y += dy;
                    }
                    g.setChanged(true);
                }
            }
        }

        cc.forcesRepaint();
    }

    /** Start dragging handle. Check if the pointer is on the handle of a
        primitive and if it is the case, enter the dragging state.
        @param px the (screen) x coordinate of the pointer.
        @param py the (screen) y coordinate of the pointer.
        @param tolerance the tolerance (screen. i.e. no of pixel).
        @param multiple specifies whether multiple selection is active.
        @param cs the coordinate mapping to be used.

    */
    public void dragHandleStart(int px, int py, int tolerance, boolean multiple,
        MapCoordinates cs)
    {
        int i;
        int isel=0;
        float mindistance=Float.MAX_VALUE;
        float distance=mindistance;
        int layer;

        hasMoved=false;

        GraphicPrimitive gp;
        List<LayerDesc> layerV=drawingModel.getLayers();

        oldpx=cs.unmapXnosnap(px);
        oldpy=cs.unmapXnosnap(py);

        firstDrag=true;

        float sptol=
            Math.abs(cs.unmapXnosnap(px+tolerance)-cs.unmapXnosnap(px));

        if (sptol<2) { sptol=2; }

        // Search for the closest primitive to the given point
        // Performs a cycle through all primitives and check their
        // distance.
        for (i=0; i<drawingModel.getPrimitiveVector().size(); ++i){
            gp=drawingModel.getPrimitiveVector().get(i);
            layer= gp.getLayer();

            // Does not allow for selecting an invisible or locked
            // primitive.
            if(layer<layerV.size() &&
                (!(layerV.get(layer)).isVisible() ||
                    (layerV.get(layer)).isLocked()) &&
                !(gp instanceof PrimitiveMacro))
            {
                continue;
            }

            if(gp.isSelected()){
                // Verify if the pointer is on a handle
                handleBeingDragged=gp.onHandle(cs, px, py);

                if(handleBeingDragged!=GraphicPrimitive.NO_DRAG) {
                    primBeingDragged=gp;
                    continue;
                }
            }
            distance=gp.getDistanceToPoint(oldpx,oldpy);
            if (distance<=mindistance) {
                isel=i;
                mindistance=distance;
            }
        }
        // Verify if the whole primitive should be drag
        if (mindistance<sptol && handleBeingDragged<0){
            primBeingDragged=
                drawingModel.getPrimitiveVector().get(isel);
            if (!multiple && !primBeingDragged.isSelected()) {
                selectionActions.setSelectionAll(false);
            }
            if(!multiple) {
                primBeingDragged.setSelected(true);
            }
            handleBeingDragged=GraphicPrimitive.DRAG_PRIMITIVE;
            firstDrag=true;
            oldpx=cs.unmapXsnap(px);
            oldpy=cs.unmapXsnap(py);
        } else if (handleBeingDragged<0) {
            // We want to select things in a rectangular area
            oldpx=cs.unmapXsnap(px);
            oldpy=cs.unmapXsnap(py);
            handleBeingDragged=GraphicPrimitive.RECT_SELECTION;
        }
    }

    /** End dragging handle.
        @param cC the editor object
        @param px the (screen) x coordinate of the pointer.
        @param py the (screen) y coordinate of the pointer.
        @param multiple specifies whether multiple selection is active.
        @param cs the coordinate mapping to be used.
    */
    public void dragHandleEnd(PrimitivesParInterface cC, int px, int py,
        boolean multiple,
        MapCoordinates cs)
    {
        // Check if we are effectively dragging something...
        cC.setEvidenceRect(0,0,-1,-1);
        if(handleBeingDragged<0){/*
            if(handleBeingDragged==GraphicPrimitive.RECT_SELECTION){
                int xa=Math.min(oldpx, cs.unmapXnosnap(px));
                int ya=Math.min(oldpy, cs.unmapYnosnap(py));
                int xb=Math.max(oldpx, cs.unmapXnosnap(px));
                int yb=Math.max(oldpy, cs.unmapYnosnap(py));
                if(!multiple) { selectionActions.setSelectionAll(false); }
                edt.selectRect(xa, ya, xb-xa, yb-ya);
            }*/

            if (handleBeingDragged == GraphicPrimitive.RECT_SELECTION) {
                float xa = Math.min(oldpx, cs.unmapXnosnap(px));
                float ya = Math.min(oldpy, cs.unmapYnosnap(py));
                float xb = Math.max(oldpx, cs.unmapXnosnap(px));
                float yb = Math.max(oldpy, cs.unmapYnosnap(py));

                RectangleG selectionRect = new RectangleG(xa, ya,
                    xb - xa, yb - ya);

                if (!multiple) {
                    selectionActions.setSelectionAll(false);
                }

                List<LayerDesc> layerV = drawingModel.getLayers();
                for (GraphicPrimitive g : drawingModel.getPrimitiveVector()) {
                    int layer = g.getLayer();
                    boolean selectable = layer>=layerV.size() ||
                        (layerV.get(layer).isVisible() &&
                            !layerV.get(layer).isLocked()) ||
                        g instanceof PrimitiveMacro;
                    if (selectable &&
                        g.intersects(selectionRect, isLeftToRightSelection))
                    {
                        g.setSelected(true);
                    }
                }
            }


            // Test if we are anyway dragging an entire primitive
            if(handleBeingDragged==GraphicPrimitive.DRAG_PRIMITIVE &&
                hasMoved && undoActions!=null)
            {
                undoActions.saveUndoState();
            }
            handleBeingDragged=GraphicPrimitive.NO_DRAG;
            return;
        }
        handleBeingDragged=GraphicPrimitive.NO_DRAG;
        if(undoActions!=null) { undoActions.saveUndoState(); }
    }

    /** Drag a handle.
        @param editorObject the editor object.
        @param px the (screen) x coordinate of the pointer.
        @param py the (screen) y coordinate of the pointer.
        @param cs the coordinates mapping to be used.
        @param isControl true if the control key is held down.
    */
    public void dragHandleDrag(PrimitivesParInterface editorObject,
        int px, int py, MapCoordinates cs, boolean isControl)
    {
        hasMoved=true;
        boolean flip=false;

        // Check if we are indeed dragging a handle...
        if(handleBeingDragged<0){
            if(handleBeingDragged==GraphicPrimitive.DRAG_PRIMITIVE) {
                dragPrimitives(editorObject, px, py, cs);
            }

            // if not, we are performing a rectangular selection
            if(handleBeingDragged==GraphicPrimitive.RECT_SELECTION) {
                int xa = cs.mapXi(oldpx, oldpy, false);
                int ya = cs.mapYi(oldpx, oldpy, false);
                int xb = opx;
                int yb = opy;

                isLeftToRightSelection = xb >= xa;

                editorObject.isLeftToRightSelection(isLeftToRightSelection);
                if(coordinatesListener!=null) {
                    if(isLeftToRightSelection) {
                        coordinatesListener.changeInfos(
                            Globals.messages.getString("completive_selection"));
                    } else {
                        coordinatesListener.changeInfos(
                            Globals.messages.getString("inclusive_selection"));

                    }
                }

                if(opx>xa && px<xa) {
                    flip=true;
                }
                if(opy>ya && py<ya) {
                    flip=true;
                }

                if(!firstDrag) {
                    int a = Math.min(xa,xb);
                    int b = Math.min(ya,yb);
                    int c = Math.abs(xb-xa);
                    int d = Math.abs(yb-ya);

                    xb=px;
                    yb=py;
                    opx=px;
                    opy=py;

                    editorObject.setEvidenceRect(Math.min(xa,xb),
                        Math.min(ya,yb), Math.abs(xb-xa), Math.abs(yb-ya));

                    a=Math.min(a, Math.min(xa,xb));
                    b=Math.min(b, Math.min(ya,yb));
                    c=Math.max(c, Math.abs(xb-xa));
                    d=Math.max(d, Math.abs(yb-ya));

                    if (flip) {
                        editorObject.forcesRepaint();
                    } else {
                        editorObject.forcesRepaint(a,b,c+10,d+10);
                    }
                    return;
                }
                xb=px;
                yb=py;
                opx=px;
                opy=py;
                firstDrag=false;
            }
            return;
        }

        if(!firstDrag) {
            editorObject.forcesRepaint();
        }
        firstDrag=false;

        // Here we adjust the new positions for the handle being drag...
        primBeingDragged.virtualPoint[handleBeingDragged].x=cs.unmapXsnap(px);
        // If control is hold, trace a square
        int ymm;
        if(!isControl || !(primBeingDragged instanceof PrimitiveOval ||
            primBeingDragged instanceof PrimitiveRectangle))
        {
            ymm=py;
        } else {
            // Transform the rectangle in a square, or the oval in a circle.
            int hn=0;
            if(handleBeingDragged==0) { hn=1; }
            ymm=cs.mapYi(primBeingDragged.virtualPoint[hn].x,
                primBeingDragged.virtualPoint[hn].y,false)+px-
                cs.mapXi(primBeingDragged.virtualPoint[hn].x,
                primBeingDragged.virtualPoint[hn].y,false);
        }
        primBeingDragged.virtualPoint[handleBeingDragged].y=cs.unmapYsnap(ymm);
        primBeingDragged.setChanged(true);
    }
}