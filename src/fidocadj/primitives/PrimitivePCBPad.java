package fidocadj.primitives;

import java.io.*;
import java.util.*;

import fidocadj.dialogs.controls.ParameterDescription;
import fidocadj.export.ExportInterface;
import fidocadj.geom.GeometricDistances;
import fidocadj.geom.MapCoordinates;
import fidocadj.globals.Globals;
import fidocadj.graphic.GraphicsInterface;

/** Class to handle the PCB pad primitive.

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
public final class PrimitivePCBPad extends GraphicPrimitive
{
    private float rx;
    private float ry;
    private int sty;
    private float ri;

    // The radius of the rounded corner in logical units. This is hardcoded
    // here as it has been done for FidoCadJ, but one may consider let this
    // value to be changed by the user interactively
    private static final int CORNER_DIAMETER = 5;

    private boolean drawOnlyPads;

    // A PCB pad is defined by one points, plus text tags

    static final int N_POINTS=3;

    // Those are data which are kept for the fast redraw of this primitive.
    // Basically, they are calculated once and then used as much as possible
    // without having to calculate everything from scratch.
    private float x1;         // NOPMD
    private float y1;         // NOPMD
    private int rrx;
    private int rry;
    private int xa;
    private int ya;
    private int rox;
    private int roy;
    private int rix;
    private int riy;
    private int rrx2;
    private int rry2;
    private int rix2;
    private int riy2;

    /** Gets the number of control points used.
        @return the number of points used by the primitive
    */
    public int getControlPointNumber()
    {
        return N_POINTS;
    }

    /** Create a PCB pad
        @param x1 the x coordinate (logical unit).
        @param y1 the y coordinate (logical unit).
        @param wx the width of the pad
        @param wy the height of the pad
        @param radi the internal radius.
        @param st the style of the pad
        @param layer the layer to be used.
        @param f the name of the font for attached text.
        @param size the size of the font for attached text.
    */
    public PrimitivePCBPad(float x1, float y1, float wx, float wy, 
        float radi, int st, int layer, String f, float size)
    {
        super();
        initPrimitive(-1, f, size);

        virtualPoint[0].x=x1;
        virtualPoint[0].y=y1;
        virtualPoint[getNameVirtualPointNumber()].x=x1+5;
        virtualPoint[getNameVirtualPointNumber()].y=y1+5;
        virtualPoint[getValueVirtualPointNumber()].x=x1+5;
        virtualPoint[getValueVirtualPointNumber()].y=y1+10;

        rx=wx;
        ry=wy;
        ri=radi;
        sty=st;

        setLayer(layer);
    }

    /** Constructor. Create an empty PCBPad object with all the sizes and
        dimensions set to zero.
        @param f the name of the font for attached text.
        @param size the size of the font for attached text.
    */
    public PrimitivePCBPad(String f, float size)
    {
        super();
        rx=0;
        ry=0;
        sty=0;
        ri=0;
        initPrimitive(-1, f, size);
    }

    /** Check if the holes should be drawn.
        @return true for a PCBpad.
    */
    @Override
    public boolean needsHoles()
    {
        return true;
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

            /* in the PCB pad primitive, the the virtual points represent
           the position of the pad to be drawn. */
            x1=virtualPoint[0].x;
            y1=virtualPoint[0].y;

            xa=coordSys.mapXi(x1,y1,false);
            ya=coordSys.mapYi(x1,y1,false);

            rrx=Math.abs(xa-coordSys.mapXi(x1+rx,y1+ry, false));
            rry=Math.abs(ya-coordSys.mapYi(x1+rx,y1+ry, false));
            rrx2 = rrx/2;
            rry2 = rry/2;

            coordSys.trackPoint(xa-rrx2,ya-rry2);
            coordSys.trackPoint(xa+rrx2,ya+rry2);

            rox=Math.abs(xa-coordSys.mapXi(x1+CORNER_DIAMETER,
                y1+CORNER_DIAMETER, false));
            roy=Math.abs(ya-coordSys.mapYi(x1+CORNER_DIAMETER,
                y1+CORNER_DIAMETER, false));

            rix=Math.abs(xa-coordSys.mapXi(x1+ri,y1+ri, false));
            riy=Math.abs(ya-coordSys.mapYi(x1+ri,y1+ri, false));

            rix2 = rix/2;
            riy2 = riy/2;
        }

        // Exit if the primitive is offscreen. This is a simplification, but
        // ensures that the primitive is correctly drawn when it is
        // partially visible.

        if(!g.hitClip(xa-rrx2,ya-rry2, rrx, rry)) {
            return;
        }

        g.applyStroke(1, 0);

        if (drawOnlyPads) {
            g.setColor(g.getColor().white()); // Drill the hole
            g.fillOval(xa-rix2, ya-riy2,rix,riy);
        } else {
            switch(sty) {
                case 1:
                    // Rectangular pad
                    g.fillRect(xa-rrx2, ya-rry2,rrx,rry);
                    break;
                case 2:
                    // Rounded corner rectangular pad
                    g.fillRoundRect(xa-rrx2, ya-rry2,rrx,rry,rox,roy);
                    break;
                case 0: //NOPMD
                default:
                    // Oval Pad
                    g.fillOval(xa-rrx2, ya-rry2,rrx,rry);
                    break;
            }
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

        if ("PA".equals(tokens[0])) {   // PCB Area pad
            /* Example PA 752 50 15 15 4 1 1 */

            if (nn<7)  {
                throw new IOException("bad arguments on PA");
            }
            // Load the points in the virtual points associated to the
            // current primitive.

            float x1 = virtualPoint[0].x=Float.parseFloat(tokens[1]);
            float y1 = virtualPoint[0].y=Float.parseFloat(tokens[2]);
            virtualPoint[getNameVirtualPointNumber()].x=x1+5;
            virtualPoint[getNameVirtualPointNumber()].y=y1+5;
            virtualPoint[getValueVirtualPointNumber()].x=x1+5;
            virtualPoint[getValueVirtualPointNumber()].y=y1+10;
            rx=Float.parseFloat(tokens[3]);
            ry=Float.parseFloat(tokens[4]);
            ri=Float.parseFloat(tokens[5]);
            sty=Integer.parseInt(tokens[6]);

            if(nn>7) { parseLayer(tokens[7]); }

        } else {
            throw new IOException("PA: Invalid primitive:"+tokens[0]+
                                          " programming error?");
        }
    }

    /** Set the draw only pads flag.
        @param pd the value of the flag.
    */
    public void setDrawOnlyPads(boolean pd)
    {
        drawOnlyPads=pd;
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

        pd.parameter= Float.valueOf(rx);
        pd.description=Globals.messages.getString("ctrl_x_radius");
        v.add(pd);
        pd = new ParameterDescription();
        pd.parameter= Float.valueOf(ry);
        pd.description=Globals.messages.getString("ctrl_y_radius");
        v.add(pd);
        pd = new ParameterDescription();
        pd.parameter= Float.valueOf(ri);
        pd.description=Globals.messages.getString("ctrl_internal_radius");
        v.add(pd);
        pd = new ParameterDescription();
        pd.parameter= Integer.valueOf(sty); // A list should be better
        pd.description=Globals.messages.getString("ctrl_pad_style");
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
            rx=((Float)pd.parameter).floatValue();
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }

        pd=v.get(i);
        ++i;
        // Check, just for sure...
        if (pd.parameter instanceof Float) {
            ry=((Float)pd.parameter).floatValue();
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }

        pd=v.get(i);
        ++i;
        // Check, just for sure...
        if (pd.parameter instanceof Float) {
            ri=((Float)pd.parameter).floatValue();
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }
        pd=v.get(i);
        ++i;
        // Check, just for sure...
        if (pd.parameter instanceof Integer) {
            sty=((Integer)pd.parameter).intValue();
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }
        return i;
    }

    /** Rotate the primitive. Here we just rotate 90° by 90° by swapping the
        x and y size of the pad
        @param bCounterClockWise specify if the rotation should be done
                counterclockwise.
        @param ix the x coordinate of the rotation point.
        @param iy the y coordinate of the rotation point.
    */
    @Override public void rotatePrimitive(boolean bCounterClockWise,
        float ix, float iy)
    {
        super.rotatePrimitive(bCounterClockWise, ix, iy);
        float swap=rx;
        rx=ry;
        ry=swap;
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

        double distance=GeometricDistances.pointToPoint(
                virtualPoint[0].x,virtualPoint[0].y,
                px,py)-Math.min(rx,ry)/2;
        return distance>0?Math.round((float)distance):0;
    }

    /** Obtain a string command descripion of the primitive.
        @param extensions true if FidoCadJ extensions to the old FidoCAD format
            should be active.
        @return the FIDOCAD command line.
    */
    public String toString(boolean extensions)
    {
        String s = "PA "
            +roundIntelligently(virtualPoint[0].x)+" "
            +roundIntelligently(virtualPoint[0].y)+" "
            +roundIntelligently(rx)+" "
            +roundIntelligently(ry)+" "
            +roundIntelligently(ri)+" "+sty+" "+getLayer()+"\n";

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
        exp.exportPCBPad(cs.mapX(virtualPoint[0].x,virtualPoint[0].y),
                       cs.mapY(virtualPoint[0].x,virtualPoint[0].y),
                       sty,
                       Math.abs(cs.mapX(virtualPoint[0].x+rx,
                            virtualPoint[0].y+ry)-
                       cs.mapX(virtualPoint[0].x,virtualPoint[0].y)),
                       Math.abs(cs.mapY(virtualPoint[0].x+rx,
                            virtualPoint[0].y+ry)-
                       cs.mapY(virtualPoint[0].x,virtualPoint[0].y)),
                       (int)(ri*cs.getXMagnitude()), getLayer(),drawOnlyPads);
    }
    /** Get the number of the virtual point associated to the Name property
        @return the number of the virtual point associated to the Name property
    */
    public int getNameVirtualPointNumber()
    {
        return 1;
    }

    /** Get the number of the virtual point associated to the Value property
        @return the number of the virtual point associated to the Value property
    */
    public  int getValueVirtualPointNumber()
    {
        return 2;
    }
}