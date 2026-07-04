package fidocadj.export;

import java.util.*;
import java.io.*;

import fidocadj.globals.Globals;
import fidocadj.layers.LayerDesc;
import fidocadj.primitives.MacroDesc;
import fidocadj.graphic.DimensionG;
import fidocadj.graphic.PointDouble;


/** Circuit export to gEDA PCB and gEDA pcb-rnd.

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

    Copyright 2008-2023 by Davide Bucci
    Copyright 2017 Erich Heinzle
   </pre>

    @author Davide Bucci, Erich Heinzle
*/

public final class ExportPCBRND implements ExportInterface
{
    private final FileWriter fstream;
    private BufferedWriter out;
    private static List<String> viaList = new ArrayList<String>();
    private static List<String> pinList = new ArrayList<String>();
    private static List<String> footprints = new ArrayList<String>();
    private static List<String> fpList = new ArrayList<String>();
    private static List<String> layerEls1 = new ArrayList<String>();
    private static List<String> layerEls2 = new ArrayList<String>();
    private static List<String> layerEls3 = new ArrayList<String>();
    private static List<String> layerEls4 = new ArrayList<String>();
    private static List<String> layerEls5 = new ArrayList<String>();
    private static List<String> layerEls6 = new ArrayList<String>();
    private static List<String> layerEls7 = new ArrayList<String>();
    private static List<String> layerEls8 = new ArrayList<String>();
    private static List<String> layerEls9 = new ArrayList<String>();
    private static List<String> layerEls10 = new ArrayList<String>();
    private static List<String> layerEls11 = new ArrayList<String>();
    private static List<String> layerEls12 = new ArrayList<String>();
    private static List<String> layerEls13 = new ArrayList<String>();
    private static List<String> layerEls14 = new ArrayList<String>();
    private static List<String> layerEls15 = new ArrayList<String>();
    private static List<String> layerEls16 = new ArrayList<String>();

    // Conversion between FidoCadJ units and Eagle units (1/10 inches)
    //static double res=5e-2;

    // these variable are used with recursive calls to embed FPs
    String currentMacro = "";
    int macroX = 0;
    int macroY = 0;
    long defaultClearance = 1000; // centimils
    long minExportedLineThickness = 1000; // centimils
    int bezierSegments = 11; // number of line elements per cubic bezier

    /** Constructor
        @param f the File object in which the export should be done.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public ExportPCBRND (File f) throws IOException
    {
        //macroList = "";
        //junctionList = "";
        fstream = new FileWriter(f);
    }

    /** Set the multiplication factor to be used for the dashing.
        @param u the factor.
    */
    public void setDashUnit(double u)
    {
        // Nothing is implemented for the moment.
    }

    /** Set the "phase" in output units of the dashing style.
        For example, if a dash style is composed by a line followed by a space
        of equal size, a phase of 0 indicates that the dash starts with the
        line.
        @param p the phase, in output units.
    */
    public void setDashPhase(float p)
    {
        // Nothing is implemented for the moment.
    }

    /** Called at the beginning of the export phase. Ideally, in this routine
        there should be the code to write the header of the file on which
        the drawing should be exported.

        @param totalSize the size of the image. Useful to calculate for example
        the bounding box.
        @param la a vector describing the attributes of each layer.
        @param grid the grid size. This is useful when exporting to another
            drawing program having some kind of grid concept. You might use
            this value to synchronize FidoCadJ's grid with the one used by
            the target.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportStart(DimensionG totalSize, List<LayerDesc> la,
        int grid)
        throws IOException
    {
        out = new BufferedWriter(fstream);
        // start with a gEDA PCB file header
        gEDALayoutHeader();
    }

    /** Called at the end of the export phase.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportEnd()
        throws IOException
    {
        writeFootprints();
        writeVias();
        writePins();
        writeLayers();
        /*        out.write(macroList); // <- footprints
                  out.write(junctionList); */
        out.close();
    }

    /** Called when exporting an Advanced Text primitive.

        @param x the x position of the beginning of the string to be written.
        @param y the y position of the beginning of the string to be written.
        @param sizex the x size of the font to be used.
        @param sizey the y size of the font to be used.
        @param fontname the font to be used.
        @param isBold true if the text should be written with a boldface font.
        @param isMirrored true if the text should be mirrored.
        @param isItalic true if the text should be written with an italic font.
        @param orientation angle of orientation (degrees).
        @param layer the layer that should be used.
        @param text the text that should be written.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportAdvText (int x, int y, double sizex, double sizey,
        String fontname, boolean isBold, boolean isMirrored, boolean isItalic,
        int orientation, int layer, String text)
        throws IOException
    {
        String line = fidoTextToPCBText(x, y, text, (int)sizey, orientation);
        pushElement(line, layer); // ignore mirroring for now
        //System.out.println("# text added on layer: " + layer);
    }

    /** Called when exporting a Bézier primitive.

        @param x1 the x position of the first point of the trace.
        @param y1 the y position of the first point of the trace.
        @param x2 the x position of the second point of the trace.
        @param y2 the y position of the second point of the trace.
        @param x3 the x position of the third point of the trace.
        @param y3 the y position of the third point of the trace.
        @param x4 the x position of the fourth point of the trace.
        @param y4 the y position of the fourth point of the trace.
        @param layer the layer that should be used.

                // from 0.22.1

        @param arrowStart specify if an arrow is present at the first point.
        @param arrowEnd specify if an arrow is present at the second point.
        @param arrowStyle the style of the arrow.
        @param arrowLength total lenght of arrows (if present).
        @param arrowHalfWidth half width of arrows (if present).
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.
        @throws IOException when things goes horribly wrong, for example if.
            the file in which the output is being done is not accessible.
    */
    public void exportBezier (int x1, int y1,
        int x2, int y2,
        int x3, int y3,
        int x4, int y4,
        int layer,
        boolean arrowStart,
        boolean arrowEnd,
        int arrowStyle,
        int arrowLength,
        int arrowHalfWidth,
        int dashStyle,
        double strokeWidth)
        throws IOException
    {

        PointDouble[] vertices
            = cubicBezierToVector(x1, y1, x2, y2, x3, y3, x4, y4,
                                  bezierSegments);

        String lines
            = fidoPolylineToPCBLines(vertices, vertices.length,
                                     strokeWidth);
        pushElement(lines, layer);
        //System.out.println("# bezier segment on layer: " + layer);
    }

    /** Called when exporting a Connection primitive.
        @param x the x position of the position of the connection.
        @param y the y position of the position of the connection.
        @param layer the layer that should be used.
        @param size specify the size of the junction.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportConnection (int x, int y, int layer, double size)
        throws IOException
    {
        //junctionList += "Junction ("+een(x*res)+" "
        //    +een((dim.height-y)*res)+");\n";
    }

    /** Called when exporting a Line primitive.
        @param x1 the x position of the first point of the segment.
        @param y1 the y position of the first point of the segment.
        @param x2 the x position of the second point of the segment.
        @param y2 the y position of the second point of the segment.
        @param layer the layer that should be used.

        // from 0.22.1

        @param arrowStart specify if an arrow is present at the first point.
        @param arrowEnd specify if an arrow is present at the second point.
        @param arrowStyle the style of the arrow.
        @param arrowLength total lenght of arrows (if present).
        @param arrowHalfWidth half width of arrows (if present).
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.
        @throws IOException when things goes horribly wrong, for example if.
            the file in which the output is being done is not accessible.
    */
    public void exportLine (double x1, double y1,
        double x2, double y2,
        int layer,
        boolean arrowStart,
        boolean arrowEnd,
        int arrowStyle,
        int arrowLength,
        int arrowHalfWidth,
        int dashStyle,
        double strokeWidth)
        throws IOException
    {

        String line = fidoLineToPCBLine(x1, y1, x2, y2, strokeWidth);
        pushElement(line, layer);
    }

    /** Called when exporting a Macro call.
        This function can just return false, to indicate that the macro should
        be rendered by means of calling the other primitives. Please note that
        a macro does not have a reference layer, since it is defined by its
        components.

        @param x the x position of the position of the macro
        @param y the y position of the position of the macro
        @param isMirrored true if the macro is mirrored
        @param orientation the macro orientation in degrees
        @param macroName the macro name
        @param macroDesc the macro description, in the FidoCad format
        @param tname the name shown
        @param xn coordinate of the name shown
        @param yn coordinate of the name shown
        @param value the shown value
        @param xv coordinate of the value shown
        @param yv coordinate of the value shown
        @param font the used font
        @param fontSize the size of the font to be used.
        @param m the library.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
        @return false if the macro has to be expanded into primitives. True
            if its export has been treated by the function.
    */
    public boolean exportMacro(int x, int y, boolean isMirrored,
                               int orientation, String macroName,
                               String macroDesc, String tname,
                               int xn, int yn,
                               String value,
                               int xv, int yv,
                               String font,
                               int fontSize,
                               Map<String, MacroDesc> m)
        throws IOException
    {
        currentMacro = "macroName=" + macroName
            // "-value=" + value // is empty
            + "-x=" + x
            + "-y="+ y
            + "-rot=" + orientation
            + "-mirror=" + isMirrored;
        //  + "-desc=" + macroDesc  <- actual definition

        // System.out.println("# About to process: " + currentMacro);
        // System.out.println("# with xn: " + xn + ", yn: " + yn
        //                   + ", xv: " + xv + ", yv: " + yv);

        if (footprintUnique(currentMacro)) { // beware duplicate calls
            macroX = x;
            macroY = y;

            String header = gEDAElementHeader(currentMacro,
                                              macroX,
                                              macroY);
            String footprintBody = parseMacro(macroDesc);
            String footer = gEDAElementFooter();
            if (!"".equals(footprintBody)) {
                pushFootprint(header + footprintBody + footer, macroName);
            }
        }

        return true; // The macro WILL NOT be expanded into primitives.

        /*
        String mirror ="";
        if (isMirrored)
            mirror = "M";

        // The component name should not contain spaces. Substitute with
        // the underline character.
        Map<String, String> subst = new HashMap<String, String>();
        subst.put(" ","_");
        String name=Globals.substituteBizarreChars(tname, subst);
        */
    }

    /** Called when exporting an Oval primitive. Specify the bounding box.
        @param x1 the x position of the first corner.
        @param y1 the y position of the first corner.
        @param x2 the x position of the second corner.
        @param y2 the y position of the second corner.
        @param isFilled it is true if the oval should be filled.
        @param layer the layer that should be used.
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportOval(int x1, int y1, int x2, int y2,
        boolean isFilled, int layer, int dashStyle, double strokeWidth)
        throws IOException
    {
        int dx = x2 - x1;
        int dy = y2 - y1;
        double midx = ((double)x1 + (double)x2)/2.0;
        double midy = ((double)y1 + (double)y2)/2.0;
        String ellipse = "";

        if (dx < 0) {
            dx = -dx;
        }
        if (dy < 0) {
            dy = -dy;
        }
        if (dx == dy) { // a perfect circle
            ellipse
                = fidoArcToPCBArc(midx, midy, dx, strokeWidth, isFilled);
        } else { // is an ellipse
            int minSegments = 22;
            double arcFrac = 2*Math.PI/minSegments;
            double theta = 0.0;

            PointDouble[] vertices = new PointDouble[minSegments + 1];

            PointDouble startVertex = new PointDouble();
            startVertex.x = midx + dx/2.0; // int to double, start at RHS
            startVertex.y = midy;
            vertices[0] = startVertex;

            PointDouble endVertex = new PointDouble();
            endVertex.x = vertices[0].x; // int to double
            endVertex.y = vertices[0].y;
            vertices[minSegments] = endVertex;

            for (int t = 1; t < minSegments; t++) {
                theta += arcFrac;
                PointDouble latestVertex = new PointDouble();
                latestVertex.x += midx + Math.cos(theta)*dx/2;
                latestVertex.y += midy + Math.sin(theta)*dy/2;
                vertices[t] = latestVertex;
            }
            ellipse = fidoPolyToPCBPoly(vertices, minSegments,
                                        strokeWidth, isFilled);
        }
        pushElement(ellipse, layer);
        //System.out.println("# circle/ellipse on layer: " + layer);
    }

    /** Called when exporting a PCBLine primitive.
        @param x1 the x position of the first point of the segment.
        @param y1 the y position of the first point of the segment.
        @param x2 the x position of the second point of the segment.
        @param y2 the y position of the second point of the segment.
        @param width the width ot the line.
        @param layer the layer that should be used.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportPCBLine(int x1, int y1, int x2, int y2, int width,
        int layer)
        throws IOException

    {
        String line = "";
        line = fidoLineToPCBLine(x1, y1, x2, y2, width);
        pushElement(line, layer);
        //System.out.println(line + "# layer: " + layer);
    }

    /** Called when exporting a PCBPad primitive.
        @param x the x position of the pad.
        @param y the y position of the pad.
        @param style the style of the pad (0: oval, 1: square, 2: rounded
            square).
        @param six the x size of the pad.
        @param siy the y size of the pad.
        @param indiam the hole internal diameter.
        @param layer the layer that should be used.
        @param onlyHole true if only the hole has to be exported.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportPCBPad(int x, int y, int style, int six, int siy,
        int indiam, int layer, boolean onlyHole)
        throws IOException
    {
        String newPad = "";
        int maxDim = six;
        if (siy > six) {
            maxDim = siy; // see which dimension is larger
        }
        /* Pin[X Y Thickness Clearance Mask Drill Name Number SFlags]*/
        if (onlyHole) {
            newPad = "\tVia["
                    + coordToPCB(x) + " "
                    + coordToPCB(y) + " "
                    + coordToPCB(maxDim) + " "       // annulus outer
                    + (coordToPCB(maxDim)+100) + " " // Cu clearance
                    + (coordToPCB(maxDim)+100) + " " // mask
                    + coordToPCB(indiam) + " "       // drill
                    + "\"\" \"hole\"]\n";     // hole flag
        } else {
            switch (style) {
                case 1: // Square pad
                    newPad = "\tVia["
                        + coordToPCB(x) + " "
                        + coordToPCB(y) + " "
                        + coordToPCB(maxDim) + " " // annulus outer
                        + (coordToPCB(maxDim)+100) + " " // Cu clearance
                        + (coordToPCB(maxDim)+100) + " " // mask
                        + coordToPCB(indiam) + " "       // drill
                        + "\"\" \"square\"]\n";     // square flag
                    break;
                case 2: // Rounded pad
                    newPad = "\tVia["
                        + coordToPCB(x) + " "
                        + coordToPCB(y) + " "
                        + coordToPCB(maxDim) + " "  // annulus outer
                        + (coordToPCB(maxDim)+100) + " " // Cu clearance
                        + (coordToPCB(maxDim)+100) + " " // mask
                        + coordToPCB(indiam) + " "       // drill
                        + "\"\" \"square,shape(17)\"]\n";// octagon
                    break;
                case 0: // round pin
                    newPad = "\tVia["
                        + coordToPCB(x) + " "
                        + coordToPCB(y) + " "
                        + coordToPCB(maxDim) + " "      // annulus outer
                        + (coordToPCB(maxDim)+100) + " "// Cu clearance
                        + (coordToPCB(maxDim)+100) + " "// mask
                        + coordToPCB(indiam) + " "      // drill
                        + "\"\" \"\"]\n";               // no flag
                    break;
                default:
                    break;
            }
            out.write(newPad);
            // ... then, drill the hole!
            out.write("# Oval and rect pad export approximated\n");
        }
    }

    /** Called when exporting a Polygon primitive
        @param vertices array containing the position of each vertex
        @param nVertices number of vertices
        @param isFilled true if the polygon is filled
        @param layer the layer that should be used
        @param dashStyle dashing style
        @param strokeWidth the width of the pen to be used when drawing
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportPolygon(PointDouble[] vertices, int nVertices,
        boolean isFilled, int layer, int dashStyle, double strokeWidth)
        throws IOException
    {
        String poly = fidoPolyToPCBPoly(vertices, nVertices,
                                        strokeWidth, isFilled);
        pushElement(poly, layer);
    }

    /** Called when exporting a Rectangle primitive.

        @param x1 the x position of the first corner
        @param y1 the y position of the first corner
        @param x2 the x position of the second corner
        @param y2 the y position of the second corner
        @param isFilled it is true if the rectangle should be filled
        @param layer the layer that should be used
        @param dashStyle dashing style
        @param strokeWidth the width of the pen to be used when drawing
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportRectangle(int x1, int y1, int x2, int y2,
        boolean isFilled, int layer, int dashStyle, double strokeWidth)
        throws IOException
    {
        String newRect = "";
        if (isFilled) { // export a solid polygon
            newRect = fidoRectToPCBPoly(x1, y1, x2, y2);
        } else { // just export the four bounding lines
            newRect = fidoRectToPCBLines(x1, y1, x2, y2, strokeWidth);
        }
        pushElement(newRect, layer);
    }

    /** Called when exporting an embedded raster Image primitive. gEDA
        PCB/pcb-rnd have no raster-image element that this hand-written
        exporter can produce, so a placeholder rectangle with a
        "[immagine]" label is drawn on the same layer instead.

        @param x1 the x position of the first corner.
        @param y1 the y position of the first corner.
        @param x2 the x position of the second corner.
        @param y2 the y position of the second corner.
        @param layer the layer that should be used.
        @param opacity the opacity, between 0.0 (transparent) and 1.0
            (fully opaque).
        @param blackAndWhite true if the image should be shown in black
            and white.
        @param mimeType the mime subtype of the original file (e.g. "png").
        @param base64Data the base64-encoded bytes of the original file.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportImage(int x1, int y1, int x2, int y2, int layer,
        double opacity, boolean blackAndWhite, String mimeType,
        String base64Data)
        throws IOException
    {
        String placeholder = fidoRectToPCBLines(x1, y1, x2, y2, 2)
            + fidoTextToPCBText((x1+x2)/2, (y1+y2)/2, "[immagine]", 20, 0);
        pushElement(placeholder, layer);
    }

    /** Called when exporting a Curve primitive.
        @param vertices array containing the position of each vertex.
        @param nVertices number of vertices.
        @param isFilled true if the polygon is filled.
        @param isClosed true if the curve is closed.
        @param layer the layer that should be used.
        @param arrowStart specify if an arrow is present at the first point.
        @param arrowEnd specify if an arrow is present at the second point.
        @param arrowStyle the style of the arrow.
        @param arrowLength total lenght of arrows (if present).
        @param arrowHalfWidth half width of arrows (if present).
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.
        @return false if the curve should be rendered using a polygon, true
            if it is handled by the function.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public boolean exportCurve(PointDouble[] vertices, int nVertices,
        boolean isFilled, boolean isClosed, int layer,
        boolean arrowStart,
        boolean arrowEnd,
        int arrowStyle,
        int arrowLength,
        int arrowHalfWidth,
        int dashStyle,
        double strokeWidth)
        throws IOException
    {
        if (nVertices == 0) { // here we avoid null pointer dereferences
            System.out.println("Ignoring empty cubic spline definition.");
            return true;
        } else if (nVertices == 2) { // simplify as a simple line
            String line = "";
            line = fidoLineToPCBLine(vertices[0].x,
                                     vertices[0].y,
                                     vertices[1].x,
                                     vertices[1].y, strokeWidth);
            pushElement(line, layer);
            return true;
        } else { // let Export.java break it up into segments
            return false;
        }
    }

    /** Called when exporting an arrow.
        @param x position of the tip of the arrow.
        @param y position of the tip of the arrow.
        @param xc direction of the tip of the arrow.
        @param yc direction of the tip of the arrow.
        @param l length of the arrow.
        @param h width of the arrow.
        @param style style of the arrow.
        @return always (0,0).
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public PointPr exportArrow(double x, double y, double xc, double yc,
        double l, double h,
        int style)
        throws IOException
    {
        // Does nothing, since it will not be useful here.
        return new PointPr();
    }

  ////////////////////// Custom routines below /////////////////////

    private void gEDALayoutHeader() throws IOException
    {
        out.write("# release: pcb 20110918\n\n");
        out.write("# To read pcb files, the pcb version "
                  + "(or the git source date) must be >="
                  + " the file version\n");
        out.write("FileVersion[20070407]\n\n");
        out.write("PCB[\"\" 600000 500000]\n\n");
        out.write("Grid[500.0 0 0 1]\n");
        out.write("Cursor[2500 62500 0.000000]\n");
        out.write("PolyArea[3100.006200]\n");
        out.write("Thermal[0.500000]\n");
        out.write("DRC[1200 900 1000 700 1500 1000]\n");
        out.write("Flags(\"nameonpcb,clearnew,snappin\")\n");
        out.write("Groups(\"1,s:2,c:3:4:5:6:7:8:9:10:11:12:13:14\")\n");
        out.write("Styles[\"Signal,1000,7874,3150,2000:Power,2000,8661"
                  + ",3937,2000:Fat,8000,13780,4724,2500:Sig-tight,"
                  + "1000,6400,3150,1200\"]\n\n");
        out.write("Attribute(\"PCB::grid::unit\" \"mil\")\n");

        out.write("# Created by FidoCadJ "+Globals.version+
                  " by Erich Heinzle, based on code by Davide Bucci.\n");
    }


    private String gEDAElementHeader(String macroName,
                                   int macroX,
                                     int macroY)
    {

        return "Element[\"\" \""
            + macroName + "\" "
            + "\"\" \"\" "
            + coordToPCB(macroX) + " "
            + coordToPCB(macroY) + " "
            + "-2500 -1500 0 100 \"\"]\n(\n";
    }

    private int sToInt(String val)
    {
        return Integer.parseInt(val);
    }

    private String parseMacro(String macroDesc) throws IOException
    {
        BufferedReader buffer =null;
        List<String> macroDefs = new ArrayList<String>();
        int padCounter = 1;
        String line;
        String [] tokens;
        String footprintElements = "";

        try {
            buffer = new BufferedReader(new StringReader(macroDesc));
            while ((line = buffer.readLine()) != null) {
                macroDefs.add(line);
            }
            for (String macro : macroDefs) {
                tokens = macro.split(" ");
                if (tokens.length >= 5) {
                    if ("LI".equals(tokens[0])
                        && "3".equals(tokens[5]))
                    { // silk
                        footprintElements = footprintElements +
                            fidoLineToPCBLineElement(sToInt(tokens[1]) - 100,
                                                     sToInt(tokens[2]) - 100,
                                                     sToInt(tokens[3]) - 100,
                                                     sToInt(tokens[4]) - 100,
                                                     2);
                    } else if ("EP".equals(tokens[0]) // filled
                               && "3".equals(tokens[5]))
                    { // silk
                        int dx = sToInt(tokens[3])-sToInt(tokens[1]);
                        if (dx < 0) {
                            dx = -dx;
                        }
                        int dy = sToInt(tokens[4])-sToInt(tokens[2]);
                        if (dy < 0) {
                            dy = -dy;
                        }
                        if (dx == dy) { // circle; ignore ellipses for now
                            double midx
                                = (sToInt(tokens[3])+sToInt(tokens[1]))/2;
                            double midy
                                = (sToInt(tokens[4])+sToInt(tokens[2]))/2;
                            // System.out.println("Filled FP ellipse");
                            footprintElements = footprintElements +
                                fidoArcToPCBArcElement(midx-100,
                                                       midy-100,
                                                       dx, 2, true);
                        }
                    } else if ("EV".equals(tokens[0]) // not filled
                               && "3".equals(tokens[5]))
                    { // silk
                        int dx
                            = sToInt(tokens[3])-sToInt(tokens[1]);
                        if (dx < 0) {
                            dx = -dx;
                        }
                        int dy = sToInt(tokens[4])-sToInt(tokens[2]);
                        if (dy < 0) {
                            dy = -dy;
                        }
                        if (dx == dy) { // circle; ignore ellipses for now
                            double midx
                                = (sToInt(tokens[3])+sToInt(tokens[1]))/2;
                            double midy
                                = (sToInt(tokens[4])+sToInt(tokens[2]))/2;
                            System.out.println("Processing empty FP ellipse");
                            footprintElements = footprintElements +
                                fidoArcToPCBArcElement(midx - 100,
                                                       midy - 100,
                                                       dx, 2, false);
                        }
                    } else if ("RP".equals(tokens[0]) //filled rectangle pad
                               && !"0".equals(tokens[5]))
                    { // not circuit
                        footprintElements = footprintElements +
                            fidoRectToPCBPadElement(sToInt(tokens[1]) - 100,
                                                    sToInt(tokens[2]) - 100,
                                                    sToInt(tokens[3]) - 100,
                                                    sToInt(tokens[4]) - 100,
                                                    sToInt(tokens[5]),
                                                    padCounter);
                        padCounter++;
                    } else if ("RV".equals(tokens[0]) // empty rectangle
                               && "3".equals(tokens[5]))
                    { // on silk
                        footprintElements = footprintElements +
                            fidoRectToPCBLineElements(sToInt(tokens[1]) - 100,
                                                      sToInt(tokens[2]) - 100,
                                                      sToInt(tokens[3]) - 100,
                                                      sToInt(tokens[4]) - 100,
                                                      sToInt(tokens[5]));
                    } else if ("PA".equals(tokens[0])) { // pin
                        footprintElements = footprintElements +
                            fidoPadToPCBPinElement(sToInt(tokens[1]) - 100,
                                                   sToInt(tokens[2]) - 100,
                                                   sToInt(tokens[3]),
                                                   sToInt(tokens[4]),
                                                   sToInt(tokens[5]),
                                                   sToInt(tokens[6]),
                                                   //sToInt(tokens[7]),
                                                   padCounter);
                        padCounter++;
                    } else if ("PV".equals(tokens[0])) { // empty polyline
                        // silk
                        int nVertices = 0;
                        if (tokens.length%2 == 0) { // an even number
                            nVertices = (tokens.length-2)/2;
                        }
                        //System.out.println("# nvertices: " + nVertices);
                        PointDouble [] vertices = new PointDouble[nVertices];
                        for (int vertex = 0;
                             vertex < 2*nVertices;
                             vertex = vertex + 2) {
                            PointDouble newV = new PointDouble();
                            newV.x = Double.parseDouble(tokens[vertex+1]) - 100;
                            newV.y = Double.parseDouble(tokens[vertex+2]) - 100;
                            vertices[vertex/2] = newV;
                        } // NB number of line segments = vertices - 1
                        footprintElements = footprintElements +
                            fidoPolylineToPCBLineElements(vertices,
                                                          nVertices,
                                                          2); // 10 mil default
                    } else if ("BE".equals(tokens[0]) // bezier
                               && !"0".equals(tokens[9]))
                    { // not circuit
                        // System.out.println("# About to process FP bezier");
                        int x1 = sToInt(tokens[1]) - 100;
                        int y1 = sToInt(tokens[2]) - 100;
                        int x2 = sToInt(tokens[3]) - 100;
                        int y2 = sToInt(tokens[4]) - 100;
                        int x3 = sToInt(tokens[5]) - 100;
                        int y3 = sToInt(tokens[6]) - 100;
                        int x4 = sToInt(tokens[7]) - 100;
                        int y4 = sToInt(tokens[8]) - 100;

                        int nVertices = 10;
                        PointDouble [] vertices
                            = new PointDouble[nVertices];
                        vertices = cubicBezierToVector(x1, y1, x2, y2, x3,
                                                       y3, x4, y4,
                                                       nVertices-1);
                        footprintElements = footprintElements +
                            fidoPolylineToPCBLineElements(vertices,
                                                          nVertices,
                                                          2);
                        // 10 mil default for exported lines
                    } else if ("TY".equals(tokens[0])) {
                        // We don't support text in footprints in gEDA
                        System.out.println("Text not supported.");
                    } else {
                        System.out.println("# Unsure what to do with: "
                                           + tokens[0] + " in macro.");
                    }
                }
            }
        } finally {
            if (buffer!=null) { buffer.close(); }
        }
        return footprintElements;
    }

    private String gEDAElementFooter()
    {
        return ")\n";
    }

    private PointDouble[] cubicBezierToVector(int x1, int y1,
                                              int x2, int y2,
                                              int x3, int y3,
                                              int x4, int y4,
                                              int nsegments)
    {

        int sPCP1deltaX = x2 - x1;
        int sPCP1deltaY = y2 - y1;
        int cP1CP2deltaX = x3 - x2;
        int cP1CP2deltaY = y3 - y2;
        int cP2EPdeltaX = x4 - x3;
        int cP2EPdeltaY = y4 - y3;

        int minSegments = nsegments;
        double segFrac = 1.0/minSegments;

        double cP1dx = sPCP1deltaX*segFrac;
        double cP1dy = sPCP1deltaY*segFrac;
        double cP2dx = cP1CP2deltaX*segFrac;
        double cP2dy = cP1CP2deltaY*segFrac;
        double ePdx = cP2EPdeltaX*segFrac;
        double ePdy = cP2EPdeltaY*segFrac;

        double currentSeg1X = x1;
        double currentSeg1Y = y1;
        double currentSeg2X = x2;
        double currentSeg2Y = y2;
        double currentSeg3X = x3;
        double currentSeg3Y = y3;

        double virtSegX1 = 0.0;
        double virtSegY1 = 0.0;
        double virtSegX2 = 0.0;
        double virtSegY2 = 0.0;

        PointDouble[] vertices = new PointDouble[minSegments + 1];

        PointDouble startVertex = new PointDouble();
        startVertex.x = x1; // int to double
        startVertex.y = y1;
        vertices[0] = startVertex;

        PointDouble endVertex = new PointDouble();
        endVertex.x = x4; // int to double
        endVertex.y = y4;
        vertices[minSegments] = endVertex;

        double tFrac = 0.0;

        for (int t = 1; t < minSegments; t++) {
            tFrac = 1.0*t/minSegments;
            currentSeg1X += cP1dx;
            currentSeg1Y += cP1dy;
            currentSeg2X += cP2dx;
            currentSeg2Y += cP2dy;
            currentSeg3X += ePdx;
            currentSeg3Y += ePdy;

            virtSegX1 = currentSeg1X + tFrac*(currentSeg2X - currentSeg1X);
            virtSegY1 = currentSeg1Y + tFrac*(currentSeg2Y - currentSeg1Y);
            virtSegX2 = currentSeg2X + tFrac*(currentSeg3X - currentSeg2X);
            virtSegY2 = currentSeg2Y + tFrac*(currentSeg3Y - currentSeg2Y);

            PointDouble latestVertex = new PointDouble();

            latestVertex.x = virtSegX1 + tFrac*(virtSegX2 - virtSegX1);
            latestVertex.y = virtSegY1 + tFrac*(virtSegY2 - virtSegY1);

            vertices[t] = latestVertex;
        }
        return vertices;
    }

    private String fidoLineToPCBLine(double x1, double y1,
                                     double x2, double y2,
                                     double thickness)
    {
        long exportedThickness = coordToPCB(thickness);
        if (exportedThickness < minExportedLineThickness) {
            exportedThickness = 1000;
        }
        return "\tLine["
            + coordToPCB(x1) + " "
            + coordToPCB(y1) + " "
            + coordToPCB(x2) + " "
            + coordToPCB(y2) + " "
            + exportedThickness + " "
            + defaultClearance
            + " \"clearline\"]\n";
    }

    private String fidoLineToPCBLine(int x1, int y1, int x2, int y2,
                                     double thickness)
    {
        return fidoLineToPCBLine((double) x1, (double) y1,
                                 (double) x2, (double) y2, thickness);
    }

/*
    private String fidoLineToPCBLine(PointDouble p1, PointDouble p2,
                                     double thickness)
    {
        return fidoLineToPCBLine(p1.x, p1.y, p2.x, p2.y, thickness);
    }


    private String fidoLineToPCBLineElement(int x1, int y1, int x2,
                                            int y2, int thickness)
    {
        return fidoLineToPCBLineElement((double) x1, (double) y1,
                                        (double) x2, (double) y2,
                                        (double) thickness);
    }

*/
    private String
        fidoLineToPCBLineElement(double x1, double y1, double x2,
                               double y2, double thickness)
    {
        long exportedThickness = coordToPCB(thickness);
        if (exportedThickness < minExportedLineThickness) {
            exportedThickness = 1000;
        }
        return "\tElementLine["
            + coordToPCB(x1) + " "
            + coordToPCB(y1) + " "
            + coordToPCB(x2) + " "
            + coordToPCB(y2) + " "
            + exportedThickness + "]\n";
    }
/*
    private String fidoLineToPCBLineElement(PointDouble p1,
                                            PointDouble p2,
                                            int thickness)
    {
        return fidoLineToPCBLineElement(p1.x, p1.y,
                                        p2.x, p2.y, thickness);
    }
*/
    private String
        fidoPolylineToPCBLineElements(PointDouble[] vertices,
                                      int nVertices, double strokeWidth)
    {
        StringBuffer newPolylines = new StringBuffer();

        for (int v = 0; v < (nVertices - 1); v++) {
            newPolylines.append(
                            fidoLineToPCBLineElement(vertices[v].x,
                                         vertices[v].y,
                                         vertices[v+1].x,
                                         vertices[v+1].y,
                                         strokeWidth));
        }
        return newPolylines.toString();
    }

    private String
        fidoRectToPCBLineElements(int x1, int y1, int x2, int y2,
                                  double strokeWidth)
    {
        return fidoRectToPCBLineElements((double) x1, (double) y1,
                                         (double) x2, (double) y2,
                                         strokeWidth);
    }

    private String
        fidoRectToPCBLineElements(double x1, double y1, double x2,
                                  double y2, double strokeWidth)
    {
        return fidoLineToPCBLineElement(x1, y1, x1, y2, strokeWidth)
            + fidoLineToPCBLineElement(x1, y2, x2, y2, strokeWidth)
            + fidoLineToPCBLineElement(x2, y2, x2, y1, strokeWidth)
            + fidoLineToPCBLineElement(x2, y1, x1, y1, strokeWidth);
    }

    private String fidoArcToPCBArc(double midx, double midy,
                                   int dx, double thickness,
                                   boolean filled)
    {
        String arc = "";
        long exportedThickness = coordToPCB(thickness);
        if (exportedThickness < minExportedLineThickness) {
            exportedThickness = 1000;
        }
        if (filled) {
            arc = "\tArc["
                + coordToPCB(midx) + " "
                + coordToPCB(midy) + " "
                + coordToPCB(dx)/4 + " "
                + coordToPCB(dx)/4 + " "
                + coordToPCB(dx)/2 + " " // thickness
                + defaultClearance + " " // clearance
                + "0 360 " // start and stop in degrees
                + "\"clearline\"]\n";
        } else {
            arc = "\tArc["
                + coordToPCB(midx) + " "
                + coordToPCB(midy) + " "
                + coordToPCB(dx)/2 + " "
                + coordToPCB(dx)/2 + " " // is a circle, so dx = dy
                + exportedThickness + " " // thickness
                + defaultClearance + " " // clearance
                + "0 360 " // start and stop in degrees
                + "\"clearline\"]\n";
        }
        return arc;
    }
/*
    private String fidoArcToPCBArc(PointDouble loc, int dx,
                                   double thickness, boolean fill)
    {
        return fidoArcToPCBArc(loc.x, loc.y, dx, thickness, fill);
    }
*/
    private String fidoArcToPCBArcElement(double midx, double midy,
                                          int dx, double thickness,
                                          boolean filled)
    {
        String arc = "";
        long exportedThickness = coordToPCB(thickness);
        if (exportedThickness < minExportedLineThickness) {
            exportedThickness = 1000;
        }
        if (filled) {
            arc = "\tElementArc["
                + coordToPCB(midx) + " "
                + coordToPCB(midy) + " "
                + coordToPCB(dx)/4 + " "
                + coordToPCB(dx)/4 + " "
                + "0 360 " // start and stop in degrees
                + coordToPCB(dx)/2 // thickness
                + "]\n";
        } else {
            arc = "\tElementArc["
                + coordToPCB(midx) + " "
                + coordToPCB(midy) + " "
                + coordToPCB(dx)/2 + " "
                + coordToPCB(dx)/2 + " " // is a circle, so dx = dy
                + "0 360 " // start and stop in degrees
                + exportedThickness + " " // thickness
                + "]\n";
        }
        return arc;
    }

    private String fidoRectToPCBPadElement(double x1, double y1,
                                           double x2, double y2,
                                           int layer,
                                           int padCounter)
    {
        int actualLayer = 2;  // we'll assume SMD is on top surface for now
        double dx = x2-x1;
        double dy = y2-y1;
        double midx = dx/2.0 + x1;
        double midy = dy/2.0 + y1;
        double xX1 = 0;
        double yY1 = 0;
        double xX2 = 0;
        double yY2 = 0;
        if (dy < 0) {
            dy = -dy;
        }
        if (dx < 0) {
            dx = -dx;
        }
        double thickness = dy;
        if (dy > dx) { // taller than wide
            thickness = dx;
            xX1 = xX2 = midx;
            yY1 = midy + (dy - thickness)/2;
            yY2 = midy - (dy - thickness)/2;
        } else {
            yY1 = yY2 = midy;
            xX1 = midx + (dx - thickness)/2;
            xX2 = midx - (dx - thickness)/2;
        }
        String flags = "square";
        if (actualLayer == 1) {
            flags = "square,onsolder";
        }
        if (layer != 3) { // not silk
            return "\tPad[" // x1, y1, x2, y2 next
                + coordToPCB(xX1) + " "
                + coordToPCB(yY1) + " "
                + coordToPCB(xX2) + " "
                + coordToPCB(yY2) + " "
                + coordToPCB(thickness) + " " // thickness
                + defaultClearance + " " // then mask
                + (coordToPCB(thickness) + 600) + " "
                + "\"" + padCounter + "\" "
                + "\"" + padCounter + "\" "
                + "\"" + flags + "\"]\n"; //refdes, pinnum, flags
        } else { // silk rectangle/poly
            return "\tElementLine[" // x1, y1, x2, y2 thickness
                + coordToPCB(xX1) + " "
                + coordToPCB(yY1) + " "
                + coordToPCB(xX2) + " "
                + coordToPCB(yY2) + " "
                + coordToPCB(thickness) + "]\n";
        }
    }

    private String fidoPadToPCBPinElement(double x1, double y1,
                                          double dx, double dy,
                                          int drill, int style,
                                          int padCounter)
    {
        double thickness = dx;
        if (dx > dy) {
            thickness = dy;
        }
        String flags = "";
        String newPin = "\tPin[" // x1, y1, x2, y2 next
            + coordToPCB(x1) + " "
            + coordToPCB(y1) + " "
            + coordToPCB(thickness) + " "
            + defaultClearance + " " // then mask
            + (coordToPCB(thickness) + 600) + " "
            + coordToPCB(drill) + " "
            + "\"" + padCounter + "\" "
            + "\"" + padCounter + "\" "
            + "\"" + flags + "\"]\n"; //refdes, pinnum, flags
        if (style > 0) { // not round
            double xX1 = x1 - dx/2.0;
            double xX2 = x1 + dx/2.0;
            double yY1 = y1 - dy/2.0;
            double yY2 = y1 + dy/2.0;
            // we put a pad on the top and bottom layer
            newPin = newPin
                + fidoRectToPCBPadElement(xX1, yY1, xX2, yY2, 1,
                                          padCounter);
            newPin = newPin
                + fidoRectToPCBPadElement(xX1, yY1, xX2, yY2, 2,
                                          padCounter);
            // need to think about rounded corners
        }
        return newPin;
    }

    private String fidoPadToPCBPinElement(int x1, int y1,
                                          int dx, int dy,
                                          int drill, int style,
                                          int padCounter)
    {
        return fidoPadToPCBPinElement((double)x1, (double)y1,
                                      (double)dx, (double)dy,
                                      drill, style, padCounter);
    }

    private String fidoTextToPCBText(double x, double y,
                                     String text, int height,
                                     int orientation)
    {

        // overall text hight at 100% in gEDA is 5789 centimil
        // based on default_font 'm', 'l', p', 'q' glyphs
        // which includes default stroke thickness of 800 centimil
        // an additional scaling factor of 2 seems necessary
        long scaling = 2*100*coordToPCB(height)/5789; // in % of gEDA size
        int gEDAorientation = 0; // default
        if (orientation > 45 && orientation <= 135) {
            gEDAorientation = 1;
        } else if  (orientation > 135 && orientation <= 225) {
            gEDAorientation = 2;
        } else if  (orientation > 225 && orientation <= 315) {
            gEDAorientation = 3;
        }

        return "\tText["
            + coordToPCB(x) + " "
            + coordToPCB(y) + " "
            + gEDAorientation + " " // orientation = 0,1,2,3 (times 90)
            + scaling + " "
            + "\"" + text
            + "\" \"clearline\"]\n";
    }

    private String fidoTextToPCBText(int x, int y, String text,
                                     int height, int orientation)
    {
        return fidoTextToPCBText((double)x,
                                 (double)y, text, height, orientation);
    }

    private String
        fidoPolyToPCBPoly(PointDouble[] vertices, int nVertices,
                        double strokeWidth, boolean fill)
    {
        String newPoly= "";
        if (fill) { // solid poly
            newPoly = "\tPolygon(\"clearpoly\")\n"
                + "\t(\n"
                + "\t\t";
            for (int v = 0; v < nVertices; v++) {
                if (v < (nVertices - 1)) {
                    newPoly = newPoly
                        + "[" + fidoCoordToPCB(vertices[v].x)
                        + " " + fidoCoordToPCB(vertices[v].y) + "] ";
                } else {
                    newPoly = newPoly
                        + "[" + fidoCoordToPCB(vertices[v].x)
                        + " " + fidoCoordToPCB(vertices[v].y) + "]\n"
                        + ")\n";
                }
            }
        } else { // not filled
            newPoly
                = fidoPolylineToPCBLines(vertices, nVertices, strokeWidth);
            newPoly = newPoly +
                fidoLineToPCBLine(vertices[nVertices-1].x,
                                  vertices[nVertices-1].y,
                                  vertices[0].x,
                                  vertices[0].y,
                                  strokeWidth);
        }
        return newPoly;
    }

    private String
        fidoPolylineToPCBLines(PointDouble[] vertices, int nVertices,
                             double strokeWidth)
    {
        StringBuffer newPolylines = new StringBuffer();
        for (int v = 0; v < (nVertices - 1); v++) {
            newPolylines.append(
                fidoLineToPCBLine(vertices[v].x,
                                    vertices[v].y,
                                    vertices[v+1].x,
                                    vertices[v+1].y,
                                    strokeWidth));
        }
        return newPolylines.toString();
    }

    private String fidoRectToPCBPoly(int x1, int y1, int x2, int y2)
    {
        return "\tPolygon(\"clearpoly\")\n"
            + "\t(\n"
            + "\t\t[" + coordToPCB(x1) + " "
            + fidoCoordToPCB(y1)  + "] "
            + "[" + fidoCoordToPCB(x1) + " "
            + fidoCoordToPCB(y2)  + "] "
            + "[" + fidoCoordToPCB(x2) + " "
            + fidoCoordToPCB(y2)  + "] "
            + "[" + fidoCoordToPCB(x2) + " "
            + fidoCoordToPCB(y1)  + "]\n"
            + "\t)\n";
    }

    private String
        fidoRectToPCBLines(int x1, int y1, int x2, int y2,
                           double strokeWidth)
    {
        return fidoLineToPCBLine(x1, y1, x1, y2, strokeWidth)
            + fidoLineToPCBLine(x1, y2, x2, y2, strokeWidth)
            + fidoLineToPCBLine(x2, y2, x2, y1, strokeWidth)
            + fidoLineToPCBLine(x2, y1, x1, y1, strokeWidth);
    }

    private String fidoPadToPCBVia(double x,double y,int dia,int drill)//NOPMD
    {
        return "#FidoPadToPCBVia stub\n";
    }

    private long fidoCoordToPCB(double coord)
    {
        // coords are in multiples of 5mil = 127micron
        // if we export in centimil PCBcoord = 500x
        return (long)(500*coord); // are they using 25.4 microns??
    }


    private long coordToPCB(int coord)
    {
        // coords are in multiples of 5mil = 127micron
        // if we export in centimil PCBcoord = 500x
        return (long)(500*coord); // are they using 25.4 microns??
    }

    private long coordToPCB(double thickness)
    {
        // coords are in multiples of 5mil = 127micron
        // if we export in centimil PCBcoord = 500x
        return (long)(500*thickness); // are they using 25.4 microns??
    }

    private boolean footprintUnique(String macroName)
    {
        boolean duplicate = false;
        for (String el : fpList) {
            if (el.equals(macroName)) {
                duplicate = true;
            }
        }
        return !duplicate;
    }

    private void pushFootprint(String fp, String macroName)
    {
        fpList.add(macroName);
        footprints.add(fp);
    }

    private void pushElement(String el, int layer)
    {
        switch (layer) {
            // here where we map FidoCadJ's stackup to gEDA pcb-rnd's
            // default 16 layer stack up, where last two are silk
            case 0:  // we put circuit, if any on bottom silk, layer 15
                layerEls15.add(el);
                break;
            case 1:
                layerEls1.add(el); // bottom copper on layer 1
                break;
            case 2:
                layerEls2.add(el); // top copper on layer 2
                break;
            case 3:
                layerEls16.add(el); // top silk goes to layer 15
                break;
            case 4:
                layerEls3.add(el); // here starteth inner layers
                break;
            case 5:
                layerEls4.add(el);
                break;
            case 6:
                layerEls5.add(el);
                break;
            case 7:
                layerEls6.add(el);
                break;
            case 8:
                layerEls7.add(el);
                break;
            case 9:
                layerEls8.add(el);
                break;
            case 10:
                layerEls9.add(el);
                break;
            case 11:
                layerEls10.add(el);
                break;
            case 12:
                layerEls11.add(el);
                break;
            case 13:
                layerEls12.add(el);
                break;
            case 14:
                layerEls13.add(el);
                break;
            case 15:
                layerEls14.add(el); // here endeth inner layers
                break;
            default:
                break;
        }
    }

    private void writeElements(List<String> elements)
        throws IOException
    {
        for (String el : elements) {
            out.write(el);
        }
        elements.clear(); // in case we export again later; it's static.
    }

    private void writeFootprints()
        throws IOException
    {
        writeElements(footprints);
    }

    private void writeVias()
        throws IOException
    {
        writeElements(viaList);
    }

    private void writePins()
        throws IOException
    {
        writeElements(pinList);
    }

    private void writeLayers()
        throws IOException
    {
        for (int layer = 0; layer < 16; layer++) {
            switch (layer) {
                case 0:
                    out.write("Layer(1 \"B.Cu\")\n(\n");
                    writeElements(layerEls1);
                    out.write(")\n");
                    break;
                case 1:
                    out.write("Layer(2 \"F.Cu\")\n(\n");
                    writeElements(layerEls2);
                    out.write(")\n");
                    break;
                case 2:
                    out.write("Layer(3 \"Inner1.Cu\")\n(\n");
                    writeElements(layerEls3);
                    out.write(")\n");
                    break;
                case 3:
                    out.write("Layer(4 \"Inner2.Cu\")\n(\n");
                    writeElements(layerEls4);
                    out.write(")\n");
                    break;
                case 4:
                    out.write("Layer(5 \"Inner3.Cu\")\n(\n");
                    writeElements(layerEls5);
                    out.write(")\n");
                    break;
                case 5:
                    out.write("Layer(6 \"Inner4.Cu\")\n(\n");
                    writeElements(layerEls6);
                    out.write(")\n");
                    break;
                case 6:
                    out.write("Layer(7 \"Inner5.Cu\")\n(\n");
                    writeElements(layerEls7);
                    out.write(")\n");
                    break;
                case 7:
                    out.write("Layer(8 \"Inner6.Cu\")\n(\n");
                    writeElements(layerEls8);
                    out.write(")\n");
                    break;
                case 8:
                    out.write("Layer(9 \"Inner7.Cu\")\n(\n");
                    writeElements(layerEls9);
                    out.write(")\n");
                    break;
                case 9:
                    out.write("Layer(10 \"Inner8.Cu\")\n(\n");
                    writeElements(layerEls10);
                    out.write(")\n");
                    break;
                case 10:
                    out.write("Layer(11 \"Inner9.Cu\")\n(\n");
                    writeElements(layerEls11);
                    out.write(")\n");
                    break;
                case 11:
                    out.write("Layer(12 \"Inner10.Cu\")\n(\n");
                    writeElements(layerEls12);
                    out.write(")\n");
                    break;
                case 12:
                    out.write("Layer(13 \"Inner11.Cu\")\n(\n");
                    writeElements(layerEls13);
                    out.write(")\n");
                    break;
                case 13:
                    out.write("Layer(14 \"Inner12.Cu\")\n(\n");
                    writeElements(layerEls14);
                    out.write(")\n");
                    break;
                case 14:
                    out.write("Layer(15 \"B.SilkS\")\n(\n");
                    writeElements(layerEls15);
                    out.write(")\n");
                    break;
                case 15:
                    out.write("Layer(16 \"F.SilkS\")\n(\n");
                    writeElements(layerEls16);
                    out.write(")\n");
                    break;
                default:
                    System.out.println("Unknown layer number for layer out: "
                                       + layer);
                    break;
            }
        }
    }

}
