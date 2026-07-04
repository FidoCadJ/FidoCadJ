package fidocadj.export;

import java.util.*;
import java.io.*;
import java.text.*;

import fidocadj.globals.Globals;
import fidocadj.layers.LayerDesc;
import fidocadj.primitives.MacroDesc;
import fidocadj.primitives.Arrow;
import fidocadj.graphic.DimensionG;
import fidocadj.graphic.ColorInterface;
import fidocadj.graphic.DecoratedText;
import fidocadj.graphic.PointDouble;
import fidocadj.graphic.TextInterface;

/**
    Drawing export in Encapsulated Postscript

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

public final class ExportEPS implements ExportInterface, TextInterface
{
    private final FileWriter fstream;
    private BufferedWriter out;
    private List layerV;
    private double actualWidth;
    private ColorInterface actualColor;
    private int currentDash;
    private float dashPhase;
    private float currentPhase=-1;
    private float currentFontSize=0;
    private DecoratedText dt;
    private String fontname;        // Some info about the font is stored
    private String bold="";
    private float textx;            // This is used in sub-sup scripts position
    private float texty;

    // Number of digits to be used when representing coordinates
    static final int PREC = 3;
    // Dash patterns
    private String sDash[];

    /*
    static final String dash[]={"[5.0 10]", "[2.5 2.5]",
        "[1.0 1.0]", "[1.0 2.5]", "[1.0 2.5 2.5 2.5]"};*/

    /** Set the multiplication factor to be used for the dashing.
        @param u the factor.
    */
    public void setDashUnit(double u)
    {
        sDash = new String[Globals.dashNumber];
        // If the line width has been changed, we need to update the
        // stroke table

        // The first entry is non dashed
        sDash[0]="";

        // Resize the dash sizes depending on the current zoom size.
        String dashArrayStretched;
        // Then, the dashed stroke styles are created.
        for(int i=1; i<Globals.dashNumber; ++i) {
            // Prepare the resized dash array.
            dashArrayStretched = "";
            for(int j=0; j<Globals.dash[i].length;++j) {
                dashArrayStretched+=(Globals.dash[i][j]*(float)u/2.0f);
                if(j<Globals.dash[i].length-1) {
                    dashArrayStretched+=" ";
                }
            }
            sDash[i]="["+dashArrayStretched+"]";
        }
    }

    /** Set the "phase" in output units of the dashing style.
        For example, if a dash style is composed by a line followed by a space
        of equal size, a phase of 0 indicates that the dash starts with the
        line.
        @param p the phase, in output units.
    */
    public void setDashPhase(float p)
    {
        dashPhase=p;
    }

    /** Constructor
        @param f the File object in which the export should be done.
        @throws IOException when things goes horribly wrong, for example if
            the file specified is not accessible.
    */
    public ExportEPS (File f) throws IOException
    {
        fstream = new FileWriter(f);
        dt=new DecoratedText(this);
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
        // We need to save layers informations, since we will use them later.

        layerV=la;
        out = new BufferedWriter(fstream);

        // An header of the EPS file

        // 200 dpi is the internal resolution of FidoCadJ
        // 72 dpi is the internal resolution of the Postscript coordinates

        double resMult=200.0/72.0;

        //resMult /= getMagnification();

        out.write("%!PS-Adobe-3.0 EPSF-3.0\n");
        out.write("%%Pages: 0\n");
        out.write("%%BoundingBox: -1 -1 "+(int)(totalSize.width/resMult+1)+" "+
            (int)(totalSize.height/resMult+1)+"\n");
        out.write("%%Creator: FidoCadJ "+Globals.version+
            ", EPS export filter by Davide Bucci\n");

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",
            Locale.forLanguageTag("en"));
        Date date = new Date();
        out.write("%%CreationDate: "+dateFormat.format(date)+"\n");
        out.write("%%EndComments\n");


        // Create a new dictionary term: ellipse
        // This is based on an example of the Blue Book
        // http://www.science.uva.nl/~robbert/ps/bluebook/program_03.html
        out.write("/ellipsedict 8 dict def\n"+
            "ellipsedict /mtrx matrix put\n"+
            "/ellipse\n"+
            "   { ellipsedict begin\n"+
            "     /endangle exch def\n"+
            "     /startangle exch def\n"+
            "     /yrad exch def\n"+
            "     /xrad exch def\n"+
            "     /y exch def\n"+
            "     /x exch def\n"+
            "     /savematrix mtrx currentmatrix def\n"+
            "     x y translate\n"+
            "     xrad yrad scale\n"+
            "     0 0 1 startangle endangle arc\n"+
            "     savematrix setmatrix\n"+
            "     end\n"+
            "   } def\n");


        // Since in a postscript drawing, the origin is at the bottom left,
        // we introduce a coordinate transformation to have it at the top
        // left of the drawing.

        out.write("0 "+(totalSize.height/resMult)+" translate\n");
        out.write(""+(1/resMult)+" "+(-1/resMult)+" scale\n");
    }

    /** Called at the end of the export phase.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportEnd()
        throws IOException
    {
        out.write("%%EOF\n");
        out.close();
    }

    /** Called when exporting an Advanced Text primitive.
        @param x the x position of the beginning of the string to be written.
        @param y the y position of the beginning of the string to be written.
        @param sizex the x size of the font to be used.
        @param sizey the y size of the font to be used.
        @param fontnameT the font to be used.
        @param isBold true if the text should be written with a boldface font.
        @param isMirrored true if the text should be mirrored.
        @param isItalic true if the text should be written with an italic font.
        @param orientation angle of orientation (degrees).
        @param layer the layer that should be used.
        @param textT the text that should be written.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportAdvText (int x, int y, double sizex, double sizey,
        String fontnameT, boolean isBold, boolean isMirrored, boolean isItalic,
        int orientation, int layer, String textT)
        throws IOException
    {
        String text = textT;
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();
        checkColorAndWidth(c, -1);
        currentFontSize = (int)(sizex*12/(double)7+.5);
        fontname = fontnameT;

        if(isBold) {
            bold="-Bold";
        } else {
            bold="";
        }

        // It seems that Postscript fonts can not handle spaces. So I substitute
        // every space with a "-" sign.

        Map<String, String> substFont = new HashMap<String, String>();
        substFont.put(" ","-");
        fontname=Globals.substituteBizarreChars(fontname, substFont);
        out.write("/"+fontname+bold+" findfont\n"+
            (int)currentFontSize+" scalefont\n"+
            "setfont\n");
        out.write("newpath\n");

        out.write("" +x+" "+y+" moveto\n");
        textx=x;
        texty=y;
        out.write("gsave\n");

        if(orientation !=0) {
            out.write("  "+(-orientation)+" rotate\n");
        }

        if(isMirrored) {
            out.write("  -1 -1 scale\n");
        } else {
            out.write("  1 -1 scale\n");
        }

        // Remember that we consider sizex/sizey=7/12 as the "normal" aspect
        // ratio.
        double ratio;

        if((int)(sizey/sizex) == 10/7){
            ratio = 1.0;
        } else {
            ratio=(double)sizey/(double)sizex*22.0/40.0;
        }

        out.write("  "+1+" "+ratio+" scale\n");
        out.write("  0 " +(-currentFontSize*0.8)+" rmoveto\n");

        checkColorAndWidth(c, 0.33);


        Map<String, String> subst = new HashMap<String, String>();
        subst.put("(","\\050");
        subst.put(")","\\051");
        text=Globals.substituteBizarreChars(text, subst);

        dt.drawString(text,x,y);
        out.write("grestore\n");
    }

    /** Called when exporting a Bézier primitive.
        @param x1 the x position of the first point of the trace
        @param y1 the y position of the first point of the trace
        @param x2 the x position of the second point of the trace
        @param y2 the y position of the second point of the trace
        @param x3 the x position of the third point of the trace
        @param y3 the y position of the third point of the trace
        @param x4 the x position of the fourth point of the trace
        @param y4 the y position of the fourth point of the trace
        @param layer the layer that should be used

                // from 0.22.1

        @param arrowStart true if an arrow is present at the first point.
        @param arrowEnd true if an arrow is present at the second point.
        @param arrowStyle the style of the arrow.
        @param arrowLength total lenght of arrows (if present).
        @param arrowHalfWidth half width of arrows (if present).
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.
        @throws IOException when things goes horribly wrong, for example if
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
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();
        checkColorAndWidth(c, strokeWidth);
        registerDash(dashStyle);

        if (arrowStart) {
            PointPr p=exportArrow(x1, y1, x2, y2, arrowLength,
                arrowHalfWidth, arrowStyle);
            // This fixes issue #172
            // If the arrow length is negative, the arrow extends
            // outside the line, so the limits must not be changed.
            if(arrowLength>0) {
                x1=(int)Math.round(p.x);
                y1=(int)Math.round(p.y);
            }
        }
        if (arrowEnd) {
            PointPr p=exportArrow(x4, y4, x3, y3, arrowLength,
                arrowHalfWidth, arrowStyle);
            // Fix #172
            if(arrowLength>0) {
                x4=(int)Math.round(p.x);
                y4=(int)Math.round(p.y);
            }
        }
        out.write(""+x1+" "+y1+" moveto \n");
        out.write(""+x2+" "+y2+" "+x3+" "+y3+" "+x4+" "+y4+" curveto stroke\n");

    }

    /** Called when exporting a Connection primitive.

        @param x the x position of the position of the connection.
        @param y the y position of the position of the connection.
        @param layer the layer that should be used.
        @param nodeSize the size of the electrical connection in logical
            units.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportConnection (int x, int y, int layer, double nodeSize)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();

        checkColorAndWidth(c, 0.33);


        out.write("newpath\n");
        out.write(""+x+" "+y+" "+
            nodeSize/2.0+ " " + nodeSize/2.0+
            " 0 360 ellipse\n");
        out.write("fill\n");
    }

    /** Called when exporting a Line primitive.
        @param x1 the x position of the first point of the segment
        @param y1 the y position of the first point of the segment
        @param x2 the x position of the second point of the segment
        @param y2 the y position of the second point of the segment
        @param layer the layer that should be used

        // from 0.22.1

        @param arrowStart specify if an arrow is present at the first point.
        @param arrowEnd specify if an arrow is present at the second point.
        @param arrowStyle the style of the arrow.
        @param arrowLength total lenght of arrows (if present).
        @param arrowHalfWidth half width of arrows (if present).
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.
        @throws IOException when things goes horribly wrong, for example if
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
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();
        checkColorAndWidth(c, strokeWidth);
        registerDash(dashStyle);
        double xstart=x1;
        double ystart=y1;
        double xend=x2;
        double yend=y2;

        if (arrowStart) {
            PointPr p=exportArrow(x1, y1, x2, y2, arrowLength,
                arrowHalfWidth, arrowStyle);
            // This fixes issue #172
            // If the arrow length is negative, the arrow extends
            // outside the line, so the limits must not be changed.
            if(arrowLength>0) {
                xstart=p.x;
                ystart=p.y;
            }
        }
        if (arrowEnd) {
            PointPr p=exportArrow(x2, y2, x1, y1, arrowLength,
                arrowHalfWidth, arrowStyle);
            // Fix #172
            if(arrowLength>0) {
                xend=p.x;
                yend=p.y;
            }
        }
        out.write(""+xstart+" "+ystart+" moveto "+
            xend+" "+yend+" lineto stroke\n");
    }

    /** Called when exporting an arrow.
        @param x position of the tip of the arrow.
        @param y position of the tip of the arrow.
        @param xc direction of the tip of the arrow.
        @param yc direction of the tip of the arrow.
        @param l length of the arrow.
        @param h width of the arrow.
        @param style style of the arrow.
        @return the coordinates of the base of the arrow.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public PointPr exportArrow(double x, double y, double xc, double yc,
        double l, double h,
        int style)
        throws IOException
    {
        double alpha;
        double x0;
        double y0;
        double x1;
        double y1;
        double x2;
        double y2;

        // At first we need the angle giving the direction of the arrow
        // a little bit of trigonometry :-)

        if (x==xc) {
            alpha = Math.PI/2.0+(y-yc<0.0?0.0:Math.PI);
        } else {
            alpha = Math.atan((double)(y-yc)/(double)(x-xc));
        }

        alpha += x-xc>0.0?0.0:Math.PI;

        // Then, we calculate the points for the polygon
        x0 = x - l*Math.cos(alpha);
        y0 = y - l*Math.sin(alpha);

        x1 = x0 - h*Math.sin(alpha);
        y1 = y0 + h*Math.cos(alpha);

        x2 = x0 + h*Math.sin(alpha);
        y2 = y0 - h*Math.cos(alpha);

        out.write("newpath\n");

        out.write(""+Globals.roundTo(x)+" "+    Globals.roundTo(y)+" moveto\n");
        out.write(""+Globals.roundTo(x1)+" "+Globals.roundTo(y1)+" lineto\n");
        out.write(""+Globals.roundTo(x2)+" "+Globals.roundTo(y2)+" lineto\n");

        out.write("closepath\n");


        if ((style & Arrow.flagEmpty) == 0) {
            out.write("fill \n");
        } else {
            out.write("stroke \n");
        }

        if ((style & Arrow.flagLimiter) != 0) {
            double x3;
            double y3;
            double x4;
            double y4;
            x3 = x - h*Math.sin(alpha);
            y3 = y + h*Math.cos(alpha);

            x4 = x + h*Math.sin(alpha);
            y4 = y - h*Math.cos(alpha);
            out.write(""+Globals.roundTo(x3)+" "+Globals.roundTo(y3)+
                " moveto\n"+Globals.roundTo(x4)+" "+Globals.roundTo(y4)+
                " lineto\nstroke\n");
        }
        return new PointPr(x0,y0);
    }

    /** Called when exporting a Macro call.
        This function can just return false, to indicate that the macro should
        be rendered by means of calling the other primitives. Please note that
        a macro does not have a reference layer, since the elements composing
        it already have their own.

        @param x the x position of the position of the macro.
        @param y the y position of the position of the macro.
        @param isMirrored true if the macro is mirrored.
        @param orientation the macro orientation in degrees.
        @param macroName the macro name.
        @param macroDesc the macro description, in the FidoCad format.
        @param name the shown name.
        @param xn coordinate of the shown name.
        @param yn coordinate of the shown name.
        @param value the shown value.
        @param xv coordinate of the shown value.
        @param yv coordinate of the shown value.
        @param font the used font.
        @param fontSize the size of the font to be used.
        @param m the library.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
        @return true if the macro is exported as a whole, false if it should be
            expanded into primitives.
    */
    public boolean exportMacro(int x, int y, boolean isMirrored,
        int orientation, String macroName, String macroDesc,
        String name, int xn, int yn, String value, int xv, int yv, String font,
        int fontSize, Map<String, MacroDesc> m)
        throws IOException
    {
        // The macro will be expanded into primitives.
        return false;
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
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();

        checkColorAndWidth(c, strokeWidth);
        registerDash(dashStyle);

        out.write("newpath\n");
        out.write(""+(x1+x2)/2.0+" "+(y1+y2)/2.0+" "+
            Math.abs(x2-x1)/2.0+ " " + Math.abs(y2-y1)/2.0+
            " 0 360 ellipse\n");
        if(isFilled) {
            out.write("fill\n");
        } else {
            out.write("stroke\n");
        }
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
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();

        checkColorAndWidth(c, width);
        registerDash(0);

        out.write("1 setlinecap\n");
        out.write(""+x1+" "+y1+" moveto "+
            x2+" "+y2+" lineto stroke\n");
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
        @param onlyHole true if only the hole (drill) should be exported.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportPCBPad(int x, int y, int style, int six, int siy,
        int indiam, int layer, boolean onlyHole)
        throws IOException
    {
        double xdd;
        double ydd;

        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();

        checkColorAndWidth(c, 0.33);

        // At first, draw the pad...
        if(!onlyHole) {
            switch (style) {
                case 2: // Rounded pad
                    roundRect(x-six/2.0, y-siy/2.0,
                        six, siy, 4, true);
                    break;

                case 1: // Square pad
                    xdd=(double)x-six/2.0;
                    ydd=(double)y-siy/2.0;
                    out.write("newpath\n");
                    out.write(""+xdd+" "+ydd+" moveto\n");
                    out.write(""+(xdd+six)+" "+ydd+" lineto\n");
                    out.write(""+(xdd+six)+" "+(ydd+siy)+" lineto\n");
                    out.write(""+xdd+" "+(ydd+siy)+" lineto\n");
                    out.write("closepath\n");
                    out.write("fill\n");
                    break;
                case 0: // Oval pad
                default:
                    out.write("newpath\n");
                    out.write(""+x+" "+y+" "+
                        six/2.0+ " " +siy/2.0+
                        " 0 360 ellipse\n");
                    out.write("fill\n");
                    break;
            }
        }
            // ... then, drill the hole!

        //out.write("1 1 1 setrgbcolor\n");
        checkColorAndWidth(c.white(), 0.33);

        out.write("newpath\n");
        out.write(""+x+" "+y+" "+
                    indiam/2.0+ " " +indiam/2.0+
                    " 0 360 ellipse\n");
        out.write("fill\n");
    }

    /** Called when exporting a Polygon primitive.
        @param vertices array containing the position of each vertex.
        @param nVertices number of vertices.
        @param isFilled true if the polygon is filled.
        @param layer the layer that should be used.
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.
    */
    public void exportPolygon(PointDouble[] vertices, int nVertices,
        boolean isFilled, int layer, int dashStyle, double strokeWidth)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();

        if (nVertices<1) {
            return;
        }

        checkColorAndWidth(c, strokeWidth);
        registerDash(dashStyle);

        out.write("newpath\n");

        out.write(""+vertices[0].x+" "+vertices[0].y+" moveto\n");

        for (int i=1; i<nVertices; ++i) {
            out.write(""+vertices[i].x+" "+vertices[i].y+" lineto\n");
        }

        out.write("closepath\n");
        if(isFilled) {
            out.write("fill\n");
        } else {
            out.write("stroke\n");
        }
    }

    /** Called when exporting a Curve primitive.
        @param vertices array containing the position of each vertex.
        @param nVertices number of vertices.
        @param isFilled true if the polygon is filled.
        @param isClosed true if the curve is closed.
        @param layer the layer that should be used.
        @param arrowStart true if an arrow is present at the first point.
        @param arrowEnd true if an arrow is present at the second point.
        @param arrowStyle the style of the arrow.
        @param arrowLength the length of the arrow.
        @param arrowHalfWidth the half width of the arrow.
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.
        @throws IOException when things goes horribly wrong, for example if
            the file in which the output is being done is not accessible.

        @return false if the curve should be rendered using a polygon, true
            if it is handled by the function.
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
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();

        checkColorAndWidth(c, strokeWidth);
        registerDash(dashStyle);
        out.write("newpath\n");
        out.write(""+Globals.roundTo(x1,PREC)+" "+Globals.roundTo(y1,PREC)+
            " moveto\n");
        out.write(""+Globals.roundTo(x2,PREC)+" "+Globals.roundTo(y1,PREC)+
            " lineto\n");
        out.write(""+Globals.roundTo(x2,PREC)+" "+Globals.roundTo(y2,PREC)+
            " lineto\n");
        out.write(""+Globals.roundTo(x1,PREC)+" "+Globals.roundTo(y2,PREC)+
            " lineto\n");
        out.write("closepath\n");
        if(isFilled) {
            out.write("fill\n");
        } else {
            out.write("stroke\n");
        }
    }

    /** Called when exporting an embedded raster Image primitive. This
        hand-written EPS exporter does not embed raster images (a real
        DCT/ASCII85 image operator was judged too high-risk to get right
        without a way to validate the resulting file): a dashed
        placeholder rectangle with a "[immagine]" label is drawn instead.

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
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();
        checkColorAndWidth(c, 0.33);
        registerDash(1);

        out.write("newpath\n");
        out.write(""+Globals.roundTo(x1,PREC)+" "+Globals.roundTo(y1,PREC)+
            " moveto\n");
        out.write(""+Globals.roundTo(x2,PREC)+" "+Globals.roundTo(y1,PREC)+
            " lineto\n");
        out.write(""+Globals.roundTo(x2,PREC)+" "+Globals.roundTo(y2,PREC)+
            " lineto\n");
        out.write(""+Globals.roundTo(x1,PREC)+" "+Globals.roundTo(y2,PREC)+
            " lineto\n");
        out.write("closepath\n");
        out.write("stroke\n");
        registerDash(0);

        out.write(""+((x1+x2)/2)+" "+((y1+y2)/2)+" moveto\n");
        out.write("gsave\n");
        out.write("  /Helvetica findfont 12 scalefont setfont\n");
        out.write("  1 -1 scale\n");
        out.write("  ([immagine]) show\n");
        out.write("grestore\n");
    }

    private void roundRect (double x1, double y1, double w, double h,
        double r, boolean filled)
        throws IOException
    {
        out.write(""+(x1+r) + " " +y1+" moveto\n");
        out.write(""+(x1+w-r) + " " +y1+" lineto\n");
        out.write(""+(x1+w) + " " +y1+" "+ (x1+w) + " "+y1
            + " "+(x1+w) + " " +(y1+r)+ " curveto\n");

        out.write(""+(x1+w) + " " +(y1+h-r)+" lineto\n");
        out.write(""+(x1+w) + " " +(y1+h)+" "+(x1+w) + " " +(y1+h)+
            " "+(x1+w-r)+" "+(y1+h)+" curveto\n");

        out.write(""+ (x1+r) + " " +(y1+h)+" lineto\n");
        out.write(""+ x1+ " " +(y1+h)+" "+ x1+ " " +(y1+h)+
            " "+ x1 + " " +(y1+h-r)+" curveto\n");

        out.write(""+ x1 + " " +(y1+r)+" lineto\n");
        out.write(""+x1 + " " +y1+" "+x1 + " " +y1+" "+
            (x1+r)+" "+y1+" curveto\n");

        out.write("  "+(filled?"fill\n":"stroke\n"));
    }

    /** Set the current color (only if necessary) and the stroke width which
        will be employed for subsequent drawing operations.
        @param c the color
        @param wl the stroke width. If a negative number is employed, a new
            stroke width will not be set.
    */
    private void checkColorAndWidth(ColorInterface c, double wl)
        throws IOException
    {
        if(!c.equals(actualColor)) {
            out.write("  "+Globals.roundTo(c.getRed()/255.0)+" "+
                Globals.roundTo(c.getGreen()/255.0)+ " "
                +Globals.roundTo(c.getBlue()/255.0)+ " setrgbcolor\n");

            actualColor=c;
        }
        if(wl>0 && wl != actualWidth) {
            out.write("  " +wl+" setlinewidth\n");
            actualWidth = wl;
        }
    }

    private void registerDash(int dashStyle)
        throws IOException
    {
        if(currentDash!=dashStyle ||currentPhase!=dashPhase) {
            currentDash=dashStyle;
            currentPhase=dashPhase;
            if(dashStyle==0) {
                out.write("[] 0 setdash\n");
            } else {
                out.write(""+sDash[dashStyle]+" "+dashPhase+" setdash\n");
            }
        }
    }

    // Functions required for the TextInterface.

    /** Get the font size.
        @return the font size.
    */
    public double getFontSize()
    {
        return currentFontSize;
    }

    /** Set the font size.
        @param size the font size.
    */
    public void setFontSize(double size)
    {
        currentFontSize=(float)size;
        try {
            out.write("/"+fontname+bold+" findfont\n"+
                (int)currentFontSize+" scalefont\n"+
                "setfont\n");
        } catch(IOException e) {
            System.err.println("Can not write to file in EPS export.");
        }
    }

    /** Get the width of the given string with the current font.
        @param s the string to be used.
        @return the width of the string, in pixels.
    */
    public int getStringWidth(String s)
    {
        return 0;
    }

    /** Draw a string on the current graphic context.
        @param str the string to be drawn.
        @param x the x coordinate of the starting point.
        @param y the y coordinate of the starting point.
    */
    public void drawString(String str,
                                int x,
                                int y)
    {
        try{
            out.write("" + (textx-x) +" "+ (texty-y)+ " rmoveto\n");
            texty=y;
            out.write("  ("+str+") show\n");
        } catch(IOException e) {
            System.err.println("Can not write to file in EPS export.");
        }
    }
}
