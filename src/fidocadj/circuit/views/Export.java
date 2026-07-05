package fidocadj.circuit.views;

import java.io.*;

import fidocadj.circuit.model.DrawingModel;
import fidocadj.graphic.PointG;
import fidocadj.graphic.DimensionG;
import fidocadj.export.ExportInterface;
import fidocadj.geom.MapCoordinates;
import fidocadj.geom.DrawingSize;
import fidocadj.layers.LayerDesc;
import fidocadj.primitives.PrimitiveMacro;
import fidocadj.primitives.PrimitivePCBPad;
import fidocadj.primitives.GraphicPrimitive;

/** Export: export the FidoCadJ drawing. This is a view of the drawing.

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
public class Export
{
    private final DrawingModel drawingModel;

    // Border to be used in the export in logical coordinates
    public static final int exportBorder=6;

    /** Creator
        @param pp the model containing the drawing to be exported.
    */
    public Export(DrawingModel pp)
    {
        drawingModel=pp;
    }

    /** Export all primitives and macros in the current model.
        @param exp the export interface to be used
        @param mp the coordinate mapping
        @param exportInvisible true if invisible objects should be exported
    */
    private void exportAllObjects(ExportInterface exp,
        boolean exportInvisible, MapCoordinates mp)
        throws IOException
    {
        GraphicPrimitive g;

        for (int i=0; i<drawingModel.getPrimitiveVector().size(); ++i) {

            g = drawingModel.getPrimitiveVector().get(i);

            if(g.getLayer()==drawingModel.getDrawOnlyLayer() &&
                !(g instanceof PrimitiveMacro))
            {
                if((drawingModel.getLayers().get(
                        g.getLayer())).isVisible()|| exportInvisible)
                {
                    g.export(exp, mp);
                }
            } else if(g instanceof PrimitiveMacro) {
                ((PrimitiveMacro)g).setDrawOnlyLayer(
                                            drawingModel.getDrawOnlyLayer());
                ((PrimitiveMacro)g).setExportInvisible(exportInvisible);

                if((drawingModel.getLayers().get(
                        g.getLayer())).isVisible() || exportInvisible)
                {
                    g.export(exp, mp);
                }
            }
        }
    }

    /** Export the file header
        @param exp the selected exporting interface.
        @param mp the coordinate mapping system to adopt.
        @throws IOException when things goes wrong, for example because there
            has been a memory error or when access to files is impossible.
    */
    public void exportHeader(ExportInterface exp, MapCoordinates mp)
        throws IOException
    {
        synchronized(this) {
            PointG o=new PointG(0,0);
            DimensionG d = DrawingSize.getImageSize(drawingModel, 1, true,o);
            d.width+=exportBorder;
            d.height+=exportBorder;
            // We remeber that getImageSize works only with logical
            // coordinates so we may trasform them:

            d.width *= mp.getXMagnitude();
            d.height *= mp.getYMagnitude();
            exp.setDashUnit(mp.getXMagnitude());
            // We finally write the header
            exp.exportStart(d, drawingModel.getLayers(), mp.getXGridStep());
        }
    }
    /** Export the file using the given interface.

        @param exp the selected exporting interface.
        @param exportInvisible specify that the primitives on invisible layers
            should be exported.
        @param mp the coordinate mapping system to adopt.
        @throws IOException when things goes wrong, for example because there
            has been a memory error or when access to files is impossible.
    */
    public void exportDrawing(ExportInterface exp,
        boolean exportInvisible, MapCoordinates mp)
        throws IOException
    {
        synchronized(this) {
            if (drawingModel.getDrawOnlyLayer() >= 0 &&
                                            !drawingModel.getDrawOnlyPads())
            {
                exportAllObjects(exp, exportInvisible, mp);
            } else if (!drawingModel.getDrawOnlyPads()) {
                for(int j=0;j<drawingModel.getLayers().size(); ++j) {
                    drawingModel.setDrawOnlyLayer(j);
                    exportAllObjects(exp, exportInvisible, mp);
                }
                drawingModel.setDrawOnlyLayer(-1);
            }

            // Export in a second time only the PCB pads, in order to ensure
            // that the drilling holes are always open.
            for (GraphicPrimitive g : drawingModel.getPrimitiveVector()) {
                if (g instanceof PrimitivePCBPad) {
                    ((PrimitivePCBPad)g).setDrawOnlyPads(true);

                    if((drawingModel.getLayers()
                            .get(g.getLayer())).isVisible() || exportInvisible)
                    {
                        g.export(exp, mp);
                    }
                    ((PrimitivePCBPad)g).setDrawOnlyPads(false);
                } else if (g instanceof PrimitiveMacro) {
                    // Uhm... not beautiful
                    ((PrimitiveMacro)g).setExportInvisible(exportInvisible);
                    ((PrimitiveMacro)g).setDrawOnlyPads(true);
                    if((drawingModel.getLayers()
                            .get(g.getLayer())).isVisible() || exportInvisible)
                    {
                        g.export(exp, mp);
                    }
                    ((PrimitiveMacro)g).setDrawOnlyPads(false);
                    ((PrimitiveMacro)g).resetExport();
                }
            }
        }
    }
}
