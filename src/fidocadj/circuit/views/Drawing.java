package fidocadj.circuit.views;

import fidocadj.circuit.model.DrawingModel;
import fidocadj.geom.MapCoordinates;
import fidocadj.layers.LayerDesc;
import fidocadj.primitives.GraphicPrimitive;
import fidocadj.graphic.GraphicsInterface;

/** Drawing: draws the FidoCadJ drawing. This is a view of the drawing.

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

    Copyright 2007-2023 by Davide Bucci
    </pre>
*/
public class Drawing
{
    private final DrawingModel drawingModel;

    // True if the drawing needs holes. This implies that the redrawing
    // step must include a cycle at the end to draw all holes.
    private boolean needHoles;


    // *********** CACHE *************

    // Here are some counters and local variables. We made them class members
    // to ensure that their place is reserved in memory and we do not need
    // some time expensive allocations, since speed is important in the draw
    // operation (used in draw).

    private double oZ;
    private double oX;
    private double oY;
    private double oO;
    private GraphicPrimitive gg;    // NOPMD
    private int i_index; // NOPMD
    private int jIndex; // NOPMD

    /** Create a drawing view.
        @param pp the model to which the view will be associated.
    */
    public Drawing (DrawingModel pp)
    {
        drawingModel=pp;
    }

    /** Draw the handles of all selected primitives
        @param gi the graphic context to be used.
        @param cs the coordinate mapping system to employ.
    */
    public void drawSelectedHandles(GraphicsInterface gi, MapCoordinates cs)
    {
        for (GraphicPrimitive gp : drawingModel.getPrimitiveVector()) {
            if(gp.isSelected()) {
                gp.drawHandles(gi, cs);
            }
        }
    }

    /** Draw the current drawing.
        This code is rather critical. Do not touch it unless you know very
        precisely what you are doing.
        @param gG the graphic context in which the drawing should be drawn.
        @param cs the coordinate mapping to be used.
    */
    public void draw(GraphicsInterface gG, MapCoordinates cs)
    {
        if(cs==null) {
            System.err.println(
                "DrawingModel.draw: ouch... cs not initialized :-(");
            return;
        }

        synchronized (this) {
            // At first, we check if the current view has changed.
            if(drawingModel.getChanged() || oZ!=cs.getXMagnitude()
                || oX!=cs.getXCenter() || oY!=cs.getYCenter()
                || oO!=cs.getOrientation())
            {
                oZ=cs.getXMagnitude();
                oX=cs.getXCenter();
                oY=cs.getYCenter();
                oO=cs.getOrientation();
                drawingModel.setChanged(false);

                // Here we force for a global refresh of graphic data at the
                // primitive level.
                for (GraphicPrimitive gp : drawingModel.getPrimitiveVector()) {
                    gp.setChanged(true);
                }

                if (!drawingModel.getDrawOnlyPads()) {
                    cs.resetMinMax();
                }
            }

            needHoles = drawingModel.getDrawOnlyPads();

            /* First possibility: we need to draw only one layer (for example
                in a macro). This is indicated by the fact that drawOnlyLayer
                is non negative.
            */
            if (drawingModel.getDrawOnlyLayer() >= 0
                                        && !drawingModel.getDrawOnlyPads())
            {
                // At first, we check if the layer is effectively used in the
                // drawing. If not, we exit directly.

                if(!drawingModel.containsLayer(
                                        drawingModel.getDrawOnlyLayer()))
                {
                    return;
                }

                drawPrimitives(drawingModel.getDrawOnlyLayer(), gG, cs);
                return;
            } else if (!drawingModel.getDrawOnlyPads()) {
                // If we want to draw all layers, we need to process with order.
                for (jIndex = 0; jIndex < LayerDesc.MAX_LAYERS; ++jIndex) {
                    if(!drawingModel.containsLayer(jIndex)) {
                        continue;
                    }
                    drawPrimitives(jIndex, gG,cs);
                }
            }
            // Draw in a second time only the PCB pads, in order to ensure that
            // the drills are always open.
            if(needHoles) {
                for (i_index = 0; i_index <
                        drawingModel.getPrimitiveVector().size(); ++i_index){

                    // We will process only primitive which require holes (pads
                    // as well as macros containing pads).

                    gg = drawingModel.getPrimitiveVector()
                                                                .get(i_index);
                    if (gg.needsHoles()) {
                        gg.setDrawOnlyPads(true);
                        gg.draw(gG, cs, drawingModel.getLayers());
                        gg.setDrawOnlyPads(false);
                    }
                }
            }
        }
    }

    /** Returns true if there is the need of drawing holes in the actual
        drawing.

        @return true if holes are needed.

    */
    public final boolean getNeedHoles()
    {
        synchronized(this) {
            return needHoles;
        }
    }

    /** Draws all the primitives and macros contained in the specified layer.
        This function is used mainly by the draw member.
        @param jIndex the layer to be considered.
        @param gG the graphic context in which to draw.
    */
    private void drawPrimitives(int jIndex, GraphicsInterface graphic,
        MapCoordinates cs)
    {
        // Here we process all the primitives, one by one!
        for (GraphicPrimitive gg : drawingModel.getPrimitiveVector()) {

            // Layers are ordered. This improves the redrawing speed.
            if (jIndex > 0 && gg.getLayer() > jIndex) {
                break;
            }

            // Process a particular primitive if it is in the layer
            // being processed.
            if(gg.containsLayer(jIndex)) {
                gg.setDrawOnlyLayer(jIndex);
                gg.draw(graphic, cs, drawingModel.getLayers());
            }

            if(gg.needsHoles()) {
                synchronized (this) {
                    needHoles=true;
                }
            }
        }
    }
}