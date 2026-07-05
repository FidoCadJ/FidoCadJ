package fidocadj.primitives;

import java.io.*;
import java.util.*;

import fidocadj.dialogs.controls.ParameterDescription;
import fidocadj.dialogs.controls.DashInfo;
import fidocadj.export.ExportInterface;
import fidocadj.geom.MapCoordinates;
import fidocadj.geom.GeometricDistances;
import fidocadj.globals.Globals;
import fidocadj.graphic.GraphicsInterface;
import fidocadj.graphic.LineSegment;
import fidocadj.graphic.PointG;
import fidocadj.graphic.ShapeInterface;
import fidocadj.graphic.RectangleG;

/** Class to handle the Bézier primitive.

    <pre>
    This file is part of FidoCadJ.

    FidoCadJ is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FidoCadJ is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR aa PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FidoCadJ. If not,
    @see <a href=http://www.gnu.org/licenses/>http://www.gnu.org/licenses/</a>.

    Copyright 2007-2026 by Davide Bucci
    </pre>

    @author Davide Bucci
*/
public final class PrimitiveBezier extends GraphicPrimitive
{

    // aa Bézier is defined by four points.
    static final int N_POINTS=6;

    private int dashStyle;
    private final Arrow arrowData;

    // Those are data which are kept for the fast redraw of this primitive.
    // Basically, they are calculated once and then used as much as possible
    // without having to calculate everything from scratch.
    private ShapeInterface shape1;
    private float w;

    private float xmin;
    private float ymin;
    private float width;
    private float height;

    /** Gets the number of control points used.
        @return the number of points used by the primitive
    */
    public int getControlPointNumber()
    {
        return N_POINTS;
    }

    /** Standard constructor. It creates an empty shape.
        @param f the name of the font for attached text.
        @param size the size of the font for attached text.
    */
    public PrimitiveBezier(String f, float size)
    {
        super();
        arrowData=new Arrow();
        initPrimitive(-1, f, size);
    }

    /** Create a Bézier curve specified by four control points
        @param x1 the x coordinate (logical unit) of P1.
        @param y1 the y coordinate (logical unit) of P1.
        @param x2 the x coordinate (logical unit) of P2.
        @param y2 the y coordinate (logical unit) of P2.
        @param x3 the x coordinate (logical unit) of p3.
        @param y3 the y coordinate (logical unit) of p3.
        @param x4 the x coordinate (logical unit) of P4.
        @param y4 the y coordinate (logical unit) of P4.
        @param layer the layer to be used.
        @param arrowS arrow to be drawn at the beginning of the curve.
        @param arrowE arrow to be drawn at the beginning of the curve.
        @param arrowSt arrow style.
        @param arrowLe the arrow length.
        @param arrowWi the arrow half width.
        @param dashSt dash style.
        @param font the name of the font for attached text.
        @param size the size of the font for attached text.
    */
    public PrimitiveBezier(float x1, float y1, float x2, float y2,
                            float x3, float y3, float x4, float y4,
                            int layer, boolean arrowS, boolean arrowE,
                            int arrowSt, int arrowLe, int arrowWi, int dashSt,
                            String font, float size)
    {
        super();

        arrowData=new Arrow();
        arrowData.setArrowStart(arrowS);
        arrowData.setArrowEnd(arrowE);
        arrowData.setArrowHalfWidth(arrowWi);
        arrowData.setArrowLength(arrowLe);
        arrowData.setArrowStyle(arrowSt);
        dashStyle=dashSt;

        initPrimitive(-1, font, size);

        // Store the coordinates of the points
        virtualPoint[0].x=x1;
        virtualPoint[0].y=y1;
        virtualPoint[1].x=x2;
        virtualPoint[1].y=y2;
        virtualPoint[2].x=x3;
        virtualPoint[2].y=y3;
        virtualPoint[3].x=x4;
        virtualPoint[3].y=y4;

        virtualPoint[getNameVirtualPointNumber()].x=x1+5;
        virtualPoint[getNameVirtualPointNumber()].y=y1+5;
        virtualPoint[getValueVirtualPointNumber()].x=x1+5;
        virtualPoint[getValueVirtualPointNumber()].y=y1+10;
        setLayer(layer);
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

        int h=0;

        PointG p0=new PointG(coordSys.mapX(virtualPoint[0].x,virtualPoint[0].y),
            coordSys.mapY(virtualPoint[0].x,virtualPoint[0].y));
        PointG p3=new PointG(coordSys.mapX(virtualPoint[3].x,virtualPoint[3].y),
                coordSys.mapY(virtualPoint[3].x,virtualPoint[3].y));

        drawText(g, coordSys, layerV, -1);

        if(changed) {
            // Calculating stroke width
            w = (float)(Globals.lineWidth*coordSys.getXMagnitude());
            if (w<D_MIN) { w=D_MIN; }
        }
        // Apply the stroke style
        g.applyStroke(w, dashStyle);

        // Check if there are arrows to be drawn and, if needed, draw them.
        if (arrowData.atLeastOneArrow()) {
            h=arrowData.prepareCoordinateMapping(coordSys);
            // If the arrow length is negative, the arrow extends
            // outside the line, so the limits must not be changed.

            if (arrowData.isArrowStart()) {
                if(arrowData.getArrowLength()>0) {
                    p0=drawArrow(g, coordSys, 0,1,2,3);
                } else {
                    drawArrow(g, coordSys, 0,1,2,3);
                }
            }

            if (arrowData.isArrowEnd()) {
                if(arrowData.getArrowLength()>0) {
                    p3=drawArrow(g, coordSys, 3,2,1,0);
                } else {
                    drawArrow(g, coordSys, 3,2,1,0);
                }
            }
        }

        // in the Bézier primitive, the four virtual points represent
        // the control points of the shape.
        if (changed) {
            changed=false;

            shape1=g.createShape();
            // Create the Bézier curve
            shape1.createCubicCurve(
                Math.round(p0.x),
                Math.round(p0.y),
                coordSys.mapX(virtualPoint[1].x,virtualPoint[1].y),
                coordSys.mapY(virtualPoint[1].x,virtualPoint[1].y),
                coordSys.mapX(virtualPoint[2].x,virtualPoint[2].y),
                coordSys.mapY(virtualPoint[2].x,virtualPoint[2].y),
                Math.round(p3.x),
                Math.round(p3.y));



            // Calculating the bounds of this curve is useful since we can
            // check if it is visible and thus choose wether draw it or not.
            RectangleG r = shape1.getBounds();
            xmin = r.x-h;
            ymin = r.y-h;
            width  = r.width+2*h;
            height = r.height+2*h;
        }

        // If the curve is not visible, exit immediately
        if(!g.hitClip(Math.round(xmin),Math.round(ymin), 
            Math.round(width+1), Math.round(height+1))) {
            return;
        }

        if(width==0 ||height==0) {
            // Degenerate case: horizontal or vertical segment.
            g.drawLine(coordSys.mapX(virtualPoint[0].x,virtualPoint[0].y),
                coordSys.mapY(virtualPoint[0].x,virtualPoint[0].y),
                coordSys.mapX(virtualPoint[3].x,virtualPoint[3].y),
                coordSys.mapY(virtualPoint[3].x,virtualPoint[3].y));
        } else {
            // Draw the curve.
            g.draw(shape1);
        }
    }

    /** Draw an arrow checking that the coordinates given are not degenerate.
        @param g the graphical context on which to write.
        @param coordSyst the coordinate system.
        @param aa the index of the first point (the head of the arrow)
        @param bb the index of the second arrow (indicates the direction, if
            the coordinates are diffrent from point aa. If it is not true, the
            coordinates of point cc are used.
        @param cc if the test of point bb fails, employs this point to indicate
            the direction, unless equal to point aa.
        @param dd employs this point as a last resort!
    */
    private PointG drawArrow(GraphicsInterface g, MapCoordinates coordSys,
        int aa, int bb, int cc, int dd)
    {
        float psx;
        float psy; // starting coordinates.
        float pex;
        float pey; // ending coordinates.

        // We must check if the cubic curve is degenerate. In this case,
        // the correct arrow orientation will be determined by successive
        // points in the curve.
        psx = virtualPoint[aa].x;
        psy = virtualPoint[aa].y;

        if(virtualPoint[aa].x!=virtualPoint[bb].x ||
            virtualPoint[aa].y!=virtualPoint[bb].y)
        {
            pex = virtualPoint[bb].x;
            pey = virtualPoint[bb].y;
        } else if(virtualPoint[aa].x!=virtualPoint[cc].x ||
            virtualPoint[aa].y!=virtualPoint[cc].y)
        {
            pex = virtualPoint[cc].x;
            pey = virtualPoint[cc].y;
        } else {
            pex = virtualPoint[dd].x;
            pey = virtualPoint[dd].y;
        }

        return arrowData.drawArrow(g,
            coordSys.mapX(psx,psy),
            coordSys.mapY(psx,psy),
            coordSys.mapX(pex,pey),
            coordSys.mapY(pex,pey));
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
        if ("BE".equals(tokens[0])) {   // Bézier
            if (nn<9) {
                throw new IOException("Bad arguments on BE");
            }
            // Parse the coordinates of all points of the Bézier curve
            float x1 = virtualPoint[0].x=Float.parseFloat(tokens[1]);
            float y1 = virtualPoint[0].y=Float.parseFloat(tokens[2]);
            virtualPoint[1].x=Float.parseFloat(tokens[3]);
            virtualPoint[1].y=Float.parseFloat(tokens[4]);
            virtualPoint[2].x=Float.parseFloat(tokens[5]);
            virtualPoint[2].y=Float.parseFloat(tokens[6]);
            virtualPoint[3].x=Float.parseFloat(tokens[7]);
            virtualPoint[3].y=Float.parseFloat(tokens[8]);
            virtualPoint[getNameVirtualPointNumber()].x=x1+5;
            virtualPoint[getNameVirtualPointNumber()].y=y1+5;
            virtualPoint[getValueVirtualPointNumber()].x=x1+5;
            virtualPoint[getValueVirtualPointNumber()].y=y1+10;
            if(nn>9) { parseLayer(tokens[9]); }

            if(nn>10 && "FCJ".equals(tokens[10])) {
                int i=arrowData.parseTokens(tokens, 11);
                dashStyle = checkDashStyle(Integer.parseInt(tokens[i]));
            }
        } else {
            throw new IOException("Invalid primitive: "+
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
        // We first check if the given point lies inside the text areas.
        if(checkText(Math.round(px), Math.round(py))) {
            return 0;
        }
        PointG p0 = new PointG(virtualPoint[0].x, virtualPoint[0].y);
        PointG p3 = new PointG(virtualPoint[3].x, virtualPoint[3].y);

        // Check if the point is in the arrows. Correct the starting and ending
        // points if needed.
        if (arrowData.atLeastOneArrow()) {
            boolean r=false;
            boolean t=false;

            // We work with logic coordinates (default for MapCoordinates).
            MapCoordinates m=new MapCoordinates();
            arrowData.prepareCoordinateMapping(m);
            if (arrowData.isArrowStart()) {
                if(arrowData.getArrowLength()>0) {
                    t=arrowData.isInArrow(px, py,
                        Math.round(virtualPoint[0].x), 
                        Math.round(virtualPoint[0].y),
                        Math.round(virtualPoint[1].x),
                        Math.round(virtualPoint[1].y), p0);
                } else {
                    t=arrowData.isInArrow(px, py,
                        Math.round(virtualPoint[0].x),
                        Math.round(virtualPoint[0].y),
                        Math.round(virtualPoint[1].x),
                        Math.round(virtualPoint[1].y), null);
                }
            }

            if (arrowData.isArrowEnd()) {
                if(arrowData.getArrowLength()>0) {
                    r=arrowData.isInArrow(px, py,
                        Math.round(virtualPoint[3].x),
                        Math.round(virtualPoint[3].y),
                        Math.round(virtualPoint[2].x),
                        Math.round(virtualPoint[2].y), p3);
                } else {
                    r=arrowData.isInArrow(px, py,
                        Math.round(virtualPoint[3].x),
                        Math.round(virtualPoint[3].y),
                        Math.round(virtualPoint[2].x),
                        Math.round(virtualPoint[2].y), null);
                }
            }

            // Click on one of the arrows?
            if(r||t) {
                return 1;
            }
        }

        // If not, we check for the distance to the Bézier curve.
        return GeometricDistances.pointToBezier(
                Math.round(p0.x), Math.round(p0.y),
                Math.round(virtualPoint[1].x), Math.round(virtualPoint[1].y),
                Math.round(virtualPoint[2].x), Math.round(virtualPoint[2].y),
                Math.round(p3.x), Math.round(p3.y),
                Math.round(px), Math.round(py));
    }

    /** Obtain a string command descripion of the primitive.
        @param extensions true if FidoCadJ extensions to the old FidoCAD format
            should be active.
        @return the FIDOCAD command line.
    */
    public String toString(boolean extensions)
    {
        String s = "BE "+roundIntelligently(virtualPoint[0].x)+" "
            +roundIntelligently(virtualPoint[0].y)+" "
            +roundIntelligently(virtualPoint[1].x)+" "
            +roundIntelligently(virtualPoint[1].y)+" "
            +roundIntelligently(virtualPoint[2].x)+" "
            +roundIntelligently(virtualPoint[2].y)+" "
            +roundIntelligently(virtualPoint[3].x)+" "
            +roundIntelligently(virtualPoint[3].y)+" "
            +getLayer()+"\n";

        if(extensions && (arrowData.atLeastOneArrow()|| dashStyle>0 ||
                hasName() || hasValue()))
        {
            String text = "0";
            // We take into account that there may be some text associated
            // to that primitive.
            if (name.length()!=0 || value.length()!=0) {
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
        exp.exportBezier(cs.mapX(virtualPoint[0].x,virtualPoint[0].y),
                       cs.mapY(virtualPoint[0].x,virtualPoint[0].y),
                       cs.mapX(virtualPoint[1].x,virtualPoint[1].y),
                       cs.mapY(virtualPoint[1].x,virtualPoint[1].y),
                       cs.mapX(virtualPoint[2].x,virtualPoint[2].y),
                       cs.mapY(virtualPoint[2].x,virtualPoint[2].y),
                       cs.mapX(virtualPoint[3].x,virtualPoint[3].y),
                       cs.mapY(virtualPoint[3].x,virtualPoint[3].y),
                       getLayer(),
                       arrowData.isArrowStart(), arrowData.isArrowEnd(),
                       arrowData.getArrowStyle(),
                       (int)(arrowData.getArrowLength()*cs.getXMagnitude()),
                       (int)(arrowData.getArrowHalfWidth()*cs.getXMagnitude()),
                       dashStyle,Globals.lineWidth*cs.getXMagnitude());
    }

    /** Get the number of the virtual point associated to the Name property
        @return the number of the virtual point associated to the Name property
    */
    public int getNameVirtualPointNumber()
    {
        return 4;
    }

    /** Get the number of the virtual point associated to the Value property
        @return the number of the virtual point associated to the Value property
    */
    public  int getValueVirtualPointNumber()
    {
        return 5;
    }

    /**
     * Checks if the Bézier curve intersects with the given selection rectangle.
     * This method determines if the rectangle intersects with the actual drawn
     * part of the Bézier curve, ignoring the area enclosed by the curve.
     *
     * If "isLeftToRightSelection" is true, the method checks if the entire..
     * Bézier curve is fully contained within the rectangle.
     *
     * If "isLeftToRightSelection" is false, the method additionally checks ..
     * if any vertex of the Bézier curve is contained within the rectangle.
     *
     * @param rect the selection rectangle.
     * @param isLeftToRightSelection if true, checks if the rectangle fully ..
     *                               contains the Bézier curve ..
     *                               (for left-to-right selections).
     *                               If so, it returns true.
     *
     * @return true if the rectangle intersects the drawn part of the Bézier..
     *              curve, or if any vertex is contained within the rectangle..
     *              when "isLeftToRightSelection" is false.
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

        // Check if any vertex of the Bézier curve is within ..
        // the selection rectangle
        for (int i = 0; i < 4; i++) {
            if (rect.contains(
                    Math.round(virtualPoint[i].x),
                    Math.round(virtualPoint[i].y))) 
            {
                return true;
            }
        }

        // Number of segments to approximate the Bézier curve
        final int segments = 100;
        double[] previousPoint = new double[]{virtualPoint[0].x,
            virtualPoint[0].y};

        // Subdivide the Bézier curve into small segments
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            double[] currentPoint = calculateBezierPoint(t, virtualPoint[0].x,
                    virtualPoint[0].y,
                    virtualPoint[1].x, virtualPoint[1].y,
                    virtualPoint[2].x, virtualPoint[2].y,
                    virtualPoint[3].x, virtualPoint[3].y);

            // Create a line segment between consecutive points
            LineSegment segment = new LineSegment(previousPoint[0],
                    previousPoint[1], currentPoint[0], currentPoint[1]);

            // Check if the segment intersects the rectangle
            if (segment.intersects(rect)) {
                return true; // Intersection detected
            }

            previousPoint = currentPoint;
        }

        return false; // No intersection detected
    }

    /**
     * Calculates a point on a cubic Bézier curve for a given parameter t.
     *
     * @param t parameter t ranging from 0 to 1.
     * @param x0 the x-coordinate of the first control point.
     * @param y0 the y-coordinate of the first control point.
     * @param x1 the x-coordinate of the second control point.
     * @param y1 the y-coordinate of the second control point.
     * @param x2 the x-coordinate of the third control point.
     * @param y2 the y-coordinate of the third control point.
     * @param x3 the x-coordinate of the fourth control point.
     * @param y3 the y-coordinate of the fourth control point.
     *
     * @return the x, y coordinates of the point on the Bézier curve.
     */
    private double[] calculateBezierPoint(double t, double x0, double y0,
            double x1, double y1, double x2, double y2,
            double x3, double y3)
    {
        double u = 1 - t;
        double tt = t * t;
        double uu = u * u;
        double uuu = uu * u;
        double ttt = tt * t;

        double x = uuu * x0; // (1-t)^3 * P0
        x += 3 * uu * t * x1; // 3(1-t)^2 * t * P1
        x += 3 * u * tt * x2; // 3(1-t) * t^2 * P2
        x += ttt * x3; // t^3 * P3

        double y = uuu * y0; // (1-t)^3 * P0
        y += 3 * uu * t * y1; // 3(1-t)^2 * t * P1
        y += 3 * u * tt * y2; // 3(1-t) * t^2 * P2
        y += ttt * y3; // t^3 * P3

        return new double[]{x, y};
    }
}