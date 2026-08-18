package fidocadj.primitives;

import java.io.*;
import java.util.*;

import fidocadj.dialogs.controls.ParameterDescription;
import fidocadj.dialogs.controls.LayerInfo;
import fidocadj.export.ExportInterface;
import fidocadj.geom.MapCoordinates;
import fidocadj.geom.GeometricDistances;
import fidocadj.globals.Globals;
import fidocadj.graphic.DecoratedText;
import fidocadj.graphic.GraphicsInterface;
import fidocadj.graphic.PointG;
import fidocadj.graphic.FontG;
import fidocadj.graphic.RectangleG;
import fidocadj.graphic.nil.GraphicsNull;

/**
 * Class to handle the advanced text primitive.
 *
 * <pre>
 * This file is part of FidoCadJ.
 *
 * FidoCadJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FidoCadJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FidoCadJ. If not,
 * @see <a href=http://www.gnu.org/licenses/>http://www.gnu.org/licenses/</a>.
 *
 * Copyright 2007-2026 by Davide Bucci
 * </pre>
 *
 * @author Davide Bucci
 */
public final class PrimitiveAdvText extends GraphicPrimitive
{
    private String txt;
    private float six;
    private float siy;
    private int sty;
    private int o;
    private String fontName;

    private boolean recalcSize;

    // Text style patterns
    static final int TEXT_BOLD = 1;
    static final int TEXT_MIRRORED = 4;
    static final int TEXT_ITALIC = 2;

    // Maximum and minimum size allowed for the text.
    static final int MAXSIZE = 2000;
    static final int MINSIZE = 1;

    // A text is defined by one point.
    static final int N_POINTS = 1;

    // Those are data which are kept for the fast redraw of this primitive.
    // Basically, they are calculated once and then used as much as possible
    // without having to calculate everything from scratch.
    private int xaSCI;              // NOPMD
    private int yaSCI;              // NOPMD
    private int orientationSCI;     // NOPMD
    private int hSCI;               // NOPMD
    private int thSCI;              // NOPMD
    private int wSCI;               // NOPMD
    // Vertical extent (relative to the baseline) of the decorated string's
    // subscript/superscript glyphs, in the same logical units as hSCI/
    // thSCI/wSCI. See issue #197.
    private int topSCI;             // NOPMD
    private int bottomSCI;          // NOPMD
    private int[] xpSCI, ypSCI;     // NOPMD

    private boolean mirror;
    private int orientation;

    private int h;
    private int th;
    private int w;
    // Screen-pixel cache of the same decorated vertical extent computed in
    // draw(), reused by getDistanceToPoint() when only the zoom changed.
    private int decoTop;
    private int decoBottom;
    private double ymagnitude;
    private boolean coordmirroring;

    private float x1;                 // NOPMD
    private float y1;                 // NOPMD
    private float xa;
    private float ya;
    private float qq;
    private double xyfactor;
    private double si;              // NOPMD
    private double co;              // NOPMD
    private boolean needsStretching;

    /**
     * Gets the number of control points used.
     *
     * @return the number of points used by the primitive.
     */
    @Override
    public int getControlPointNumber()
    {
        return N_POINTS;
    }

    /**
     * Standard "empty" constructor.
     */
    public PrimitiveAdvText() {
        super();
        six = 3;
        siy = 4;
        o = 0;
        txt = "";
        fontName = Globals.defaultTextFont;
        virtualPoint = new PointG[N_POINTS];
        for (int i = 0; i < N_POINTS; ++i) {
            virtualPoint[i] = new PointG();
        }
        changed = true;
        recalcSize = true;
    }

    /**
     * Complete constructor.
     *
     * @param x the x position of the control point of the text.
     * @param y the y position of the control point of the text.
     * @param sx the x size of the font.
     * @param sy the y size of the font.
     * @param fn font name.
     * @param or the orientation of the text.
     * @param st the style of the text.
     * @param t the text to be used.
     * @param l the layer to be used.
     */
    public PrimitiveAdvText(float x, float y, float sx, float sy, 
            String fn, int or,
            int st, String t, int l) {
        this();
        virtualPoint[0] = new PointG(x, y);
        six = sx;
        siy = sy;
        sty = st;
        txt = t;
        o = or;
        fontName = fn;
        setLayer(l);
        changed = true;
        recalcSize = true;
    }

    /**
     * Draw the graphic primitive on the given graphic context.
     *
     * @param g the graphic context in which the primitive should be drawn.
     * @param coordSys the graphic coordinates system to be applied.
     * @param layerV the layer description.
     */
    @Override
    public void draw(GraphicsInterface g, MapCoordinates coordSys, List layerV)
    {
        if (!selectLayer(g, layerV)) {
            return;
        }

        /*
            For this:
            http://sourceforge.net/tracker/?func=detail&aid=2908420&group_id=
            274886&atid=1167997
            we are now checking if the text is "" before printing it.
         */
        if (txt.length() == 0) {
            return;
        }
        changed = true;
        ymagnitude = coordSys.getYMagnitude();
        coordmirroring = coordSys.getMirror();

        if (changed) {
            changed = false;
            mirror = false;
            recalcSize = true;
            /*
                in the simple text primitive, the the virtual point represents
                the position of the text to be drawn.
             */
            x1 = virtualPoint[0].x;
            y1 = virtualPoint[0].y;
            xa = coordSys.mapX(x1, y1);
            ya = coordSys.mapY(x1, y1);
            /*
                siy is the font horizontal size in mils (1/1000 of an inch).
                1 typographical point is 1/72 of an inch.
             */

            g.setFont(fontName, six * 12 * coordSys.getYMagnitude() / 7 + .5,
                    (sty & TEXT_ITALIC) != 0, (sty & TEXT_BOLD) != 0);

            orientation = o;
            mirror = false;
            if ((sty & TEXT_MIRRORED) != 0) {
                mirror = !mirror;
                orientation = -orientation;
            }
            if (six == 0 || siy == 0) {
                siy = 10;
                six = 7;
            }
            orientation -= coordSys.getOrientation() * 90;

            if (coordmirroring) {
                mirror = !mirror;
                orientation = -orientation;
            }

            // Determination of the size of the text string.
            h = g.getFontAscent();
            th = h + g.getFontDescent();

            DecoratedText dt = new DecoratedText(g.getTextInterface());
            w = dt.getDecoratedStringWidth(txt);
            int[] decoExtent = dt.getDecoratedVerticalExtent(txt,
                six * 12 * coordSys.getYMagnitude() / 7 + .5, h, th-h);
            decoTop = decoExtent[0];
            decoBottom = decoExtent[1];

            xyfactor = 1.0;
            needsStretching = false;

            if (siy / six != 10 / 7) {
                // Create a transformation for the font.
                xyfactor = (double) siy / (double) six * 22.0 / 40.0;
                needsStretching = true;
            }

            if (orientation == 0) {
                if (mirror) {
                    coordSys.trackPoint(xa - w, ya);
                    coordSys.trackPoint(xa, ya + (int) (th * xyfactor));
                } else {
                    coordSys.trackPoint(xa + w, ya);
                    coordSys.trackPoint(xa, ya + (int) (h * xyfactor));
                }
            } else {
                if (mirror) {
                    si = Math.sin(Math.toRadians(-orientation));
                    co = Math.cos(Math.toRadians(-orientation));
                } else {
                    si = Math.sin(Math.toRadians(orientation));
                    co = Math.cos(Math.toRadians(orientation));
                }
                // Calculate the bounding box.

                double bbx1 = xa;
                double bby1 = ya;

                double bbx2 = xa + th * si;
                double bby2 = ya + th * co * xyfactor;

                double bbx3 = xa + w * co + th * si;
                double bby3 = ya + (th * co - w * si) * xyfactor;

                double bbx4 = xa + w * co;
                double bby4 = ya - w * si * xyfactor;

                if (mirror) {
                    bbx2 = xa - th * si;
                    bbx3 = xa - w * co - th * si;
                    bbx4 = xa - w * co;
                }

                coordSys.trackPoint((int) bbx1, (int) bby1);
                coordSys.trackPoint((int) bbx2, (int) bby2);
                coordSys.trackPoint((int) bbx3, (int) bby3);
                coordSys.trackPoint((int) bbx4, (int) bby4);
            }
            qq = (float)(ya / xyfactor);
        }

        g.drawAdvText(xyfactor, Math.round(xa), Math.round(ya), 
                Math.round(qq), Math.round(h), Math.round(w), Math.round(h),
                needsStretching, orientation, mirror, txt);
    }

    /**
     * Parse a token array and store the graphic data for a given primitive
     * Obviously, that routine should be called *after* having recognized that
     * the called primitive is correct. That routine also sets the current
     * layer.
     *
     * @param tokens the tokens to be processed. tokens[0] should be the command
     * of the actual primitive.
     * @param nn the number of tokens present in the array.
     *
     * @throws IOException if the arguments are incorrect or a problem occurs.
     */
    @Override
    public void parseTokens(String[] tokens, int nn) throws IOException
    {
        // assert it is the correct primitive
        changed = true;
        recalcSize = true;

        if ("TY".equals(tokens[0])) {   // Text (advanced)
            if (nn < 9) {
                throw new IOException("bad arguments on TY");
            }

            virtualPoint[0].x = Float.parseFloat(tokens[1]);
            virtualPoint[0].y = Float.parseFloat(tokens[2]);
            siy = Float.parseFloat(tokens[3]);
            six = Float.parseFloat(tokens[4]);
            checkSizes();
            o = Integer.parseInt(tokens[5]);
            sty = Integer.parseInt(tokens[6]);
            parseLayer(tokens[7]);

            int j = 8;
            StringBuffer txtb = new StringBuffer();

            if ("*".equals(tokens[8])) {
                fontName = Globals.defaultTextFont;
            } else {
                fontName = tokens[8].replaceAll("\\+\\+", " ");
            }

            /*
             siy is the font horizontal size in mils (1/1000 of an inch).
             1 typographical point is 1/72 of an inch.
             */
            while (j < nn - 1) {
                txtb.append(tokens[++j]);
                if (j < nn - 1) {
                    txtb.append(" ");
                }
            }
            txt = txtb.toString();
        } else {
            if ("TE".equals(tokens[0])) {    // Text (simple)
                if (nn < 4) {
                    throw new IOException("bad arguments on TE");
                }

                virtualPoint[0].x = Float.parseFloat(tokens[1]);
                virtualPoint[0].y = Float.parseFloat(tokens[2]);
                // Default sizes and styles
                six = 3;
                siy = 4;
                o = 0;
                sty = 0;

                int j = 2;
                txt = "";

                while (j < nn - 1) {
                    txt += tokens[++j] + " ";
                }

                // In the original simple text primitive, the layer was not
                // handled. Here we just suppose it is always 0.
                parseLayer("0");

            } else {
                throw new IOException("Invalid primitive:"
                        + " programming error?");
            }
        }
    }

    /**
     * Check and correct if necessary the text size range.
     */
    public void checkSizes()
    {
        // Safety checks!
        if (siy < MINSIZE) {
            siy = MINSIZE;
        }
        if (six < MINSIZE) {
            six = MINSIZE;
        }

        if (siy > MAXSIZE) {
            siy = MAXSIZE;
        }
        if (six > MAXSIZE) {
            six = MAXSIZE;
        }
    }

    /**
     * Gets the distance (in primitive's coordinates space) between a given
     * point and the primitive. When it is reasonable, the behaviour can be
     * binary (polygons, ovals...). In other cases (lines, points), it can be
     * proportional.
     *
     * @param px the x coordinate of the given point.
     * @param py the y coordinate of the given point.
     *
     * @return the distance in logical units.
     */
    @Override
    public int getDistanceToPoint(float px, float py)
    {
        // This calculation takes a lot of time, since we need to obtain the
        // size of the font used, calculate the area which is active for the
        // mouse and so on. For this reason, we make it only when necessary,
        // by exploiting exactly the same principle of the optimized drawing
        // routines.

        if (changed || recalcSize) {
            if (changed) {
                GraphicsNull gSCI = new GraphicsNull();

                gSCI.setFont(fontName, (int) (six * 12.0 / 7.0 + .5),
                        (sty & TEXT_ITALIC) != 0, (sty & TEXT_BOLD) != 0);

                DecoratedText dt = new DecoratedText(gSCI);
                wSCI = dt.getDecoratedStringWidth(txt);

                hSCI = gSCI.getFontAscent();
                thSCI = hSCI + gSCI.getFontDescent();

                int[] extentSCI = dt.getDecoratedVerticalExtent(txt,
                        (int) (six * 12.0 / 7.0 + .5), hSCI, thSCI-hSCI);
                topSCI = extentSCI[0];
                bottomSCI = extentSCI[1];
            } else {
                hSCI = (int) (h / ymagnitude);
                thSCI = (int) (th / ymagnitude);
                wSCI = (int) (w / ymagnitude);
                topSCI = (int) (decoTop / ymagnitude);
                bottomSCI = (int) (decoBottom / ymagnitude);
            }
            // recalcSize is set to true when the draw method detects that the
            // graphical appearance of the text should be recalculated.

            recalcSize = false;

            xaSCI = Math.round(virtualPoint[0].x);
            yaSCI = Math.round(virtualPoint[0].y);

            orientationSCI = o;

            if (siy / six != 10 / 7) {
                double scaleFactor = (double) siy * 22.0 / 40.0
                        / (double) six;
                hSCI = (int) Math.round(hSCI * scaleFactor);
                thSCI = (int) Math.round((double) thSCI * scaleFactor);
                topSCI = (int) Math.round(topSCI * scaleFactor);
                bottomSCI = (int) Math.round(bottomSCI * scaleFactor);
            }

            // TODO: the calculation fails when mirrored text or rotated is
            // included into a mirrored or rotated macro.
            // Corrections for the mirrored text.
            if ((sty & TEXT_MIRRORED) != 0) {
                orientationSCI = -orientationSCI;
                wSCI = -wSCI;
            }

            if (coordmirroring) {
                //orientationSCI=-orientationSCI;
                wSCI = -wSCI;
            }

            // If there is a tilt of the text, we calculate the four corner
            // of the tilted text area and we put them in a polygon.
            if (orientationSCI != 0) {
                double si = Math.sin(Math.toRadians(orientation));
                double co = Math.cos(Math.toRadians(orientation));

                // The origin corner and the perpendicular-to-text extent
                // are expanded (rather than starting exactly at yaSCI and
                // spanning thSCI) so that a deeply nested subscript or
                // superscript -- whose glyphs are shifted well outside the
                // plain ascent/descent box -- is still covered (#197).
                int perpExtent = bottomSCI-topSCI;
                double ox = xaSCI + (hSCI + topSCI) * si;
                double oy = yaSCI + (hSCI + topSCI) * co;

                xpSCI = new int[4];
                ypSCI = new int[4];

                xpSCI[0] = (int) ox;
                ypSCI[0] = (int) oy;
                xpSCI[1] = (int) (ox + perpExtent * si);
                ypSCI[1] = (int) (oy + perpExtent * co);
                xpSCI[2] = (int) (ox + perpExtent * si + wSCI * co);
                ypSCI[2] = (int) (oy + perpExtent * co - wSCI * si);
                xpSCI[3] = (int) (ox + wSCI * co);
                ypSCI[3] = (int) (oy - wSCI * si);
            }
        }

        if (orientationSCI == 0) {
            // The hit-test box is expanded (rather than spanning exactly
            // [yaSCI, yaSCI+thSCI]) to cover subscript/superscript glyphs
            // shifted outside the plain ascent/descent box (#197).
            int baseline = yaSCI + hSCI;
            int boxTop = baseline + topSCI;
            int boxHeight = bottomSCI - topSCI;
            if (GeometricDistances.pointInRectangle(Math.min(xaSCI,
                    xaSCI + wSCI), boxTop, Math.abs(wSCI), boxHeight, px, py))
            {
                return 0;
            }
        } else {
            if (GeometricDistances.pointInPolygon(xpSCI, ypSCI, 4, px, py)) {
                return 0;
            }
        }

        // It is better not to obtain Integer.MAX_VALUE, but a value which
        // is very large yet smaller than Integer.MAX_VALUE. In fact, in some
        // cases, a test is done to see if a layer is present and this test
        // tries to see if the distance of a symbol is less than
        // Integer.MAX_VALUE, as this should be true when a symbol is present
        // and visible on the screen.
        return Integer.MAX_VALUE / 2;

    }

    /**
     * Get the control parameters of the given primitive.
     *
     * @return a vector of ParameterDescription containing each control
     * parameter. The first parameters should always be the virtual points.
     *
     */
    @Override
    public List<ParameterDescription> getControls()
    {
        List<ParameterDescription> v = new ArrayList<>(10);

        addParameter(v, txt, "ctrl_text");
        addParameter(v, new LayerInfo(getLayer()), "ctrl_layer");
        addParameter(v, six, "ctrl_xsize");
        addParameter(v, siy, "ctrl_ysize");
        addParameter(v, o, "ctrl_angle");
        addParameter(v, (sty & TEXT_MIRRORED) != 0, "ctrl_mirror");
        addParameter(v, (sty & TEXT_ITALIC) != 0, "ctrl_italic");
        addParameter(v, (sty & TEXT_BOLD) != 0, "ctrl_boldface");
        addParameter(v, new FontG(fontName), "ctrl_font");

        return v;
    }

    /**
     * Adds a new ParameterDescription to the provided list.
     * <p>
     * This method creates a ParameterDescription object with the given
     * parameter and description. The description is retrieved using the
     * specified key from the global messages resource bundle.
     * </p>
     *
     * @param list the list to which the ParameterDescription will be added
     * @param parameter the value to set in the ParameterDescription
     * @param descriptionKey the key used to fetch the description string from
     * the global messages
     *
     */
    private void addParameter(List<ParameterDescription> list, Object parameter,
            String descriptionKey)
    {
        ParameterDescription pd = new ParameterDescription();
        pd.parameter = parameter;
        pd.description = Globals.messages.getString(descriptionKey);
        list.add(pd);
    }

    /**
     * Set the control parameters of the given primitive. This method is
     * specular to getControls().
     *
     * @param parameters a vector of ParameterDescription containing each
     * control parameter.
     *
     * @return the next index in v to be scanned (if needed) after the execution
     * of this function.
     */
    @Override
    public int setControls(List<ParameterDescription> parameters)
    {
        changed = true;
        recalcSize = true;

        int i = 0;

        for (ParameterDescription pd : parameters) {
            Object param = pd.parameter;

            if (param instanceof String) {
                txt = (String) param;
            } else {
                if (param instanceof LayerInfo) {
                    setLayer(((LayerInfo) param).getLayer());
                } else {
                    if (param instanceof Float) {
                        switch (i) {
                            case 2 ->
                                six = (Float) param;
                            case 3 ->
                                siy = (Float) param;
                        }
                    } else if (param instanceof Integer) {
                        switch (i) {
                            case 2 ->
                                six = (Integer) param;
                            case 3 ->
                                siy = (Integer) param;
                            case 4 ->
                                o = (Integer) param;
                        }
                    } else {
                        if (param instanceof Boolean) {
                            boolean flag = (Boolean) param;
                            switch (i) {
                                case 5 ->
                                    sty = flag ? sty | TEXT_MIRRORED
                                            : sty & (~TEXT_MIRRORED);
                                case 6 ->
                                    sty = flag ? sty | TEXT_ITALIC
                                            : sty & (~TEXT_ITALIC);
                                case 7 ->
                                    sty = flag ? sty | TEXT_BOLD
                                            : sty & (~TEXT_BOLD);
                            }
                        } else {
                            if (param instanceof FontG) {
                                fontName = ((FontG) param).getFamily();
                            } else {
                                System.out.println(
                                        "Warning: unexpected parameter!" + pd);
                            }
                        }
                    }
                }
            }

            i++;
        }

        checkSizes();
        return i;
    }

    /**
     * Rotate the primitive. Here we just rotate 90° by 90°
     *
     * @param bc specify if the rotation should be done counterclockwise.
     * @param ix the x coordinate of the rotation center
     * @param iy the y coordinate of the rotation center
     */
    @Override
    public void rotatePrimitive(boolean bc, float ix, float iy)
    {
        boolean bCounterClockWise = bc;
        super.rotatePrimitive(bCounterClockWise, ix, iy);

        int po = o / 90;

        if ((sty & TEXT_MIRRORED) != 0) {
            bCounterClockWise = !bCounterClockWise;
        }

        if (bCounterClockWise) {
            po = ++po % 4;
        } else {
            po = (po + 3) % 4;
        }

        o = 90 * po;
    }

    /**
     * Mirror the primitive. For the text, it is different than for the other
     * primitives, since we just need to toggle the mirror flag.
     *
     * @param xPos is the symmetry axis
     *
     */
    @Override
    public void mirrorPrimitive(float xPos)
    {
        super.mirrorPrimitive(xPos);
        sty ^= TEXT_MIRRORED;
        changed = true;
        recalcSize = true;
    }

    /**
     * Obtain a string command descripion of the primitive.
     *
     * @param extensions true if the FidoCadJ extensions to the old FidoCAD
     * formad should be taken into account.
     *
     * @return the FidoCad code corresponding to the primitive.
     */
    @Override
    public String toString(boolean extensions)
    {
        String subsFont;

        // Assicurati che fontName non sia null
        if (fontName == null) {
            fontName = Globals.defaultTextFont;
        }

        // The standard font is indicated with an asterisk
        if (fontName.equals(Globals.defaultTextFont)) {
            subsFont = "*";
        } else {
            StringBuffer s = new StringBuffer("");
            // All spaces are substituted with "++" in order to avoid problems
            // while parsing.
            for (int i = 0; i < fontName.length(); ++i) {
                if (fontName.charAt(i) == ' ') {
                    s.append("++");
                } else {
                    s.append(fontName.charAt(i));
                }
            }
            subsFont = s.toString();
        }

        return "TY " + roundIntelligently(virtualPoint[0].x) + " "
                + roundIntelligently(virtualPoint[0].y)
                + " " + roundIntelligently(siy) + " " 
                + roundIntelligently(six) + " " + o + " " + sty + " "
                + getLayer() + " " + subsFont + " " + txt + "\n";
    }

    /**
     * Export the text primitive on a vector graphic format.
     *
     * @param exp the export interface to employ.
     * @param cs the coordinate mapping to employ.
     *
     * @throws IOException if a problem occurs, such as it is impossible to
     * write on the output file.
     */
    @Override
    public void export(ExportInterface exp, MapCoordinates cs)
            throws IOException
    {
        // Compute orientation and mirror for export, matching draw().
        // Both TEXT_MIRRORED and coordinate mirror negate orientation.
        int resultingO = o;
        boolean resultingMirror = (sty & TEXT_MIRRORED) != 0;
        if (resultingMirror) {
            resultingO = -resultingO;
        }
        resultingO -= cs.getOrientation() * 90;
        if (cs.getMirror()) {
            resultingMirror = !resultingMirror;
            resultingO = -resultingO;
        }

        exp.exportAdvText(cs.mapX(virtualPoint[0].x, virtualPoint[0].y),
                cs.mapY(virtualPoint[0].x, virtualPoint[0].y),
                Math.abs(cs.mapXr(six, six) - cs.mapXr(0, 0)),
                Math.abs(cs.mapYr(siy, siy) - cs.mapYr(0, 0)),
                fontName,
                (sty & TEXT_BOLD) != 0,
                resultingMirror,
                (sty & TEXT_ITALIC) != 0,
                resultingO, getLayer(), txt);
    }

    /**
     * Get the number of the virtual point associated to the Name property.
     *
     * @return the number of the virtual point associated to the Name property.
     */
    @Override
    public int getNameVirtualPointNumber()
    {
        return -1;
    }

    /**
     * Get the number of the virtual point associated to the Value property.
     *
     * @return the number of the virtual point associated to the Value property.
     */
    @Override
    public int getValueVirtualPointNumber()
    {
        return -1;
    }

    /**
     * Determines whether the bounding box of the text defined by the current
     * font settings intersects with the specified rectangle.
     *
     * Calculate the dimensions of the text based on the current font settings.
     * It then creates a bounding box around the text and checks if this
     * bounding box intersects with the given rectangle.
     *
     * @param rect the Rectangle object to check for intersection.
     *
     * @param isLeftToRightSelection Determine the direction of the selection
     *
     * @return true if the bounding box of the text intersects the rectangle,
     * false otherwise.
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

        GraphicsNull g = new GraphicsNull();
        g.setFont(fontName, (int) (six * 12.0 / 7.0 + .5),
                (sty & TEXT_ITALIC) != 0, (sty & TEXT_BOLD) != 0);

        DecoratedText dt = new DecoratedText(g);
        int textWidth = dt.getDecoratedStringWidth(txt);

        int textHeight = g.getFontAscent() + g.getFontDescent();

        float angleRad = (float) Math.toRadians(o);
        float sin = (float)Math.sin(angleRad);
        float cos = (float)Math.cos(angleRad);

        // Adjust for mirroring
        if ((sty & TEXT_MIRRORED) != 0) {
            cos = -cos;
        }

        float x = virtualPoint[0].x;
        float y = virtualPoint[0].y;

        float x1 = x;
        float y1 = y - textHeight;

        float x2 = x + (textWidth * cos);
        float y2 = y - (textWidth * sin);

        float x3 = x + (textWidth * cos - textHeight * sin);
        float y3 = y - (textWidth * sin + textHeight * cos);

        float x4 = x - (textHeight * sin);
        float y4 = y - (textHeight * cos);

        float minX = Math.min(Math.min(x1, x2), Math.min(x3, x4));
        float minY = Math.min(Math.min(y1, y2), Math.min(y3, y4));
        float maxX = Math.max(Math.max(x1, x2), Math.max(x3, x4));
        float maxY = Math.max(Math.max(y1, y2), Math.min(y3, y4));

        RectangleG boundingBox = new RectangleG(
            minX, minY, 
            maxX - minX, maxY - minY);

        return rect.intersects(boundingBox);
    }
}
