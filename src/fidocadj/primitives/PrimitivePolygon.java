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
import fidocadj.graphic.PointDouble;
import fidocadj.graphic.PointG;
import fidocadj.graphic.PolygonInterface;
import fidocadj.graphic.RectangleG;

/** Class to handle the Polygon primitive.

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
public final class PrimitivePolygon extends GraphicPrimitive
{
    private int nPoints;
    private boolean isFilled;
    private int dashStyle;
    private PolygonInterface p;

    // If needed, we might increase this stuff.
    // In other words, we initially create space for storing 5 points and
    // we increase that if needed.
    int storageSize=5;

    // Some private data cached.
    private int xmin;
    private int ymin;
    private int width;
    private int height;

    // Those are data which are kept for the fast redraw of this primitive.
    // Basically, they are calculated once and then used as much as possible
    // without having to calculate everything from scratch.
    private float w;


    /** Gets the number of control points used.
        @return the number of points used by the primitive
    */
    public int getControlPointNumber()
    {
        return nPoints+2;
    }

    /** Constructor.
        @param f the name of the font for attached text.
        @param size the size of the font for attached text.
    */
    public PrimitivePolygon(String f, float size)
    {
        super();
        isFilled=false;
        nPoints=0;
        p = null;
        initPrimitive(storageSize, f, size);
    }

    /** Create a polygon. Add points with the addPoint method.
        @param f specifies if the polygon should be filled
        @param layer the layer to be used.
        @param dashSt the dash style
        @param font the name of the font for attached text.
        @param size the size of the font for attached text.
    */
    public PrimitivePolygon(boolean f, int layer, int dashSt,
        String font, float size)
    {
        super();
        p = null;
        initPrimitive(storageSize, font,  size);
        nPoints=0;
        isFilled=f;
        dashStyle=dashSt;
        setLayer(layer);
    }

    /** Remove the control point of the polygon closest to the given
        coordinates, if the distance is less than a certain tolerance

        @param x            the x coordinate of the target
        @param y            the y coordinate of the target
        @param tolerance    the tolerance

    */
    public void removePoint(float x, float y, double tolerance)
    {
        // We can not have a polygon with less than three vertices
        if (nPoints<=3) {
            return;
        }

        double distance;
        double minDistance= GeometricDistances.pointToPoint(virtualPoint[0].x,
                virtualPoint[0].y,x,y);
        int selI=-1;

        for(int i=1;i<nPoints;++i) {
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
            for(int i=0;i<nPoints;++i) {
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

    /** Add a point in the polygon, by splitting the closes side to the point
        inserted.
        @param px x coordinates of the point to insert.
        @param py y coordinates of the point to insert.
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
        // we calculate the distance between the
        // given point and all the segments composing the polygon and we
        // take the smallest one.

        float distance=(float)Math.sqrt((px-xp[0])*(px-xp[0])+
            (py-yp[0])*(py-yp[0]));

        int j;
        float d;
        int minv=0;
        for(int i=0; i<nPoints; ++i) {
            j=i;
            if (j==nPoints-1) {
                j=-1;
            }

            d=(float)GeometricDistances.pointToSegment(xp[i],
                yp[i], xp[j+1],
                yp[j+1], px,py);

            if(d<distance) {
                distance = d;
                minv=j+1;
            }
        }

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

    /** Add a point at the current polygon
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

    /** Store the polygon, which must be already calculated.
        @param coordSys the coordinates mapping system.
        @param g the graphic context.
    */
    public void createPolygon(MapCoordinates coordSys, GraphicsInterface g)
    {
        int j;
        xmin = Integer.MAX_VALUE;
        ymin = Integer.MAX_VALUE;

        int xmax = -Integer.MAX_VALUE;
        int ymax = -Integer.MAX_VALUE;

        int x;
        int y;
        p=g.createPolygon();
        p.reset();
        for(j=0;j<nPoints;++j) {
            x = coordSys.mapX(virtualPoint[j].x,virtualPoint[j].y);
            y = coordSys.mapY(virtualPoint[j].x,virtualPoint[j].y);
            p.addPoint(x,y);

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
            createPolygon(coordSys, g);

            w = (float)(Globals.lineWidth*coordSys.getXMagnitude());
            if (w<D_MIN) { w=D_MIN; }
        }

        if(!g.hitClip(xmin,ymin, width, height)) {
            return;
        }

        g.applyStroke(w, dashStyle);

        // Here we implement a small optimization: when the polygon is very
        // small, it is not filled.
        if (isFilled && width>=2 && height >=2) {
            g.fillPolygon(p);
        }

        g.drawPolygon(p);
        // It seems that under MacOSX, drawing a polygon by cycling with
        // the lines is much more efficient than the drawPolygon method.
        // Probably, a further investigation is needed to determine if
        // this situation is the same with more recent Java runtimes
        // (mine is 1.5.something on an iMac G5 at 2 GHz and I made
        // the same comparison with the same results with a MacBook 2GHz).
         /*
        for(int i=0; i<nPoints-1; ++i) {
            g.drawLine(p.xpoints[i], p.ypoints[i], p.xpoints[i+1],
                p.ypoints[i+1]);
        }
        g.drawLine(p.xpoints[nPoints-1], p.ypoints[nPoints-1], p.xpoints[0],
            p.ypoints[0]);
        */
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
        if ("PP".equals(tokens[0])||"PV".equals(tokens[0])) {
            if (nn<6) {
                throw new IOException("Bad arguments on PP/PV");
            }
            // Load the points in the virtual points associated to the
            // current primitive.
            int j=1;
            int i=0;
            float x1 = 0;
            float y1 = 0;

            while(j<nn-1){
                if (j+1<nn-1 && "FCJ".equals(tokens[j+1])) {
                    break;
                }
                x1 = Float.parseFloat(tokens[j++]);
                y1 = Float.parseFloat(tokens[j++]);
                ++i;
                addPoint(x1,y1);
            }
            nPoints=i;
            virtualPoint[getNameVirtualPointNumber()].x=x1+5;
            virtualPoint[getNameVirtualPointNumber()].y=y1+5;
            virtualPoint[getValueVirtualPointNumber()].x=x1+5;
            virtualPoint[getValueVirtualPointNumber()].y=y1+10;
            if(nn>j) {
                parseLayer(tokens[j++]);
                if(j<nn-1 && "FCJ".equals(tokens[j])) {
                    dashStyle = checkDashStyle(Integer.parseInt(tokens[++j]));
                }
                ++j;
            }

            if ("PP".equals(tokens[0])) {
                isFilled=true;
            } else {
                isFilled=false;
            }
        } else {
            throw new IOException("PP/PV: Invalid primitive:"+tokens[0]+
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

        float[] xp=new float[storageSize];
        float[] yp=new float[storageSize];

        int k;

        for(k=0;k<nPoints;++k){
            xp[k]=virtualPoint[k].x;
            yp[k]=virtualPoint[k].y;
        }

        if(isFilled&&GeometricDistances.pointInPolygon(xp,yp,nPoints, px,py)) {
            return 1;
        }


        // If the curve is not filled, we calculate the distance between the
        // given point and all the segments composing the curve and we
        // take the smallest one.

        double distance=Math.sqrt((px-xp[0])*(px-xp[0])+
            (py-yp[0])*(py-yp[0]));

        int j;
        double d;
        for(int i=0; i<nPoints; ++i) {
            j=i;
            if (j==nPoints-1) {
                j=-1;
            }

            d=GeometricDistances.pointToSegment(xp[i],
                yp[i], xp[j+1],
                yp[j+1], px,py);

            if(d<distance) {
                distance = d;
            }
        }

        return (int)Math.round(distance);
    }

    /** Obtain a string command descripion of the primitive.
        @param extensions true if FidoCadJ extensions to the old FidoCAD format
            should be active.
        @return the FIDOCAD command line.
    */
    public String toString(boolean extensions)
    {
        StringBuffer temp=new StringBuffer(25);

        if(isFilled) {
            temp.append("PP ");
        } else {
            temp.append("PV ");
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
        PointDouble[] vertices = new PointDouble[nPoints];

        for(int i=0; i<nPoints;++i){
            vertices[i]=new PointDouble();
            vertices[i].x=cs.mapX(virtualPoint[i].x,virtualPoint[i].y);
            vertices[i].y=cs.mapY(virtualPoint[i].x,virtualPoint[i].y);
        }

        exp.exportPolygon(vertices, nPoints, isFilled, getLayer(), dashStyle,
            Globals.lineWidth*cs.getXMagnitude());
    }
    /** Get the number of the virtual point associated to the Name property
        @return the number of the virtual point associated to the Name property
    */
    public int getNameVirtualPointNumber()
    {
        return nPoints;
    }

    /** Get the number of the virtual point associated to the Value property
        @return the number of the virtual point associated to the Value property
    */
    public  int getValueVirtualPointNumber()
    {
        return nPoints+1;
    }

    /**
     * Checks if the polygon intersects with the given selection rectangle.
     * This method determines if any part of the polygon intersects with the
     * rectangle, including edges and vertices.
     *
     * @param rect the selection rectangle to check for intersection.
     * @param isLeftToRightSelection if true, checks if the rectangle fully
     * contains the polygon (for left-to-right selections).
     *
     * @return true if the rectangle intersects with any part of the polygon,
     *         false otherwise.
     */
    @Override
    public boolean intersects(RectangleG rect,
                              boolean isLeftToRightSelection)
    {
        if (getCurrentLayer().isVisible() == false) {
            return false;
        }

        // Convert the polygon's points to arrays for easy processing
        float[] xp = new float[nPoints];
        float[] yp = new float[nPoints];

        for (int i = 0; i < nPoints; i++) {
            xp[i] = virtualPoint[i].x;
            yp[i] = virtualPoint[i].y;
        }

        if (isLeftToRightSelection) {
            /* Check if all vertices of the polygon are..
               inside the selection rectangle.
            */
            for (int i = 0; i < nPoints; i++) {
                if (!rect.contains(xp[i], yp[i])) {
                    return false;
                }
            }
            return true;
        } else {
            /* Check if any vertex of the polygon is inside
               the selection rectangle
             */
            for (int i = 0; i < nPoints; i++) {
                if (rect.contains(xp[i], yp[i])) {
                    return true;
                }
            }

            /* Check if any edge of the polygon
               intersects with the rectangle's edges
             */
            for (int i = 0; i < nPoints; i++) {
                int next = (i + 1) % nPoints;
                if (rect.intersectsLine(xp[i], yp[i], xp[next], yp[next])) {
                    return true;
                }
            }

            // Check if the rectangle is fully contained within the polygon
            if (isFilled && GeometricDistances.pointInPolygon(xp, yp, nPoints,
                    rect.getX(), rect.getY())
                    && GeometricDistances.pointInPolygon(xp, yp, nPoints,
                            rect.getX() + rect.getWidth(), rect.getY())
                    && GeometricDistances.pointInPolygon(xp, yp, nPoints,
                            rect.getX(), rect.getY() + rect.getHeight())
                    && GeometricDistances.pointInPolygon(xp, yp, nPoints,
                            rect.getX() + rect.getWidth(),
                            rect.getY() + rect.getHeight()))
            {
                return true;
            }
        }
        return false;
    }
}