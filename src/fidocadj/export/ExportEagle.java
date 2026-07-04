package fidocadj.export;

import java.util.*;
import java.io.*;
import java.text.*;

import fidocadj.globals.Globals;
import fidocadj.layers.LayerDesc;
import fidocadj.primitives.MacroDesc;
import fidocadj.graphic.DimensionG;
import fidocadj.graphic.PointDouble;

/** Circuit export towards Cadsoft Eagle scripts.

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
   </pre>


    @author Davide Bucci
*/

public final class ExportEagle implements ExportInterface
{
    private final FileWriter fstream;
    private BufferedWriter out;
    private DimensionG dim;
    private int oldtextsize;
    private String macroList;
    private String junctionList;

    static final double text_stretch = 0.73;
    static final String EagleFidoLib = "FidoCadJLIB";
    static final String ExportFormatString = "####.####";

    // Conversion between FidoCadJ units and Eagle units (1/10 inches)
    static double res=5e-2;

    /** Constructor
        @param f the File object in which the export should be done.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public ExportEagle (File f) throws IOException
    {
        macroList = "";
        junctionList = "";
        fstream = new FileWriter(f);
    }

    /** Set the multiplication factor to be used for the dashing.
        @param u the factor.
    */
    public void setDashUnit(double u)
    {
        // Nothing particular to do here, dashing is not supported.
    }

    /** Set the "phase" in output units of the dashing style.
        For example, if a dash style is composed by a line followed by a space
        of equal size, a phase of 0 indicates that the dash starts with the
        line.
        @param p the phase, in output units.
    */
    public void setDashPhase(float p)
    {
        // Nothing particular to do here, dashing is not supported.
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
        dim=totalSize;
        out = new BufferedWriter(fstream);
        oldtextsize=-1;
        macroList = "";
        junctionList = "";
        // A basic configuration of an Eagle script

        out.write("# Created by FidoCadJ "+Globals.version+
            " by Davide Bucci\n");
        out.write("Set Wire_Bend 2; \n");
        out.write("Grid inch "+een(grid*res)+";\n");
        out.write("Change font fixed;\n");
        out.write("Set auto_junction off;\n");
    }

    /** Called at the end of the export phase.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportEnd()
        throws IOException
    {
        out.write(macroList);
        out.write(junctionList);
        out.write("Window Fit; \n");
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
        String mirror="";

        if(isMirrored) {
            mirror="M";
        }

        if(oldtextsize!=sizey) {
            out.write("Change size "+sizey*res*text_stretch+"\n");
        }
        oldtextsize=(int)sizey;

        out.write("Text "+text+" "+mirror+"R"+(-orientation)+" ("+een(x*res)+
            " "+een((dim.height-y)*res)+");\n");
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
        out.write("# Bézier export not implemented yet\n");
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
        junctionList += "Junction ("+een(x*res)+" "
            +een((dim.height-y)*res)+");\n";
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
        out.write("Net ("+een(x1*res)+" "+een((dim.height-y1)*res)+") ("+
            een(x2*res)+" "+een((dim.height-y2)*res)+");\n");
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
        int orientation, String macroName, String macroDesc,
        String tname, int xn, int yn, String value, int xv, int yv, String font,
        int fontSize, Map<String, MacroDesc> m)
        throws IOException
    {
        String mirror ="";
        if (isMirrored) {
            mirror = "M";
        }

        // The component name should not contain spaces. Substitute with
        // the underline character.
        Map<String, String> subst = new HashMap<String, String>();
        subst.put(" ","_");
        String name=Globals.substituteBizarreChars(tname, subst);

        macroList += "Add "+ macroName+"@"+EagleFidoLib+ " "+name+" "+mirror+"R"
            +(-orientation)+" ("+een(x*res)+" "+een((dim.height-y)*res)+");\n";

        macroList +="Value "+name+" "+value+";\n";

        // The macro will NOT be expanded into primitives.
        return true;
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
        out.write("# Circle export not fully implemented\n");

        out.write("Circle ("+een(x1*res)+" "+een((dim.height-y1)*res)+") ("
            +een((x2-x1)*res)+ " "+een((y2-y1)*res)+");");
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
        out.write("# PCBLine export not implemented yet\n");
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
        // At first, draw the pad...
        if(!onlyHole) {
            switch (style) {
                case 1: // Square pad
                    break;
                case 2: // Rounded pad
                    break;
                case 0: // Oval pad
                default:
                    break;
            }
        }
        // ... then, drill the hole!
        out.write("# PCBpad export not implemented yet\n");
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
        out.write("# Polygon export not implemented yet\n");
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
        out.write("Layer 94;\n");

        if(isFilled) {
            out.write("Rect ("+een(x1*res)+" "+een((dim.height-y1)*res)+
                  ") ("+een(x2*res)+" "+een((dim.height-y2)*res)+");\n");
        } else {
            out.write("Set Wire_Bend 0;\n");
            out.write("Wire ("+een(x1*res)+" "+een((dim.height-y1)*res)+") ("+
                een(x2*res)+" "+een((dim.height-y2)*res)+");\n");
            out.write("Wire ("+een(x2*res)+" "+een((dim.height-y2)*res)+") ("+
                een(x1*res)+" "+een((dim.height-y1)*res)+");\n");
            out.write("Set Wire_Bend 2;\n");
        }

        out.write("Layer 91;\n");
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
        return false;
    }

    /** Called when exporting an embedded raster Image primitive. Eagle
        scripts have no command to import a raster image reachable from
        this text-based scripting interface, so a placeholder rectangle
        with a "[immagine]" label is drawn instead.

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
        out.write("Layer 94;\n");
        out.write("Set Wire_Bend 0;\n");
        out.write("Wire ("+een(x1*res)+" "+een((dim.height-y1)*res)+") ("+
            een(x2*res)+" "+een((dim.height-y1)*res)+");\n");
        out.write("Wire ("+een(x2*res)+" "+een((dim.height-y1)*res)+") ("+
            een(x2*res)+" "+een((dim.height-y2)*res)+");\n");
        out.write("Wire ("+een(x2*res)+" "+een((dim.height-y2)*res)+") ("+
            een(x1*res)+" "+een((dim.height-y2)*res)+");\n");
        out.write("Wire ("+een(x1*res)+" "+een((dim.height-y2)*res)+") ("+
            een(x1*res)+" "+een((dim.height-y1)*res)+");\n");
        out.write("Set Wire_Bend 2;\n");
        out.write("Text [immagine] R0 ("+een(((x1+x2)/2.0)*res)+" "+
            een((dim.height-(y1+y2)/2.0)*res)+");\n");
        out.write("Layer 91;\n");
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

    /** Export a number: truncate it to four decimals

    */
    private String een(double n)
    {
        // Force the Java system to use ALWAYS the dot as a decimal separator,
        // regardless the locale settings (in Italy and France, the
        // decimal separator is the comma).

        DecimalFormatSymbols separators = new DecimalFormatSymbols();
        separators.setDecimalSeparator('.');

        DecimalFormat exportFormat = new DecimalFormat(ExportFormatString,
            separators);
        return exportFormat.format(n);
    }
}