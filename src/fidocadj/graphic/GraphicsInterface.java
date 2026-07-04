package fidocadj.graphic;

import java.io.IOException;

import fidocadj.geom.MapCoordinates;
import fidocadj.layers.LayerDesc;

/** Provides a general way to draw on the screen.

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

    Copyright 2014-2023 by Davide Bucci
    </pre>
*/

public interface GraphicsInterface
{
    /** Get the current color.
        @return the current color.
    */
    ColorInterface getColor();

    /** Set the current zoom factor. Currently employed for resizing the dash
        styles.
        @param z the current zoom factor (pixels for logical units).
    */
    void setZoom(double z);

    /** Get the current zoom factor. Currently employed for resizing the dash
        styles.
        @return the current zoom factor (pixels for logical units).
    */
    double getZoom();

    /** Set the current color.
        @param c the current color.
    */
    void setColor(ColorInterface c);

    /** Retrieves an object implementing an appropriate TextInterface.
        @return an object implementing TextInterface.
    */
    TextInterface getTextInterface();

    /** Retrieves or create a BasicStroke object having the wanted with and
        style and apply it to the current graphic context.
        @param w the width in pixel
        @param dashStyle the style of the stroke
    */
    void applyStroke(float w, int dashStyle);

    /** Draws a rectangle on the current graphic context.
        @param x the x coordinate of the uppermost left corner
        @param y the y coordinate of the uppermost left corner
        @param width the width of the rectangle
        @param height the height of the rectangle
    */
    void drawRect(int x, int y, int width, int height);

    /** Fill a rectangle on the current graphic context.
        @param x the x coordinate of the uppermost left corner
        @param y the y coordinate of the uppermost left corner
        @param width the width of the rectangle
        @param height the height of the rectangle
    */
    void fillRect(int x, int y, int width, int height);

    /** Fill a rounded rectangle on the current graphic context.
        @param x the x coordinate of the uppermost left corner.
        @param y the y coordinate of the uppermost left corner.
        @param width the width of the rectangle.
        @param height the height of the rectangle.
        @param arcWidth the width of the arc of the round corners.
        @param arcHeight the height of the arc of the round corners.
    */
    void fillRoundRect(int x,
                                   int y,
                                   int width,
                                   int height,
                                   int arcWidth,
                                   int arcHeight);

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
    boolean hitClip(int x,
                       int y,
                       int width,
                       int height);

    /** Draw a segment between two points
        @param x1 first coordinate x value
        @param y1 first coordinate y value
        @param x2 second coordinate x value
        @param y2 second coordinate y value
    */
    void drawLine(int x1,
                              int y1,
                              int x2,
                              int y2);

    /** Set the current font for drawing text.
        @param name the name of the typeface to be used.
        @param size the size in pixels
    */
    void setFont(String name, double size);

    /**  Set the current font.
        @param name the name of the typeface.
        @param size the vertical size in pixels.
        @param isItalic true if an italic variant should be used.
        @param isBold true if a bold variant should be used.
    */
    void setFont(String name, double size, boolean isItalic,
        boolean isBold);

    /** Get the font size.
        @return the font size.
    */
    double getFontSize();

    /** Set the font size.
        @param size the font size to be set.
    */
    void setFontSize(double size);

    /** Get the ascent metric of the current font.
        @return the value of the ascent, in pixels.
    */
    int getFontAscent();

    /** Get the descent metric of the current font.
        @return the value of the descent, in pixels.
    */
    int getFontDescent();

    /** Get the width of the given string with the current font.
        @param s the string to be used.
        @return the width of the string, in pixels.
    */
    int getStringWidth(String s);

    /** Draw a string on the current graphic context.
        @param str the string to be drawn.
        @param x the x coordinate of the starting point.
        @param y the y coordinate of the starting point.
    */
    void drawString(String str,
                                int x,
                                int y);

    /** Set the transparency (alpha) of the current color.
        @param alpha the transparency, between 0.0 (transparent) and 1.0
            (fully opaque).
    */
    void setAlpha(float alpha);

    /** Draw a completely filled oval in the current graphic context.
        @param x the x coordinate of the starting point.
        @param y the y coordinate of the starting point.
        @param width the width of the oval.
        @param height the height of the oval.
    */
    void fillOval(int x,
                              int y,
                              int width,
                              int height);

    /** Draw an enmpty oval in the current graphic context.
        @param x the x coordinate of the starting point.
        @param y the y coordinate of the starting point.
        @param width the width of the oval.
        @param height the height of the oval.
    */
    void drawOval(int x,
                              int y,
                              int width,
                              int height);

    /** Fill a given  shape.
        @param s the shape to be filled.
    */
    void fill(ShapeInterface s);

    /** Draw a given  shape.
        @param s the shape to be drawn.
    */
    void draw(ShapeInterface s);

    /** Fill a given  polygon.
        @param p the polygon to be filled.
    */
    void fillPolygon(PolygonInterface p);

    /** Draw a given  polygon.
        @param p the polygon to be drawn.
    */
    void drawPolygon(PolygonInterface p);

    /** Select the selection color (normally, green) for the current graphic
        context.
        @param l the layer whose color should be blended with the selection
            color (green).
    */
    void activateSelectColor(LayerDesc l);

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
    void drawAdvText(double xyfactor, int xa, int ya,
        int qq, int h, int w, int th, boolean needsStretching,
        int orientation, boolean mirror,
        String txt);

    /** Draw the grid in the given graphic context.
        @param cs the coordinate map description.
        @param xmin the x (screen) coordinate of the upper left corner.
        @param ymin the y (screen) coordinate of the upper left corner.
        @param xmax the x (screen) coordinate of the bottom right corner.
        @param ymax the y (screen) coordinate of the bottom right corner.
        @param colorDots the color for dot grid
        @param colorLines the color for lines grid
    */
    void drawGrid(MapCoordinates cs, int xmin, int ymin, int xmax, int ymax,
            ColorInterface colorDots, ColorInterface colorLines);

    /** Create a polygon object, compatible with the current implementation.
        @return a polygon object.
    */
    PolygonInterface createPolygon();

    /** Create a color object, compatible with the current implementation.
        @return a color object.
    */
    ColorInterface createColor();

    /** Create a shape object, compatible with the current implementation.
        @return a shape object.
    */
    ShapeInterface createShape();

    /** Decode a raster image from its raw byte encoding (e.g. the bytes of
        a PNG or JPEG file), compatible with the current implementation.
        @param data the raw, encoded image bytes.
        @return the decoded image, or null if the data could not be decoded.
        @throws IOException if an I/O error occurs while decoding.
    */
    ImageInterface createImage(byte[] data) throws IOException;

    /** Draw a raster image, scaled to fit the given rectangle.
        @param img the image to be drawn.
        @param x the x coordinate of the upper left corner.
        @param y the y coordinate of the upper left corner.
        @param width the width of the destination rectangle.
        @param height the height of the destination rectangle.
    */
    void drawImage(ImageInterface img, int x, int y, int width, int height);

    /** Retrieve the current screen density in dots-per-inch.
        @return the screen resolution (density) in dots-per-inch.
    */
    float getScreenDensity();
}