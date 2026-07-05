package fidocadj.primitives;

import java.io.*;
import java.util.*;

import fidocadj.dialogs.controls.ParameterDescription;
import fidocadj.dialogs.controls.LayerInfo;
import fidocadj.export.ExportInterface;
import fidocadj.geom.MapCoordinates;
import fidocadj.geom.GeometricDistances;
import fidocadj.globals.Globals;
import fidocadj.layers.LayerDesc;
import fidocadj.graphic.DecoratedText;
import fidocadj.graphic.PointG;
import fidocadj.graphic.DimensionG;
import fidocadj.graphic.GraphicsInterface;
import fidocadj.graphic.RectangleG;

/** GraphicPrimitive is an abstract class implementing the basic behaviour
    of a graphic primitive, which should be derived from it.

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

    Copyright 2008-2026 by Davide Bucci, phylum2
    </pre>
*/
public abstract class GraphicPrimitive
{
    // Tell that the dragging handle is invalid
    public static final int NO_DRAG=-1;
    // Tell that we are dragging the whole primitive
    public static final int DRAG_PRIMITIVE=-2;
    // Tell that we want to perform a selection in a rectangular area
    public static final int RECT_SELECTION=-3;

    // Indicates wether the primitive is selected or not
    private boolean selectedState;

    // Minimum width size of a line in pixel
    protected static final float D_MIN = 0.5f;

    // The layer
    private int layer;

    // The multiplication factor which is calculated for adjusting the screen
    // resolution, so that the size of handles or other important graphic stuff
    // is more or less the same.
    private float mult;

    // This is the screen resolution of the laptop on which I do most of the
    // development (DB), in dpi
    private static final int BASE_RESOLUTION=112;

    // Handle dimension. This is rescaled depending of the screen pixel
    // density. It is the size in pixel for a BASE_RESOLUTION dpi monitor.
    private static final int HANDLE_WIDTH=10;

    // Internal tolerance for snapping a double precision value to an integer.
    // Employed by roundIntelligently.
    private static final double INT_TOLERANCE=1E-5;

    // Array containing the points defining the primitive
    public PointG[] virtualPoint;

    // If changed is true, this means that the redraw operation should involve
    // an in-depth calculation of the primitive. Otherwise, a lot of
    // information is stored to speed up the redraw.
    protected boolean changed;

    private float macroFontSize;
    protected String macroFont;
    protected String name;
    protected String value;

    // Some caching data
    private LayerDesc currentLayer;
    private float alpha;
    private static float oldalpha=1.0f;
    private int old_layer=-1;

    // Those are data which are kept for the fast redraw of this primitive.
    // Basically, they are calculated once and then used as much as possible
    // without having to calculate everything from scratch.
    private float xa;
    private float ya;
    private float xb;
    private float yb;
    // Text sizes in pixels
    private float h;
    private float th;
    private float w1;
    private float w2;

    // Text sizes in logical units.
    private float t_th;
    private float t_w1;
    private float t_w2;
    private float x2;         // NOPMD
    private float y2;         // NOPMD
    private float x3;         // NOPMD
    private float y3;         // NOPMD

    /* At first, non abstract methods */

    /** Standard constructor.
        @param f the font to be employed for the associated text.
        @param size the size to be employed for the associated text.
    */
    public GraphicPrimitive(String f, int size)
    {
        selectedState=false;
        layer=0;
        changed=true;
        name = "";
        value = "";
        mult = 1.0f;
        setMacroFontSize(size);
        macroFont=f;
    }

    /** Standard constructor.
    */
    public GraphicPrimitive()
    {
        selectedState=false;
        layer=0;
        changed=true;
        name = "";
        value = "";
        mult = 1.0f;
        setMacroFontSize(4);
        macroFont="";
    }


    /**
     * @return the currentLayer
     */
    public LayerDesc getCurrentLayer()
    {
        return currentLayer;
    }

    /** Set the font to be used for name and value.
        @param f the font name.
        @param size the font size.
    */
    public void setMacroFont(String f, float size)
    {
        macroFont = f;
        setMacroFontSize(size);
        changed=true;
    }

    /** Prepare the array of points for storing the different virtual points
        needed by the primitive. This method also prepares the name and value
        strings, as well as the font to be used.
        @param number if number is negative, obtain the number of points by
            using getControlPointNumber(); if it is positive, use the number
            of points given for the size of the array.
        @param font the font to be employed for the associated text.
        @param size the size to be employed for the associated text.
    */
    public void initPrimitive(int number, String font, float size)
    {
        // Not very elegant. In fact, it would be better to use settings
        // present in DrawingModel, and not to have to use prefs here.

        setMacroFontSize(size);
        macroFont= font;
        name = "";
        value = "";
        int npoints=number;

        if (npoints<0) {
            npoints = getControlPointNumber();
        }

        virtualPoint = new PointG[npoints];
        for(int i=0;i<npoints;++i) {
            virtualPoint[i]=new PointG();
        }
    }

    /** Get the font used for name and value
        @return the font name
    */
    public String getMacroFont()
    {
        return macroFont;
    }

    /** Get the size of the macro font.
        @return the size of the macro font.
    */
    public float getMacroFontSize()
    {
        return macroFontSize;
    }

    /** Set the size of the macro font.
        @param size the size of the macro font.
    */
    public void setMacroFontSize(float size)
    {
        macroFontSize=size;
        // Silently correct a wrong size. This should never happen (the dialog
        // has a control, but avoids a wrong configuration to sneak somewhere
        // else.
        if(macroFontSize<=0) {
            macroFontSize=1;
        }
    }

    /** Check and correct if necessary the dashStyle number.
        @param dashStyle the style number to be checked.
        @return the checked dash style index.
    */
    public int checkDashStyle(int dashStyle)
    {
        if(dashStyle>=Globals.dashNumber) {
            return Globals.dashNumber-1;
        } else if(dashStyle<0) {
            return 0;
        }
        return dashStyle;
    }

    /** Writes the macro name and value fields. This method uses heavily the
        caching system implemented via the precalculation of the sizes and
        positions. This means that the "changed" flag is tested, BUT NOT
        UPDATED, since this method should be one of the first to be called
        when a primitive implements its drawing.
        The primitive will HAVE TO update the "changed" flag accordingly to
        its needs, BEFORE calling drawText.
        @param g the graphic context.
        @param coordSys the current coordinate mapping system.
        @param layerV the list containing the layers.
        @param drawOnlyLayer current layer which should be drawn (or -1).
    */
    protected void drawText(GraphicsInterface g, MapCoordinates coordSys,
                              List layerV, int drawOnlyLayer)
    {
        // If this method is not needed, exit immediately.
        if (value==null && name==null) {
            return;
        }

        if ("".equals(value) && "".equals(name)) {
            return;
        }

        if(drawOnlyLayer>=0 && drawOnlyLayer!=getLayer()) {
            return;
        }

        if(changed) {
            // Calculate the positions of the text lines
            x2=virtualPoint[getNameVirtualPointNumber()].x;
            y2=virtualPoint[getNameVirtualPointNumber()].y;
            x3=virtualPoint[getValueVirtualPointNumber()].x;
            y3=virtualPoint[getValueVirtualPointNumber()].y;

            xa=coordSys.mapX(x2,y2);
            ya=coordSys.mapY(x2,y2);
            xb=coordSys.mapX(x3,y3);
            yb=coordSys.mapY(x3,y3);

            // At first, write the name and the value fields in the given
            // positions

            g.setFont(macroFont,
                (int)(macroFontSize*12*coordSys.getYMagnitude()/7+.5));

            h = g.getFontAscent();
            th = h+g.getFontDescent();

            if(name==null) {
                w1=0;
            } else {
                w1 = g.getStringWidth(name);
            }

            if(value==null) {
                w2 = 0;
            } else {
                w2 = g.getStringWidth(value);
            }

            // Calculates the size of the text in logical units. This is
            // useful for calculating wether the user has clicked inside a
            // text line (see getDistanceToPoint)

            t_w1 = (int)(w1/coordSys.getXMagnitude());
            t_w2 = (int)(w2/coordSys.getXMagnitude());
            t_th = (int)(th/coordSys.getYMagnitude());

            // Track the points for calculating the drawing size

            coordSys.trackPoint(xa,ya);
            coordSys.trackPoint(xa+w1,ya+th);
            coordSys.trackPoint(xb,yb);
            coordSys.trackPoint(xb+w2, yb+th);
        }

        // If there is no need to draw the text, just exit.
        if(!g.hitClip(Math.round(xa),Math.round(ya),
                      Math.round(w1),Math.round(th)) &&
           !g.hitClip(Math.round(xb),Math.round(yb), 
                      Math.round(w2),
                      Math.round(th)))
        {
            return;
        }

        // This is useful and faster for small zooms
        if(th<Globals.textSizeLimit) {
            g.drawLine(Math.round(xa),Math.round(ya), 
                Math.round(xa+w1-1),Math.round(ya));
            g.drawLine(Math.round(xb),Math.round(yb),
                Math.round(xb+w2-1),Math.round(yb));
            return;
        }

        if(!changed) {
            g.setFont(macroFont,
                (int)(macroFontSize*12*coordSys.getYMagnitude()/7+.5));
        }

        DecoratedText dt=new DecoratedText(g.getTextInterface());
        /* The if's have been added thanks to this information:
         http://sourceforge.net/projects/fidocadj/forums/forum/997486
            /topic/3474689?message=7798139
        */
        if (name!=null && name.length()!=0) {
            dt.drawString(name,Math.round(xa),Math.round(ya+h));
        }
        if (value!=null && value.length()!=0) {
            dt.drawString(value,Math.round(xb),Math.round(yb+h));
        }
    }

    /** Creates the text strings containing the name and value of the
        primitive.
        @param extensions if true, outputs the FCJ tag before the two TY
            commands.
        @return a string containing the commands.
    */
    public String saveText(boolean extensions)
    {
        String subsFont;
        StringBuffer s2=new StringBuffer();

        // Check if the font is default and in this case, just put an asterisk.
        if (macroFont.equals(Globals.defaultTextFont)) {
            subsFont = "*";
        } else {
            StringBuffer s1=new StringBuffer("");

            for (int i=0; i<macroFont.length(); ++i) {
                if(macroFont.charAt(i)==' ') {
                    s1.append("++");
                } else {
                    s1.append(macroFont.charAt(i));
                }
            }
            subsFont=s1.toString();
        }

        // Write down the extensions only if needed
        if (name!=null && !"".equals(name) ||
            value!=null && !"".equals(value))
        {
            if(extensions) {
                s2.append("FCJ\n");
            }

            s2.append("TY ");
            s2.append(virtualPoint[getNameVirtualPointNumber()].x);
            s2.append(" ");
            s2.append(virtualPoint[getNameVirtualPointNumber()].y);
            s2.append(" ");
            s2.append(macroFontSize*4/3);
            s2.append(" ");
            s2.append(macroFontSize);
            s2.append(" 0 0 ");
            s2.append(getLayer());
            s2.append(" ");
            s2.append(subsFont);
            s2.append(" ");
            s2.append(name==null?"":name);
            s2.append("\n");

            s2.append("TY ");
            s2.append(virtualPoint[getValueVirtualPointNumber()].x);
            s2.append(" ");
            s2.append(virtualPoint[getValueVirtualPointNumber()].y);
            s2.append(" ");
            s2.append(macroFontSize*4/3);
            s2.append(" ");
            s2.append(macroFontSize);
            s2.append(" 0 0 ");
            s2.append(getLayer());
            s2.append(" ");
            s2.append(subsFont);
            s2.append(" ");
            s2.append(value==null?"":value);
            s2.append("\n");
        }
        return s2.toString();
    }

    /** Export the name and the value text lines associated to the primitive.
        This is done rather automatically by exploiting the export of the
        advanced text feature. It should be noted that the export is done only
        if necessary.
        @param exp the ExportInterface to be used.
        @param cs the coordinate mapping system to be used.
        @param drawOnlyLayer the layer to be drawn (or -1).
        @throws IOException if something wrong happens during the export, such
            as it is, or becomes impossible to write on the output file.
    */
    public void exportText(ExportInterface exp, MapCoordinates cs,
        int drawOnlyLayer)
        throws IOException
    {
        double size=
            Math.abs(cs.mapXr(macroFontSize,macroFontSize)-cs.mapXr(0,0));

        // Export the text associated to the name and value of the macro
        if(drawOnlyLayer<0 || drawOnlyLayer==getLayer()) {
            if(!"".equals(name)) {
                exp.exportAdvText (cs.mapX(
                    virtualPoint[getNameVirtualPointNumber()].x,
                    virtualPoint[getNameVirtualPointNumber()].y),
                    cs.mapY(virtualPoint[getNameVirtualPointNumber()].x,
                    virtualPoint[getNameVirtualPointNumber()].y),
                    size,
                    size*12.0/7.0,
                    macroFont,
                    false,
                    false,
                    false,
                    0, getLayer(), name);
            }

            if(!"".equals(value)) {
                exp.exportAdvText (cs.mapX(
                    virtualPoint[getValueVirtualPointNumber()].x,
                    virtualPoint[getValueVirtualPointNumber()].y),
                    cs.mapY(
                    virtualPoint[getValueVirtualPointNumber()].x,
                    virtualPoint[getValueVirtualPointNumber()].y),
                    size,
                    size*12.0/7.0,
                    macroFont,
                    false,
                    false,
                    false,
                    0, getLayer(), value);
            }
        }
    }

    /** Check if the given point (in logical units) lies inside one of the
        two text lines associated to the primitive.

        @param px the x coordinates of the given point.
        @param py the y coordinates of the given point.
        @return true if the point is inside one of the two text lines.
    */
    public boolean checkText(int px, int py)
    {
        return !"".equals(name) && GeometricDistances.pointInRectangle(
            virtualPoint[getNameVirtualPointNumber()].x,
            virtualPoint[getNameVirtualPointNumber()].y,t_w1,t_th,px,py) ||
            !"".equals(value) && GeometricDistances.pointInRectangle(
            virtualPoint[getValueVirtualPointNumber()].x,
            virtualPoint[getValueVirtualPointNumber()].y,t_w2,t_th,px,py);
    }

    /** Reads the TY line describing the "value" field
        @param tokens the array of tokens to be parsed
        @param nn the number of tokens to be parsed.
        @throws IOException if something goes wrong, for example there is
            an invalid primitive found at an incongruous place (probably a
            programming error).
    */
    public void setValue(String[] tokens, int nn)
        throws IOException
    {
        StringBuffer txtb=new StringBuffer();
        int j=8;
        changed=true;
        if ("TY".equals(tokens[0])) {   // Text (advanced)
            if (nn<9) {
                throw new IOException("Bad arguments on TY");
            }

            virtualPoint[getValueVirtualPointNumber()].x=
                Float.parseFloat(tokens[1]);
            virtualPoint[getValueVirtualPointNumber()].y=
                Float.parseFloat(tokens[2]);

            if("*".equals(tokens[8])) {
                macroFont = Globals.defaultTextFont;
            } else {
                macroFont = tokens[8].replaceAll("\\+\\+"," ");
            }

            // Adding the following line should fix bug #3522962
            setMacroFontSize(Float.parseFloat(tokens[4]));

            while(j<nn-1){
                txtb.append(tokens[++j]);
                if (j<nn-1) {
                    txtb.append(" ");
                }
            }
            value=txtb.toString();
        } else {
            throw new IOException("Invalid primitive: "+tokens[0]+
                                          " programming error?");
        }
    }

    /** Reads the TY line describing the "name" field
        @param tokens the array of tokens to be parsed
        @param nn the number of tokens to be parsed.
        @throws IOException if something goes wrong, for example there is
            an invalid primitive found at an incongruous place (probably a
            programming error).
    */
    public void setName(String[] tokens, int nn)
        throws IOException
    {
        StringBuffer txtb=new StringBuffer();
        int j=8;
        changed=true;
        if ("TY".equals(tokens[0])) {   // Text (advanced)
            if (nn<9) {
                throw new IOException("bad arguments on TY");
            }

            virtualPoint[getNameVirtualPointNumber()].x=
                Float.parseFloat(tokens[1]);
            virtualPoint[getNameVirtualPointNumber()].y=
                Float.parseFloat(tokens[2]);

            while(j<nn-1) {
                txtb.append(tokens[++j]);
                if (j<nn-1) {
                    txtb.append(" ");
                }
            }
            name=txtb.toString();

        } else {
            throw new IOException("Invalid primitive:"+tokens[0]+
                                          " programming error?");
        }
    }

    /** Specifies that the current primitive has been modified or not.
        If it is true, during the redraw all parameters should be calulated
        from scratch.
        @param c the wanted changed state.
    */
    public void setChanged(boolean c)
    {
        changed=c;
    }

    /** Get the first control point of the primitive
        @return the coordinates of the first control point of the object.
    */
    public PointG getFirstPoint()
    {
        return virtualPoint[0];
    }

    /** Move the primitive.

        @param dx the relative x displacement (logical units)
        @param dy the relative y displacement (logical units)

    */
    public void movePrimitive(float dx, float dy)
    {
        // Check if the move would result in any virtual point having negative
        // coordinates
        for (int a = 0; a < getControlPointNumber(); ++a) {
            float newX = virtualPoint[a].x + dx;
            float newY = virtualPoint[a].y + dy;

            // If any new coordinate is negative, abort the move
            if (newX < 0 || newY < 0) {
                return; // Aborts the move
            }
        }

        // If all checks are passed, perform the move
        for (int a = 0; a < getControlPointNumber(); ++a) {
            virtualPoint[a].x += dx;
            virtualPoint[a].y += dy;
        }

        changed = true;
    }

    /** Mirror the primitive. Adapted from Lorenzo Lutti's original code.
        @param xPos is the symmetry axis

    */
    public void mirrorPrimitive(float xPos)
    {
        float xtmp;

        for(int a=0; a<getControlPointNumber(); ++a) {
            xtmp = virtualPoint[a].x;
            virtualPoint[a].x = 2*xPos - xtmp;
        }
        changed=true;
    }

    /** Rotate the primitive. Adapted from Lorenzo Lutti's original code.
        @param bCounterClockWise specify if the rotation should be done
                counterclockwise.

        @param ix the x coordinate of the center of rotation
        @param iy the y coordinate of the center of rotation
    */
    public void rotatePrimitive(boolean bCounterClockWise, float ix, float iy)
    {

        PointG ptTmp=new PointG();
        PointG pt=new PointG();

        pt.x=ix;
        pt.y=iy;

        for(int b=0; b<getControlPointNumber(); ++b) {
            ptTmp.x = virtualPoint[b].x;
            ptTmp.y = virtualPoint[b].y;

            if(bCounterClockWise) {
                virtualPoint[b].x = pt.x + ptTmp.y-pt.y;
                virtualPoint[b].y = pt.y - (ptTmp.x-pt.x); // NOPMD
            } else {
                virtualPoint[b].x = pt.x - (ptTmp.y-pt.y); // NOPMD
                virtualPoint[b].y = pt.y + ptTmp.x-pt.x;
            }
        }
        changed=true;
    }

    /** Specifies that only the given layer should be drawn.
        This is in practice useful only for macros, since they have an
        internal layer structure.
        @param i the layer to be used.
    */
    public void setDrawOnlyLayer (int i)
    {
        // Normally, this does nothing, except for macros.
    }

    /** Returns true if the primitive contains the specified layer.
        @param l the index of the layer to check.
        @return true or false, if the specified layer is contained in the
            primitive.
    */
    public boolean containsLayer(int l)
    {
        return l==layer;
    }

    /** Obtains the maximum layer which is contained by this primitive. It
        should redefined for macros, since they can contain more than one
        layer. The standard implementation returns the layer of the
        primitive, since this is the only one which is used.
        @return the maximum value of the layer contained in the primitive.
    */
    public int getMaxLayer()
    {
        return layer;
    }

    /** Set the primitive as selected.
        @param s the new state.
    */
    final public void setSelected(boolean s)
    {
        selectedState=s;
    }

    /** Get the selection state of the primitive.
        @return true if the primitive is selected, false otherwise.
    */
    final public boolean isSelected()
    {
        return selectedState;
    }

    /** Get the layer of the current primitive.
        @return the layer number.
    */
    public final int getLayer()
    {
        return layer;
    }

    /** Parse the current string and interpret it as a layer indication.
        If this is correct, the layer is saved in the current primitive.
        @param token the token which corresponds to the layer.
    */
    public void parseLayer(String token)
    {
        int l;
        try {
            l=Integer.parseInt(token);

        } catch (NumberFormatException e) {
            // We are unable to get the layer. Just suppose it's zero.
            l=0;
        }

        // We do check if everything is OK.
        if (l<0 || l>=LayerDesc.MAX_LAYERS) {
            layer=0;
        } else {
            layer=l;
        }
        changed=true;
    }

    /** Set the layer of the current primitive. A quick check is done.
        @param l the desired layer.
    */
    final public void setLayer(int l)
    {
        if (l<0 || l>=LayerDesc.MAX_LAYERS) {
            layer=0;
        } else {
            layer=l;
        }
        changed=true;
    }

    /** Treat the current layer. In particular, select the corresponding
        color in the actual graphic context. If the primitive is selected,
        select the corrisponding color. This is a speed sensitive context.
        @param g the graphic context used for the drawing.
        @param layerV a LayerDesc vector with the descriptions of the layers
                being used.
        @return true if the layer is visible, false otherwise.
    */
    protected final boolean selectLayer(GraphicsInterface g, List layerV)
    {
        // At first, we see if we need to retrieve the current layer.
        // It is important to check also the changed flag, since if not we
        // would now show changes apported to the layer being drawn when it is
        // modified.

        if(old_layer != layer || changed) {
            if(layer>=layerV.size()) {
                layer=layerV.size()-1;
            }
            currentLayer= (LayerDesc)layerV.get(layer);
            old_layer = layer;
        }

        // If the layer is not visible, we just exit, returning false. This
        // will made the caller not to draw the graphical element.

        if (!currentLayer.isVisible()) {
            return false;
        }

        if(selectedState) {
            // We change the color for selected objects
            g.activateSelectColor(getCurrentLayer());
        } else {
            if(g.getColor()!=getCurrentLayer().getColor() || oldalpha!=alpha) {
                g.setColor(getCurrentLayer().getColor());
                alpha=getCurrentLayer().getAlpha();
                oldalpha = alpha;
                g.setAlpha(alpha);
            }
        }
        return true;
    }

    /** Draw the handles for the current primitive.
        @param g the graphic context to be used.
        @param cs the coordinate mapping used.
    */
    public void drawHandles(GraphicsInterface g, MapCoordinates cs)
    {
        int xa;
        int ya;

        g.setColor(g.getColor().red());
        g.applyStroke(2.0f,0);

        // Calculation of the reasonable multiplication factor.
        mult=g.getScreenDensity()/BASE_RESOLUTION;

        int sizeX=Math.round(mult*HANDLE_WIDTH);
        int sizeY=Math.round(mult*HANDLE_WIDTH);

        for(int i=0;i<getControlPointNumber();++i) {
            if (!testIfValidHandle(i)) {
                continue;
            }

            xa=cs.mapX(virtualPoint[i].x,virtualPoint[i].y);
            ya=cs.mapY(virtualPoint[i].x,virtualPoint[i].y);

            if(!g.hitClip(xa-sizeX/2,ya-sizeY/2, sizeX,sizeY)) {
                continue;
            }

            // A handle is a small red rectangle
            g.fillRect(xa-sizeX/2,ya-sizeY/2, sizeX,sizeY);
        }
    }

    /** Tells if the pointer is on an handle. The handles associated to the
        name and value strings are not considered if they are not defined.

        @param cs the coordinate mapping used.
        @param px the x (screen) coordinate of the pointer.
        @param py the y (screen) coordinate of the pointer.
        @return NO_DRAG if the pointer is not on an handle, or the index of the
            selected handle.
    */
    public int onHandle(MapCoordinates cs, int px, int py)
    {
        int xa;
        int ya;

        int increase = 5;
        int hw2=Math.round(mult*HANDLE_WIDTH/2);
        int hl2=Math.round(mult*HANDLE_WIDTH/2);

        for(int i=0;i<getControlPointNumber();++i) {
            if (!testIfValidHandle(i)) {
                continue;
            }

            xa=cs.mapX(virtualPoint[i].x,virtualPoint[i].y);
            ya=cs.mapY(virtualPoint[i].x,virtualPoint[i].y);

            // Recognize if we have clicked on a handle. Basically, we check
            // if the point lies inside the rectangle given by the handle.

            if(GeometricDistances.pointInRectangle(
                xa-hw2-Math.round(mult*increase),
                ya-hl2-Math.round(mult*increase),
                Math.round(mult*(HANDLE_WIDTH+2*increase)),
                Math.round(mult*(HANDLE_WIDTH+2*increase)),
                px,py))
            {
                return i;
            }
        }
        return NO_DRAG;
    }

    /** Select the primitive if one of its virtual point is in the specified
        rectangular region (given in logical coordinates).
        @param px the x coordinate of the top left point.
        @param py the y coordinate of the top left point.
        @param w the width of the region
        @param h the height of the region
        @return true if at least a primitive has been selected
    */
    public boolean selectRect(int px, int py, int w, int h)
    {
        float xa;
        float ya;

        for(int i=0;i<getControlPointNumber();++i) {
            if (!testIfValidHandle(i)) {
                continue;
            }

            xa=virtualPoint[i].x;
            ya=virtualPoint[i].y;

            if(px<=xa && xa<px+w && py<=ya&& ya< py+h) {
                setSelected(true);
                return true;
            }
        }
        return false;
    }

    /** Function to determine if the name field is set
        @return true if the name field is set
    */
    public boolean hasName()
    {
        return name!=null && name.length()!=0;
    }

    /** Function to determine if the value field is set
        @return true if the value field is set
    */
    public boolean hasValue()
    {
        return value!=null && value.length()!=0;
    }


    /** Determines whether the handle specified is valid or is disabled.
        Are disabled in particular the handles associated to the name and
        value strings when they are not defined.
        @return true if the handle is active
    */
    protected boolean testIfValidHandle(int i)
    {
        if (i==getNameVirtualPointNumber()) {
            if (name==null) {
                return false;
            }
            if(name.length()==0) {
                return false;
            }
        }
        if (i==getValueVirtualPointNumber()) {
            if(value==null) {
                return false;
            }
            if(value.length()==0) {
                return false;
            }
        }
        return true;
    }

    /** Get the control parameters of the given primitive. Each
        primitive should probably overload this version. We give here a very
        general implementation, allowing to change only virtual points.

        @return a vector of ParameterDescription containing each control
                parameter.
                The first parameters should always be the name and the
                value fields, followed by the layer.
    */
    public List<ParameterDescription> getControls()
    {
        List<ParameterDescription> v = new Vector<ParameterDescription>(10);
        ParameterDescription pd = new ParameterDescription();

        pd.parameter=(name==null?"":name);
        pd.description=Globals.messages.getString("ctrl_name");
        pd.isExtension = true;
        v.add(pd);

        pd = new ParameterDescription();

        pd.parameter=(value==null?"":value);
        pd.description=Globals.messages.getString("ctrl_value");
        pd.isExtension = true;

        v.add(pd);

        pd = new ParameterDescription();
        pd.parameter=new LayerInfo(layer);
        pd.description=Globals.messages.getString("ctrl_layer");
        v.add(pd);

        return v;
    }

    /** Set the control parameters of the given primitive. Each
        primitive should probably overload this version. We give here a very
        general implementation, allowing to change only virtual points.
        This method is specular to getControls().
        @param v a vector of ParameterDescription containing each control
            parameter. The first parameters should always be the virtual
            points.
        @return the index of the next parameter which remains to be read
            after this function ends.

    */
    public int setControls(List<ParameterDescription> v)
    {
        int i=0;
        ParameterDescription pd;
        changed=true;

        pd=v.get(i);
        ++i;
        // Check, just for sure...
        if (pd.parameter instanceof String) {
            name=((String)pd.parameter);
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }

        pd=v.get(i);
        ++i;
        // Check, just for sure...
        if (pd.parameter instanceof String) {
            value=((String)pd.parameter);
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }

        pd = v.get(i);
        // Check, just for sure...
        if (pd.parameter instanceof LayerInfo) {
            layer=((LayerInfo)pd.parameter).getLayer();
        } else {
            System.out.println("Warning: unexpected parameter! (layer)");
        }

        return ++i;
    }

    /** This function should be redefined if the graphic primitive needs holes.
        This implies that the redraw strategy should include a final pass
        to be sure that the holes are drawn correctly.
        Override this function if the primitive needs holes. The standard
        implementation just returns false.
        @return true if there are elements in the drawing which need holes.
    */
    public boolean needsHoles()
    {
        return false;
    }

    /** Specify whether during the drawing phase the primitive should draw
        only the pads. This is useful only for the PrimitiveMacro and
        PrimitivePCBPad subclasses.
        @param t the wanted state.
    */
    public void setDrawOnlyPads(boolean t)
    {
        // Does nothing, except for macros and pcbpads.
    }

    /** Draw the graphic primitive on the given graphic context.
        @param g the graphic context in which the primitive should be drawn.
        @param coordSys the graphic coordinates system to be applied.
        @param layerDesc the layer description.
    */
    public abstract void draw(GraphicsInterface g, MapCoordinates coordSys,
                              List layerDesc);

    /** Parse a token array and store the graphic data for a given primitive
        Obviously, that routine should be called *after* having recognized
        that the called primitive is correct.
        That routine also sets the correct layer.
        An IOException is thrown if there is an error.

        @param tokens the tokens to be processed. tokens[0] should be the
        command of the actual primitive.
        @param nn the number of tokens present in the array.
        @throws IOException if something goes wrong.
    */
    public abstract void parseTokens(String[] tokens, int nn)
        throws IOException;

    /** Gets the distance (in primitive's coordinates space) between a
        given point and the primitive.
        When it is reasonable, the behaviour can be binary (polygons,
        ovals...). In other cases (lines, points), it can be proportional.
        @param px the x coordinate of the given point.
        @param py the y coordinate of the given point.
        @return the distance to point in logical coordinates.
    */
    public abstract int getDistanceToPoint(float px, float py);

    /** Gets the number of control points used.
        @return the number of points used by the primitive
    */
    public abstract int getControlPointNumber();

    /** Obtain a string command descripion of the primitive.
        @param extensions produce a string eventually containing FidoCadJ
            extensions over the original FidoCad format.
        @return the FIDOCAD command line.
    */
    public abstract String toString(boolean extensions);

    /** Each graphic primitive should call the appropriate exporting method
        of the export interface specified.
        @param exp the export interface that should be used.
        @param cs the actual coordinate mapping.
        @throws IOException if an error occurs, for example because it becomes
            impossible to access to the files being written.
    */
    public abstract void export(ExportInterface exp, MapCoordinates cs)
        throws IOException;

    /** Get the number of the virtual point associated to the Name property
        @return the number of the virtual point associated to the Name property
    */
    public abstract int getNameVirtualPointNumber();

    /** Get the number of the virtual point associated to the Value property.
        @return the number of the virtual point associated to the Value
            property.
    */
    public abstract int getValueVirtualPointNumber();

    /** Get the size of the current element.
        @return the size.
    */
    public DimensionG getSize()
    {
        GraphicPrimitive p = this;
        float qx = 0;
        float qy = 0;
        for (int i = 0; i < p.getControlPointNumber(); i++) {
            if (i == p.getNameVirtualPointNumber()
                    || i == p.getValueVirtualPointNumber())
            {
                continue;
            }
            for (int j = i + 1; j < p.getControlPointNumber(); j++) {
                if (j == p.getNameVirtualPointNumber()
                        || j == p.getValueVirtualPointNumber())
                {
                    continue;
                }
                qx = Math.abs(p.virtualPoint[i].x - p.virtualPoint[j].x);
                qy = Math.abs(p.virtualPoint[i].y - p.virtualPoint[j].y);
            }
        }
        return new DimensionG(Math.round(qx),Math.round(qy));
    }

    /** Get the minimum x and y values of all control points of the element.
        @return the minimum x and y coordinates.
    */
    public PointG getPosition()
    {
        GraphicPrimitive p = this;
        float qx = Float.MAX_VALUE;
        float qy = Float.MAX_VALUE;
        for (int i = 0; i < p.getControlPointNumber(); i++) {
            if (i == p.getNameVirtualPointNumber()
                    || i == p.getValueVirtualPointNumber())
            {
                continue;
            }
            if (p.virtualPoint[i].x<qx) {
                qx = p.virtualPoint[i].x;
            }
            if (p.virtualPoint[i].y<qy) {
                qy = p.virtualPoint[i].y;
            }
        }
        return new PointG(qx,qy);
    }

    /** Check wether we are very close to an integer value. In this case,
        the output will be done as an integer. This improves backward
        compatibility in cases where the fractional part is not needed.
        The output code is also marginally more compact.
        The rounding is done on two digits after the decimal point.

        For example, roundIntelligently(1.00) produces "1" whereas
        roundIntelligently(1.23) produces "1.23".
        @param v the value to be rounded.
        @return a string containing the rounded value.
    */
    public StringBuffer roundIntelligently(double v)
    {
        StringBuffer sb;
        if(Math.abs(v-Math.round(v))<INT_TOLERANCE) {
            int w=(int)Math.round(v);
            sb = new StringBuffer(""+w);
        } else {
            long w=Math.round(v*100);
            sb = new StringBuffer(""+((float)w)/100);
        }
        return sb;
    }

    /**
     * Determines if the virtual points are fully contained within the specified
     * rectangle based on the selection direction.
     *
     * @param rect                   the selection rectangle.
     *
     * @return true if all points are within the rectangle for left-to-right
     *         selection, or if any point is within the rectangle for
     *         right-to-left selection.
     */
    public boolean isFullyContained(RectangleG rect)
    {
        // Check if all points are fully contained within the rectangle
        for (PointG point : virtualPoint) {
            if (!rect.contains(Math.round(point.x), Math.round(point.y))) {
                return false; // At least one point is outside the rectangle
            }
        }
        return true; // All points are contained
    }


    /**
     * Determines whether the primitive is contained within or intersects with
     * the specified rectangle, depending on the selection direction.
     *
     * @param rect                   the Rectangle object to check for
     *                               containment or intersection.
     * @param isLeftToRightSelection Determine the direction of the selection.
     *
     * @return true if the primitive should be selected based on the selection
     *         criteria, false otherwise.
     */
    public boolean intersects(RectangleG rect, boolean isLeftToRightSelection)
    {
        if (isLeftToRightSelection)
            return isFullyContained(rect);

        for (PointG point : virtualPoint) {
            if (rect.contains(Math.round(point.x), Math.round(point.y))) {
                return true;
            }
        }
        return false;
    }
}
