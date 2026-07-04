package fidocadj.export;

import java.util.*;
import java.io.*;

import fidocadj.globals.Globals;
import fidocadj.layers.LayerDesc;
import fidocadj.primitives.Arrow;
import fidocadj.graphic.DimensionG;
import fidocadj.graphic.GraphicsInterface;
import fidocadj.graphic.ColorInterface;
import fidocadj.graphic.DecoratedText;
import fidocadj.graphic.PointDouble;
import fidocadj.graphic.TextInterface;

/**
    Export drawing in the Scalable Vector Graphics format.

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

public final class ExportSVG implements ExportInterface, TextInterface
{
    //private File fileExp;
    final private OutputStreamWriter fstream;
    private BufferedWriter out;
    private List layerV;

    private ColorInterface c;       // Current colour (used in advText export)
    private float layerAlpha=1.0f;  // Current layer opacity
    private double strokeWidth;
    private String sDash[];
    private float dashPhase;
    private float currentPhase=-1;
    private float currentFontSize=0;
    private double baseFontAscent=0;  // Ascent of the base text font
    private DecoratedText dt;
    private String fontname;        // Some info about the font is stored
    private float textx;            // This is used in sub-sup scripts position
    private float texty;
    private boolean isItalic;
    private boolean isBold;

    // A graphic interface object is used here to get information about the
    // size of the different glyphs in the font.
    private final GraphicsInterface gi;
    /*
    static final String dash[]={"2.5,5", "1.25,1.25",
        "0.5,0.5", "0.5,1.25", "0.5,1.25,1.25,1.25"};*/

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
                    dashArrayStretched+=",";
                }
            }
            sDash[i]=dashArrayStretched;
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

    private double cLe(double l)
    {
        //return (int)(l*sizeMagnification);
        return Math.round(l*100.0)/100.0;
    }
    /** Constructor.
        @param f the File object in which the export should be done.
        @param g the GraphicsInterface needed for things such as font size
            calculations, etc.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public ExportSVG (File f, GraphicsInterface g) throws IOException
    {
        gi=g;
        fstream = new OutputStreamWriter(new FileOutputStream(f),
            Globals.encoding);
        dt = new DecoratedText(this);
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
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportStart(DimensionG totalSize, List<LayerDesc> la,
        int grid)
        throws IOException
    {
        // We need to save layers informations, since we will use them later.

        layerV=la;
        out = new BufferedWriter(fstream);
        //numberPath=0;

        int wi=(int)totalSize.width;
        int he=(int)totalSize.height;

        // A dumb, basic header of the SVG file

        // Globals.encoding
        out.write("<?xml version=\"1.0\" encoding=\""+"UTF-8"+"\" " +
            "standalone=\"no\"?> \n<!DOCTYPE svg PUBLIC"+
            " \"-//W3C//Dtd SVG 1.1//EN\" " +
            "\"http://www.w3.org/Graphics/SVG/1.1/Dtd/svg11.dtd\">\n"+
            "<svg width=\""+cLe(wi)+"\" height=\""+cLe(he)+
            "\" version=\"1.1\" " + "xmlns=\"http://www.w3.org/2000/svg\" " +
            "xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n"+
            "<!-- Created by FidoCadJ ver. "+Globals.version+
            ", export filter by Davide Bucci -->\n");
    }

    /** Called at the end of the export phase.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportEnd()
        throws IOException
    {
        out.write("</svg>");
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
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportAdvText (int x, int y, double sizex, double sizey,
        String fontname, boolean isBold, boolean isMirrored, boolean isItalic,
        int orientation, int layer, String text)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        c=l.getColor();
        layerAlpha=l.getAlpha();
        this.isItalic=isItalic;
        this.isBold=isBold;
        this.fontname=fontname;

        /*  THIS VERSION OF TEXT EXPORT IS NOT COMPLETE! IN PARTICULAR,
            MIRRORING EFFECTS, ANGLES AND A PRECISE SIZE CONTROL IS NOT
            HANDLED
        */

        setFontSize(sizex*12.0/7.0+.5);
        gi.setFont(fontname, currentFontSize);
        baseFontAscent = gi.getFontAscent();
        double yscale;
        if((int)(sizey/sizex) == 10/7){
            yscale = 1.0;
        } else {
            yscale=(double)sizey/(double)sizex*22.0/40.0;
        }
        double xscale = isMirrored ? -1 : 1;

        out.write("<g transform=\"translate("+cLe(x)+","+cLe(y)+")");
        if(orientation !=0) {
            out.write(" rotate("+(-orientation)+")");
        }
        out.write(" scale("+xscale+","+yscale+")");
        out.write("\">");
        textx=x;
        texty=y;
        dt.drawString(text,x,y);
        out.write("</g>\n");
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
        @param sW the width of the pen to be used when drawing.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
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
        double sW)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        c=l.getColor();
        layerAlpha=l.getAlpha();

        strokeWidth=sW;

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
        out.write("<path d=\"M "+cLe(x1)+","+cLe(y1)+" C "+
                  cLe(x2)+ ","+cLe(y2)+" "+cLe(x3)+","+cLe(y3)+" "+cLe(x4)+
                  ","+cLe(y4)+"\" ");
        checkColorAndWidth("fill=\"none\"", dashStyle);
    }

    /** Called when exporting a Connection primitive.

        @param x the x position of the position of the connection.
        @param y the y position of the position of the connection.
        @param layer the layer that should be used.
        @param nodeSize the size of the connection in logical units.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportConnection (int x, int y, int layer, double nodeSize)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        c=l.getColor();
        layerAlpha=l.getAlpha();
        strokeWidth = cLe(0.33);

        out.write("<circle cx=\""+cLe(x)+"\" cy=\""+cLe(y)+"\""+
            " r=\""+cLe(nodeSize/2.0)+"\" style=\"stroke:#"+
                  convertToHex2(c.getRed())+
                  convertToHex2(c.getGreen())+
                  convertToHex2(c.getBlue())+";stroke-width:"+strokeWidth+
                  "\" fill=\"#"+
                  convertToHex2(c.getRed())+
                  convertToHex2(c.getGreen())+
                  convertToHex2(c.getBlue())+"\"/>\n");

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
        @param sW the width of the pen to be used when drawing.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
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
        double sW)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        c=l.getColor();
        layerAlpha=l.getAlpha();
        strokeWidth=sW;

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
        out.write("<line x1=\""+cLe(xstart)+"\" y1=\""+cLe(ystart)+"\" x2=\""+
            cLe(xend)+"\" y2=\""+cLe(yend)+"\" ");
        checkColorAndWidth("fill=\"none\"", dashStyle);
    }

    /** Called when exporting a Macro call.
        This function can just return false, to indicate that the macro should
        be rendered by means of calling the other primitives. Please note that
        a macro does not have a reference layer, since it is defined by its
        components.

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
        @return true if the export is handled by this function, false if the
            macro has to be expanded into primitives.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public boolean exportMacro(int x, int y, boolean isMirrored,
        int orientation, String macroName, String macroDesc,
        String name, int xn, int yn, String value, int xv, int yv, String font,
        int fontSize, Map m)
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
        @param sW the width of the pen to be used when drawing.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportOval(int x1, int y1, int x2, int y2,
        boolean isFilled, int layer, int dashStyle, double sW)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        c=l.getColor();
        layerAlpha=l.getAlpha();
        String fillPattern="";
        strokeWidth=sW;
        if(isFilled) {
            fillPattern="fill=\"#"+
                  convertToHex2(c.getRed())+
                  convertToHex2(c.getGreen())+
                  convertToHex2(c.getBlue())+"\"";
        } else {
            fillPattern="fill=\"none\"";

        }

        out.write("<ellipse cx=\""+cLe((x1+x2)/2.0)+"\" cy=\""+
                  cLe((y1+y2)/2.0)+
                  "\" rx=\""+cLe(Math.abs(x2-x1)/2.0)+"\" ry=\""+
                  cLe(Math.abs(y2-y1)/2.0)+"\" ");
        checkColorAndWidth(fillPattern, dashStyle);
    }

    /** Called when exporting a PCBLine primitive.

        @param x1 the x position of the first point of the segment.
        @param y1 the y position of the first point of the segment.
        @param x2 the x position of the second point of the segment.
        @param y2 the y position of the second point of the segment.
        @param width the width ot the line.
        @param layer the layer that should be used.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportPCBLine(int x1, int y1, int x2, int y2, int width,
        int layer)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        c=l.getColor();
        layerAlpha=l.getAlpha();

        out.write("<line x1=\""+cLe(x1)+"\" y1=\""+cLe(y1)+"\" x2=\""+
            cLe(x2)+"\" y2=\""+cLe(y2)+"\" style=\"stroke:#"+
                  convertToHex2(c.getRed())+
                  convertToHex2(c.getGreen())+
                  convertToHex2(c.getBlue())+
                  ";stroke-linejoin:round;stroke-linecap:round"+
                  ";stroke-width:"+(width > 0 ? width : 0.5)+
                  "\"/>\n");
    }

    /** Called when exporting a PCBPad primitive.

        @param x the x position of the pad.s
        @param y the y position of the pad.
        @param style the style of the pad (0: oval, 1: square, 2: rounded
            square).
        @param six the x size of the pad.
        @param siy the y size of the pad.
        @param indiam the hole internal diameter.
        @param layer the layer that should be used.
        @param onlyHole true if only the hole has to be exported.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */

    public void exportPCBPad(int x, int y, int style, int six, int siy,
        int indiam, int layer, boolean onlyHole)
        throws IOException
    {
        double xdd;
        double ydd;

        strokeWidth=0.33;

        LayerDesc l=(LayerDesc)layerV.get(layer);
        c=l.getColor();
        layerAlpha=l.getAlpha();

        if(onlyHole) {
            // ... then, drill the hole!
            out.write("<circle cx=\""+cLe(x)+"\" cy=\""+cLe(y)+"\""+
                " r=\""+cLe(indiam/2.0)+
                "\" style=\"stroke:white;stroke-width:"+strokeWidth+
                "\" fill=\"white\"/>\n");
        } else {
            // At first, draw the pad...
            switch (style) {
                case 1: // Square pad
                    xdd=cLe((double)x-six/2.0);
                    ydd=cLe((double)y-siy/2.0);

                    out.write("<rect x=\""+xdd+"\" y=\""+
                        ydd+    "\" rx=\"0\" ry=\"0\" "+
                        "width=\""+cLe(six)+"\" height=\""+
                        cLe(siy)+"\" style=\"stroke:#"+
                        convertToHex2(c.getRed())+
                        convertToHex2(c.getGreen())+
                        convertToHex2(c.getBlue())+
                        ";stroke-width:"+strokeWidth+"\" fill=\"#"+
                        convertToHex2(c.getRed())+
                        convertToHex2(c.getGreen())+
                        convertToHex2(c.getBlue())+"\"/>\n");

                    break;
                case 2: // Rounded pad
                    xdd=cLe((double)x-six/2.0);
                    ydd=cLe((double)y-siy/2.0);
                    double rd = cLe(2.5);
                    out.write("<rect x=\""+xdd+"\" y=\""+ydd+
                        "\" rx=\""+rd+"\" ry=\""+rd+"\" "+
                        "width=\""+cLe(six)+"\" height=\""+
                        cLe(siy)+"\" style=\"stroke:#"+
                        convertToHex2(c.getRed())+
                        convertToHex2(c.getGreen())+
                        convertToHex2(c.getBlue())+
                        ";stroke-width:"+strokeWidth+"\" fill=\"#"+
                        convertToHex2(c.getRed())+
                        convertToHex2(c.getGreen())+
                        convertToHex2(c.getBlue())+"\"/>\n");
                    break;
                case 0: // Oval pad
                default:
                    out.write("<ellipse cx=\""+cLe(x)+"\" cy=\""+cLe(y)+"\""+
                        " rx=\""+cLe(six/2.0)+"\" ry=\""+cLe(siy/2.0)+
                        "\" style=\"stroke:#"+
                        convertToHex2(c.getRed())+
                        convertToHex2(c.getGreen())+
                        convertToHex2(c.getBlue())+";stroke-width:"+
                        cLe(strokeWidth)+
                        "\" fill=\"#"+
                        convertToHex2(c.getRed())+
                        convertToHex2(c.getGreen())+
                        convertToHex2(c.getBlue())+"\"/>\n");
                    break;
            }
        }
    }

    /** Called when exporting a Polygon primitive.

        @param vertices array containing the position of each vertex.
        @param nVertices number of vertices.
        @param isFilled true if the polygon is filled.
        @param layer the layer that should be used.
        @param dashStyle dashing style.
        @param sW the width of the pen to be used when drawing.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportPolygon(PointDouble[] vertices, int nVertices,
        boolean isFilled, int layer, int dashStyle, double sW)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        c=l.getColor();
        layerAlpha=l.getAlpha();
        String fillPattern="";
        strokeWidth=sW;
        if(isFilled) {
            fillPattern="fill=\"#"+
                  convertToHex2(c.getRed())+
                  convertToHex2(c.getGreen())+
                  convertToHex2(c.getBlue())+"\"";
        } else {
            fillPattern="fill=\"none\"";

        }
        int i;

        //LayerDesc l=(LayerDesc)layerV.get(layer);
        out.write("<polygon points=\"");
        for (i=0; i<nVertices; ++i) {
            out.write(""+cLe(vertices[i].x)+","+cLe(vertices[i].y)+" ");

        }
        out.write("\" ");
        checkColorAndWidth(fillPattern, dashStyle);
    }
    /** Called when exporting a Curve primitive.

        @param vertices array containing the position of each vertex.
        @param nVertices number of vertices.
        @param isFilled true if the polygon is filled.
        @param isClosed true if the curve is closed.
        @param layer the layer that should be used.
        @param arrowStart true if an arrow should be drawn at the start point.
        @param arrowEnd true if an arrow should be drawn at the end point.
        @param arrowStyle the style of the arrow.
        @param arrowLength total lenght of arrows (if present).
        @param arrowHalfWidth half width of arrows (if present).
        @param dashStyle dashing style.
        @param sW the width of the pen to be used when drawing.

        @return false if the curve should be rendered using a polygon, true
            if it is handled by the function.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public boolean exportCurve(PointDouble[] vertices, int nVertices,
        boolean isFilled, boolean isClosed, int layer,
        boolean arrowStart,
        boolean arrowEnd,
        int arrowStyle,
        int arrowLength,
        int arrowHalfWidth,
        int dashStyle,
        double sW)
        throws IOException
    {
        return false;
    }

    /** Called when exporting a Rectangle primitive.

        @param x1 the x position of the first corner.
        @param y1 the y position of the first corner.
        @param x2 the x position of the second corner.
        @param y2 the y position of the second corner.
        @param isFilled it is true if the rectangle should be filled.
        @param layer the layer that should be used.
        @param dashStyle dashing style.
        @param sW the width of the pen to be used when drawing.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportRectangle(int x1, int y1, int x2, int y2,
        boolean isFilled, int layer, int dashStyle, double sW)
        throws IOException
    {
        strokeWidth=sW;
        LayerDesc l=(LayerDesc)layerV.get(layer);
        c=l.getColor();
        layerAlpha=l.getAlpha();
        String fillPattern="";

        if(isFilled) {
            fillPattern="fill=\"#"+
                  convertToHex2(c.getRed())+
                  convertToHex2(c.getGreen())+
                  convertToHex2(c.getBlue())+"\"";
        } else {
            fillPattern="fill=\"none\"";

        }

        double rw = Math.abs(x2-x1);
        double rh = Math.abs(y2-y1);
        if (rw == 0) { rw = 0.5; }
        if (rh == 0) { rh = 0.5; }
        out.write("<rect x=\""+cLe(Math.min(x1,x2))+"\" y=\""+
                  cLe(Math.min(y1,y2))+
                  "\" rx=\"0\" ry=\"0\" "+
                  "width=\""+cLe(rw)+"\" height=\""+
                  cLe(rh)+"\" ");
        checkColorAndWidth(fillPattern, dashStyle);

    }

    /** Called when exporting an embedded raster Image primitive. SVG
        natively supports embedding raster images via a data URI, so the
        original file bytes are reused as-is (no re-encoding, no quality
        loss). The "black and white" option is rendered with a CSS
        grayscale filter, since the exporter only has the original file
        bytes, not decoded pixels, available.

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
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportImage(int x1, int y1, int x2, int y2, int layer,
        double opacity, boolean blackAndWhite, String mimeType,
        String base64Data)
        throws IOException
    {
        double x = Math.min(x1,x2);
        double y = Math.min(y1,y2);
        double w = Math.abs(x2-x1);
        double h = Math.abs(y2-y1);
        if (w==0) { w=0.5; }
        if (h==0) { h=0.5; }

        out.write("<image x=\""+cLe(x)+"\" y=\""+cLe(y)+"\" width=\""+
            cLe(w)+"\" height=\""+cLe(h)+"\" preserveAspectRatio=\"none\" "+
            "opacity=\""+opacity+"\" ");
        if (blackAndWhite) {
            out.write("style=\"filter:grayscale(1)\" ");
        }
        out.write("xlink:href=\"data:image/"+mimeType+";base64,"+
            base64Data+"\"/>\n");
    }

    /** Just be sure that the HEX values are given with two digits...
        NOT a speed sensitive context.
    */
    private String convertToHex2(int v)
    {
        String s=Integer.toHexString(v);
        if (s.length()==1) {
            s="0"+s;
        }
        return s;
    }

    //private Color oc;
    //private double owl;
    //private String ofp;
    //private int ods;


    /** This routine ensures that the following items will be drawn with the
        correct stroke pattern and color.
        TODO: it is not currently working. Improve this.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    private void checkColorAndWidth(String fillPattern, int dashStyle)
        throws IOException
    {

        // Write only if necessary, to save space.
        // It does not work...

        //if(oc!=c || owl!=wl || !fillPattern.equals(ofp) || ods!=dashStyle) {
        //if(true) {
        {
            out.write("style=\"stroke:#"+
                convertToHex2(c.getRed())+
                convertToHex2(c.getGreen())+
                convertToHex2(c.getBlue()));

            if (dashStyle>0) {
                out.write(";stroke-dasharray: "+sDash[dashStyle]);
            }

            if (currentPhase!=dashPhase) {
                currentPhase=dashPhase;
                out.write(";stroke-dashoffset: "+dashPhase);
            }

            double sw = strokeWidth > 0 ? strokeWidth : 0.5;
            out.write(";stroke-width:"+sw+
                  ";stroke-linejoin:round;stroke-linecap:round"+
                  ";fill-rule: evenodd;\"");
            if (layerAlpha < 1.0f) {
                out.write(" opacity=\""+layerAlpha+"\"");
            }
            out.write(" " + fillPattern + "/>\n");
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
        @return the coordinates of the base of the arrow.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
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
        String fillPattern;


        // Then, we calculate the points for the polygon
        x0 = x - l*Math.cos(alpha);
        y0 = y - l*Math.sin(alpha);

        x1 = x0 - h*Math.sin(alpha);
        y1 = y0 + h*Math.cos(alpha);

        x2 = x0 + h*Math.sin(alpha);
        y2 = y0 - h*Math.cos(alpha);

        out.write("<polygon points=\"");

        out.write(""+Globals.roundTo(x)+","
            +Globals.roundTo(y)+" ");
        out.write(""+Globals.roundTo(x1)+","
            +Globals.roundTo(y1)+" ");
        out.write(""+Globals.roundTo(x2)+","
            +Globals.roundTo(y2)+"\" ");

        if ((style & Arrow.flagEmpty) == 0) {
            fillPattern="fill=\"#"+
                  convertToHex2(c.getRed())+
                  convertToHex2(c.getGreen())+
                  convertToHex2(c.getBlue())+"\"";
        } else {
            fillPattern="fill=\"none\"";
        }

        checkColorAndWidth(fillPattern,0);

        if ((style & Arrow.flagLimiter) != 0) {
            double x3;
            double y3;
            double x4;
            double y4;
            x3 = x - h*Math.sin(alpha);
            y3 = y + h*Math.cos(alpha);

            x4 = x + h*Math.sin(alpha);
            y4 = y - h*Math.cos(alpha);
            out.write("<line x1=\""+cLe(x3)+"\" y1=\""+cLe(y3)+"\" x2=\""+
                cLe(x4)+"\" y2=\""+cLe(y4)+"\" ");
            checkColorAndWidth("fill=\"none\"", 0);
        }
        return new PointPr(x0,y0);
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
    }

    /** Get the width of the given string with the current font.
        @param s the string to be used.
        @return the width of the string, in pixels.
    */
    public int getStringWidth(String s)
    {
        gi.setFont(fontname,currentFontSize);
        return gi.getStringWidth(s);
    }

    /** Map a font name to a CSS generic family fallback.
        @param name the font name.
        @return the generic family keyword to use as fallback.
    */
    private String genericFontFamily(String name)
    {
        String lower = name.toLowerCase();
        if (lower.contains("courier") || lower.contains("mono")
                || lower.contains("consolas") || lower.contains("fixed"))
        {
            return "monospace";
        } else if (lower.contains("times") || lower.contains("serif")
                || lower.contains("georgia") || lower.contains("palatino"))
        {
            return "serif";
        }
        return "sans-serif";
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
            out.write("<text x=\""+(x-textx)+"\" y=\""
                +cLe(baseFontAscent+y-texty)
                +"\" font-family=\""+
                fontname+", "+genericFontFamily(fontname)
                +"\" font-size=\""+cLe(currentFontSize)+
                "\" font-style=\""+
                (isItalic?"italic":"")+"\" font-weight=\""+
                (isBold?"bold":"")+"\" "+
                "xml:space=\"preserve\" "+
                "fill=\"#"+
                    convertToHex2(c.getRed())+
                    convertToHex2(c.getGreen())+
                    convertToHex2(c.getBlue())+"\""+
                ">");
            // Substitute potentially dangerous characters (issue #162)
            String outtxt=str.replace("&", "&amp;");
            outtxt=outtxt.replace("<", "&lt;");
            outtxt=outtxt.replace(">", "&gt;");
            outtxt=outtxt.replace("\"", "&quot;");
            outtxt=outtxt.replace("'", "&apos;");

            out.write(outtxt);
            out.write("</text>\n");
        } catch(IOException e) {
            System.err.println("Can not write to file in SVG export.");
        }
    }
}
