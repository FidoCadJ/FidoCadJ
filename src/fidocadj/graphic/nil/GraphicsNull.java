package fidocadj.graphic.nil;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

import fidocadj.geom.MapCoordinates;
import fidocadj.layers.LayerDesc;
import fidocadj.graphic.GraphicsInterface;
import fidocadj.graphic.ImageInterface;
import fidocadj.graphic.ShapeInterface;
import fidocadj.graphic.TextInterface;
import fidocadj.graphic.PolygonInterface;
import fidocadj.graphic.ColorInterface;


/**         SWING VERSION


    Null graphic class. Does nothing. Nil. Zero. :-)
    Except... calculating text size correctly!

    Yes. There is a reason for that. This is used for calculating the size of
    a drawing. In practice, a redraw is launched to keep track of all the
    drawing elements. They are not drawn, but the program keeps track of their
    positions and size.

<pre>
    This file is part of FidoCadJ.

    FidoCadJ is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FidoCadJ is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FidoCadJ. If not,
    @see <a href=http://www.gnu.org/licenses/>http://www.gnu.org/licenses/</a>.

    Copyright 2014-2023 by Davide Bucci
</pre>
*/

public class GraphicsNull implements GraphicsInterface, TextInterface
{
    private FontMetrics fm;
    Graphics g;

    /** Standard constructor.
    */
    public GraphicsNull()
    {
        // Unfortunately, to get the image size, we need to redraw it.
        // I do not like it, even if here we are not in a speed sensitive
        // context!
        // Create a dummy image on which the drawing will be done
        BufferedImage bufferedImage = new BufferedImage(10, 10,
                                          BufferedImage.TYPE_INT_RGB);

        // Create a graphics contents on the buffered image
        g = bufferedImage.createGraphics();
        fm = g.getFontMetrics();
        /* Is that useful??? */
        ((Graphics2D) g).setRenderingHint(
            RenderingHints.KEY_FRACTIONALMETRICS,
            RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    /** Retrieves an object implementing an appropriate TextInterface.
        @return an object implementing TextInterface.
    */
    public TextInterface getTextInterface()
    {
        return this;
    }

    /** Set the current color. Here nothing is done.
        @param c the current color.
    */
    public void setColor(ColorInterface c)
    {
        // nothing to do
    }

    /** Set the current zoom factor. Currently employed for resizing the dash
        styles.
        @param z the current zoom factor (pixels for logical units).
    */
    public void setZoom(double z)
    {
        // nothing to do
    }

    /** Get the current zoom factor. Currently employed for resizing the dash
        styles.
        @return the current zoom factor (pixels for logical units).
    */
    public double getZoom()
    {
        return 1.0;
    }

    /** Get the current color.
        @return the current color.
    */
    public ColorInterface getColor()
    {
        return new ColorNull();
    }

    /** Retrieves or create a BasicStroke object having the wanted with and
        style and apply it to the current graphic context.
        @param w the width in pixel.
        @param dashStyle the style of the stroke.
    */
    public void applyStroke(float w, int dashStyle)
    {
        // nothing to do
    }

    /** Draws a rectangle on the current graphic context.
        @param x the x coordinate of the uppermost left corner.
        @param y the y coordinate of the uppermost left corner.
        @param width the width of the rectangle.
        @param height the height of the rectangle.
    */
    public void drawRect(int x, int y, int width, int height)
    {
        // nothing to do
    }

    /** Fills a rectangle on the current graphic context.
        @param x the x coordinate of the uppermost left corner.
        @param y the y coordinate of the uppermost left corner.
        @param width the width of the rectangle.
        @param height the height of the rectangle.
    */
    public void fillRect(int x, int y, int width, int height)
    {
        // nothing to do
    }

    /** Fill a rounded rectangle on the current graphic context.
        @param x the x coordinate of the uppermost left corner.
        @param y the y coordinate of the uppermost left corner.
        @param width the width of the rectangle.
        @param height the height of the rectangle.
        @param arcWidth the width of the arc of the round corners.
        @param arcHeight the height of the arc of the round corners.
    */
    public void fillRoundRect(int x,
                              int y,
                              int width,
                              int height,
                              int arcWidth,
                              int arcHeight)
    {
        // nothing to do
    }

    /** Check whether the rectangle specified falls in a region which need
        to be updated because it is "dirty" on the screen.
        Implementing correctly this method is very important to achieve a good
        redrawing speed because only "dirty" regions on the screen will be
        actually redrawn.
        @param x the x coordinate of the uppermost left corner of rectangle.
        @param y the y coordinate of the uppermost left corner of rectangle.
        @param width the width of the rectangle of the rectangle.
        @param height the height of the rectangle of the rectangle.
        @return true if the rectangle hits the dirty region.
    */
    public boolean hitClip(int x,
                           int y,
                           int width,
                           int height)
    {
        return false;
    }

    /** Draw a segment between two points
        @param x1 first coordinate x value
        @param y1 first coordinate y value
        @param x2 second coordinate x value
        @param y2 second coordinate y value
    */
    public void drawLine(int x1,
                         int y1,
                         int x2,
                         int y2)
    {
        // nothing to do
    }

    /** Set the current font for drawing text.
        @param name the name of the typeface to be used.
        @param size the size in pixels.
        @param isItalic true if an italic variant should be used.
        @param isBold true if a bold variant should be used.
    */
    public void setFont(String name, double size, boolean isItalic,
        boolean isBold)
    {
        /*Font f = new Font(name,
            Font.PLAIN+(isItalic?Font.ITALIC:0)+(isBold?Font.BOLD:0),
            size);*/
        Font ft = new Font(name,
            Font.PLAIN+(isItalic?Font.ITALIC:0)+(isBold?Font.BOLD:0), 100);
        Font f = ft.deriveFont(
            AffineTransform.getScaleInstance(
                size/100.0,size/100.0));

        fm=g.getFontMetrics(f);
    }
    /** Get the font size.
        @return the size.
    */
    public double getFontSize()
    {
        return g.getFont().getSize();
    }

    /** Set the font size.
        @param size the fort size.
    */
    public void setFontSize(double size)
    {
        g.setFont(g.getFont().deriveFont((int)Math.round(size)));
    }
    /** Simple version. It sets the current font.
        @param name the name of the typeface.
        @param size the vertical size in pixels.
    */
    public void setFont(String name, double size)
    {
        setFont(name, size, false, false);
    }

    /** Get the ascent metric of the current font.
        @return the value of the ascent, in pixels.
    */
    public int getFontAscent()
    {
        // TODO: is there a way to implement something without a graphic
        // context?
        return fm.getAscent();
    }

    /** Get the descent metric of the current font.
        @return the value of the descent, in pixels.
    */
    public int getFontDescent()
    {
        return fm.getDescent();
    }

    /** Get the width of the given string with the current font.
        @param s the string to be used.
        @return the width of the string, in pixels.
    */
    public int getStringWidth(String s)
    {
        //System.out.println("nil: "+fm.stringWidth(s));
        return fm.stringWidth(s);
    }

    /** Draw a string on the current graphic context.
        @param str the string to be drawn.
        @param x the x coordinate of the starting point.
        @param y the y coordinate of the starting point.
    */
    public void drawString(String str,
                                int x,
                                int y)
    {
        // nothing to do
    }

    /** Set the transparency (alpha) of the current color.
        @param alpha the transparency, between 0.0 (transparent) and 1.0
            (fully opaque).
    */
    public void setAlpha(float alpha)
    {
        // nothing to do
    }

    /** Draw a completely filled oval in the current graphic context.
        @param x the x coordinate of the starting point.
        @param y the y coordinate of the starting point.
        @param width the width of the oval.
        @param height the height of the oval.
    */
    public void fillOval(int x,
                              int y,
                              int width,
                              int height)
    {
        // nothing to do
    }

    /** Draw an enmpty oval in the current graphic context.
        @param x the x coordinate of the starting point.
        @param y the y coordinate of the starting point.
        @param width the width of the oval.
        @param height the height of the oval.
    */
    public void drawOval(int x,
                              int y,
                              int width,
                              int height)
    {
        // nothing to do
    }

    /** Fill a given  shape.
        @param s the shape to be filled.
    */
    public void fill(ShapeInterface s)
    {
        // nothing to do
    }

    /** Draw a given  shape.
        @param s the shape to be drawn.
    */
    public void draw(ShapeInterface s)
    {
        // nothing to do
    }

    /** Fill a given  polygon.
        @param p the polygon to be filled.
    */
    public void fillPolygon(PolygonInterface p)
    {
        // nothing to do
    }

    /** Draw a given  polygon.
        @param p the polygon to be drawn.
    */
    public void drawPolygon(PolygonInterface p)
    {
        // nothing to do
    }

    /** Select the selection color (normally, green) for the current graphic
        context.
        @param l the layer whose color should be blended with the selection
            color (green).
    */
    public void activateSelectColor(LayerDesc l)
    {
        // nothing to do
    }

    /** Draw a string by allowing for a certain degree of flexibility in
        specifying how the text will be handled.
        @param xyfactor the text font is specified by giving its height in the
            setFont() method. If the text should be stretched (i.e. its width
            should be modified), this parameter gives the amount of stretching.
        @param xa the x coordinate of the point where the text will be placed.
        @param ya the y coordinate of the point where the rotation is
            calculated.
        @param qq the y coordinate of the point where the text will be placed.
        @param h the height of the text, in pixels.
        @param w the width of the string, in pixels.
        @param th the total height of the text (ascent+descents).
        @param needsStretching true if some stretching is needed.
        @param orientation orientation in degrees of the text.
        @param mirror true if the text is mirrored.
        @param txt the string to be drawn.
    */
    public void drawAdvText(double xyfactor, int xa, int ya,
        int qq, int h, int w, int th, boolean needsStretching,
        int orientation, boolean mirror,
        String txt)
    {
        // nothing to do
    }

    /** Draw the grid in the given graphic context.
        @param cs the coordinate map description.
        @param xmin the x (screen) coordinate of the upper left corner.
        @param ymin the y (screen) coordinate of the upper left corner.
        @param xmax the x (screen) coordinate of the bottom right corner.
        @param ymax the y (screen) coordinate of the bottom right corner.
        @param colorDots the color for dot grid
        @param colorLines the color for lines grid
    */
    public void drawGrid(MapCoordinates cs, int xmin, int ymin, int xmax,
            int ymax, ColorInterface colorDots, ColorInterface colorLines)
    {
            // nothing to do
    }

    /** Create a polygon object, compatible with the current implementation.
        @return a polygon object.
    */
    public PolygonInterface createPolygon()
    {
        return new PolygonNull();
    }

    /** Create a color object, compatible with the current implementation.
        @return a color object.
    */
    public ColorInterface createColor()
    {
        return new ColorNull();
    }

    /** Create a shape object, compatible with the current implementation.
        @return a shape object.
    */
    public ShapeInterface createShape()
    {
        return new ShapeNull();
    }

    /** Does nothing.
        @param data ignored.
        @return a new ImageNull instance.
    */
    public ImageInterface createImage(byte[] data)
    {
        return new ImageNull();
    }

    /** Does nothing.
        @param img ignored.
        @param x ignored.
        @param y ignored.
        @param width ignored.
        @param height ignored.
    */
    public void drawImage(ImageInterface img, int x, int y,
        int width, int height)
    {
        // nothing to do
    }

    /** Retrieve the current screen density in dots-per-inch.
        @return the screen resolution (density) in dots-per-inch.
    */
    public float getScreenDensity()
    {
        // If GraphicsNull is used correctly, this magic number should not
        // be very important.
        return 72;
    }
}