package fidocadj.geom;
import java.util.*;

/** MapCoordinates.java

<pre>
    @author D. Bucci

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

    MapCoordinates performs the coordinate mapping between the logical units
    used in FidoCad schematics with the corrisponding pixel position
    to be used when drawing on the screen or on an image file. The class
    tracks also the minimum and maximum values of the pixel x/y coordinates, in
    order to obtain the drawing size.

    The logical Fidocad resolution is 5 mils (127um). Thus, for the following
    resolutions, we obtain:

<pre>
    Resolution              units/pixel         x/y magnitude
    ---------------------------------------------------------
    72  pixels/inch         2.7778              0.36000
    150 pixels/inch         1.3333              0.75000
    300 pixels/inch         0.66666             1.50000
    600 pixels/inch         0.33333             3.00000
    1200 pixels/inch        0.16667             6.00000

</pre>

    This class allows to concatenate a translation with the xCenter and yCenter
    variables. This should NOT be used to scroll the actual drawing in the
    viewport, since this is done by using the JScrollPane Swing control.
    This is indeed very useful when exporting or when drawing macros.
*/


public class MapCoordinates
{
    // Every member should be made private sooner or later...
    private double xCenter;
    private double yCenter;
    private double xMagnitude;
    private double yMagnitude;
    private int orientation;
    public boolean mirror;
    public boolean isMacro;
    private boolean snapActive;

    public static final double MIN_MAGNITUDE=0.25;
    public static final double MAX_MAGNITUDE=100.0;

    private double vx;
    private int ivx;    // NOPMD this is not a local variable for efficiency
    private double vy;
    private int ivy;    // NOPMD this is not a local variable for efficiency

    private int xMin;
    private int xMax;
    private int yMin;
    private int yMax;

    private int xGridStep;
    private int yGridStep;

    private final Deque<MapCoordinates> stack;

    /** Standard constructor
    */
    public MapCoordinates()
    {
        xCenter=0.0;
        yCenter=0.0;
        xMagnitude=1.0;
        yMagnitude=1.0;
        orientation=0;
        xGridStep=5;
        yGridStep=5;
        isMacro=false;
        snapActive=true;
        resetMinMax();
        stack = new ArrayDeque<MapCoordinates>();
    }

    /** Change the current orientation.
        @param o the wanted orientation (comprised between 0 and 3).
        NOTE: if o is greater than 3, it will be truncated to 3.
    */
    public void setOrientation(int o)
    {
        orientation=o;

        // Check for sanity
        if (orientation<0) {
            orientation=0;
        }

        if (orientation>3) {
            orientation=3;
        }
    }

    /** Get the current orientation.
        @return the current orientation.
    */
    public int getOrientation()
    {
        return orientation;
    }

    /** Get the current mirroring state
        @return the current mirroring state.
    */
    public boolean getMirror()
    {
        return mirror;
    }


    /** Save in a stack the current coordinate state.
    */
    public void push()
    {
        MapCoordinates m = new MapCoordinates();
        m.xCenter=xCenter;
        m.yCenter=yCenter;
        m.xMagnitude=xMagnitude;
        m.yMagnitude=yMagnitude;
        m.orientation=orientation;
        m.mirror=mirror;
        m.isMacro=isMacro;
        m.snapActive=snapActive;
        m.xMin=xMin;
        m.xMax=xMax;
        m.yMin=yMin;
        m.yMax=yMax;

        m.xGridStep=xGridStep;
        m.yGridStep=yGridStep;
        stack.addFirst(m);
    }

    /** Pop from a stack the coordinate state.
    */
    public void pop()
    {
        if(stack.isEmpty()) {
            System.out.println("Warning: I can not pop the coordinate state "+
                "out of an empty stack!");
        } else {
            MapCoordinates m=stack.removeFirst();
            xCenter=m.xCenter;
            yCenter=m.yCenter;
            xMagnitude=m.xMagnitude;
            yMagnitude=m.yMagnitude;
            orientation=m.orientation;
            mirror=m.mirror;
            isMacro=m.isMacro;
            snapActive=m.snapActive;
            xMin=m.xMin;
            xMax=m.xMax;
            yMin=m.yMin;
            yMax=m.yMax;

            xGridStep=m.xGridStep;
            yGridStep=m.yGridStep;
        }
    }

    /** Set the snapping state (used in the unmapping functions)
        @param s the wanted state.
    */
    public final void setSnap(boolean s)
    {
        snapActive=s;
    }

    /** Get the snapping state (used in the unmapping functions)
        @return the current snapping state.
    */
    public final boolean getSnap()
    {
        return snapActive;
    }

    /** Set the X grid step
        @param xg the X grid step
    */
    public final void setXGridStep(int xg)
    {
        if (xg>0) {
            xGridStep=xg;
        }
    }

    /** Set the Y grid step
        @param yg the Y grid step
    */
    public final void setYGridStep(int yg)
    {
        if (yg>0) {
            yGridStep=yg;
        }
    }

    /** Get the X grid step
        @return the X grid step used
    */
    public final int getXGridStep()
    {
        return xGridStep;
    }

    /** Get the Y grid step
        @return the Y grid step used
    */
    public final int getYGridStep()
    {
        return yGridStep;
    }

    /** Get the X magnification factor
        @return the X magnification factor
    */
    public final double getXMagnitude()
    {
        return xMagnitude;
    }

    /** Get the Y magnification factor
        @return the Y magnification factor
    */
    public final double getYMagnitude()
    {
        return yMagnitude;
    }

    /** Set the X magnification factor. The factor is checked and will always
        be comprised between limits defined by MIN_MAGNITUDE and MAX_MAGNITUDE.
        @param txm the X magnification factor.
    */
    public final void setXMagnitude(double txm)
    {
        double xm=txm;
        if (Math.abs(xm)<MIN_MAGNITUDE) {
            xm=MIN_MAGNITUDE;
        }

        if (Math.abs(xm)>MAX_MAGNITUDE) {
            xm=MAX_MAGNITUDE;
        }

        xMagnitude=xm;
    }

    /** Set the Y magnification factor.  The factor is checked and will always
        be comprised between limits defined by MIN_MAGNITUDE and MAX_MAGNITUDE.
        @param tym the Y magnification factor.
    */
    public final void setYMagnitude(double tym)
    {
        double ym=tym;
        if (Math.abs(ym)<MIN_MAGNITUDE) {
            ym=MIN_MAGNITUDE;
        }
        if (Math.abs(ym)>MAX_MAGNITUDE) {
            ym=MAX_MAGNITUDE;
        }

        yMagnitude=ym;
    }

    /** Set the X magnification factor. Does not check for limits of the
        magnification factor. Use with care!
        @param xm the X magnification factor.
    */
    public final void setXMagnitudeNoCheck(double xm)
    {
        xMagnitude=xm;
    }

    /** Set the Y magnification factor. Does not check for limits of the
        magnification factor. Use with care!
        @param ym the Y magnification factor.
    */
    public final void setYMagnitudeNoCheck(double ym)
    {
        yMagnitude=ym;
    }

    /** Get the X shift of the coordinate systems, in pixels
        @return the X shift in pixels
    */
    public final double getXCenter()
    {
        return xCenter;
    }

    /** Get the Y shift of the coordinate systems, in pixels
        @return the Y shift in pixels
    */
    public final double getYCenter()
    {
        return yCenter;
    }

    /** Set the X shift of the coordinate systems, in pixels
        @param xm the X shift in pixel
    */
    public final void setXCenter(double xm)
    {
        xCenter=xm;
    }

    /** Set the Y shift of the coordinate systems, in pixels
        @param ym the Y shift in pixels
    */
    public final void setYCenter(double ym)
    {
        yCenter=ym;
    }

    /** Set both X and Y magnification factors.
        @param xm the X magnification factor.
        @param ym the Y magnification factor.
    */
    public final void setMagnitudes(double xm, double ym)
    {
        setXMagnitude(xm);
        setYMagnitude(ym);
    }

    /** Set both X and Y magnification factors. Does not check for limits of
        the magnification factor. Use with care!
        @param xm the X magnification factor.
        @param ym the Y magnification factor.
    */
    public final void setMagnitudesNoCheck(double xm, double ym)
    {
        setXMagnitudeNoCheck(xm);
        setYMagnitudeNoCheck(ym);
    }

    /** Get the maximum tracked X coordinate
        @return the maximum tracked X coordinate
    */
    public final int getXMax()
    {
        return xMax;
    }

    /** Get the maximum tracked Y coordinate
        @return the maximum tracked Y coordinate
    */
    public final int getYMax()
    {
        return yMax;
    }

    /** Get the minimum tracked X coordinate
        @return the minimum tracked X coordinate
    */
    public final int getXMin()
    {
        return xMin;
    }

    /** Get the minimum tracked Y coordinate
        @return the minimum tracked Y coordinate
    */
    public final int getYMin()
    {
        return yMin;
    }

    /** Reset the minimum and maximum X/Y pixel coordinates tracked. */
    public final void resetMinMax()
    {
        xMin=yMin=Integer.MAX_VALUE;
        xMax=yMax=Integer.MIN_VALUE;
    }

    /** Map the xc,yc coordinate given in the X pixel coordinate. The tracking
        is active.
        @param xc the horizontal coordinate in the drawing coordinate system.
        @param yc the vertical coordinate in the drawing coordinate system.
        @return the X coordinates in pixels.
    */
    public final int mapX(double xc,double yc)
    {
        return mapXi(xc, yc, true);
    }

    /** Map the xc,yc coordinate given in the X pixel coordinate.
        @param xc the horizontal coordinate in the drawing coordinate system.
        @param yc the vertical coordinate in the drawing coordinate system.
        @param track specifies if the tracking should be active or not.
        @return the X coordinates in pixels.
    */
    public final int mapXi(double xc,double yc, boolean track)
    {
        ivx=(int)Math.round(mapXr(xc,yc));   /* The integer cast cuts decimals
            to the lowest integer. We need to round correctly; */

        if(track) {
            if(ivx<xMin) {
                xMin=ivx;
            }
            if(ivx>xMax) {
                xMax=ivx;
            }
        }

        return ivx;
    }

    /** Map the txc,tyc coordinate given in the X pixel coordinate. The results
        are given as double precision. Tracking is not active.
        @param txc the horizontal coordinate in the drawing coordinate system.
        @param tyc the vertical coordinate in the drawing coordinate system.
        @return the X coordinates in pixels.
    */
    public final double mapXr(double txc,double tyc)
    {
        double xc=txc;
        double yc=tyc;
        // The orientation data is not used outside a macro
        if(isMacro){
            xc-=100.0;
            yc-=100.0;

            if(mirror) {
                switch(orientation){
                    case 0:
                        vx=-xc*xMagnitude;
                        break;
                    case 1:
                        vx=yc*yMagnitude;
                        break;
                    case 2:
                        vx=xc*xMagnitude;
                        break;

                    case 3:
                        vx=-yc*yMagnitude;
                        break;

                    default:
                        vx=-xc*xMagnitude;
                        break;
                }
            } else {
                switch(orientation){
                    case 0:
                        vx=xc*xMagnitude;
                        break;
                    case 1:
                        vx=-yc*yMagnitude;
                        break;
                    case 2:
                        vx=-xc*xMagnitude;
                        break;
                    case 3:
                        vx=yc*yMagnitude;
                        break;
                    default:
                        vx=xc*xMagnitude;
                        break;
                }
            }
        } else {
            vx=xc*xMagnitude;
        }
        return vx+xCenter;
    }

    /** Map the xc,yc coordinate given in the Y pixel coordinate. The tracking
        is active.
        @param xc the horizontal coordinate in the drawing coordinate system.
        @param yc the vertical coordinate in the drawing coordinate system.
        @return the Y coordinates in pixels.
    */
    public final int mapY(double xc,double yc)
    {
        return mapYi(xc, yc, true);
    }

    /** Map the xc,yc coordinate given in the Y pixel coordinate.
        @param xc the horizontal coordinate in the drawing coordinate system.
        @param yc the vertical coordinate in the drawing coordinate system.
        @param track specify if the point should be tracked.
        @return the Y coordinates in pixels.
    */
    public final int mapYi(double xc,double yc, boolean track)
    {
        ivy=(int)Math.round(mapYr(xc,yc));   /* The integer cast cuts decimals
            to the lowest integer. We need to round correctly; */

        if(track) {
            if(ivy<yMin) {
                yMin=ivy;
            }

            if(ivy>yMax) {
                yMax=ivy;
            }
        }
        return ivy;
    }

    /** Map the xc,yc coordinate given in the Y pixel coordinate. The results
        are given as double precision. Tracking is not active.
        @param txc the horizontal coordinate in the drawing coordinate system.
        @param tyc the vertical coordinate in the drawing coordinate system.
        @return the Y coordinates in pixels.
    */
    public final double mapYr(double txc,double tyc)
    {
        double xc=txc;
        double yc=tyc;
        if(isMacro){
            xc-=100.0;
            yc-=100.0;

            switch(orientation){
                case 0:
                    vy=yc*yMagnitude;
                    break;
                case 1:
                    vy=xc*xMagnitude;
                    break;
                case 2:
                    vy=-yc*yMagnitude;
                    break;
                case 3:
                    vy=-xc*xMagnitude;
                    break;
                default:
                    vy=0.0;
                    break;
            }
        } else {
            vy=yc*yMagnitude;
        }

        return vy+yCenter;
    }

    /** Add a point in the min/max tracking system. The point should be
        specified in the SCREEN coordinates.
        @param xp the X coordinate of the point being tracked.
        @param yp the Y coordinate of the point being tracked.
    */
    public final void trackPoint(double xp, double yp)
    {
        if(yp<yMin) {
            yMin=(int)yp;
        }
        if(yp>yMax) {
            yMax=(int)yp;
        }
        if(xp<xMin) {
            xMin=(int)xp;
        }
        if(xp>xMax) {
            xMax=(int)xp;
        }
    }

    /** Un Map the X screen coordinate given in the drawing coordinate.
        If the snapping is active, it is NOT applied here.
        @param x the horizontal coordinate in the screen coordinate system
            (pixels).
        @return the X coordinates in logical units.
    */
    public float unmapXnosnap(float x)
    {
        return (float)((x-xCenter)/xMagnitude);
    }

    /** Un Map the Y screen coordinate given in the drawing coordinate.
        If the snapping is active, it is NOT applied here.
        @param y the horizontal coordinate in the screen coordinate system.
        @return the Y coordinates in logical units.
    */
    public float unmapYnosnap(float y)
    {
        return (float)((y-yCenter)/yMagnitude);
    }

    /** Un Map the X screen coordinate given in the drawing coordinate.
        If the snapping is active, it is applied here.
        @param x the horizontal coordinate in the screen coordinate system.
        @return the X coordinates in logical units.
    */
    public float unmapXsnap(float x)
    {
        float xc=unmapXnosnap(x);
        // perform the snapping.
        if(snapActive) {
            xc= (int)Math.round((double)xc/xGridStep);
            xc*=xGridStep;
        }
        return xc;
    }

    /** Un Map the Y screen coordinate given in the drawing coordinate.
        If the snapping is active, it is applied here.
        @param y the horizontal coordinate in the screen coordinate system
            (pixels).
        @return the Y coordinates in logical units.
    */
    public float unmapYsnap(float y)
    {
        float yc=unmapYnosnap(y);
        // perform the snapping.
        if(snapActive) {
            yc=(int)Math.round((double)yc/yGridStep);
            yc*=yGridStep;
        }
        return yc;
    }

    /** Create a string containing all possibly interesting info about the
        internal state of this class.
        @return a {@link String} describing the coordinates system, mainly
            for debugging purposes.
    */
    @Override public String toString()
    {
        String s="";
        s+="[xCenter="+ xCenter;
        s+="|yCenter="+ yCenter;
        s+="|xMagnitude="+xMagnitude;
        s+="|yMagnitude="+yMagnitude;
        s+="|orientation="+orientation;
        s+="|mirror="+ mirror;
        s+="|isMacro="+isMacro;
        s+="|snapActive="+snapActive;

        s+="|xMin="+ xMin;
        s+="|xMax="+xMax;
        s+="|yMin="+yMin;
        s+="|yMax="+yMax;

        s+="|xGridStep="+xGridStep;
        s+="|yGridStep="+ yGridStep+"]";

        return s;
    }
}