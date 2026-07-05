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
import fidocadj.graphic.PointG;
import fidocadj.graphic.PolygonInterface;
import fidocadj.graphic.ShapeInterface;
import fidocadj.graphic.PointDouble;
import fidocadj.graphic.RectangleG;

/** Class to handle the ComplexCurve primitive.

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

    Copyright 2011-2026 by Davide Bucci

    Spline calculations by Tim Lambert
    http://www.cse.unsw.edu.au/~lambert/splines/
    </pre>

    @author Davide Bucci
*/
public final class PrimitiveComplexCurve
    extends GraphicPrimitive
{

    private int nPoints;
    private boolean isFilled;
    private boolean isClosed;

    private final Arrow arrowData;
    private int dashStyle;

    // The natural spline is drawn as a polygon. Even if this is a rather
    // crude technique, it fits well with the existing architecture (in
    // particular for the export facilities), since everything that it is
    // needed for a polygon is available and can be reused here.
    // In some cases (for example for drawing), a ShapeInterface is created,
    // since it gives better results than a polygon.

    // A first polygon stored in screen coordinates.
    private PolygonInterface p;

    // A second polygon stored in logical coordinates.
    private PolygonInterface q;

    // 5 points is the initial storage size, which is increased if needed.
    // In other words, we initially create space for storing 5 points and
    // we increase that if needed.
    int storageSize=5;

    static final int STEPS=24;

    // Some stored data
    private int xmin;
    private int ymin;
    private int width;
    private int height;

    // Those are data which are kept for the fast redraw of this primitive.
    // Basically, they are calculated once and then used as much as possible
    // without having to calculate everything from scratch.
    private float w;
    private ShapeInterface gp;

    /** Gets the number of control points used.
        @return the number of points used by the primitive.
    */

    public int getControlPointNumber()
    {
        return nPoints+2;
    }

    /** Constructor.
        Create a ComplexCurve. Add points with the addPoint method.
        @param f the name of the font for attached text.
        @param size the size of the font for attached text.
    */
    public PrimitiveComplexCurve(String f, float size)
    {
        super();
        arrowData=new Arrow();
        isFilled=false;
        nPoints=0;
        p = null;
        initPrimitive(storageSize, f, size);
    }

    /** Create a ComplexCurve. Add points with the addPoint method.

        @param f specifies if the ComplexCurve should be filled.
        @param c specifies if the ComplexCurve should be closed.
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
    public PrimitiveComplexCurve(boolean f, boolean c, int layer,
        boolean arrowS, boolean arrowE,
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

        p = null;
        initPrimitive(storageSize, font, size);
        nPoints=0;
        isFilled=f;
        isClosed=c;
        dashStyle=dashSt;
        setLayer(layer);
    }

    /** Add the given point to the closest part of the curve.
        @param px the x coordinate of the point to add
        @param py the y coordinate of the point to add
    */
    public void addPointClosest(float px, float py)
    {
        float[] xp=new float[storageSize];
        float[] yp=new float[storageSize];

        int k;

        for(k=0;k<nPoints;++k){
            xp[k]=virtualPoint[k].x;
            yp[k]=virtualPoint[k].y;
        }
        // Calculate the distance between the
        // given point and all the segments composing the polygon and we
        // take the smallest one.

        int distance=(int)Math.sqrt((px-xp[0])*(px-xp[0])+
            (py-yp[0])*(py-yp[0]));

        int d;
        int minv=0;

        for(int i=0; i<q.getNpoints()-1; ++i) {
            d=GeometricDistances.pointToSegment(q.getXpoints()[i],
                q.getYpoints()[i], q.getXpoints()[i+1],
                q.getYpoints()[i+1], Math.round(px), Math.round(py));

            if(d<distance) {
                distance = d;
                minv=i-1;
            }
        }

        minv /= STEPS;
        ++minv;
        if(minv<0) { minv=nPoints-1; }

        // Now minv contains the index of the vertex before the one which
        // should be entered. We begin to enter the new vertex at the end...

        addPoint(px, py);

        // ...then we do the swap
        for(int i=nPoints-1; i>minv; --i) {
            virtualPoint[i].x=virtualPoint[i-1].x;
            virtualPoint[i].y=virtualPoint[i-1].y;
        }

        virtualPoint[minv].x=px;
        virtualPoint[minv].y=py;

        changed = true;
    }

    /** Add a point at the current ComplexCurve. The point is always added
        at the end of the already existing path.
        @param x the x coordinate of the point.
        @param y the y coordinate of the point.
    */
    public void addPoint(float x, float y)
    {
        if(nPoints+2>=storageSize) {
            int oN=storageSize;
            int i;
            storageSize += 10;
            PointG[] nv = new PointG[storageSize];
            for(i=0;i<oN;++i) {
                nv[i]=virtualPoint[i];
            }
            for(;i<storageSize;++i) {
                nv[i]=new PointG();
            }
            virtualPoint=nv;
        }
        // And now we enter the position of the point we are interested with
        virtualPoint[nPoints].x=x;
        virtualPoint[nPoints++].y=y;
        // We do need to shift the two points describing the position
        // of the text lines
        virtualPoint[getNameVirtualPointNumber()].x=x+5;
        virtualPoint[getNameVirtualPointNumber()].y=y+5;
        virtualPoint[getValueVirtualPointNumber()].x=x+5;
        virtualPoint[getValueVirtualPointNumber()].y=y+10;
        changed = true;
    }

    /** Create the CurveStorage associated to the complex curve. This is a crude
        technique, but it is very easy to implement.
        @param coordSys the coordinate mapping to be employed.
        @return the CurveStorage approximating the complex curve.
    */
    public CurveStorage createComplexCurve(MapCoordinates coordSys)
    {
        // A spline needs at least two points
        if (nPoints < 2) {
            return null;
        }

        double [] xPoints = new double[nPoints];
        double [] yPoints = new double[nPoints];

        // The first and the last points may be the base of the arrows, if
        // the latter are present and the points have been updated.
        for (int i=0; i<nPoints; ++i) {
            xPoints[i] = coordSys.mapXr(virtualPoint[i].x,virtualPoint[i].y);
            yPoints[i] = coordSys.mapYr(virtualPoint[i].x,virtualPoint[i].y);
        }

        Cubic[] xx;
        Cubic[] yy;

        if(isClosed) {
            xx = calcNaturalCubicClosed(nPoints-1, xPoints);
            yy = calcNaturalCubicClosed(nPoints-1, yPoints);
        } else {
            xx = calcNaturalCubic(nPoints-1, xPoints);
            yy = calcNaturalCubic(nPoints-1, yPoints);
            // Here we don't check if a point is in the arrow, but we exploit
            // the code for calculating the base of the head of the arrows.
            if (arrowData.atLeastOneArrow()) {
                arrowData.prepareCoordinateMapping(coordSys);
                if (arrowData.isArrowStart()) {
                    PointG pp = new PointG();
                    arrowData.isInArrow(0, 0,
                        (int)Math.round(xx[0].eval(0)),
                        (int)Math.round(yy[0].eval(0)),
                        (int)Math.round(xx[0].eval(0.05)),
                        (int)Math.round(yy[0].eval(0.05)), pp);
                    if(arrowData.getArrowLength()>0) {
                        xPoints[0]=pp.x;
                        yPoints[0]=pp.y;
                    }
                }

                if (arrowData.isArrowEnd()) {
                    int l=xx.length-1;
                    PointG pp = new PointG();
                    arrowData.isInArrow(0, 0,
                        (int)Math.round(xx[l].eval(1)),
                        (int)Math.round(yy[l].eval(1)),
                        (int)Math.round(xx[l].eval(0.95)),
                        (int)Math.round(yy[l].eval(0.95)), pp);
                    if(arrowData.getArrowLength()>0) {
                        xPoints[nPoints-1]=pp.x;
                        yPoints[nPoints-1]=pp.y;
                    }
                }
                // Since the arrow will occupy a certain size, the curve has
                // to be recalculated. This means that the previous evaluation
                // are just approximations, but the practice shows that they
                // are enough for all purposes that can be foreseen.
                // This is not needed if the length is negative, as in this
                // case the arrow extends outside the curve.
                if(arrowData.getArrowLength()>0) {
                    xx = calcNaturalCubic(nPoints-1, xPoints);
                    yy = calcNaturalCubic(nPoints-1, yPoints);
                }
            }
        }

        if(xx==null || yy==null) { return null; }

        // very crude technique: just break each segment up into steps lines
        CurveStorage c = new CurveStorage();

        c.pp.add(new PointDouble(xx[0].eval(0), yy[0].eval(0)));

        for (int i = 0; i < xx.length; ++i) {
            c.dd.add(new PointDouble(xx[i].d1, yy[i].d1));
            for (int j = 1; j <= STEPS; ++j) {
                double u = j / (double) STEPS;
                c.pp.add(new PointDouble(xx[i].eval(u), yy[i].eval(u)));
            }
        }
        c.dd.add(new PointDouble(xx[xx.length-1].d2, yy[xx.length-1].d2));

        return c;
    }

    /** Create the polygon associated to the complex curve. This is a crude
        technique, but it is very easy to implement.
        @param coordSys the coordinate mapping to be employed.
        @param poly the polygon to which the points will be added.
        @return the polygon approximating the complex curve.
    */
    public PolygonInterface createComplexCurvePoly(MapCoordinates coordSys,
        PolygonInterface poly)
    {
        xmin = Integer.MAX_VALUE;
        ymin = Integer.MAX_VALUE;

        int xmax = -Integer.MAX_VALUE;
        int ymax = -Integer.MAX_VALUE;

        CurveStorage c = createComplexCurve(coordSys);

        if (c==null) { return null; }
        List<PointDouble> pp = c.pp;
        if (pp==null) { return null; }

        int x;
        int y;

        for (PointDouble ppp : pp) {
            x=(int)Math.round(ppp.x);
            y=(int)Math.round(ppp.y);
            poly.addPoint(x, y);
            coordSys.trackPoint(x,y);
            if (x<xmin) {
                xmin=x;
            }
            if (x>xmax) {
                xmax=x;
            }
            if(y<ymin) {
                ymin=y;
            }
            if(y>ymax) {
                ymax=y;
            }
        }
        width = xmax-xmin;
        height = ymax-ymin;

        return poly;
    }


    /** Code adapted from Tim Lambert's snippets:
        http://www.cse.unsw.edu.au/~lambert/splines/
        Used here with permissions (hey, thanks a lot, Tim!).
        @param n the number of points.
        @param x the vector containing x coordinates of nodes.
        @return the cubic curve spline'ing the nodes.
    */
    Cubic[] calcNaturalCubic(int n, double... x)
    {

        if(n<1) { return new Cubic[0]; }

        double[] gamma = new double[n+1];
        double[] delta = new double[n+1];
        double[] dd = new double[n+1];
        int i;

        /* We solve the equation
        [2 1       ] [dd[0]]   [3(x[1] - x[0])  ]
        |1 4 1     | |dd[1]|   |3(x[2] - x[0])  |
        |  1 4 1   | | .  | = |      .         |
        |    ..... | | .  |   |      .         |
        |     1 4 1| | .  |   |3(x[n] - x[n-2])|
        [       1 2] [dd[n]]   [3(x[n] - x[n-1])]

        by using row operations to convert the matrix to upper triangular
        and then back substitution.  The dd[i] are the derivatives at the knots.
       */
        gamma[0] = 1.0/2.0;
        for (i = 1; i<n; ++i) {
            gamma[i] = 1.0/(4.0-gamma[i-1]);
        }
        gamma[n] = 1.0/(2.0-gamma[n-1]);

        delta[0] = 3*(x[1]-x[0])*gamma[0];
        for (i = 1; i < n; ++i) {
            delta[i] = (3.0*(x[i+1]-x[i-1])-delta[i-1])*gamma[i];
        }
        delta[n] = (3.0*(x[n]-x[n-1])-delta[n-1])*gamma[n];

        dd[n] = delta[n];
        for (i = n-1; i>=0; --i) {
            dd[i] = delta[i] - gamma[i]*dd[i+1];
        }

        /* now compute the coefficients of the cubics */
        Cubic[] cc = new Cubic[n];
        for (i = 0; i<n; ++i) {
            cc[i] = new Cubic(x[i], dd[i], 3.0*(x[i+1]-x[i])-2.0*dd[i]-dd[i+1],
               2.0*(x[i] - x[i+1]) + dd[i] + dd[i+1]);
            cc[i].d1=dd[i];
            cc[i].d2=dd[i+1];
        }
        return cc;
    }

    /** Code mainly taken from Tim Lambert's snippets:
        http://www.cse.unsw.edu.au/~lambert/splines/

        Used here with permissions (hey, thanks a lot, Tim!)
        calculates the closed natural cubic spline that interpolates
         x[0], x[1], ... x[n]
        The first segment is returned as
        cc[0].a + cc[0].b*u + cc[0].c*u^2 + cc[0].d*u^3 0<=u <1
        the other segments are in cc[1], cc[2], ...  cc[n] */

    Cubic[] calcNaturalCubicClosed(int n, double... x)
    {
        if(n<1) { return new Cubic[0]; }

        double[] w = new double[n+1];
        double[] v = new double[n+1];
        double[] y = new double[n+1];
        double[] dd = new double[n+1];
        double z;
        double ff;
        double gg;
        double hh;
        int k;
        /* We solve the equation
           [4 1      1] [dd[0]]   [3(x[1] - x[n])  ]
           |1 4 1     | |dd[1]|   |3(x[2] - x[0])  |
           |  1 4 1   | | .  | = |      .         |
           |    ..... | | .  |   |      .         |
           |     1 4 1| | .  |   |3(x[n] - x[n-2])|
           [1      1 4] [dd[n]]   [3(x[0] - x[n-1])]

            by decomposing the matrix into upper triangular and lower matrices
            and then back substitution.  See Spath "Spline Algorithms for
            Curves and Surfaces" pp 19--21. The dd[i] are the derivatives at
            the knots.
        */
        w[1] = v[1] = z = 1.0f/4.0f;
        y[0] = z * 3 * (x[1] - x[n]);
        hh = 4;
        ff = 3 * (x[0] - x[n-1]);
        gg = 1;
        for (k = 1; k < n; ++k) {
            v[k+1] = z = 1/(4 - v[k]);
            w[k+1] = -z * w[k];
            y[k] = z * (3*(x[k+1]-x[k-1]) - y[k-1]);
            hh = hh - gg * w[k];
            ff = ff - gg * y[k-1];
            gg = -v[k] * gg;
        }
        hh = hh - (gg+1)*(v[n]+w[n]);
        y[n] = ff - (gg+1)*y[n-1];

        dd[n] = y[n]/hh;
        dd[n-1] = y[n-1] - (v[n]+w[n])*dd[n]; /* This equation is WRONG! in
                                               my copy of Spath */
        for (k = n-2; k >= 0; --k) {
            dd[k] = y[k] - v[k+1]*dd[k+1] - w[k+1]*dd[n];
        }

        /* now compute the coefficients of the cubics */
        Cubic[] cc = new Cubic[n+1];
        for (k = 0; k < n; ++k) {
            cc[k] = new Cubic((float)x[k], dd[k],
                3*(x[k+1] - x[k]) - 2*dd[k] - dd[k+1],
                2*(x[k] - x[k+1]) + dd[k] + dd[k+1]);
            cc[k].d1=dd[k];
            cc[k].d2=dd[k+1];
        }
        cc[n] = new Cubic((float)x[n], dd[n], 3*(x[0] - x[n]) - 2*dd[n] - dd[0],
             2*(x[n] - x[0]) + dd[n] + dd[0]);
        cc[n].d1=dd[n];
        cc[n].d2=dd[0];

        return cc;
    }


    /** Remove the control point of the spline closest to the given
        coordinates, if the distance is less than a certain tolerance

        @param x            the x coordinate of the target
        @param y            the y coordinate of the target
        @param tolerance    the tolerance

    */
    public void removePoint(float x, float y, double tolerance)
    {
        // We can not have a spline with less than three vertices
        if (nPoints<=3) {
            return;
        }

        int i;
        double distance;
        double minDistance= GeometricDistances.pointToPoint(virtualPoint[0].x,
                virtualPoint[0].y,x,y);
        int selI=-1;

        for(i=1;i<nPoints;++i) {
            distance = GeometricDistances.pointToPoint(virtualPoint[i].x,
                virtualPoint[i].y,x,y);

            if (distance<minDistance) {
                minDistance=distance;
                selI=i;
            }
        }

        // Check if the control node losest to the given coordinates
        // is closer than the given tolerance
        if(minDistance<=tolerance){
            --nPoints;
            for(i=0;i<nPoints;++i) {
                // Shift all the points subsequent to the one which needs
                // to be erased.
                if(i>=selI) {
                    virtualPoint[i].x=virtualPoint[i+1].x;
                    virtualPoint[i].y=virtualPoint[i+1].y;
                }
                changed=true;
            }
        }
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

            // Important: notice that createComplexCurve has some important
            // side effects as the update of the xmin, ymin, width and height
            // variables. This means that the order of the two following
            // commands is important!
            q=createComplexCurvePoly(new MapCoordinates(), g.createPolygon());
            p=createComplexCurvePoly(coordSys, g.createPolygon());

            CurveStorage c = createComplexCurve(coordSys);
            // Prevent a null pointer exception when the user does three clicks
            // on the same point. TODO: an incomplete toString output is
            // created.
            if (c==null) {
                return;
            }

            List<PointDouble> dd = c.dd;
            List<PointDouble> pp = c.pp;

            if(q==null) { return; }

            gp = g.createShape();
            gp.createGeneralPath(q.getNpoints());

            gp.moveTo((float)pp.get(0).x, (float)pp.get(0).y);

            int increment=STEPS;
            double derX1=0.0;
            double derX2=0.0;
            double derY1=0.0;
            double derY2=0.0;
            double w1=0.666667;
            double w2=0.666667;
            int j=0; // TODO: check if using i instead of j is sufficient.
            for(int i=0; i<pp.size()-increment; i+=increment) {

                derX1=dd.get(j).x/2.0*w1;
                derY1=dd.get(j).y/2.0*w1;

                derX2=dd.get(j+1).x/2.0*w2;
                derY2=dd.get(j+1).y/2.0*w2;

                ++j;

                gp.curveTo((float)(pp.get(i).x+derX1),
                    (float)(pp.get(i).y+derY1),
                    (float)(pp.get(i+increment).x-derX2),
                    (float)(pp.get(i+increment).y-derY2),
                    (float)pp.get(i+increment).x,
                    (float)pp.get(i+increment).y);
            }

            if (isClosed) { gp.closePath(); }

            w = (float)(Globals.lineWidth*coordSys.getXMagnitude());
            if (w<D_MIN) { w=D_MIN; }
        }

        if (p==null || gp==null) {
            return;
        }
        g.applyStroke(w, dashStyle);

        // Draw the arrows if they are needed
        // Ensure that there are enough points to calculate a derivative.
        if (arrowData.atLeastOneArrow() && p.getNpoints()>2) {
            arrowData.prepareCoordinateMapping(coordSys);

            if (arrowData.isArrowStart()&&!isClosed) {
                arrowData.drawArrow(g,
                    coordSys.mapX(virtualPoint[0].x,virtualPoint[0].y),
                    coordSys.mapY(virtualPoint[0].x,virtualPoint[0].y),
                    p.getXpoints()[1], p.getYpoints()[1]);
            }

            if (arrowData.isArrowEnd()&&!isClosed) {
                int l=nPoints-1;
                arrowData.drawArrow(g,
                    coordSys.mapX(virtualPoint[l].x,virtualPoint[l].y),
                    coordSys.mapY(virtualPoint[l].x,virtualPoint[l].y),
                    p.getXpoints()[p.getNpoints()-2],
                    p.getYpoints()[p.getNpoints()-2]);
            }
        }
        // If the curve is outside of the shown portion of the drawing,
        // exit immediately.
        if(!g.hitClip(xmin,ymin, width+1, height+1)) {
            return;
        }

        // If needed, fill the interior of the shape
        if (isFilled) {
            g.fill(gp);
        }

        if(width==0 || height==0) {
            // Degenerate case: draw a segment.
            int d=nPoints-1;
            g.drawLine(coordSys.mapX(virtualPoint[0].x,virtualPoint[0].y),
                coordSys.mapY(virtualPoint[0].x,virtualPoint[0].y),
                coordSys.mapX(virtualPoint[d].x,virtualPoint[d].y),
                coordSys.mapY(virtualPoint[d].x,virtualPoint[d].y));
        } else {
            // Draw the curve.
            g.draw(gp);
        }
    }

    /** Parse a token array and store the graphic data for a given primitive
        Obviously, that routine should be called *after* having recognized
        that the called primitive is correct.
        That routine also sets the current layer.
        @param tokens the tokens to be processed. tokens[0] should be the
        command of the actual primitive.
        @param nn the number of tokens present in the array.
        @throws IOException it parsing goes wrong, parameters can not be read
            or primitive is incorrect.
    */
    public void parseTokens(String[] tokens, int nn)
        throws IOException
    {
        changed=true;

        // assert it is the correct primitive

        if ("CP".equals(tokens[0])||"CV".equals(tokens[0])) {
            if (nn<6) {
                throw  new IOException("Bad arguments on CP/CV");
            }
            // Load the points in the virtual points associated to the
            // current primitive.
            int j=1;
            int i=0;
            float x1 = 0;
            float y1 = 0;

            // The first token says if the spline is opened or closed
            if("1".equals(tokens[j])) {
                isClosed = true;
            } else {
                isClosed = false;
            }
            ++j;

            // Then we have the points defining the curve
            while(j<nn-1) {
                if (j+1<nn-1 && "FCJ".equals(tokens[j+1])) {
                    break;
                }
                x1 =Float.parseFloat(tokens[j++]);

                // Check if the following point is available
                if(j>=nn-1) {
                    throw new IOException("bad arguments on CP/CV");
                }
                y1 =Float.parseFloat(tokens[j++]);
                ++i;
                addPoint(x1,y1);
            }
            nPoints=i;

            // We specify now the standard position of the name and value
            virtualPoint[getNameVirtualPointNumber()].x=x1+5;
            virtualPoint[getNameVirtualPointNumber()].y=y1+5;
            virtualPoint[getValueVirtualPointNumber()].x=x1+5;
            virtualPoint[getValueVirtualPointNumber()].y=y1+10;

            // And we check finally for extensions (FCJ)
            if(nn>j) {
                parseLayer(tokens[j++]);
                if(nn>j) {
                    if ("FCJ".equals(tokens[j])) {
                        ++j;
                        j=arrowData.parseTokens(tokens, j);
                        dashStyle =
                            checkDashStyle(Integer.parseInt(tokens[j++]));
                    } else {
                        ++j;
                    }
                }
            }

            // See if the curve should be filled (command CP) or empty (CV)
            if ("CP".equals(tokens[0])) {
                isFilled=true;
            } else {
                isFilled=false;
            }
        } else {
            throw new IOException("CP/CV: Invalid primitive:"+tokens[0]+
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

        pd.parameter=Boolean.valueOf(isFilled);
        pd.description=Globals.messages.getString("ctrl_filled");
        v.add(pd);

        pd = new ParameterDescription();
        pd.parameter=Boolean.valueOf(isClosed);
        pd.description=Globals.messages.getString("ctrl_closed_curve");
        pd.isExtension = true;
        v.add(pd);

        arrowData.getControlsForArrow(v);

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
            parameter. The first parameters should always be the virtual
            points.
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
        // Check, just for sure...
        if (pd.parameter instanceof Boolean) {
            isClosed=((Boolean)pd.parameter).booleanValue();
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }

        i=arrowData.setParametersForArrow(v, i);

        pd=v.get(i++);
        if (pd.parameter instanceof DashInfo) {
            dashStyle=((DashInfo)pd.parameter).style;
        } else {
            System.out.println("Warning: unexpected parameter 6!"+pd);
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
        When it is reasonable, the behaviour can be binary (ComplexCurves,
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

        int distance = 100;

        // In this case, the user has not introduced a complete curve,
        // but just one point.
        if(p==null) {
            return GeometricDistances.pointToPoint(
                Math.round(virtualPoint[0].x),
                Math.round(virtualPoint[0].y),
                Math.round(px),
                Math.round(py));
        }

        // If the curve is filled, we check if the given point lies inside
        // the polygon.
        if(isFilled && q.contains(Math.round(px), Math.round(py))) {
            return 1;
        }

        // If the curve is not filled, we calculate the distance between the
        // given point and all the segments composing the curve and we
        // take the smallest one.

        int [] xpoints=q.getXpoints();
        int [] ypoints=q.getYpoints();

        // Check if the point is in the arrows. Correct the starting and ending
        // points if needed.
        if (arrowData.atLeastOneArrow()&& !isClosed) {
            boolean r=false;
            boolean t=false;

            // We work with logic coordinates (default for MapCoordinates).
            MapCoordinates m=new MapCoordinates();
            arrowData.prepareCoordinateMapping(m);
            if (arrowData.isArrowStart()) {
                t=arrowData.isInArrow(px, py,
                    Math.round(virtualPoint[0].x), 
                    Math.round(virtualPoint[0].y),
                    xpoints[0], ypoints[0], null);
            }
            if (arrowData.isArrowEnd()) {
                r=arrowData.isInArrow(px, py,
                    Math.round(xpoints[q.getNpoints()-1]),
                    Math.round(ypoints[q.getNpoints()-1]),
                    Math.round(virtualPoint[nPoints-1].x),
                    Math.round(virtualPoint[nPoints-1].y),
                    null);
            }

            // Click on one of the arrows.
            if(r||t) {
                return 1;
            }
        }

        for(int i=0; i<q.getNpoints()-1; ++i) {
            int d=GeometricDistances.pointToSegment(xpoints[i], ypoints[i],
                xpoints[i+1], ypoints[i+1], Math.round(px),Math.round(py));

            if(d<distance) {
                distance = d;
            }
        }
        return distance;
    }

    /** Obtain a string command descripion of the primitive.
        @param extensions true if FidoCadJ extensions to the old FidoCAD format
            should be active.
        @return the FidoCadJ command line.
    */
    public String toString(boolean extensions)
    {
        // A single point curve without anything is not worth converting.
        if (name.length()==0 && value.length()==0 && nPoints==1) {
            return "";
        }

        StringBuffer temp=new StringBuffer(25);

        if(isFilled) {
            temp.append("CP ");
        } else {
            temp.append("CV ");
        }

        if(isClosed) {
            temp.append("1 ");
        } else {
            temp.append("0 ");
        }

        for(int i=0; i<nPoints;++i) {
            temp.append(roundIntelligently(virtualPoint[i].x));
            temp.append(" ");
            temp.append(roundIntelligently(virtualPoint[i].y));
            temp.append(" ");
        }

        temp.append(getLayer());
        temp.append("\n");

        String cmd=temp.toString();

        if(extensions && (arrowData.atLeastOneArrow() || dashStyle>0 ||
            hasName() || hasValue()))
        {
            String text = "0";
            // We take into account that there may be some text associated
            // to that primitive.
            if (name.length()!=0 || value.length()!=0) {
                text = "1";
            }
            cmd+="FCJ "+arrowData.createArrowTokens()+" "+dashStyle+" "
                +text+"\n";
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
        double [] xPoints = new double[nPoints];
        double [] yPoints = new double[nPoints];
        PointDouble[] vertices = new PointDouble[nPoints*STEPS+1];

        for (int i=0; i<nPoints; ++i) {
            xPoints[i] = cs.mapXr(virtualPoint[i].x,virtualPoint[i].y);
            yPoints[i] = cs.mapYr(virtualPoint[i].x,virtualPoint[i].y);

            // This is a trick: we do not use another array, but we pre-charge
            // the control points in vertices (surely we have some place, at
            // least if STEPS>-1). If the export is done via a polygon, those
            // points will be discarded and the array reused.
            vertices[i] = new PointDouble();
            vertices[i].x = xPoints[i];
            vertices[i].y = yPoints[i];
        }

        // Check if the export is handled via a dedicated curve primitive.
        // If not, we continue using a polygon with an high number of
        // vertex
        if (!exp.exportCurve(vertices, nPoints, isFilled, isClosed, getLayer(),
                arrowData.isArrowStart(), arrowData.isArrowEnd(),
                arrowData.getArrowStyle(),
                (int)(arrowData.getArrowLength()*cs.getXMagnitude()),
                (int)(arrowData.getArrowHalfWidth()*cs.getXMagnitude()),
                dashStyle, Globals.lineWidth*cs.getXMagnitude()))
        {
            exportAsPolygonInterface(xPoints, yPoints, vertices, exp, cs);

            int totalnP=q.getNpoints();

            // Draw the arrows if they are needed
            if(q.getNpoints()>2) {
                if (arrowData.isArrowStart()&&!isClosed) {
                    exp.exportArrow(
                        cs.mapX(virtualPoint[0].x,virtualPoint[0].y),
                        cs.mapY(virtualPoint[0].x,virtualPoint[0].y),
                        vertices[1].x, vertices[1].y,
                        arrowData.getArrowLength()*cs.getXMagnitude(),
                        arrowData.getArrowHalfWidth()*cs.getXMagnitude(),
                        arrowData.getArrowStyle());
                }
                if (arrowData.isArrowEnd()&&!isClosed) {
                    int l=nPoints-1;
                    exp.exportArrow(
                        cs.mapX(virtualPoint[l].x,virtualPoint[l].y),
                        cs.mapY(virtualPoint[l].x,virtualPoint[l].y),
                        vertices[totalnP-2].x, vertices[totalnP-2].y,
                        arrowData.getArrowLength()*cs.getXMagnitude(),
                        arrowData.getArrowHalfWidth()*cs.getXMagnitude(),
                        arrowData.getArrowStyle());
                }
            }
        }
        exportText(exp, cs, -1);

    }

    /** Expansion of the curve in a polygon with a big number of corners.
        This is useful when some sort of spline command is not available on
        the export format chosen.

    */
    private void exportAsPolygonInterface(double [] xPoints, double [] yPoints,
        PointDouble[] vertices,
        ExportInterface exp, MapCoordinates cs)
        throws IOException
    {
        Cubic[] xx;
        Cubic[] yy;
        int i;

        if(isClosed) {
            xx = calcNaturalCubicClosed(nPoints-1, xPoints);
            yy = calcNaturalCubicClosed(nPoints-1, yPoints);
        } else {
            xx = calcNaturalCubic(nPoints-1, xPoints);
            yy = calcNaturalCubic(nPoints-1, yPoints);
            // Here we don't check if a point is in the arrow, but we exploit
            // the code for calculating the base of the head of the arrows.
            if (arrowData.atLeastOneArrow()) {
                arrowData.prepareCoordinateMapping(cs);
                if (arrowData.isArrowStart()) {
                    PointG pp = new PointG();
                    arrowData.isInArrow(0, 0,
                        (int)Math.round(xx[0].eval(0)),
                        (int)Math.round(yy[0].eval(0)),
                        (int)Math.round(xx[0].eval(0.05)),
                        (int)Math.round(yy[0].eval(0.05)), pp);
                    if(arrowData.getArrowLength()>0) {
                        xPoints[0]=pp.x;
                        yPoints[0]=pp.y;
                    }
                }

                if (arrowData.isArrowEnd()) {
                    int l=xx.length-1;
                    PointG pp = new PointG();
                    arrowData.isInArrow(0, 0,
                        (int)Math.round(xx[l].eval(1)),
                        (int)Math.round(yy[l].eval(1)),
                        (int)Math.round(xx[l].eval(0.95)),
                        (int)Math.round(yy[l].eval(0.95)), pp);
                    if(arrowData.getArrowLength()>0) {
                        xPoints[nPoints-1]=pp.x;
                        yPoints[nPoints-1]=pp.y;
                    }
                }
                // Since the arrow will occupy a certain size, the curve has
                // to be recalculated. This means that the previous evaluation
                // are just approximations, but the practice shows that they
                // are enough for all purposes that can be foreseen.
                // This is not needed if the length is negative, as in this
                // case the arrow extends outside the curve.
                if(arrowData.getArrowLength()>0) {
                    xx = calcNaturalCubic(nPoints-1, xPoints);
                    yy = calcNaturalCubic(nPoints-1, yPoints);
                }
            }
        }

        if(xx==null || yy==null) { return; }

        /* very crude technique - just break each segment up into steps lines */

        vertices[0]=new PointDouble();

        vertices[0].x=xx[0].eval(0);
        vertices[0].y=yy[0].eval(0);

        for (i = 0; i < xx.length; ++i) {
            for (int j = 1; j <= STEPS; ++j) {
                double u = j / (double) STEPS;
                vertices[i*STEPS+j]=new PointDouble();
                vertices[i*STEPS+j].x=xx[i].eval(u);
                vertices[i*STEPS+j].y=yy[i].eval(u);
            }
        }

        vertices[xx.length*STEPS]=new PointDouble();
        vertices[xx.length*STEPS].x=xx[xx.length-1].eval(1.0);
        vertices[xx.length*STEPS].y=yy[xx.length-1].eval(1.0);

        if (isClosed || isFilled) {
            exp.exportPolygon(vertices, xx.length*STEPS+1, isFilled,
                getLayer(),
                dashStyle, Globals.lineWidth*cs.getXMagnitude());
        } else {
            float phase=0;
            for(i=1; i<xx.length*STEPS+1;++i){
                exp.setDashPhase(phase);
                exp.exportLine(vertices[i-1].x,
                       vertices[i-1].y,
                       vertices[i].x,
                       vertices[i].y,
                       getLayer(),
                       false, false,
                       0, 0, 0,
                       dashStyle,
                       Globals.lineWidth*cs.getXMagnitude());
                phase+=Math.sqrt(Math.pow(vertices[i-1].x-vertices[i].x,2)+
                    Math.pow(vertices[i-1].y-vertices[i].y,2));
            }
        }
    }
    /** Get the number of the virtual point associated to the Name property
        @return the number of the virtual point associated to the Name property
    */
    public int getNameVirtualPointNumber()
    {
        return nPoints;
    }

    /** Get the number of the virtual point associated to the Value property
        @return the number of the virtual point associated to the Value
        property
    */
    public  int getValueVirtualPointNumber()
    {
        return nPoints+1;
    }

    /**
     * Determines whether the shape defined by the points in the polygon "q"
     * intersects with the specified selection rectangle. This method checks
     * for intersections in two different ways depending on the selection
     * direction.
     *
     * If "isLeftToRightSelection" is true, the method checks if the entire
     * curve (defined by its points) is fully contained within the selection
     * rectangle. If all points of the curve are contained,
     * the method returns true.
     *
     * If "isLeftToRightSelection" is false, the method checks for any..
     * intersections between the rectangle and the segments of the curve.
     * It also returns true if any vertex of the curve lies inside the
     * rectangle.
     *
     * @param rect the RectangleG object representing the selection rectangle.
     * @param isLeftToRightSelection true if the selection is from left to right
     *                               and requires the entire curve to be ..
     *                               contained within the rectangle for a match.
     *
     * @return true if any part of the curve intersects the rectangle, or if any
     *              vertex is contained within the rectangle when..
     *              "isLeftToRightSelection" is false. Otherwise, returns false.
     */
    @Override
    public boolean intersects(RectangleG rect, boolean isLeftToRightSelection)
    {
        if (getCurrentLayer().isVisible() == false) {
            return false;
        }

        if (isLeftToRightSelection) {
            // Check if all points of the curve are contained..
            // within the selection rectangle.
            if (q != null) {
                int[] xpoints = q.getXpoints();
                int[] ypoints = q.getYpoints();
                for (int i = 0; i < q.getNpoints(); i++) {
                    if (!rect.contains(xpoints[i], ypoints[i])) {
                        // If even a single point is not contained, return false
                        return false;
                    }
                }
                // If all points are contained, return true
                return true;
            }
        } else {
            // Check if there is an intersection between the rectangle ..
            // and the curve's lines.
            if (q != null) {
                int[] xpoints = q.getXpoints();
                int[] ypoints = q.getYpoints();

                // Check if any vertex is inside the selection rectangle
                for (int i = 0; i < q.getNpoints(); i++) {
                    if (rect.contains(xpoints[i], ypoints[i])) {
                        return true;
                    }
                }

                // Check intersections between the rectangle ..
                // and the curve's segments.
                for (int i = 0; i < q.getNpoints() - 1; i++) {
                    if (rect.intersectsLine(xpoints[i], ypoints[i],
                            xpoints[i + 1], ypoints[i + 1]))
                    {
                        return true;
                    }
                }

                // If the curve is closed, also check the closing segment
                if (isClosed && q.getNpoints() > 1) {
                    if (rect.intersectsLine(xpoints[q.getNpoints() - 1],
                            ypoints[q.getNpoints() - 1],
                            xpoints[0], ypoints[0]))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

/** this class represents a cubic polynomial, by Tim Lambert */

class Cubic
{
    double a;         /* a + b*u + c*u^2 +d*u^3 */
    double b;
    double c;
    double d;
    public double d1;       // Derivatives
    public double d2;

    public Cubic(double a, double b, double c, double d)
    {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    /** evaluate cubic */
    public double eval(double u)
    {
        return ((d*u + c)*u + b)*u + a;
    }
}

/** The curve is stored in two vectors.
    The first contains a curve representation as a polygon with a lot of
    vertices.
    The second has as much as elements as the number of control vertices and
    stores only the derivatives.
*/
class CurveStorage
{
    List<PointDouble> pp; // Curve as a polygon (relatively big)
    List<PointDouble> dd; // Derivatives

    public CurveStorage()
    {
        pp = new Vector<PointDouble>();
        dd = new Vector<PointDouble>();
    }
}
