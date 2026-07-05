package fidocadj.export;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import javax.imageio.*;

import fidocadj.circuit.controllers.SelectionActions;
import fidocadj.circuit.model.DrawingModel;
import fidocadj.circuit.views.Export;
import fidocadj.circuit.views.Drawing;
import fidocadj.geom.MapCoordinates;
import fidocadj.geom.DrawingSize;
import fidocadj.layers.LayerDesc;
import fidocadj.graphic.DimensionG;
import fidocadj.graphic.PointG;
import fidocadj.graphic.swing.Graphics2DSwing;
import fidocadj.graphic.swing.ColorSwing;
import fidocadj.graphic.nil.GraphicsNull;


/** ExportGraphic.java

    Handle graphic export of a FidoCadJ file
    This class should be used to export the circuit under different graphic
    formats.

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

    @author Davide Bucci
*/
public final class ExportGraphic
{
    private ExportGraphic()
    {
        // Nothing to do.
    }

    /** Exports the circuit contained in circ using the specified parsing
        class.

        @param file the file name of the graphic file which will be created.
        @param pp the parsing schematics class which should be used (libraries).
        @param format the graphic format which should be used {png|jpg}.
        @param unitPerPixel the number of unit for each graphic pixel.
        @param antiAlias specify whether the anti alias option should be on.
        @param blackWhite specify that the export should be done in B/W.
        @param ext activate FidoCadJ extensions when exporting
        @param shiftMin shift the exported image at the origin.
        @param splitLayers write each layer on a separate output file.
        @throws IOException if the file can not be created or an error occurs.
    */
    public static void export(File file,
                        DrawingModel pp,
                        String format,
                        double unitPerPixel,
                        boolean antiAlias,
                        boolean blackWhite,
                        boolean ext,
                        boolean shiftMin,
                        boolean splitLayers)
        throws IOException
    {
        exportSizeP(file,
             pp,
             format,
             0,
             0,
             unitPerPixel,
             false,
             antiAlias,
             blackWhite,
             ext,
             shiftMin,
             splitLayers);
    }

    /** Exports the circuit contained in circ using the specified parsing
        class.

        @param file the file name of the graphic file which will be created.
        @param pp the parsing schematics class which should be used (libraries).
        @param format the graphic format which should be used {png|jpg}.
        @param width the image width in pixels (raster images only)
        @param height the image heigth in pixels (raster images only)
        @param antiAlias specify whether the anti alias option should be on.
        @param blackWhite specify that the export should be done in B/W.
        @param ext activate FidoCadJ extensions when exporting
        @param shiftMin shift the exported image at the origin.
        @param splitLayers split each layer into a different output file.
        @throws IOException if an error occurs.
    */
    public static void exportSize(File file,
                        DrawingModel pp,
                        String format,
                        int width,
                        int height,
                        boolean antiAlias,
                        boolean blackWhite,
                        boolean ext,
                        boolean shiftMin,
                        boolean splitLayers)
        throws IOException
    {
        exportSizeP(file,
             pp,
             format,
             width,
             height,
             1,
             true,
             antiAlias,
             blackWhite,
             ext,
             shiftMin,
             splitLayers);
    }

    /** Exports the circuit contained in circ using the specified parsing
        class.

        @param file the file name of the graphic file which will be created.
        @param pp the parsing schematics class which should be used (libraries).
        @param format the graphic format which should be used {png|jpg}.
        @param unitperpixel the number of unit for each graphic pixel.
        @param width the image width in pixels (raster images only)
        @param heith the image heigth in pixels (raster images only)
        @param setSize if true, calculate resolution from size. If false, it
            does the opposite strategy.
        @param antiAlias specify whether the anti alias option should be on.
        @param blackWhite specify that the export should be done in B/W.
        @param ext activate FidoCadJ extensions when exporting.
        @param shiftMin shift the exported image at the origin.
        @param splitLayers if true, split layers into different files.
        @throws IOException if an error occurs.
    */
    private static void exportSizeP(File file,
                        DrawingModel pp,
                        String format,
                        int widthT,
                        int heightT,
                        double unitPerPixelT,
                        boolean setSize,
                        boolean antiAlias,
                        boolean blackWhite,
                        boolean ext,
                        boolean shiftMin,
                        boolean splitLayers)
        throws IOException
    {
        int width=widthT;
        int height=heightT;
        double unitPerPixel=unitPerPixelT;

        // obtain drawing size
        MapCoordinates m=new MapCoordinates();

        // This solves bug #3299281
        new SelectionActions(pp).setSelectionAll(false);

        PointG org=new PointG(0,0);

        DimensionG d = DrawingSize.getImageSize(pp, 1,true,org);
        if (setSize) {
            // In this case, the image size is set and so we need to calculate
            // the correct zoom factor in order to fit the drawing in the
            // specified area.

            d.width+=Export.exportBorder;
            d.height+=Export.exportBorder;

            unitPerPixel = Math.min((double)width/(double)d.width,
                (double)height/(double)d.height);
        } else {
            // In this situation, we do have to calculate the size from the
            // specified resolution.
            width=(int)((d.width+Export.exportBorder)*unitPerPixel);
            height=(int)((d.height+Export.exportBorder)*unitPerPixel);
        }
        org.x *=unitPerPixel;
        org.y *=unitPerPixel;

        org.x -= Export.exportBorder*unitPerPixel/2.0;
        org.y -= Export.exportBorder*unitPerPixel/2.0;

        java.util.List<LayerDesc> ol=pp.getLayers();

        BufferedImage bufferedImage;

        // To print in black and white, we only need to create an array layer
        // in which all colours will be black.
        if(blackWhite) {
            java.util.List<LayerDesc> v=new Vector<LayerDesc>();
            for (int i=0; i<16;++i) {
                v.add(new LayerDesc((new ColorSwing()).black(), // NOPMD
                    (ol.get(i)).isVisible(),
                    "B/W",(ol.get(i)).getAlpha()));
            }
            pp.setLayers(v);
        }

        // Center the drawing in the given space.
        m.setMagnitudes(unitPerPixel, unitPerPixel);

        if(shiftMin && !"pcb".equals(format)) {// don't alter geometry
            m.setXCenter(-org.x);              // if exported to pcb-rnd
            m.setYCenter(-org.y);
        }
        if ("png".equals(format)||"jpg".equals(format)) {

            // Create a buffered image in which to draw

            /*  To get an error, try to export this in png at 1200 dpi:

                [FIDOCAD]
                RP 25 15 11000 95000 2

            */
            try {
                bufferedImage = new BufferedImage(width, height,
                                          BufferedImage.TYPE_INT_RGB);

                // Create a graphics contents on the buffered image
                Graphics2D g2d =
                    bufferedImage.createGraphics();

                if(antiAlias) {
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                }
                g2d.setColor(Color.white);
                g2d.fillRect(0,0, width, height);
                // Save bitmap
                Drawing drawingAgent = new Drawing(pp);
                Graphics2DSwing graphicSwing=new Graphics2DSwing(g2d);
                // This is important for taking into account the dashing size
                graphicSwing.setZoom(m.getXMagnitude());
                drawingAgent.draw(graphicSwing,m);

                ImageIO.write(bufferedImage, format, file);
                // Graphics context no longer needed so dispose it
                g2d.dispose();
            } finally {
                pp.setLayers(ol);
            }
        } else {
            exportVectorFormats(pp, format,file,m,ext,splitLayers);
        }
        pp.setLayers(ol);
    }

    /** Create a file name containing an index from a template.
        For example, if the input name is example.txt and the index is 5,
        the created name should be example_5.txt. If the input name does
        not contain an extension, the "_5" will be added to the name.
        @param name the template name.
        @param index the index.
        @return the new name containing the index separated by an underscore.
    */
    private static String addIndexInFilename(String name, int index)
    {
        int dotpos=name.lastIndexOf('.');
        if(dotpos<0) {
            return name+"_"+index;
        }
        return name.substring(0,dotpos)+"_"+index+"."+name.substring(dotpos+1);
    }

    private static ExportInterface createExportInterface(String format,
        File file, boolean ext)
        throws IOException
    {
        ExportInterface ei;

        if("eps".equals(format)) {
            ei = new ExportEPS(file);
        } else if("pgf".equals(format)) {
            ei = new ExportPGF(file);
        } else if("pdf".equals(format)) {
            ei = new ExportPDF(file, new GraphicsNull());
        } else if("scr".equals(format)) {
            ei = new ExportEagle(file);
        } else if("pcb".equals(format)) {
            ei = new ExportPCBRND(file);
        } else if("fcd".equals(format)) {
            ExportFidoCad ef = new ExportFidoCad(file);
            ef.setSplitStandardMacros(false);
            ef.setExtensions(ext);
            ei=ef;
        } else if("fcda".equals(format)) {
            ExportFidoCad ef = new ExportFidoCad(file);
            ef.setSplitStandardMacros(true);
            ef.setExtensions(ext);
            ei=ef;
        } else if("svg".equals(format)) {
            ei = new ExportSVG(file, new GraphicsNull());
        } else if("dxf".equals(format)) {
            ei = new ExportDXF(file);
        } else {
            throw new IOException("Wrong file format");
        }
        return ei;
    }

    /** Export a file in a vector format.
        @param pp the model to be used.
        @param format the file format code.
        @param file the output file to be written.
        @param m the coordinate system to be used.
        @param splitLayer true if the export should separate the different
            layers into different files.
    */
    private static void exportVectorFormats(DrawingModel pp, String format,
        File file, MapCoordinates m, boolean ext, boolean splitLayer)
        throws IOException
    {
        ExportInterface ei;

        System.out.println("SplitLayer: "+splitLayer);
        if(splitLayer) {
            for(int i=0; i<16;++i) {
                if(!pp.containsLayer(i)) {   // Don't export empty layers.
                    break;
                }
                // Create a new file and export the current layer.
                File layerFile=new File(addIndexInFilename(file.toString(),i));
                ei=createExportInterface(format, layerFile, ext);
                Export e = new Export(pp);
                pp.setDrawOnlyLayer(-1);
                e.exportHeader(ei, m);
                pp.setDrawOnlyLayer(i);
                e.exportDrawing(ei, false, m);
                ei.exportEnd();
            }
            pp.setDrawOnlyLayer(-1);
        } else {
            ei=createExportInterface(format, file,ext);
            Export e = new Export(pp);
            e.exportHeader(ei, m);
            e.exportDrawing(ei, false, m);
            ei.exportEnd();
        }
    }
}
