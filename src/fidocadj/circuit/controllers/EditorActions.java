package fidocadj.circuit.controllers;

import java.util.*;

import fidocadj.circuit.model.ProcessElementsInterface;
import fidocadj.circuit.model.DrawingModel;
import fidocadj.geom.MapCoordinates;
import fidocadj.layers.LayerDesc;
import fidocadj.primitives.GraphicPrimitive;
import fidocadj.primitives.PrimitiveMacro;

/** EditorActions: contains a controller which can perform basic editor actions
    on a primitive database. Those actions include rotating and mirroring
    objects and selecting/deselecting them.

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

public class EditorActions
{
    private final DrawingModel drawingModel;
    private final UndoActions undoActions;
    private final SelectionActions selectionActions;

    // Tolerance in pixels to select an object
    public int sel_tolerance = 10;


    /** Standard constructor: provide the database class.
        @param pp the Model containing the database.
        @param s the SelectionActions controller
        @param u the Undo controller, to ease undo operations.
    */
    public EditorActions (DrawingModel pp, SelectionActions s, UndoActions u)
    {
        drawingModel=pp;
        undoActions=u;
        selectionActions=s;
        sel_tolerance = 10;
    }

    /** Set the current selection tolerance in pixels (the default when
        the class is created is 10 pixels.
        @param s the new tolerance.
    */
    public void setSelectionTolerance(int s)
    {
        sel_tolerance = s;
    }

    /** Get the selection tolerance in pixels.
        @return the current selection tolerance.
    */
    public int getSelectionTolerance()
    {
        return sel_tolerance;
    }

    /** Rotate all selected primitives.
    */
    public void rotateAllSelected()
    {
        GraphicPrimitive g = selectionActions.getFirstSelectedPrimitive();

        if(g==null) {
            return;
        }

        final float ix = g.getFirstPoint().x;
        final float iy = g.getFirstPoint().y;

        selectionActions.applyToSelectedElements(new ProcessElementsInterface()
        {
            public void doAction(GraphicPrimitive g)
            {
                g.rotatePrimitive(false, ix, iy);
            }
        });

        if(undoActions!=null) { undoActions.saveUndoState(); }
    }

    /** Move all selected primitives.
        @param dx relative x movement
        @param dy relative y movement
    */
    public void moveAllSelected(final int dx, final int dy)
    {
        selectionActions.applyToSelectedElements(new ProcessElementsInterface()
        {
            public void doAction(GraphicPrimitive g)
            {
                g.movePrimitive(dx, dy);
            }
        });

        if(undoActions!=null) { undoActions.saveUndoState(); }
    }

    /** Mirror all selected primitives.
    */
    public void mirrorAllSelected()
    {
        GraphicPrimitive g = selectionActions.getFirstSelectedPrimitive();
        if(g==null) {
            return;
        }

        final float ix = g.getFirstPoint().x;

        selectionActions.applyToSelectedElements(new ProcessElementsInterface(){
            public void doAction(GraphicPrimitive g)
            {
                g.mirrorPrimitive(ix);
            }
        });

        if(undoActions!=null) { undoActions.saveUndoState(); }
    }

    /** Delete all selected primitives.
        @param saveState true if the undo controller should save the state
            of the drawing, after the delete operation is done. It should
            be put to false, when the delete operation is part of a more
            complex operation which is not yet ended after the call to this
            method.
    */
    public void deleteAllSelected(boolean saveState)
    {
        int i;
        List<GraphicPrimitive> v=drawingModel.getPrimitiveVector();

        for (i=0; i<v.size(); ++i){
            if(v.get(i).isSelected()) {
                v.remove(v.get(i--));
            }
        }
        if (saveState && undoActions!=null) {
            undoActions.saveUndoState();
        }
    }

    /** Deselect every primitive lying on the given layer. Used when a
        layer is locked, so that a primitive already selected before the
        lock was applied does not stay editable.
        @param layer the layer index whose primitives should be
            deselected.
    */
    public void deselectLayer(int layer)
    {
        for (GraphicPrimitive g: drawingModel.getPrimitiveVector()) {
            if (g.getLayer()==layer) {
                g.setSelected(false);
            }
        }
    }

    /** Sets the layer for all selected primitives.
        @param l the wanted layer index.
        @return true if at least a layer has been changed.
    */
    public boolean setLayerForSelectedPrimitives(int l)
    {
        boolean toRedraw=false;
        // Search for all selected primitives.
        for (GraphicPrimitive g: drawingModel.getPrimitiveVector()) {
            // If selected, change the layer. Macros must be always associated
            // to layer 0.
            if (g.isSelected() && ! (g instanceof PrimitiveMacro)) {
                g.setLayer(l);
                toRedraw=true;
            }
        }
        if(toRedraw) {
            drawingModel.sortPrimitiveLayers();
            drawingModel.setChanged(true);
            undoActions.saveUndoState();
        }
        return toRedraw;
    }

    /** Calculates the minimum distance between the given point and
        a set of primitive. Every coordinate is logical.

        @param px the x coordinate of the given point.
        @param py the y coordinate of the given point.
        @return the distance in logical units.
    */
    public int distancePrimitive(int px, int py)
    {
        int distance;
        int mindistance=Integer.MAX_VALUE;
        int layer=0;
        List<LayerDesc> layerV=drawingModel.getLayers();

        // Check the minimum distance by searching among all
        // primitives
        for (GraphicPrimitive g: drawingModel.getPrimitiveVector()) {
            distance=g.getDistanceToPoint(px,py);
            if(distance<=mindistance) {
                layer = g.getLayer();

                if(layerV.get(layer).isVisible() &&
                    !layerV.get(layer).isLocked())
                {
                    mindistance=distance;
                }
            }
        }
        return mindistance;
    }

    /** Handle the selection (or deselection) of objects. Search the closest
        graphical objects to the given (screen) coordinates.
        This method provides an interface to the {@link #selectPrimitive}
        method, which is oriented towards a more low-level process.

        @param cs the coordinate mapping to be employed.
        @param x the x coordinate of the click (screen).
        @param y the y coordinate of the click (screen).
        @param toggle select always if false, toggle selection on/off if true.
    */
    public void handleSelection(MapCoordinates cs, int x, int y,
        boolean toggle)
    {
        // Deselect primitives if needed.
        if(!toggle) {
            selectionActions.setSelectionAll(false);
        }

        // Calculate a reasonable tolerance. If it is too small, we ensure
        // that it is rounded up to 2.
        int toll=Math.round(cs.unmapXnosnap(x+sel_tolerance)
            -cs.unmapXnosnap(x));
        if (toll<2) { toll=2; }
        selectPrimitive(Math.round(cs.unmapXnosnap(x)),
            Math.round(cs.unmapYnosnap(y)),
            toll, toggle);
    }

    /** Select primitives close to the given point. Every parameter is given in
        logical coordinates.
        @param px the x coordinate of the given point (logical).
        @param py the y coordinate of the given point (logical).
        @param tolerance tolerance for the selection.
        @param toggle select always if false, toggle selection on/off if true
        @return true if a primitive has been selected.
    */
    private boolean selectPrimitive(int px, int py, int tolerance,
        boolean toggle)
    {
        int distance;
        int mindistance=Integer.MAX_VALUE;
        int layer;
        GraphicPrimitive gpsel=null;
        List<LayerDesc> layerV=drawingModel.getLayers();

        /*  The search method is very simple: we compute the distance of the
            given point from each primitive and we retain the minimum value, if
            it is less than a given tolerance.
        */
        for  (GraphicPrimitive g: drawingModel.getPrimitiveVector()) {
            layer = g.getLayer();
            if((layerV.get(layer).isVisible() &&
                !layerV.get(layer).isLocked()) ||
                g instanceof PrimitiveMacro)
            {
                distance=g.getDistanceToPoint(px,py);
                if (distance<=mindistance) {
                    gpsel=g;
                    mindistance=distance;
                }
            }
        }

        // Check if we found something!
        if (mindistance<tolerance && gpsel!=null) {
            if(toggle) {
                gpsel.setSelected(!gpsel.isSelected());
            } else {
                gpsel.setSelected(true);
            }
            return true;
        }
        return false;
    }
    /** Select primitives in a rectangular region (given in logical
        coordinates)
        @param px the x coordinate of the top left point.
        @param py the y coordinate of the top left point.
        @param w the width of the region
        @param h the height of the region
        @return true if at least a primitive has been selected
    */
    public boolean selectRect(int px, int py, int w, int h)
    {
        int layer;
        boolean s=false;

        // Avoid processing a trivial case.
        if(w<1 || h <1) {
            return false;
        }

        List<LayerDesc> layerV=drawingModel.getLayers();
        // Process every primitive, if the corresponding layer is visible.
        for (GraphicPrimitive g: drawingModel.getPrimitiveVector()){
            layer= g.getLayer();
            if((layer>=layerV.size() ||
                (layerV.get(layer).isVisible() &&
                    !layerV.get(layer).isLocked()) ||
                g instanceof PrimitiveMacro) && g.selectRect(px,py,w,h))
            {
                s=true;
            }
        }
        return s;
    }

    /**
     Align all selected primitives to the leftmost position.
     This method finds the leftmost x coordinate among all selected primitives
     and aligns all selected primitives to that x coordinate.
     */
    public void alignLeftSelected()
    {
        // Find the leftmost x coordinate among selected primitives
        float leftmost = Integer.MAX_VALUE;
        for (GraphicPrimitive g : drawingModel.getPrimitiveVector()) {
            if (g.isSelected()) {
                float x = g.getPosition().x;
                if (x < leftmost) {
                    leftmost = x;
                }
            }
        }

        // Move all selected primitives to the leftmost x coordinate
        final float finalLeftmost = leftmost;
        selectionActions.applyToSelectedElements(new ProcessElementsInterface()
        {
            public void doAction(GraphicPrimitive g)
            {
                float dx = finalLeftmost - g.getPosition().x;
                g.movePrimitive(dx, 0);
            }
        });

        // Save the state for the undo operation
        if (undoActions != null) {
            undoActions.saveUndoState();
        }
    }

    /**
     Align all selected primitives to the rightmost position.
     This method finds the rightmost x coordinate among all selected primitives
     and aligns all selected primitives to that x coordinate.
     */
    public void alignRightSelected()
    {
        // Find the rightmost x coordinate among selected primitives
        float rightmost = Integer.MIN_VALUE;
        for (GraphicPrimitive g : drawingModel.getPrimitiveVector()) {
            if (g.isSelected()) {
                float x = g.getPosition().x + g.getSize().width;
                if (x > rightmost) {
                    rightmost = x;
                }
            }
        }

        // Move all selected primitives to the rightmost x coordinate
        final float finalRightmost = rightmost;
        selectionActions.applyToSelectedElements(new ProcessElementsInterface()
        {
            public void doAction(GraphicPrimitive g)
            {
                float dx = finalRightmost -
                        (g.getPosition().x + g.getSize().width);
                g.movePrimitive(dx, 0);
            }
        });

        // Save the state for the undo operation
        if (undoActions != null) {
            undoActions.saveUndoState();
        }
    }

    /**
     Align all selected primitives to the topmost position.
     This method finds the topmost y coordinate among all selected primitives
     and aligns all selected primitives to that y coordinate.
     */
    public void alignTopSelected()
    {
        // Find the topmost y coordinate among selected primitives
        float topmost = Integer.MAX_VALUE;
        for (GraphicPrimitive g : drawingModel.getPrimitiveVector()) {
            if (g.isSelected()) {
                float y = g.getPosition().y;
                if (y < topmost) {
                    topmost = y;
                }
            }
        }

        // Move all selected primitives to the topmost y coordinate
        final float finalTopmost = topmost;
        selectionActions.applyToSelectedElements(new ProcessElementsInterface()
        {
            public void doAction(GraphicPrimitive g)
            {
                float dy = finalTopmost - g.getPosition().y;
                g.movePrimitive(0, dy);
            }
        });

        // Save the state for the undo operation
        if (undoActions != null) {
            undoActions.saveUndoState();
        }
    }

    /**
     Align all selected primitives to the bottommost position.
     This method finds the bottommost y coordinate among all selected primitives
     and aligns all selected primitives to that y coordinate.
     */
    public void alignBottomSelected()
    {
        // Find the bottommost y coordinate among selected primitives
        float bottommost = Integer.MIN_VALUE;
        for (GraphicPrimitive g : drawingModel.getPrimitiveVector()) {
            if (g.isSelected()) {
                float y = g.getPosition().y + g.getSize().height;
                if (y > bottommost) {
                    bottommost = y;
                }
            }
        }

        // Move all selected primitives to the bottommost y coordinate
        final float finalBottommost = bottommost;
        selectionActions.applyToSelectedElements(new ProcessElementsInterface()
        {
            public void doAction(GraphicPrimitive g)
            {
                float dy = finalBottommost -
                        (g.getPosition().y + g.getSize().height);
                g.movePrimitive(0, dy);
            }
        });

        // Save the state for the undo operation
        if (undoActions != null) {
            undoActions.saveUndoState();
        }
    }

    /**
     Align all selected primitives to the horizontal center.
     This method finds the horizontal center among all selected primitives
     and aligns all selected primitives to that y coordinate.
     */
    public void alignHorizontalCenterSelected()
    {
        // Find the minimum and maximum y coordinates among selected primitives
        float topmost = Integer.MAX_VALUE;
        float bottommost = Integer.MIN_VALUE;
        for (GraphicPrimitive g : drawingModel.getPrimitiveVector()) {
            if (g.isSelected()) {
                float yTop = g.getPosition().y;
                float yBottom = g.getPosition().y + g.getSize().height;
                if (yTop < topmost) {
                    topmost = yTop;
                }
                if (yBottom > bottommost) {
                    bottommost = yBottom;
                }
            }
        }

        // Calculate the vertical center
        final float verticalCenter = (topmost + bottommost) / 2;

        // Move all selected primitives to align with the vertical center
        selectionActions.applyToSelectedElements(new ProcessElementsInterface()
        {
            public void doAction(GraphicPrimitive g)
            {
                float currentCenterY =
                        g.getPosition().y + (g.getSize().height / 2);
                float dy = verticalCenter - currentCenterY;
                g.movePrimitive(0, dy);
            }
        });

        // Save the state for the undo operation
        if (undoActions != null) {
            undoActions.saveUndoState();
        }
    }

    /**
     Align all selected primitives to the vertical center.
     This method finds the vertical center among all selected primitives
     and aligns all selected primitives to that x coordinate.
     */
    public void alignVerticalCenterSelected()
    {
        // Find the minimum and maximum x coordinates among selected primitives
        float leftmost = Integer.MAX_VALUE;
        float rightmost = Integer.MIN_VALUE;
        for (GraphicPrimitive g : drawingModel.getPrimitiveVector()) {
            if (g.isSelected()) {
                float xLeft = g.getPosition().x;
                float xRight = g.getPosition().x + g.getSize().width;
                if (xLeft < leftmost) {
                    leftmost = xLeft;
                }
                if (xRight > rightmost) {
                    rightmost = xRight;
                }
            }
        }

        // Calculate the horizontal center
        final float horizontalCenter = (leftmost + rightmost) / 2;

        // Move all selected primitives to align with the horizontal center
        selectionActions.applyToSelectedElements(new ProcessElementsInterface()
        {
            public void doAction(GraphicPrimitive g)
            {
                float currentCenterX =
                        g.getPosition().x + (g.getSize().width / 2);
                float dx = horizontalCenter - currentCenterX;
                g.movePrimitive(dx, 0);
            }
        });

        // Save the state for the undo operation
        if (undoActions != null) {
            undoActions.saveUndoState();
        }
    }

    /**
     Distribute all selected primitives evenly between the leftmost ..
     and rightmost primitives.
     This method finds the leftmost and rightmost primitives among all ..
     selected primitives and distributes all other selected primitives ..
     evenly between them.
     */
    public void distributeHorizontallySelected()
    {
        List<GraphicPrimitive> selectedPrimitives = new ArrayList<>();

        // Find all selected primitives
        for (GraphicPrimitive g : drawingModel.getPrimitiveVector()) {
            if (g.isSelected()) {
                selectedPrimitives.add(g);
            }
        }

        // If less than 3 primitives are selected, distribution is not possible
        if (selectedPrimitives.size() < 3) {
            return;
        }

        // Sort primitives by their x position
        selectedPrimitives.sort(
                Comparator.comparingDouble(g -> g.getPosition().x));

        // Calculate the total distance between ..
        // the leftmost and rightmost primitives
        float leftmostX = selectedPrimitives.get(0).getPosition().x;
        float rightmostX = selectedPrimitives.get(
                selectedPrimitives.size() - 1).getPosition().x;

        float totalSpace = rightmostX - leftmostX;

        // Calculate the spacing between each primitive
        float spacing = totalSpace / (selectedPrimitives.size() - 1);

        // Move the primitives to distribute them evenly
        for (int i = 1; i < selectedPrimitives.size() - 1; i++) {
            float targetX = leftmostX + i * spacing;
            GraphicPrimitive g = selectedPrimitives.get(i);
            float dx = targetX - g.getPosition().x;
            g.movePrimitive(dx, 0);
        }

        // Save the state for the undo operation
        if (undoActions != null) {
            undoActions.saveUndoState();
        }
    }

    /**
     Distribute all selected primitives evenly between the topmost ..
     and bottommost primitives.
     This method finds the topmost and bottommost primitives among ..
     all selected primitives and distributes all other selected ..
     primitives evenly between them.
     */
    public void distributeVerticallySelected()
    {
        List<GraphicPrimitive> selectedPrimitives = new ArrayList<>();

        // Find all selected primitives
        for (GraphicPrimitive g : drawingModel.getPrimitiveVector()) {
            if (g.isSelected()) {
                selectedPrimitives.add(g);
            }
        }

        // If less than 3 primitives are selected, distribution is not possible
        if (selectedPrimitives.size() < 3) {
            return;
        }

        // Sort primitives by their y position
        selectedPrimitives.sort(
                Comparator.comparingDouble(g -> g.getPosition().y));

        // Calculate the total distance between the topmost and bottommost
        // primitives
        float topmostY = selectedPrimitives.get(0).getPosition().y;
        float bottommostY = selectedPrimitives.get(
                selectedPrimitives.size() - 1).getPosition().y;

        float totalSpace = bottommostY - topmostY;

        // Calculate the spacing between each primitive
        float spacing = totalSpace / (selectedPrimitives.size() - 1);

        // Move the primitives to distribute them evenly
        for (int i = 1; i < selectedPrimitives.size() - 1; i++) {
            float targetY = topmostY + i * spacing;
            GraphicPrimitive g = selectedPrimitives.get(i);
            float dy = targetY - g.getPosition().y;
            g.movePrimitive(0, dy);
        }

        // Save the state for the undo operation
        if (undoActions != null) {
            undoActions.saveUndoState();
        }
    }
}