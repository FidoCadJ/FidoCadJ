package fidocadj.primitives;

import java.io.*;
import java.util.*;

import fidocadj.dialogs.controls.ParameterDescription;
import fidocadj.export.ExportInterface;
import fidocadj.geom.GeometricDistances;
import fidocadj.geom.MapCoordinates;
import fidocadj.globals.Globals;
import fidocadj.graphic.GraphicsInterface;
import fidocadj.graphic.ImageInterface;

/** Class to handle the embedded raster image primitive ("IM" command).

    Unlike the "attach a background image" feature (a single, non-persisted,
    non-selectable image kept by {@link fidocadj.circuit.ImageAsCanvas}),
    this primitive is a first-class element of the drawing: it is stored in
    the drawing's primitive list, selectable/movable/resizable exactly like
    a rectangle, and its (base64-encoded) pixel data is embedded directly in
    the FidoCadJ text file so the drawing is fully self-contained.

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

    @author Manuel Finessi

    Copyright 2026 by the FidoCadJ team
    </pre>
*/
public final class PrimitiveImage extends GraphicPrimitive
{
    // An image is defined by two opposite corners, plus name/value points.
    static final int N_POINTS=4;

    // This is the value which is given for the distance calculation when the
    // user clicks inside the image (an image is always treated as a filled,
    // opaque rectangle for hit-testing purposes):
    static final int DISTANCE_IN = 1;
    static final int DISTANCE_OUT = 1000;

    // The image data, exactly as stored in the .fcd file.
    private String mimeType;
    private String base64Data;

    // Per-instance display properties.
    private float opacity;
    private boolean blackAndWhite;

    // Decoded image cache: rebuilt only when base64Data actually changes
    // (via parseTokens or the full constructor), never on a mere move/
    // resize (which only touches the "changed" flag used below for the
    // screen-coordinate cache).
    private boolean imageDecoded;
    private ImageInterface colorImage;
    private ImageInterface grayImage;

    // Those are data which are kept for the fast redraw of this primitive.
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
    public PrimitiveImage(String f, float size)
    {
        super();
        opacity = 1.0f;
        blackAndWhite = false;
        mimeType = "png";
        base64Data = "";
        initPrimitive(-1, f, size);

        changed=true;
    }

    /** Create an image defined by its bounding box.
        @param x1 the start x coordinate (logical unit).
        @param y1 the start y coordinate (logical unit).
        @param x2 the end x coordinate (logical unit).
        @param y2 the end y coordinate (logical unit).
        @param mime the mime subtype of the original file (e.g. "png").
        @param data the base64-encoded bytes of the original file.
        @param op the opacity, between 0.0 (transparent) and 1.0 (opaque).
        @param bw true if the image should be displayed in black and white.
        @param layer the layer to be used.
        @param font the name of the font for attached text.
        @param size the size of the font for attached text.
    */
    public PrimitiveImage(float x1, float y1, float x2, float y2,
        String mime, String data, float op, boolean bw, int layer,
        String font, float size)
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

        mimeType=mime;
        base64Data=data;
        opacity=op;
        blackAndWhite=bw;
        changed = true;

        setLayer(layer);
    }

    /** Lazily decode the base64 image data (once) into the color and,
        if needed, grayscale image caches.
        @param g the graphic context, used to decode the image through the
            platform-independent graphic abstraction.
    */
    private void ensureImageDecoded(GraphicsInterface g)
    {
        if (imageDecoded) {
            return;
        }
        imageDecoded = true;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Data);
            colorImage = g.createImage(bytes);
        } catch (Exception e) {
            // A corrupted/unreadable image should not prevent the rest of
            // the drawing from being usable: just skip drawing this one.
            colorImage = null;
        }
        grayImage = colorImage==null ? null : colorImage.toGrayscale();
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
            x1=coordSys.mapX(virtualPoint[0].x,virtualPoint[0].y);
            y1=coordSys.mapY(virtualPoint[0].x,virtualPoint[0].y);
            x2=coordSys.mapX(virtualPoint[1].x,virtualPoint[1].y);
            y2=coordSys.mapY(virtualPoint[1].x,virtualPoint[1].y);

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

            width = xb-xa;
            height = yb-ya;
        }

        if(!g.hitClip(xa,ya, width+1,height+1)) {
            return;
        }

        ensureImageDecoded(g);
        ImageInterface img = blackAndWhite ? grayImage : colorImage;
        if (img!=null) {
            g.setAlpha(opacity);
            g.drawImage(img, xa, ya, width+1, height+1);
            // Restore the alpha value selectLayer() applied, so that the
            // caching it relies on (to avoid redundant setAlpha calls on
            // the next primitive) is not left in a stale state.
            g.setAlpha(getCurrentLayer().getAlpha());
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
        if (!"IM".equals(tokens[0])) {
            throw new IOException("IM: Invalid primitive: "
                +tokens[0]+" programming error?");
        }
        if (nn<10) {
            throw new IOException("Bad arguments on IM");
        }
        float xp1 = virtualPoint[0].x=Float.parseFloat(tokens[1]);
        float yp1 = virtualPoint[0].y=Float.parseFloat(tokens[2]);
        virtualPoint[1].x=Float.parseFloat(tokens[3]);
        virtualPoint[1].y=Float.parseFloat(tokens[4]);

        virtualPoint[getNameVirtualPointNumber()].x=xp1+5;
        virtualPoint[getNameVirtualPointNumber()].y=yp1+5;
        virtualPoint[getValueVirtualPointNumber()].x=xp1+5;
        virtualPoint[getValueVirtualPointNumber()].y=yp1+10;

        opacity=Float.parseFloat(tokens[5]);
        blackAndWhite="1".equals(tokens[6]);
        parseLayer(tokens[7]);
        mimeType=tokens[8];
        base64Data=tokens[9];

        imageDecoded=false;
        colorImage=null;
        grayImage=null;
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

        pd.parameter=Float.valueOf(opacity);
        pd.description=Globals.messages.getString("ctrl_opacity");
        v.add(pd);

        pd = new ParameterDescription();
        pd.parameter=Boolean.valueOf(blackAndWhite);
        pd.description=Globals.messages.getString("ctrl_black_and_white");
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
        if (pd.parameter instanceof Float) {
            opacity=((Float)pd.parameter).floatValue();
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }

        pd=v.get(i);
        ++i;
        if (pd.parameter instanceof Boolean) {
            blackAndWhite=((Boolean)pd.parameter).booleanValue();
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }

        // Parameters validation and correction
        if (opacity<0.0f) {
            opacity=0.0f;
        }
        if (opacity>1.0f) {
            opacity=1.0f;
        }

        return i;
    }

    /** Gets the distance (in primitive's coordinates space) between a
        given point and the primitive. An image is always treated as an
        opaque, filled rectangle (binary hit-test).
        @param px the x coordinate of the given point.
        @param py the y coordinate of the given point.
        @return the distance in logical units.
    */
    public int getDistanceToPoint(float px, float py)
    {
        if(checkText(Math.round(px), Math.round(py))) {
            return 0;
        }
        float xa=Math.min(virtualPoint[0].x,virtualPoint[1].x);
        float ya=Math.min(virtualPoint[0].y,virtualPoint[1].y);
        float xb=Math.max(virtualPoint[0].x,virtualPoint[1].x);
        float yb=Math.max(virtualPoint[0].y,virtualPoint[1].y);

        if(GeometricDistances.pointInRectangle(xa,ya, xb-xa, yb-ya,px,py)) {
            return DISTANCE_IN;
        }
        return DISTANCE_OUT;
    }

    /** Obtain a string command descripion of the primitive.
        @param extensions true if FidoCadJ extensions to the old FidoCad
            format should be active (required for this primitive, which
            does not exist in the original FidoCad format at all).
        @return the FIDOCAD command line.
    */
    public String toString(boolean extensions)
    {
        String cmd="IM "
            +roundIntelligently(virtualPoint[0].x)+" "
            +roundIntelligently(virtualPoint[0].y)+" "
            +roundIntelligently(virtualPoint[1].x)+" "
            +roundIntelligently(virtualPoint[1].y)+" "
            +opacity+" "
            +(blackAndWhite?"1":"0")+" "
            +getLayer()+" "
            +mimeType+" "
            +base64Data+"\n";

        cmd+=saveText(extensions);
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
        exp.exportImage(cs.mapX(virtualPoint[0].x,virtualPoint[0].y),
                       cs.mapY(virtualPoint[0].x,virtualPoint[0].y),
                       cs.mapX(virtualPoint[1].x,virtualPoint[1].y),
                       cs.mapY(virtualPoint[1].x,virtualPoint[1].y),
                       getLayer(),
                       opacity,
                       blackAndWhite,
                       mimeType,
                       base64Data);
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
}
