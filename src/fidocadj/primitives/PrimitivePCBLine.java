package fidocadj.primitives;

import java.io.*;
import java.util.*;

import fidocadj.dialogs.controls.ParameterDescription;
import fidocadj.export.ExportInterface;
import fidocadj.geom.GeometricDistances;
import fidocadj.geom.MapCoordinates;
import fidocadj.globals.Globals;
import fidocadj.graphic.GraphicsInterface;
import fidocadj.graphic.RectangleG;

/** Class to handle the PCB line primitive.

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

    Copyright 2007-2026 by Davide Bucci
    </pre>

    @author Davide Bucci
*/

public final class PrimitivePCBLine extends GraphicPrimitive
{

    private float width;

    // A PCB segment is defined by two points.

    static final int N_POINTS=4;

    // Those are data which are kept for the fast redraw of this primitive.
    // Basically, they are calculated once and then used as much as possible
    // without having to calculate everything from scratch.
    private float xa;
    private float ya;
    private float xb;             // NOPMD
    private float yb;             // NOPMD
    private float x1;
    private float y1;
    private float x2;
    private float y2;
    private float wiPix;
    private float xbpap1;
    private float ybpap1;

    /** Gets the number of control points used.
        @return the number of points used by the primitive
    */
    public int getControlPointNumber()
    {
        return N_POINTS;
    }

    /** Standard constructor.
        @param f the name of the font for attached text.
        @param size the size of the font for attached text.
    */
    public PrimitivePCBLine(String f, float size)
    {
        super();
        width=0;
        initPrimitive(-1, f, size);
    }
    /** Create a PCB line between two points
        @param x1 the start x coordinate (logical unit).
        @param y1 the start y coordinate (logical unit).
        @param x2 the end x coordinate (logical unit).
        @param y2 the end y coordinate (logical unit).
        @param w specifies the line width.
        @param layer the layer to be used.
        @param f the name of the font for attached text.
        @param size the size of the font for attached text.
    */
    public PrimitivePCBLine(float x1, float y1, float x2, float y2,
            float w, int layer,
            String f, float size)
    {
        super();
        initPrimitive(-1, f, size);

        virtualPoint[0].x=x1;
        virtualPoint[0].y=y1;
        virtualPoint[1].x=x2;
        virtualPoint[1].y=y2;
        virtualPoint[getNameVirtualPointNumber()].x=x1+5;
        virtualPoint[getNameVirtualPointNumber()].y=y1+5;
        virtualPoint[getValueVirtualPointNumber()].x=x1+5;
        virtualPoint[getValueVirtualPointNumber()].y=y1+10;
        width=w;

        setLayer(layer);
    }

    /** Draw the graphic primitive on the given graphic context.
        @param g the graphic context in which the primitive should be drawn.
        @param coordSys the graphic coordinates system to be applied.
        @param layerV the layer description.
    */
    public void draw(GraphicsInterface g, MapCoordinates coordSys,
                              List layerV)
    {

        if(!selectLayer(g,layerV)) {
            return;
        }

        drawText(g, coordSys, layerV, -1);

        /* in the PCB line primitive, the first two virtual points represent
           the beginning and the end of the segment to be drawn. */

        if(changed) {
            changed=false;
            x1=coordSys.mapX(virtualPoint[0].x,virtualPoint[0].y);
            y1=coordSys.mapY(virtualPoint[0].x,virtualPoint[0].y);
            x2=coordSys.mapX(virtualPoint[1].x,virtualPoint[1].y);
            y2=coordSys.mapY(virtualPoint[1].x,virtualPoint[1].y);
            wiPix=(float)Math.abs(coordSys.mapXr(virtualPoint[0].x,
                virtualPoint[0].y)
                -coordSys.mapXr(virtualPoint[0].x+width,
                virtualPoint[0].y+width));

            xa=(int)(Math.min(x1, x2)-wiPix/2.0f);
            ya=(int)(Math.min(y1, y2)-wiPix/2.0f);
            xb=(int)(Math.max(x1, x2)+wiPix/2.0f);
            yb=(int)(Math.max(y1, y2)+wiPix/2.0f);

            coordSys.trackPoint(xa,ya);
            coordSys.trackPoint(xb,yb);

            xbpap1=xb-xa+1;
            ybpap1=yb-ya+1;
        }

        // Exit if the primitive is offscreen. This is a simplification, but
        // ensures that the primitive is correctly drawn when it is
        // partially visible.

        if(!g.hitClip(Math.round(xa),Math.round(ya),
            Math.round(xbpap1),Math.round(ybpap1)))
        {
            return;
        }

        g.applyStroke(wiPix, 0);
        g.drawLine(Math.round(x1), Math.round(y1), 
                   Math.round(x2), Math.round(y2));
    }

    /** Parse a token array and store the graphic data for a given primitive
        Obviously, that routine should be called *after* having recognized
        that the called primitive is correct.
        That routine also sets the current layer.
        @param tokens the tokens to be processed. tokens[0] should be the
        command of the actual primitive.
        @param nn the number of tokens present in the array.
        @throws IOException if the arguments are incorrect or the primitive
            is invalid.
    */
    public void parseTokens(String[] tokens, int nn)
        throws IOException
    {
        changed=true;

        // assert it is the correct primitive

        if ("PL".equals(tokens[0])) {   // Line
            if (nn<6) {
                throw new IOException("Bad arguments on PL");
            }
            // Load the points in the virtual points associated to the
            // current primitive.

            float x1 = virtualPoint[0].x=Float.parseFloat(tokens[1]);
            float y1 = virtualPoint[0].y=Float.parseFloat(tokens[2]);
            virtualPoint[1].x=Float.parseFloat(tokens[3]);
            virtualPoint[1].y=Float.parseFloat(tokens[4]);

            virtualPoint[getNameVirtualPointNumber()].x=x1+5;
            virtualPoint[getNameVirtualPointNumber()].y=y1+5;
            virtualPoint[getValueVirtualPointNumber()].x=x1+5;
            virtualPoint[getValueVirtualPointNumber()].y=y1+10;

            width=Float.parseFloat(tokens[5]);
            if(nn>6) { parseLayer(tokens[6]); }


        } else {
            throw new IOException("PL: Invalid primitive:"+tokens[0]+
                                          " programming error?");
        }
    }

    /** Get the control parameters of the given primitive.
        @return a vector of ParameterDescription containing each control
                parameter.
                The first parameters should always be the virtual points.

    */
    public List<ParameterDescription> getControls()
    {
        List<ParameterDescription> v=super.getControls();
        ParameterDescription pd = new ParameterDescription();

        pd.parameter= Float.valueOf(width);
        pd.description=Globals.messages.getString("ctrl_width");
        v.add(pd);


        return v;
    }

    /** Set the control parameters of the given primitive.
        This method is specular to getControls().

        @param v a vector of ParameterDescription containing each control
                parameter.
                The first parameters should always be the virtual points.
        @return the next index in v to be scanned (if needed) after the
            execution of this function.
    */
    public int setControls(List<ParameterDescription> v)
    {
        int i=super.setControls(v);
        ParameterDescription pd;

        pd=v.get(i);
        ++i;
        // Check, just for sure...
        if (pd.parameter instanceof Float) {
            width=((Float)pd.parameter).floatValue();
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }
        return i;
    }



    /** Gets the distance (in primitive's coordinates space) between a
        given point and the primitive.
        When it is reasonable, the behaviour can be binary (polygons,
        ovals...). In other cases (lines, points), it can be proportional.
        @param px the x coordinate of the given point.
        @param py the y coordinate of the given point.
        @return the distance in logical units.
    */
    public int getDistanceToPoint(float px, float py)
    {
        // Here we check if the given point lies inside the text areas

        if(checkText(Math.round(px), Math.round(py))) {
            return 0;
        }

        float distance=(float)(GeometricDistances.pointToSegment(
                virtualPoint[0].x,virtualPoint[0].y,
                virtualPoint[1].x,virtualPoint[1].y,
                px,py)-width/2.0f);

        return Math.round(distance<0?0:distance);
    }

    /** Obtain a string command descripion of the primitive.
        @param extensions true if FidoCadJ extensions to the old FidoCAD format
            should be active.
        @return the FIDOCAD command line.
    */
    public String toString(boolean extensions)
    {
        String s= "PL "
            +roundIntelligently(virtualPoint[0].x)+" "
            +roundIntelligently(virtualPoint[0].y)+" "
            +roundIntelligently(virtualPoint[1].x)+" "
            +roundIntelligently(virtualPoint[1].y)+" "
            +roundIntelligently(width)+" "+getLayer()+"\n";

        s+=saveText(extensions);

        return s;
    }

    /** Export the primitive on a vector graphic format.
        @param exp the export interface to employ.
        @param cs the coordinate mapping to employ.
        @throws IOException if a problem occurs, such as it is impossible to
            write on the output file.
    */
    public void export(ExportInterface exp, MapCoordinates cs)
        throws IOException
    {
        exportText(exp, cs, -1);
        exp.exportPCBLine(cs.mapX(virtualPoint[0].x,virtualPoint[0].y),
                cs.mapY(virtualPoint[0].x,virtualPoint[0].y),
                cs.mapX(virtualPoint[1].x,virtualPoint[1].y),
                cs.mapY(virtualPoint[1].x,virtualPoint[1].y),
                (int)(width*cs.getXMagnitude()), getLayer());
    }
    /** Get the number of the virtual point associated to the Name property
        @return the number of the virtual point associated to the Name property
    */
    public int getNameVirtualPointNumber()
    {
        return 2;
    }

    /** Get the number of the virtual point associated to the Value property
        @return the number of the virtual point associated to the Value property
    */
    public  int getValueVirtualPointNumber()
    {
        return 3;
    }

    /**
     * Determines whether the line defined by the points in virtualPoint
     * intersects the specified rectangle.
     *
     * If "isLeftToRightSelection" is true, the method checks if the entire line
     * is fully contained within the rectangle.
     *
     * If "isLeftToRightSelection" is false, the method additionally checks if
     * either endpoint of the line is contained within the rectangle.
     *
     * @param rect the Rectangle object to check for intersection.
     * @param isLeftToRightSelection Determine the direction of the selection.
     *
     * @return true if the line intersects the rectangle or if any vertex is
     * contained within the rectangle when "isLeftToRightSelection" is
     * false. Otherwise, returns false.
     */
    @Override
    public boolean intersects(RectangleG rect, boolean isLeftToRightSelection)
    {
        if (getCurrentLayer().isVisible() == false) {
            return false;
        }

        if (isLeftToRightSelection) {
            return isFullyContained(rect);
        }

        float x1 = virtualPoint[0].x;
        float y1 = virtualPoint[0].y;
        float x2 = virtualPoint[1].x;
        float y2 = virtualPoint[1].y;

        // Check if either endpoint of the line is within the selection
        // rectangle
        if (rect.contains(x1, y1) || rect.contains(x2, y2)) {
            return true;
        }

        // Check if the line intersects the selection rectangle
        return rect.intersectsLine(x1, y1, x2, y2);
    }

}