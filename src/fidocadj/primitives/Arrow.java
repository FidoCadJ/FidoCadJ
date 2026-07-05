package fidocadj.primitives;

import java.util.*;

import fidocadj.geom.MapCoordinates;
import fidocadj.geom.GeometricDistances;
import fidocadj.graphic.PointG;
import fidocadj.graphic.GraphicsInterface;
import fidocadj.graphic.PolygonInterface;
import fidocadj.dialogs.controls.ParameterDescription;
import fidocadj.dialogs.controls.ArrowInfo;
import fidocadj.globals.Globals;
import fidocadj.export.PointPr;

/**
    Arrow class: draws an arrow of the given size, style and direction.

    @author Davide Bucci

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

    Copyright 2009-2026 by Davide Bucci
   </pre>

*/

public final class Arrow
{
    /** A few constants in order to define the arrow style.
    */
    public static final int flagLimiter = 0x01;
    public static final int flagEmpty = 0x02;

    // From version 0.24.8, those become floating point values.
    private float arrowLength;
    private float arrowHalfWidth;

    // If the length and the width differ from an integer less than the given
    // tolerance, the result will be rounded
    private static final float roundTolerance=1e-5f;

    // The coordinate mapping must be kept so that the drawn points can be
    // tracked for the calculation of the size of the drawing.
    MapCoordinates m;

    // Style of the arrow.
    private int arrowStyle;

    private boolean arrowStart;     // Draw arrow at the start point.
    private boolean arrowEnd;       // Draw arrow at the end point.

    // Arrow sizes in pixels.
    private int h;
    private int l;

    /** Constructor is private since this is an utility class.
    */
    public Arrow()
    {
        arrowLength = 3;
        arrowHalfWidth = 1;
    }

    /** Determine if at least one arrow has to be drawn.
        @return true if at least one arrow has to be drawn.
    */
    public boolean atLeastOneArrow()
    {
        return arrowStart || arrowEnd;
    }

    /** Create the string describing the arrow. If the arrowLength and
        arrowHalfWidth differs from an integer less than a given tolerance
        (constant roundTolerance in the class definition), the sizes are
        rounded and issued as integer values. This allows to have a better
        compatibility with former versions of FidoCadJ (<0.24.7) that can
        not parse a non-integer value in those places. The code issued is
        a little bit more compact, too.
        @return a string containing the codes
    */
    public String createArrowTokens()
    {
        int arrows = (arrowStart?0x01:0x00)|(arrowEnd?0x02:0x00);
        String result="";
        result+=arrows+" ";
        result+=arrowStyle+" ";
        if (Math.abs(arrowLength-Math.round(arrowLength))<roundTolerance) {
            result+= Math.round(arrowLength);
        } else {
            result+=arrowLength;
        }
        result+=" ";
        if (Math.abs(arrowHalfWidth-Math.round(arrowHalfWidth))<roundTolerance){
            result+= Math.round(arrowHalfWidth);
        } else {
            result+=arrowHalfWidth;
        }

        return result;
    }

    /** Determine if the arrow on the start point should be drawn.
        @return true if the arrow on the start point should be drawn.
    */
    public boolean isArrowStart()
    {
        return arrowStart;
    }

    /** Set if an arrow has to be drawn at the start (beginning) of the
        element.
        @param as true if it is the case.
    */
    public void setArrowStart(boolean as)
    {
        arrowStart=as;
    }

    /** Determine if the arrow on the start point should be drawn.
        @return true if the arrow on the start point should be drawn.
    */
    public boolean isArrowEnd()
    {
        return arrowEnd;
    }

    /** Set if an arrow has to be drawn at the end of the element.
        @param ae true if it is the case.
    */
    public void setArrowEnd(boolean ae)
    {
        arrowEnd=ae;
    }

    /** Get the code of the current arrow style
        @return the code.
    */
    public int getArrowStyle()
    {
        return arrowStyle;
    }

    /** Set the current style.
        @param as the code style.
    */
    public void setArrowStyle(int as)
    {
        arrowStyle=as;
    }

    /** Get the current arrow length.
        @return the arrow length.
    */
    public float getArrowLength()
    {
        return arrowLength;
    }

    /** Set the current arrow length.
        @param al the arrow length.
    */
    public void setArrowLength(float al)
    {
        arrowLength=al;
    }

    /** Get the current arrow half width.
        @return the arrow half width.
    */
    public float getArrowHalfWidth()
    {
        return arrowHalfWidth;
    }

    /** Set the current arrow half width.
        @param ahw the arrow half width
    */
    public void setArrowHalfWidth(float ahw)
    {
        arrowHalfWidth=ahw;
    }

    /** Parse the tokens for the description of an arrow.
        They always come (in a FCJ line) in the same order, but the starting
        index may differ. The order is as follows: 1. the presence of which
        arrow (start/end points), 2. the style code, 3. the length, 4. the
        half width, 5. the style.
        @param tokens the tokens to be interpreted.
        @param startIndex the index where to start having a look around.
        @return the index of the token following the one which has been just
        read.
    */
    public int parseTokens(String[] tokens, int startIndex)
    {
        int i=startIndex;
        int arrows = Integer.parseInt(tokens[i++]);
        arrowStart = (arrows & 0x01) !=0;
        arrowEnd = (arrows & 0x02) !=0;

        arrowStyle = Integer.parseInt(tokens[i++]);
        // These rounding operations should be removed in version
        // 0.24.8 (see Issue #111).
        arrowLength = Float.parseFloat(tokens[i++]);
        arrowHalfWidth= Float.parseFloat(tokens[i++]);
        return i;
    }

    /** The drawing operation is done in two step. The first one is performed
        here and consists in calculating all the relevant size parameters in
        pixels, given the current coordinate mapping. The results are stored
        and used during the drawing operations. This function does not need
        to be employed each time a drawing operation is needed, but only when
        something changes, such as the coordinate mapping or the sizes of the
        arrows.
        @param coordSys the current coordinate mapping system.
        @return the half width in pixels.
    */
    public int prepareCoordinateMapping(MapCoordinates coordSys)
    {
        m=coordSys;
        // Heigth and width of the arrows in pixels
        h=Math.abs(coordSys.mapXi(arrowHalfWidth,arrowHalfWidth,false)-
            coordSys.mapXi(0,0, false));
        l=Math.abs(coordSys.mapXi(arrowLength,arrowLength, false)-
            coordSys.mapXi(0,0,false));
        // h and l must conserve the sign of arrowHalfWidth and
        // arrowLength, regardless of the coordinate system
        // orientation.
        if(arrowHalfWidth<0) {
            h=-h;
        }
        if(arrowLength<0) {
            l=-l;
        }
        return h;
    }

    /** Add elements for the Arrow description inside a parameter description
        array used to create a parameters dialog.
        @param v the List to which the elements have to be added (of course,
            this means that that List will be modified!
        @return the vector itself.
    */
    public List<ParameterDescription> getControlsForArrow(
        List<ParameterDescription> v)
    {
        ParameterDescription pd = new ParameterDescription();
        pd.parameter=Boolean.valueOf(isArrowStart());
        pd.description=Globals.messages.getString("ctrl_arrow_start");
        pd.isExtension = true;
        v.add(pd);
        pd = new ParameterDescription();
        pd.parameter=Boolean.valueOf(isArrowEnd());
        pd.description=Globals.messages.getString("ctrl_arrow_end");
        pd.isExtension = true;
        v.add(pd);
        pd = new ParameterDescription();
        pd.parameter=Float.valueOf(getArrowLength());
        pd.description=Globals.messages.getString("ctrl_arrow_length");
        pd.isExtension = true;
        v.add(pd);
        pd = new ParameterDescription();
        pd.parameter=Float.valueOf(getArrowHalfWidth());
        pd.description=Globals.messages.getString("ctrl_arrow_half_width");
        pd.isExtension = true;
        v.add(pd);
        pd = new ParameterDescription();
        pd.parameter=new ArrowInfo(getArrowStyle());
        pd.description=Globals.messages.getString("ctrl_arrow_style");
        pd.isExtension = true;
        v.add(pd);

        return v;
    }

    /** Read the elements for the Arrow description inside a parameter
        description List, coming from a parameters dialog.
        @param v the List to which the elements have been added
        @param start the starting index to which the parameters should be
            interpreted as describing an Arrow.
        @return the index+1 of the last element employed for the Arrow.
    */
    public int setParametersForArrow(List<ParameterDescription> v, int start)
    {
        int i=start;
        ParameterDescription pd=v.get(i++);
        if (pd.parameter instanceof Boolean) {
            setArrowStart(((Boolean)pd.parameter).booleanValue());
        } else {
            System.out.println("Warning: 1-unexpected parameter!"+pd);
        }
        pd=v.get(i++);
        if (pd.parameter instanceof Boolean) {
            setArrowEnd(((Boolean)pd.parameter).booleanValue());
        } else {
            System.out.println("Warning: 2-unexpected parameter!"+pd);
        }

        pd=v.get(i++);
        if (pd.parameter instanceof Float) {
            setArrowLength(((Float)pd.parameter).floatValue());
        } else {
            System.out.println("Warning: 3-unexpected parameter!"+pd);
        }

        pd=v.get(i++);
        if (pd.parameter instanceof Float) {
            setArrowHalfWidth(((Float)pd.parameter).floatValue());
        } else {
            System.out.println("Warning: 4-unexpected parameter!"+pd);
        }

        pd=v.get(i++);
        if (pd.parameter instanceof ArrowInfo) {
            setArrowStyle(((ArrowInfo)pd.parameter).style);
        } else {
            System.out.println("Warning: 5-unexpected parameter!"+pd);
        }

        return i;
    }

    /** Draw an arrow at the given position. Version useful in those cases
        where the sizes have to be set directly in pixels and no data deserve
        to be stored for very long.

        @param g the graphic context to be used.
        @param x the x coordinate of the arrow point.
        @param y the y coordinate of the arrow point.
        @param xc the x coordinate of the direction point.
        @param yc the y coordinate of the direction point.
        @return the coordinate of the base point of the arrow head.
        @param tl the length of the arrow in pixels.
        @param th the half width of the arrow in pixels.
        @param as the style of the arrow.
    */
    public PointG drawArrowPixels(GraphicsInterface g, int x, int y, int xc,
        int yc, int tl, int th, int as)
    {
        h=th;
        l=tl;
        arrowStyle=as;
        return drawArrow(g, x,y, xc,yc);
    }

    /** Check if the logic coordinates (xs,ys) are inside an arrow.
        @param xs the x coordinate of the point to check.
        @param ys the y coordinate of the point to check.
        @param x the x coordinate of the arrow tip.
        @param y the y coordinate of the arrow tip.
        @param xc the x coordinate of the direction point.
        @param yc the y coordinate of the direction point.
        @param pBase return the coordinate of the base point of the arrow head.
            If pBase is specified, it modifies its values. If it is null,
            nothing will be stored.
        @return true if the coordinates are inside the arrow.
    */
    public boolean isInArrow(float xs, float ys, float x, float y, 
        float xc, float yc, PointG pBase)
    {
        // Consider the arrow as a polygon.
        float[] xp=new float[3];
        float[] yp=new float[3];

        PointPr[] p=calculateArrowPoints(x,y,xc,yc);
        xp[0]=x;
        xp[1]=(float)p[1].x;
        xp[2]=(float)p[2].x;
        yp[0]=y;
        yp[1]=(float)p[1].y;
        yp[2]=(float)p[2].y;

        if(pBase!=null) {
            pBase.x=(int)Math.round(p[0].x);
            pBase.y=(int)Math.round(p[0].y);
        }
        return GeometricDistances.pointInPolygon(xp,yp,3,xs,ys);
    }

    private PointPr[] calculateArrowPoints(float x, float y, float xc, float yc)
    {
        PointPr[] p;
        // At first we need the angle giving the direction of the arrow
        double alpha=getArrowAngle(x,y,xc,yc);

        // Then, we calculate the points for the polygon
        double cosalpha=Math.cos(alpha);
        double sinalpha=Math.sin(alpha);
        p = new PointPr[5];

        p[0] = new PointPr(x - l*cosalpha, y - l*sinalpha);
        p[1] = new PointPr(p[0].x - h*sinalpha, p[0].y + h*cosalpha);
        p[2] = new PointPr(p[0].x + h*sinalpha, p[0].y - h*cosalpha);

        if ((arrowStyle & flagLimiter) != 0) {
            p[3] = new PointPr(x - h*sinalpha, y + h*cosalpha);
            p[4] = new PointPr(x + h*sinalpha, y - h*cosalpha);
        }
        return p;
    }


    private double getArrowAngle(float x, float y, float xc, float yc)
    {
        double alpha;
        // a little bit of trigonometry :-)
        // The idea is that the arrow head should be oriented in the direction
        // specified by the second point.

        if (x==xc) {
            alpha = Math.PI/2.0+(y-yc<0.0?0.0:Math.PI);
        } else {
            alpha = Math.atan((double)(y-yc)/(double)(x-xc));
        }

        // Alpha is the angle of the arrow, against an horizontal line with
        // the trigonometric convention (anti clockwise is positive).

        alpha += x-xc>0.0?0.0:Math.PI;
        return alpha;
    }

    /** Draw an arrow at the given position.
        @param g the graphic context to be used.
        @param x the x coordinate of the arrow point.
        @param y the y coordinate of the arrow point.
        @param xc the x coordinate of the direction point.
        @param yc the y coordinate of the direction point.
        @return the coordinate of the base point of the arrow head.
    */
    public PointG drawArrow(GraphicsInterface g, int x, int y, int xc, int yc)
    {
        PointPr[] p = calculateArrowPoints(x,y,xc,yc);

        // The arrow head is traced using a polygon. Here we create the
        // object and populate it with the calculated coordinates.
        PolygonInterface pp = g.createPolygon();

        pp.addPoint(x,y);
        pp.addPoint((int)Math.round(p[1].x),(int)Math.round(p[1].y));
        pp.addPoint((int)Math.round(p[2].x),(int)Math.round(p[2].y));

        if(m!=null) {
            m.trackPoint(x,y);
            m.trackPoint(p[1].x,p[1].y);
            m.trackPoint(p[2].x,p[2].y);
        }

        if ((arrowStyle & flagEmpty) == 0) {
            g.fillPolygon(pp);
        } else {
            g.drawPolygon(pp);
        }

        // Check if we need to draw the limiter or not
        // This is a small line useful for quotes.
        if ((arrowStyle & flagLimiter) != 0) {
            g.drawLine((int)Math.round(p[3].x),(int)Math.round(p[3].y),
                (int)Math.round(p[4].x),(int)Math.round(p[4].y));
            if(m!=null) {
                m.trackPoint(p[3].x,p[3].y);
                m.trackPoint(p[4].x,p[4].y);
            }
        }
        return new PointG((int)p[0].x,(int)p[0].y);
    }
}