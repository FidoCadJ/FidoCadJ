package fidocadj.primitives;

import java.io.*;
import java.util.*;

import fidocadj.dialogs.controls.ParameterDescription;
import fidocadj.dialogs.controls.DashInfo;
import fidocadj.export.ExportInterface;
import fidocadj.geom.GeometricDistances;
import fidocadj.geom.MapCoordinates;
import fidocadj.globals.Globals;
import fidocadj.graphic.GraphicsInterface;
import fidocadj.graphic.RectangleG;

/** Class to handle the rectangle primitive.

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
public final class PrimitiveRectangle extends GraphicPrimitive
{
    // A rectangle is defined by two points.
    static final int N_POINTS=4;
    // The state: filled or not.
    private boolean isFilled;
    // The dashing style.
    private int dashStyle;

    // This is the value which is given for the distance calculation when the
    // user clicks inside the rectangle:
    static final int DISTANCE_IN = 1;

    // This is the value given instead when the clicks is done outside:
    static final int DISTANCE_OUT = 1000;

    // Those are data which are kept for the fast redraw of this primitive.
    // Basically, they are calculated once and then used as much as possible
    // without having to calculate everything from scratch.
    // For this reason, they are NOT local variables.
    private int xa;         // NOPMD
    private int ya;         // NOPMD
    private int xb;         // NOPMD
    private int yb;         // NOPMD
    private int x1;         // NOPMD
    private int y1;         // NOPMD
    private int x2;         // NOPMD
    private int y2;         // NOPMD
    private int width;      // NOPMD
    private int height;     // NOPMD
    private float w;        // NOPMD

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
    public PrimitiveRectangle(String f, float size)
    {
        super();
        isFilled=false;
        initPrimitive(-1, f, size);

        changed=true;
    }
    /** Create a rectangle defined by two points
        @param x1 the start x coordinate (logical unit).
        @param y1 the start y coordinate (logical unit).
        @param x2 the end x coordinate (logical unit).
        @param y2 the end y coordinate (logical unit).
        @param f specifies if the rectangle should be filled.
        @param layer the layer to be used.
        @param dashSt the dashing style.
        @param font the name of the font for attached text.
        @param size the size of the font for attached text.
    */
    public PrimitiveRectangle(float x1, float y1, float x2, float y2,
                              boolean f, int layer, int dashSt, String font,
                              float size)
    {
        super();
        initPrimitive(-1, font, size);

        virtualPoint[0].x=x1;
        virtualPoint[0].y=y1;
        virtualPoint[1].x=x2;
        virtualPoint[1].y=y2;
        virtualPoint[getNameVirtualPointNumber()].x=x1+5;
        virtualPoint[getNameVirtualPointNumber()].y=y1+5;
        virtualPoint[getValueVirtualPointNumber()].x=x1+5;
        virtualPoint[getValueVirtualPointNumber()].y=y1+10;
        isFilled=f;
        dashStyle=dashSt;
        changed = true;

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
        // in the rectangle primitive, the first two virtual points represent
        //   the two corners of the segment

        if(changed) {
            changed=false;
            x1=coordSys.mapX(virtualPoint[0].x,virtualPoint[0].y);
            y1=coordSys.mapY(virtualPoint[0].x,virtualPoint[0].y);
            x2=coordSys.mapX(virtualPoint[1].x,virtualPoint[1].y);
            y2=coordSys.mapY(virtualPoint[1].x,virtualPoint[1].y);

            // Sort the coordinates.
            if (x1>x2) {
                xa=x2;
                xb=x1;
            } else {
                xa=x1;
                xb=x2;
            }
            if (y1>y2) {
                ya=y2;
                yb=y1;
            } else {
                ya=y1;
                yb=y2;
            }
            // Calculate the stroke width
            w = (float)(Globals.lineWidth*coordSys.getXMagnitude());
            if (w<D_MIN) { w=D_MIN; }

            width = xb-xa;
            height = yb-ya;
        }

        // If we do not need to perform the drawing, exit immediately
        if(!g.hitClip(xa,ya, width+1,height+1)) {
            return;
        }

        g.applyStroke(w, dashStyle);

        if(isFilled){
            // We need to add 1 to the rectangle, since the behaviour of
            // Java api is to skip the rightmost and bottom pixels
            g.fillRect(xa,ya,width+1,height+1);
        } else {
            if(xb!=xa || yb!=ya) {
                // It seems that under MacOSX, drawing a rectangle by cycling
                // with the lines is much more efficient than the drawRect
                // method. Probably, a further investigation is needed to
                // determine if  this situation is the same with more recent
                // Java runtimes (mine was 1.5.something on an iMac G5 at
                // 2 GHz when I did the tests).

                g.drawLine(xa, ya, xb,ya);
                g.drawLine(xb, ya, xb,yb);
                g.drawLine(xb, yb, xa,yb);
                g.drawLine(xa, yb, xa,ya);
            }
        }
    }

    /** Parse a token array and store the graphic data for a given primitive
        Obviously, that routine should be called *after* having recognized
        that the called primitive is correct.
        That routine also sets the current layer.
        @param tokens the tokens to be processed. tokens[0] should be the
        command of the actual primitive.
        @param nn the number of tokens present in the array
        @throws IOException if the arguments are incorrect or the primitive
            is invalid.
    */
    public void parseTokens(String[] tokens, int nn)
        throws IOException
    {
        changed=true;
        // assert it is the correct primitive
        if ("RV".equals(tokens[0])||"RP".equals(tokens[0])) {   // Oval
            if (nn<5) {
                throw new IOException("Bad arguments on RV/RP");
            }
            float x1 = virtualPoint[0].x=Float.parseFloat(tokens[1]);
            float y1 = virtualPoint[0].y=Float.parseFloat(tokens[2]);
            virtualPoint[1].x=Float.parseFloat(tokens[3]);
            virtualPoint[1].y=Float.parseFloat(tokens[4]);

            virtualPoint[getNameVirtualPointNumber()].x=x1+5;
            virtualPoint[getNameVirtualPointNumber()].y=y1+5;
            virtualPoint[getValueVirtualPointNumber()].x=x1+5;
            virtualPoint[getValueVirtualPointNumber()].y=y1+10;
            if(nn>5) { parseLayer(tokens[5]); }

            if("RP".equals(tokens[0])) {
                isFilled=true;
            } else {
                isFilled=false;
            }

            if(nn>6 && "FCJ".equals(tokens[6])) {
                dashStyle = checkDashStyle(Integer.parseInt(tokens[7]));
            }
        } else {
            throw new IOException("RV/RP: Invalid primitive: "
                +tokens[0]+" programming error?");
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

        pd.parameter=Boolean.valueOf(isFilled);
        pd.description=Globals.messages.getString("ctrl_filled");
        v.add(pd);

        pd = new ParameterDescription();
        pd.parameter=new DashInfo(dashStyle);
        pd.description=Globals.messages.getString("ctrl_dash_style");
        pd.isExtension = true;
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
        if (pd.parameter instanceof Boolean) {
            isFilled=((Boolean)pd.parameter).booleanValue();
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }

        pd=v.get(i++);
        if (pd.parameter instanceof DashInfo) {
            dashStyle=((DashInfo)pd.parameter).style;
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }

        // Parameters validation and correction
        if(dashStyle>=Globals.dashNumber) {
            dashStyle=Globals.dashNumber-1;
        }

        if(dashStyle<0) {
            dashStyle=0;
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
        float xa=Math.min(virtualPoint[0].x,virtualPoint[1].x);
        float ya=Math.min(virtualPoint[0].y,virtualPoint[1].y);
        float xb=Math.max(virtualPoint[0].x,virtualPoint[1].x);
        float yb=Math.max(virtualPoint[0].y,virtualPoint[1].y);


        if(isFilled) {
            if(GeometricDistances.pointInRectangle(xa,ya, xb-xa, yb-ya,px,py)) {
                return DISTANCE_IN;
            } else {
                return DISTANCE_OUT;
            }
        }
        return (int)Math.round(
            GeometricDistances.pointToRectangle(xa,ya, xb-xa, yb-ya,px,py));
    }

    /** Obtain a string command descripion of the primitive.
        @param extensions true if FidoCadJ extensions to the old FidoCAD format
            should be active.
        @return the FIDOCAD command line.
    */
    public String toString(boolean extensions)
    {
        String cmd;

        if (isFilled) {
            cmd="RP ";
        } else {
            cmd="RV ";
        }

        cmd+=roundIntelligently(virtualPoint[0].x)+" "
            +roundIntelligently(virtualPoint[0].y)+" "
            +roundIntelligently(virtualPoint[1].x)+" "
            +roundIntelligently(virtualPoint[1].y)+" "+
            getLayer()+"\n";

        if(extensions && (dashStyle>0 || hasName() || hasValue())) {
            String text = "0";
            if (name.length()!=0 || value.length()!=0) {
                text = "1";
            }
            cmd+="FCJ "+dashStyle+" "+text+"\n";
        }
        // The false is needed since saveText should not write the FCJ tag.
        cmd+=saveText(false);

        return cmd;
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
        exp.exportRectangle(cs.mapX(virtualPoint[0].x,virtualPoint[0].y),
                       cs.mapY(virtualPoint[0].x,virtualPoint[0].y),
                       cs.mapX(virtualPoint[1].x,virtualPoint[1].y),
                       cs.mapY(virtualPoint[1].x,virtualPoint[1].y),
                       isFilled,
                       getLayer(),
                       dashStyle,
                       Globals.lineWidth*cs.getXMagnitude());
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
     * Determines whether the rectangle defined by the points in virtualPoint
     * intersects any of the edges of the specified selection rectangle.
     *
     * If "isLeftToRightSelection" is true, the method checks whether the entire
     * primitive rectangle is fully contained within the selection rectangle.
     * If isLeftToRightSelection is false, the method checks whether any edge
     * of the primitive rectangle intersects with the selection rectangle or
     * if any vertex of the primitive rectangle is contained within the
     * selection rectangle.
     *
     * @param rect the RectangleG object representing the selection rectangle.
     * @param isLeftToRightSelection true if the selection is from left to right
     *                               and should consider the entire rectangle..
     *                               contained within the selection rectangle.
     *
     * @return true if any part of the rectangle intersects the selection..
     *              rectangle, or if any vertex is contained within the ..
     *              selection rectangle when isLeftToRightSelection is false.
     *              Otherwise, returns false.
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

        float x1 = Math.min(virtualPoint[0].x, virtualPoint[1].x);
        float y1 = Math.min(virtualPoint[0].y, virtualPoint[1].y);
        float x2 = Math.max(virtualPoint[0].x, virtualPoint[1].x);
        float y2 = Math.max(virtualPoint[0].y, virtualPoint[1].y);

        // Check if any vertex of the rectangle is within the selection
        // rectangle
        if (rect.contains(x1, y1) || rect.contains(x2, y1)
                || rect.contains(x1, y2) || rect.contains(x2, y2))
        {
            return true;
        }

        // Check if any edge of the rectangle intersects with the selection
        // rectangle
        boolean topEdge = rect.intersectsLine(x1, y1, x2, y1);
        boolean bottomEdge = rect.intersectsLine(x1, y2, x2, y2);
        boolean leftEdge = rect.intersectsLine(x1, y1, x1, y2);
        boolean rightEdge = rect.intersectsLine(x2, y1, x2, y2);

        return topEdge || bottomEdge || leftEdge || rightEdge;
    }
}