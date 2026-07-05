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
import fidocadj.graphic.PointG;
import fidocadj.graphic.RectangleG;

/** Class to handle the line primitive.

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
public final class PrimitiveLine extends GraphicPrimitive
{
    static final int N_POINTS=4;

    // Info about arrow.
    private final Arrow arrowData;

    private int dashStyle;


    // Those are data which are kept for the fast redraw of this primitive.
    // Basically, they are calculated once and then used as much as possible
    // without having to calculate everything from scratch.
    private int xa;
    private int ya;
    private int xb;         // NOPMD
    private int yb;         // NOPMD
    private int x1;         // NOPMD
    private int x2;         // NOPMD
    private int y1;         // NOPMD
    private int y2;         // NOPMD
    private float w;
    private int length2;
    private int xbpap1;
    private int ybpap1;
    private boolean arrows;

    /** Constructor.
        @param x1       the x coordinate of the start point of the line.
        @param y1       the y coordinate of the start point of the line.
        @param x2       the x coordinate of the end point of the line.
        @param y2       the y coordinate of the end point of the line.
        @param layer    the layer to be used.
        @param arrowS   true if there is an arrow at the beginning of the line.
        @param arrowE   true if there is an arrow at the end of the line.
        @param arrowSt  style of the arrow.
        @param arrowLe  length of the arrow.
        @param arrowWi  width of the arrow.
        @param dashSt   the dashing style.
        @param f        the name of the font for attached text.
        @param size     the size of the font for attached text.
    */
    public PrimitiveLine(float x1, float y1, float x2, float y2, int layer,
                        boolean arrowS, boolean arrowE,
                        int arrowSt, float arrowLe, float arrowWi, int dashSt,
                        String f, float size)
    {
        super();

        arrowData=new Arrow();
        arrowData.setArrowStart(arrowS);
        arrowData.setArrowEnd(arrowE);
        arrowData.setArrowHalfWidth(arrowWi);
        arrowData.setArrowLength(arrowLe);
        arrowData.setArrowStyle(arrowSt);

        dashStyle = dashSt;

        initPrimitive(-1, f, size);

        virtualPoint[0].x=x1;
        virtualPoint[0].y=y1;
        virtualPoint[1].x=x2;
        virtualPoint[1].y=y2;
        virtualPoint[getNameVirtualPointNumber()].x=x1+5;
        virtualPoint[getNameVirtualPointNumber()].y=y1+5;
        virtualPoint[getValueVirtualPointNumber()].x=x1+5;
        virtualPoint[getValueVirtualPointNumber()].y=y1+10;

        setLayer(layer);
    }

    /** Constructor.
        @param f        the name of the font for attached text.
        @param size     the size of the font for attached text.
    */
    public PrimitiveLine(String f, float size)
    {
        super();
        arrowData=new Arrow();

        initPrimitive(-1, f, size);
    }

    /** Gets the number of control points used.
        @return the number of points used by the primitive
    */

    public int getControlPointNumber()
    {
        return N_POINTS;
    }

    /** Get the control parameters of the given primitive.
        @return a vector of ParameterDescription containing each control
                parameter.
    */
    public List<ParameterDescription> getControls()
    {
        List<ParameterDescription> v=super.getControls();

        arrowData.getControlsForArrow(v);
        ParameterDescription pd = new ParameterDescription();
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
        i=arrowData.setParametersForArrow(v, i);

        ParameterDescription pd=v.get(i++);
        if (pd.parameter instanceof DashInfo) {
            dashStyle=((DashInfo)pd.parameter).style;
        } else {
            System.out.println("Warning: 6-unexpected parameter!"+pd);
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

        if(changed) {
            changed=false;
            // in the line primitive, the first two virtual points represent
            // the beginning and the end of the segment to be drawn.
            x1=coordSys.mapX(virtualPoint[0].x,virtualPoint[0].y);
            y1=coordSys.mapY(virtualPoint[0].x,virtualPoint[0].y);
            x2=coordSys.mapX(virtualPoint[1].x,virtualPoint[1].y);
            y2=coordSys.mapY(virtualPoint[1].x,virtualPoint[1].y);

            // We store the coordinates in an ordered way in order to ease
            // the determination of the clip rectangle.
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

            // Calculate the width of the stroke in pixel. It should not
            // make our lines disappear, even at very small zoom ratios.
            // So we put a limit D_MIN.
            w = (float)(Globals.lineWidth*coordSys.getXMagnitude());
            if (w<D_MIN) { w=D_MIN; }

            // Calculate the square of the length in pixel.
            length2=(xa-xb)*(xa-xb)+(ya-yb)*(ya-yb);

            arrows = arrowData.atLeastOneArrow();

            // This correction solves bug #3101041 (old SF code)
            // We do need to apply a correction to the clip calculation
            // rectangle if necessary to take into account the arrow heads
            if (arrows) {
                int h=arrowData.prepareCoordinateMapping(coordSys);
                xa -= Math.abs(h);
                ya -= Math.abs(h);
                xb += Math.abs(h);
                yb += Math.abs(h);
            }
            xbpap1=xb-xa+1;
            ybpap1=yb-ya+1;
        }
        // This is a trick. We skip drawing the line if it is too short.
        if(length2>2) {
            if(!g.hitClip(xa,ya, xbpap1,ybpap1)) {
                return;
            }

            g.applyStroke(w, dashStyle);

            float xstart=x1;
            float ystart=y1;
            float xend=x2;
            float yend=y2;
            // If needed, we draw the arrows at the extremes.
            if (arrows) {
                arrowData.prepareCoordinateMapping(coordSys);
                if (arrowData.isArrowStart()) {
                    PointG p=arrowData.drawArrow(g,x1,y1,x2,y2);
                    // This fixes issue #172
                    // If the arrow length is negative, the arrow extends
                    // outside the line, so the limits must not be changed.
                    if(arrowData.getArrowLength()>0) {
                        xstart=p.x;
                        ystart=p.y;
                    }
                }
                if (arrowData.isArrowEnd()) {
                    PointG p=arrowData.drawArrow(g,x2,y2,x1,y1);
                    // This fixes issue #172
                     // If the arrow length is negative, the arrow extends
                    // outside the line, so the limits must not be changed.
                    if(arrowData.getArrowLength()>0) {
                        xend=p.x;
                        yend=p.y;
                    }
                }
            }
            g.drawLine(Math.round(xstart),Math.round(ystart),
                Math.round(xend),Math.round(yend));
        }
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
        if ("LI".equals(tokens[0])) {   // Line
            if (nn<5) {
                throw new IOException("Bad arguments on LI");
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

            if(nn>5) { parseLayer(tokens[5]); }

            // FidoCadJ extensions

            if(nn>6 && "FCJ".equals(tokens[6])) {
                int i=arrowData.parseTokens(tokens, 7);
                dashStyle = checkDashStyle(Integer.parseInt(tokens[i]));
            }
        } else {
            throw new IOException("LI: Invalid primitive:"+tokens[0]+
                                          " programming error?");
        }
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
        // Check if the point is in the arrows. Correct the starting and ending
        // points if needed.
        if (arrowData.atLeastOneArrow()) {
            boolean r=false;
            boolean t=false;

            // We work with logic coordinates (default for MapCoordinates).
            MapCoordinates m=new MapCoordinates();
            arrowData.prepareCoordinateMapping(m);
            PointG pp=new PointG();
            if (arrowData.isArrowStart()) {
                t=arrowData.isInArrow(px, py,
                    Math.round(virtualPoint[0].x), 
                    Math.round(virtualPoint[0].y),
                    Math.round(virtualPoint[1].x),
                    Math.round(virtualPoint[1].y), pp);
            }

            if (arrowData.isArrowEnd()) {
                r=arrowData.isInArrow(px, py,
                    Math.round(virtualPoint[1].x),
                    Math.round(virtualPoint[1].y),
                    Math.round(virtualPoint[0].x),
                    Math.round(virtualPoint[0].y), pp);
            }

            // Click on one of the arrows.
            if(r||t) {
                return 1;
            }
        }
        return GeometricDistances.pointToSegment(
                Math.round(virtualPoint[0].x),
                Math.round(virtualPoint[0].y),
                Math.round(virtualPoint[1].x),
                Math.round(virtualPoint[1].y),
                Math.round(px), Math.round(py));
    }

    /** Obtain a string command descripion of the primitive.
        @param extensions true if FidoCadJ extensions to the old FidoCAD format
            should be active.
        @return the FIDOCAD command line.
    */
    public String toString(boolean extensions)
    {
        // A single point line without anything is not worth converting.
        if (name.length()==0 && value.length()==0 &&
            virtualPoint[0].x==virtualPoint[1].x &&
            virtualPoint[0].y==virtualPoint[1].y)
        {
            return "";
        }

        String s= "LI "+roundIntelligently(virtualPoint[0].x)+" "
            +roundIntelligently(virtualPoint[0].y)+" "
            +roundIntelligently(virtualPoint[1].x)+" "
            +roundIntelligently(virtualPoint[1].y)+" "
            +getLayer()+"\n";

        if(extensions && (arrowData.atLeastOneArrow() || dashStyle>0 ||
            name!=null && name.length()!=0) ||
            value!=null && value.length()!=0)
        {
            String text = "0";
            // We take into account that there may be some text associated
            // to that primitive.
            if (hasName() || hasValue()) {
                text = "1";
            }
            s+="FCJ "+arrowData.createArrowTokens()+" "+dashStyle+
                " "+text+"\n";
        }

        // The false is needed since saveText should not write the FCJ tag.
        s+=saveText(false);

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
        exp.exportLine(cs.mapX(virtualPoint[0].x,virtualPoint[0].y),
                       cs.mapY(virtualPoint[0].x,virtualPoint[0].y),
                       cs.mapX(virtualPoint[1].x,virtualPoint[1].y),
                       cs.mapY(virtualPoint[1].x,virtualPoint[1].y),
                       getLayer(),
                       arrowData.isArrowStart(), arrowData.isArrowEnd(),
                       arrowData.getArrowStyle(),
                       (int)(arrowData.getArrowLength()*cs.getXMagnitude()),
                       (int)(arrowData.getArrowHalfWidth()*cs.getXMagnitude()),
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
     * Determines whether the line defined by the points in virtualPoint
     * intersects the specified rectangle.
     *
     * If "isLeftToRightSelection" is true, the method checks if the entire line
     * is fully contained within the rectangle.
     *
     * If "isLeftToRightSelection" is false, the method also checks if either
     * endpoint of the line is contained within the rectangle.
     *
     * @param rect the Rectangle object to check for intersection.
     * @param isLeftToRightSelection Determine the direction of the selection.
     *
     * @return true if the line intersects the rectangle or if any vertex is..
     *              contained within the rectangle when "isLeftToRightSelection"
     *              is false. Otherwise, returns false.
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