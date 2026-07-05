package fidocadj.graphic.swing;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;

import fidocadj.geom.MapCoordinates;
import fidocadj.globals.Globals;
import fidocadj.layers.LayerDesc;
import fidocadj.graphic.DecoratedText;
import fidocadj.graphic.ColorInterface;
import fidocadj.graphic.ImageInterface;
import fidocadj.graphic.PolygonInterface;
import fidocadj.graphic.ShapeInterface;
import fidocadj.graphic.TextInterface;
import fidocadj.graphic.GraphicsInterface;

/** This class maps the general interface to java.awt.Graphics2D.
    It also provides a method to draw grid. It turns out that it is not
    trivial to draw grids in an efficient way, and the best strategy generally
    depends on the particular context. So the drawGrid method is present
    in the GraphicsInterface and of course its implementation is here.

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

    Copyright 2014-2025 by Davide Bucci
    Performance optimizations 2025, Manuel Finessi
</pre>
*/

public class Graphics2DSwing implements GraphicsInterface, TextInterface
{
    // Practical sizes to the image used a "brush" for the tiled fill.
    // Larger images may yield faster results, until the memory penalty will
    // decrease performance. Used to be 1000x1000, but it seems that this is
    // too big on some Linux systems (see issue #166).
    private final static int maxAllowableGridBrushWidth = 500;
    private final static int maxAllowableGridBrushHeight = 500;

    Graphics2D g;

    // Grid rendering optimization: cache for texture paints
    private static class GridCacheEntry 
    {
        TexturePaint texturePaint;
        BufferedImage image;
        double zoom;
        int width;
        int height;
        long lastUsed;
        
        GridCacheEntry(TexturePaint tp, BufferedImage img, 
                double z, int w, int h) 
        {
            this.texturePaint = tp;
            this.image = img;
            this.zoom = z;
            this.width = w;
            this.height = h;
            this.lastUsed = System.currentTimeMillis();
        }
    }
    
    // Cache for grid textures to avoid recreating them on every redraw
    private Map<String, GridCacheEntry> gridCache = 
            new LinkedHashMap<String, GridCacheEntry>(16, 0.75f, true) 
    {
        private static final int MAX_CACHE_SIZE = 10;
        
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, 
                GridCacheEntry> eldest) 
        {
            return size() > MAX_CACHE_SIZE;
        }
    };

    // Here are some other local variables made global for avoiding memory
    // allocations (used in drawGrid).
    private BufferedImage bufferedImage; // Useful for grid calculation
    private TexturePaint tp;
    private int width;                   // NOPMD (complains -> local variable)
    private int height;                  // NOPMD (complains -> local variable)

    // Grid rendering optimization: skip re-deriving the grid parameters
    // (which strategy to use, coordinate lists, ...) when nothing that
    // affects the result has changed since the last call -- e.g. while
    // dragging a primitive, the grid itself does not change for many
    // consecutive repaints.
    private static final int GRID_MODE_LINES = 1;
    private static final int GRID_MODE_TEXTURE = 2;
    private static final int GRID_MODE_FALLBACK = 3;

    private String lastGridSignature;
    private int lastGridMode;
    private java.util.List<Integer> lastXLines;
    private java.util.List<Integer> lastYLines;
    private int[] lastXDotsScreen;
    private int[] lastYDotsScreen;

    private BasicStroke[] strokeList;
    private float actual_w;

    private double zoom;
    private double actualZoom;

    private Color selectedColor;

    /*  Strategy in 0.24.7:
        -------------------
        The font size affects the way the font is drawn. For this reason (as
        things such as the zoom and scaling should not change the relative
        size of the text), the font size is kept always equal to 100. Then, a
        coordinate change is applied to the font so that it is rescaled to the
        wanted size.

        Strategy in 0.24.8:
        -------------------
        Due to a strange bug for some (large) font sizes, I (DB) reverted the
        mechanism and I derive a new font of a given calculated size from the
        original font.
        The situation does not seem to be now much worse than 0.24.7 as I can
        set up the font size as a float and thus cope better with smaller
        font sizes.
    */
    private final static int FONTSIZE=100; // The size of the unscaled font.
    private Font f;                 // This is the scaled font.
    private double fontScale=1.0;   // This is the scaling factor.
    private Font mf;                // This is the original (unscaled) font.

    /** Constructor: fabricate a new object form a java.awt.Graphics2D object.
        @param gg the java.awt.Graphics2D graphic context.
    */
    public Graphics2DSwing(Graphics2D gg)
    {
        g=gg;
        //oldZoom = -1;
        actualZoom = -1;
        zoom=1;
        selectedColor = Color.GREEN;
        /* Is that useful??? */
        g.setRenderingHint(
            RenderingHints.KEY_FRACTIONALMETRICS,
            RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /** Constructor: fabricate a new object form a java.awt.Graphics object.
        @param gg the java.awt.Graphics graphic context.
    */
    public Graphics2DSwing(Graphics gg)
    {
        g=(Graphics2D)gg;
        //oldZoom = -1;
        actualZoom = -1;
        zoom=1;
        selectedColor = Color.GREEN;
    }

    /** Constructor: fabricate a new object without associating a graphic
        object. You should use {@link #setGraphicContext} method to setup a
        graphic object in a second time
        to avoid a runtime exception.
    */
    public Graphics2DSwing()
    {
        g=null;
        //oldZoom = -1;
        actualZoom = -1;
        zoom=1;
        selectedColor = Color.GREEN;
    }

    /** Retrieves an object implementing an appropriate TextInterface.
        @return an object implementing TextInterface.
    */
    public TextInterface getTextInterface()
    {
        return this;
    }

    /** Retrieves or create a BasicStroke object having the wanted with and
        style and apply it to the current graphic context.
        @param w the width in pixel
        @param dashStyle the style of the stroke
    */
    public void applyStroke(float w, int dashStyle)
    {
        if (w!=actual_w && w>0 || zoom!=actualZoom) {
            strokeList = new BasicStroke[Globals.dashNumber];

            // If the line width has been changed, we need to update the
            // stroke table

            // The first entry is non dashed
            strokeList[0]=new BasicStroke(w, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND);
            // Resize the dash sizes depending on the current zoom size.
            float[] dashArrayStretched;
            // Then, the dashed stroke styles are created.
            if(zoom<1.0) {
                zoom=1.0;
            }
            for(int i=1; i<Globals.dashNumber; ++i) {
                // Prepare the resized dash array.
                dashArrayStretched = new float[Globals.dash[i].length];
                for(int j=0; j<Globals.dash[i].length;++j) {
                    dashArrayStretched[j]=Globals.dash[i][j]*(float)zoom/2.0f;
                }
                strokeList[i]=new BasicStroke(w, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    (float)(10.0f*zoom), dashArrayStretched,
                    0.0f);
            }
            actual_w=w;
            actualZoom=zoom;
        }

        // Here we retrieve the stroke style corresponding to the given
        // dashStyle
        BasicStroke stroke=strokeList[dashStyle];

        // Apparently, on some systems (like my iMac G5 with MacOSX 10.4.11)
        // setting the stroke takes a lot of time!
        if(!stroke.equals(g.getStroke())) {
            g.setStroke(stroke);
        }
    }

    /** Set the current zoom factor. Currently employed for resizing the dash
        styles.
        @param z the current zoom factor (pixels for logical units).
    */
    public void setZoom(double z)
    {
        zoom=z;
    }

    /** Get the current zoom factor. Currently employed for resizing the dash
        styles.
        @return the current zoom factor (pixels for logical units).
    */
    public double getZoom()
    {
        return zoom;
    }

    /** This is a Swing-related method: it sets the current graphic context
        to the given Swing one.
        @param gg the Swing graphic context.
    */
    public void setGraphicContext(Graphics2D gg)
    {
        g=gg;
    }

    /** This is a Swing-related method: it gets the current graphic context.
        @return the Swing graphic context
    */
    public Graphics2D getGraphicContext()
    {
        return g;
    }

    /** Sets the current color.
        @param c the color to be set. Must be cast-able to ColorSwing class.
    */
    public void setColor(ColorInterface c)
    {
        ColorSwing cc = (ColorSwing) c;
        g.setColor(cc.getColorSwing());
    }

    /** Sets the current color of selected item
        @param color the color to be set.
    */
    public void setSelectedColor(Color color)
    {
        selectedColor = color;
    }

    /** Gets the current color.
        @return the actual color. Can be cast-able to ColorSwing class.
    */
    public ColorInterface getColor()
    {
        return new ColorSwing(g.getColor());
    }

    /** Draw a rectangle on the current graphic context.
        @param x the x coordinate of the uppermost left corner
        @param y the y coordinate of the uppermost left corner
        @param width the width of the rectangle
        @param height the height of the rectangle
    */
    public void drawRect(int x, int y, int width, int height)
    {
        g.drawRect(x,y,width,height);
    }
    
    /** Fill a rectangle on the current graphic context.
        @param x the x coordinate of the uppermost left corner
        @param y the y coordinate of the uppermost left corner
        @param width the width of the rectangle
        @param height the height of the rectangle
    */
    public void fillRect(int x, int y, int width, int height)
    {
        g.fillRect(x,y,width,height);
    }

    /** Fill a rounded rectangle on the current graphic context.
        @param x the x coordinate of the uppermost left corner
        @param y the y coordinate of the uppermost left corner
        @param width the width of the rectangle
        @param height the height of the rectangle
        @param arcWidth the width of the arc of the round corners
        @param arcHeight the height of the arc of the round corners
    */
    public void fillRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight)
    {
        g.fillRoundRect(x,y,width,height,arcWidth,arcHeight);
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
    public boolean hitClip(int x, int y, int width, int height)
    {
        return g.hitClip(x,y,width,height);
    }

    /** Draw a segment between two points.
        @param x1 first coordinate x value.
        @param y1 first coordinate y value.
        @param x2 second coordinate x value.
        @param y2 second coordinate y value.
    */
    public void drawLine(int x1, int y1, int x2, int y2)
    {
        g.drawLine(x1,y1,x2,y2);
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
        mf = new Font(name,
            Font.PLAIN+(isItalic?Font.ITALIC:0)+(isBold?Font.BOLD:0),
            FONTSIZE);
        fontScale=size;
        f = mf.deriveFont((float)size);

        // Check if there is the need to change the current font. Apparently,
        // on some systems (I have seen this on MacOSX), setting up the font
        // takes a surprisingly long amount of time.

        if(!g.getFont().equals(f)) {
            g.setFont(f);
        }
    }

    /** Simple version. It sets the current font.
        @param name the name of the typeface.
        @param size the vertical size in pixels.
    */
    public void setFont(String name, double size)
    {
        setFont(name, size, false, false);
    }
    
    /** Get the font size.
        @return the font size.
    */
    public double getFontSize()
    {
        return fontScale;
    }

    /** Set the font size.
        @param size the font size.
    */
    public void setFontSize(double size)
    {
        fontScale=size;
        if(mf==null) {
            return;
        }
        f = mf.deriveFont((float)size);

        if(!g.getFont().equals(f)) {
            g.setFont(f);
        }
    }

    /** Get the ascent metric of the current font.
        @return the value of the ascent, in pixels.
    */
    public int getFontAscent()
    {
        FontMetrics fm = g.getFontMetrics(g.getFont());
        return fm.getAscent();
    }

    /** Get the descent metric of the current font.
        @return the value of the descent, in pixels.
    */
    public int getFontDescent()
    {
        FontMetrics fm = g.getFontMetrics(g.getFont());
        return fm.getDescent();
    }

    /** Get the width of the given string with the current font.
        @param s the string to be used.
        @return the width of the string, in pixels.
    */
    public int getStringWidth(String s)
    {
        FontMetrics fm = g.getFontMetrics(g.getFont());
        return fm.stringWidth(s);
    }

    /** Draw a string on the current graphic context.
        @param str the string to be drawn.
        @param x the x coordinate of the starting point.
        @param y the y coordinate of the starting point.
    */
    public void drawString(String str, int x, int y)
    {
        g.drawString(str,x,y);
    }

    /** Set the transparency (alpha) of the current color.
        @param alpha the transparency, between 0.0 (transparent) and 1.0
            (fully opaque).
    */
    public void setAlpha(float alpha)
    {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
            alpha));
    }

    /** Draw a completely filled oval in the current graphic context.
        @param x the x coordinate of the starting point.
        @param y the y coordinate of the starting point.
        @param width the width of the oval.
        @param height the height of the oval.
    */
    public void fillOval(int x, int y, int width, int height)
    {
        g.fillOval(x,y,width,height);
    }

    /** Draw an enmpty oval in the current graphic context.
        @param x the x coordinate of the starting point.
        @param y the y coordinate of the starting point.
        @param width the width of the oval.
        @param height the height of the oval.
    */
    public void drawOval(int x, int y, int width, int height)
    {
        g.drawOval(x,y,width,height);
    }

    /** Fill a given  shape.
        @param s the shape to be filled.
    */
    public void fill(ShapeInterface s)
    {
        ShapeSwing ss=(ShapeSwing) s;
        g.fill(ss.getShapeInSwing());
    }

    /** Draw a given  shape.
        @param s the shape to be drawn.
    */
    public void draw(ShapeInterface s)
    {
        ShapeSwing ss=(ShapeSwing) s;
        g.draw(ss.getShapeInSwing());
    }

    /** Fill a given  polygon.
        @param p the polygon to be filled.
    */
    public void fillPolygon(PolygonInterface p)
    {
        PolygonSwing pp=(PolygonSwing) p;
        g.fillPolygon(pp.getSwingPolygon());
    }

    /** Draw a given  polygon.
        @param p the polygon to be drawn.
    */
    public void drawPolygon(PolygonInterface p)
    {
        PolygonSwing pp=(PolygonSwing) p;
        g.drawPolygon(pp.getSwingPolygon());
    }

    /** Select the selection color (normally, green) for the current graphic
        context.
        @param l the layer whose color should be blended with the selection
            color (green).
    */
    public void activateSelectColor(LayerDesc l)
    {
        // We blend the layer color with green, in such a way that the
        // selected objects bear a certain reminescence of their original
        // color.

        if (l==null) {
            g.setColor(selectedColor);
        } else {
            ColorSwing c =(ColorSwing) l.getColor();
            g.setColor(blendColors(selectedColor, c.getColorSwing(), 0.6f));
        }
        g.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, 1.0f));

    }
    
    /**
        Blend two colors. From
     http://www.java2s.com/Code/Java/2D-Graphics-GUI/Commoncolorutilities.htm

        @param color1  First color to blend.
        @param color2  Second color to blend.
        @param r   Blend ratio. 0.5 will give even blend, 1.0 will return
                   color1, 0.0 will return color2 and so on.
        @return        Blended color.
    */
    public static Color blendColors (Color color1, Color color2, float r)
    {
        float ir = (float) 1.0 - r;

        float rgb1[] = new float[3];
        float rgb2[] = new float[3];

        color1.getColorComponents (rgb1);
        color2.getColorComponents (rgb2);

        return new Color (rgb1[0] * r + rgb2[0] * ir,
            rgb1[1] * r + rgb2[1] * ir,
            rgb1[2] * r + rgb2[2] * ir);
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
        // TODO: is it possible to unify qq and ya? For example, get rid of qq?

        /*  At first, I tried to use an affine transform on the font, without
            pratically touching the graphic context. This technique worked well,
            but I noticed it produced bugs on the case of a jar packed on a
            MacOSX application bundle.
            I therefore choose (from v. 0.20.2) to use only graphic context
            transforms. What a pity!

            February 20, 2009: I noticed this is in fact a bug on JRE < 1.5
            December 14, 2015: Maybe it is a way to obtain a more consistant
                text output? Now FidoCadJ requires Java 1.7, get back to it?
        */
        AffineTransform at=(AffineTransform)g.getTransform().clone();

        // Ats is used to save the current coordinate transform.
        AffineTransform ats=(AffineTransform)at.clone();
        AffineTransform stretching= new AffineTransform();
        AffineTransform mm= new AffineTransform();
        DecoratedText dt=new DecoratedText(this);

        stretching.scale(1,xyfactor);

        if (mirror) {
            mm.scale(-1,1);
        }

        // If it's a simple normal text, draw it in the simple (fastest) way.
        if(orientation==0) {
            if (mirror) {
                // Here the text is mirrored
                at.scale(-1,xyfactor);
                g.setTransform(at);
                if(g.hitClip(-xa,qq,w,h)) {
                    if(!g.getFont().equals(f)) {
                        g.setFont(f);
                    }
                    dt.drawString(txt,-xa,qq+h);
                }
            } else {
                // Here the text is normal
                if(needsStretching) {
                    at.concatenate(stretching);
                    g.setTransform(at);
                }

                if(g.hitClip(xa,qq, w, th)){
                    if(th<Globals.textSizeLimit) {
                        g.drawLine(xa,qq,xa+w,qq);
                        if(needsStretching) {
                            g.setTransform(ats);
                        }
                        return;
                    } else {
                        if(!g.getFont().equals(f)) {
                            g.setFont(f);
                        }
                        dt.drawString(txt,xa,qq+h);
                        if(needsStretching) {
                            g.setTransform(ats);
                        }
                        return;
                    }
                }
            }
        } else {    // Text is rotated.
            if(mirror) {
                // Here the text is rotated and mirrored
                at.concatenate(mm);
                at.rotate(Math.toRadians(orientation),-xa, ya);
                if(needsStretching) {
                    at.concatenate(stretching);
                }
                g.setTransform(at);
                if(!g.getFont().equals(f)) {
                    g.setFont(f);
                }

                dt.drawString(txt,-xa,qq+h);

            } else {
                // Here the text is just rotated
                at.rotate(Math.toRadians(-orientation),xa,ya);
                if(needsStretching) {
                    at.concatenate(stretching);
                }
                g.setTransform(at);
                if(!g.getFont().equals(f)) {
                    g.setFont(f);
                }
                dt.drawString(txt,xa,qq+h);
            }
        }
        g.setTransform(ats);
    }

    /** Generate a cache key for grid texture based on current parameters.
        @param z the zoom level
        @param dx the horizontal grid step
        @param dy the vertical grid step
        @param width the texture width
        @param height the texture height
        @return a unique string key for caching
    */
    private String generateGridCacheKey(double z, int dx, int dy, 
            int width, int height)
    {
        // Round zoom to 3 decimal places to avoid excessive cache entries
        // for imperceptible zoom differences
        return String.format("%.3f_%d_%d_%d_%d", z, dx, dy, width, height);
    }

    /** Check if we need to recreate the grid texture or can reuse cached one.
        @param z the current zoom level
        @param dx the horizontal grid step
        @param dy the vertical grid step
        @param width the desired texture width
        @param height the desired texture height
        @return true if texture needs to be recreated
    */
    private boolean shouldRecreateGridTexture(double z, int dx, int dy, 
            int width, int height)
    {
        String cacheKey = generateGridCacheKey(z, dx, dy, width, height);
        GridCacheEntry cached = gridCache.get(cacheKey);
        
        // If we have a valid cached entry, reuse it
        if (cached != null) {
            this.tp = cached.texturePaint;
            this.bufferedImage = cached.image;
            this.width = cached.width;
            this.height = cached.height;
            return false;
        }
        
        return true;
    }

    /** Create and cache a new grid texture.
        @param cs the coordinate map description
        @param dx the horizontal grid step in logical units
        @param dy the vertical grid step in logical units
        @param mul the multiplier for the pattern size
        @param z the zoom level
        @param colorDots the color for grid dots
    */
    private void createGridTexture(MapCoordinates cs, int dx, int dy, 
                                   int mul, double z, ColorInterface colorDots)
    {
        // Calculate texture dimensions
        width = Math.abs(cs.mapX(mul*dx,0)-cs.mapX(0,0));
        if (width <= 0) {
            width = 1;
        }

        height = Math.abs(cs.mapY(0,0)-cs.mapY(0,mul*dy));
        if (height <= 0) {
            height = 1;
        }

        try {
            // Create a buffered image in which to draw
            GraphicsEnvironment env =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
            
            GraphicsDevice device = env.getDefaultScreenDevice();
            
            GraphicsConfiguration config = device.getDefaultConfiguration();
            
            bufferedImage = config.createCompatibleImage(width, height,
                                      Transparency.TRANSLUCENT);

        } catch (OutOfMemoryError e) {
            System.out.println("Out of memory error when painting grid");
            return;
        }

        // Create a graphics contents on the buffered image
        Graphics2D g2d = bufferedImage.createGraphics();
        ColorSwing clrd = (ColorSwing) colorDots;
        g2d.setColor(clrd.getColorSwing());

        double dd = 0; // DB: I tried with d/2 instead of 0, but I get some very
                       // unpleasant aliasing effects for zoom such as 237%
        double d = 1;

        // Pre-calculate all X coordinates to avoid repeated ..
        // coordinate transformations.
        int numXPoints = (int)(cs.unmapXsnap(width) / dx) + 2;
        double[] xCoords = new double[numXPoints];
        int xIdx = 0;
        
        for (double x = 0; x <= cs.unmapXsnap(width) && 
                xIdx < numXPoints; x += dx) 
        {
            xCoords[xIdx++] = cs.mapXr(x, 0);
        }

        // Pre-calculate all Y coordinates
        int numYPoints = (int)(cs.unmapYsnap(height) / dy) + 2;
        double[] yCoords = new double[numYPoints];
        int yIdx = 0;
        
        for (double y = 0; y <= cs.unmapYsnap(height) && 
                yIdx < numYPoints; y += dy) 
        {
            yCoords[yIdx++] = cs.mapYr(0, y);
        }

        // Draw the grid using pre-calculated coordinates
        for (int i = 0; i < xIdx; i++) {
            for (int j = 0; j < yIdx; j++) {
                g2d.fillRect(
                    (int)Math.round(xCoords[i] - dd),
                    (int)Math.round(yCoords[j] - dd),
                    (int)d, (int)d
                );
            }
        }

        g2d.dispose();

        Rectangle anchor = new Rectangle(width, height);
        tp = new TexturePaint(bufferedImage, anchor);

        // Store in cache
        String cacheKey = generateGridCacheKey(z, dx, dy, width, height);
        
        gridCache.put(cacheKey, new GridCacheEntry(tp, bufferedImage, 
                z, width, height));
    }

    /** Draw the grid in the given graphic context.
        OPTIMIZED VERSION: This method uses several performance optimizations:
        - Caching of texture paints to avoid recreating them
        - Pre-calculation of coordinates to minimize transformation calls
        - Smart fallback strategies for different grid densities
        - Efficient line drawing for large pitch grids
        
        This is the interface-compliant version that delegates to the optimized
        internal implementation.
        
        @param cs the coordinate map description
        @param xmin the x (screen) coordinate of the upper left corner
        @param ymin the y (screen) coordinate of the upper left corner
        @param xmax the x (screen) coordinate of the bottom right corner
        @param ymax the y (screen) coordinate of the bottom right corner
        @param colorDots the color for dot grid
        @param colorLines the color for lines grid
    */
    public void drawGrid(MapCoordinates cs, int xmin, int ymin, int xmax,
            int ymax, ColorInterface colorDots, ColorInterface colorLines)
    {
        // Delegate to the internal optimized implementation
        drawGridOptimized(cs, xmin, ymin, xmax, ymax, colorDots, colorLines);
    }

    /** Build a signature string covering every parameter that affects the
        outcome of {@link #drawGridOptimized}, so that repeated calls with
        an unchanged view (e.g. while dragging a primitive) can skip
        straight to replaying the previous frame's draw commands instead
        of re-deriving everything from scratch.
        @param cs the coordinate map description.
        @param xmin the x (screen) coordinate of the upper left corner.
        @param ymin the y (screen) coordinate of the upper left corner.
        @param xmax the x (screen) coordinate of the bottom right corner.
        @param ymax the y (screen) coordinate of the bottom right corner.
        @param colorDots the color for dot grid.
        @param colorLines the color for lines grid.
        @return a string uniquely identifying this combination of
            parameters.
    */
    private String generateFrameSignature(MapCoordinates cs,
            int xmin, int ymin, int xmax, int ymax,
            ColorInterface colorDots, ColorInterface colorLines)
    {
        ColorSwing cd = (ColorSwing) colorDots;
        ColorSwing cl = (ColorSwing) colorLines;
        // Panning (xCenter/yCenter) and orientation/mirroring must be
        // included here even though the pre-existing texture-cache key
        // (generateGridCacheKey) does not track them: unlike that key,
        // which only identifies a repeatable texture tile, this
        // signature must identify the exact rendered frame, and panning
        // changes where the grid points fall within a fixed xmin..xmax/
        // ymin..ymax screen window.
        return String.format(java.util.Locale.ROOT,
            "%.3f_%d_%d_%d_%d_%d_%d_%.3f_%.3f_%d_%b_%d_%d",
            cs.getYMagnitude(), cs.getXGridStep(), cs.getYGridStep(),
            xmin, ymin, xmax, ymax,
            cs.getXCenter(), cs.getYCenter(), cs.getOrientation(),
            cs.mirror,
            cd.getColorSwing().getRGB(), cl.getColorSwing().getRGB());
    }

    /** Temporarily switch to the cheapest rendering hints for a large
        flat-color fill such as the grid's texture blit or its fallback
        dot-by-dot loop: a repeating, already pixel-aligned pattern does
        not benefit from antialiasing/bicubic interpolation, but those
        hints (enabled by CircuitPanel.activateDrawingSettings for the
        rest of the drawing) are still active when the grid is painted.
        @return the previous rendering hints, to be restored via
            {@link #endFastFill}.
    */
    private RenderingHints beginFastFill()
    {
        RenderingHints saved = g.getRenderingHints();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        return saved;
    }

    /** Restore rendering hints saved by {@link #beginFastFill}.
        @param saved the hints returned by {@link #beginFastFill}.
    */
    private void endFastFill(RenderingHints saved)
    {
        g.setRenderingHints(saved);
    }

    /** Replay the draw commands of the last frame, when nothing that
        affects the result has changed since then.
        @param xmin the x (screen) coordinate of the upper left corner.
        @param ymin the y (screen) coordinate of the upper left corner.
        @param xmax the x (screen) coordinate of the bottom right corner.
        @param ymax the y (screen) coordinate of the bottom right corner.
        @param colorDots the color for dot grid.
        @param colorLines the color for lines grid.
    */
    private void replayLastFrame(int xmin, int ymin, int xmax, int ymax,
            ColorInterface colorDots, ColorInterface colorLines)
    {
        switch (lastGridMode) {
            case GRID_MODE_LINES:
                ColorSwing clrl = (ColorSwing) colorLines;
                g.setColor(clrl.getColorSwing());
                for (Integer xCoord : lastXLines) {
                    g.drawLine(xCoord, ymin, xCoord, ymax);
                }
                for (Integer yCoord : lastYLines) {
                    g.drawLine(xmin, yCoord, xmax, yCoord);
                }
                break;
            case GRID_MODE_FALLBACK:
                ColorSwing clrd = (ColorSwing) colorDots;
                g.setColor(clrd.getColorSwing());
                RenderingHints savedFallback = beginFastFill();
                for (int i = 0; i < lastXDotsScreen.length; ++i) {
                    g.fillRect(lastXDotsScreen[i], lastYDotsScreen[i], 1, 1);
                }
                endFastFill(savedFallback);
                break;
            case GRID_MODE_TEXTURE:
            default:
                if (tp != null) {
                    RenderingHints savedTexture = beginFastFill();
                    g.setPaint(tp);
                    g.fillRect(0, 0, xmax, ymax);
                    endFastFill(savedTexture);
                }
                break;
        }
    }

    /** Internal optimized implementation of grid drawing.
        This method contains all the performance optimizations.

        @param cs the coordinate map description
        @param xmin the x (screen) coordinate of the upper left corner
        @param ymin the y (screen) coordinate of the upper left corner
        @param xmax the x (screen) coordinate of the bottom right corner
        @param ymax the y (screen) coordinate of the bottom right corner
        @param colorDots the color for dot grid
        @param colorLines the color for lines grid
    */
    private void drawGridOptimized(MapCoordinates cs,
                        int xmin, int ymin,
                        int xmax, int ymax,
                        ColorInterface colorDots, ColorInterface colorLines)
    {
        // If the view has not changed at all since the last call (e.g.
        // while dragging a primitive, panning/zooming apart), skip
        // straight to replaying the previous frame instead of
        // re-deriving the grid strategy/coordinates from scratch.
        String signature = generateFrameSignature(cs, xmin, ymin, xmax, ymax,
            colorDots, colorLines);
        if (signature.equals(lastGridSignature)) {
            replayLastFrame(xmin, ymin, xmax, ymax, colorDots, colorLines);
            return;
        }

        // Drawing the grid seems easy, but it appears that setting a pixel
        // takes a lot of time. Basically, we create a textured brush and we
        // use it to paint the entire specified region.
        int dx = cs.getXGridStep();   // Horizontal grid pitch in logical units.
        int dy = cs.getYGridStep();   // Vertical grid pitch in logical units.
        int mul = 1;
        double toll = 0.01;
        double z = cs.getYMagnitude();

        double ddx = Math.abs(cs.mapXi(dx, 0, false) - cs.mapXi(0, 0, false));
        double ddy = Math.abs(cs.mapYi(0, dy, false) - cs.mapYi(0, 0, false));

        // Use line rendering for large grid pitch.
        // This code applies a correction: draws lines if the pitch
        // is very large, or draw much less dots if it is too dense.
        if (ddx > 35 || ddy > 35) {
            // Lines! This is the fastest approach for widely spaced grids
            ColorSwing clrl = (ColorSwing)colorLines;
            g.setColor(clrl.getColorSwing());

            // Pre-calculate all X coordinates to avoid
            // repeated unmapXsnap/mapXr calls in the loop
            java.util.List<Integer> xCoords = new java.util.ArrayList<>();
            for (double x = cs.unmapXsnap(xmin);
                    x <= cs.unmapXsnap(xmax); x += dx) {
                xCoords.add((int)Math.round(cs.mapXr(x, 0)));
            }

            // Draw vertical lines using pre-calculated coordinates
            for (Integer xCoord : xCoords) {
                g.drawLine(xCoord, ymin, xCoord, ymax);
            }

            // Pre-calculate and draw horizontal lines
            java.util.List<Integer> yCoords = new java.util.ArrayList<>();
            for (double y = cs.unmapYsnap(ymin);
                    y <= cs.unmapYsnap(ymax); y += dy) {
                yCoords.add((int)Math.round(cs.mapYr(0, y)));
            }

            for (Integer yCoord : yCoords) {
                g.drawLine(xmin, yCoord, xmax, yCoord);
            }

            lastGridMode = GRID_MODE_LINES;
            lastXLines = xCoords;
            lastYLines = yCoords;
            lastGridSignature = signature;
            return;
        }

        // Reduce grid density when it's too fine
        if (ddx < 3 || ddy < 3) {
            // Less dots - multiply pitch by 5 to make grid sparser
            dx = 5 * cs.getXGridStep();
            dy = 5 * cs.getYGridStep();
            ddx = Math.abs(cs.mapXi(dx, 0, false) - cs.mapXi(0, 0, false));
        }

        // Calculate optimal texture pattern size.
        // It turns out that drawing the grid in an efficient way is not a
        // trivial task. The program here tries to calculate the minimum
        // common integer multiple of the dot espacement, to calculate the
        // size of an image in order to be an integer.
        // The pattern filling (which is fast) is then used to replicate the
        // image (very fast!) over the working surface.
        for (int l = 1; l < 105; ++l) {
            if (Math.abs(l * z - Math.round(l * z)) < toll) {
                mul = l;
                break;
            }
        }

        // Calculate the required texture size
        int reqWidth = Math.abs(cs.mapX(mul*dx, 0) - cs.mapX(0, 0));
        if (reqWidth <= 0) {
            reqWidth = 1;
        }

        int reqHeight = Math.abs(cs.mapY(0, 0) - cs.mapY(0, mul*dy));
        if (reqHeight <= 0) {
            reqHeight = 1;
        }

        /* Smart fallback for oversized textures.
           Nowadays computers have generally a lot of memory, but this is
           not a good reason to waste it. If it turns out that the image
           size is utterly impractical, use the standard dot by dot grid
           construction.
           This should happen rarely, only for particular zoom sizes.
        */
        if (reqWidth > maxAllowableGridBrushWidth ||
            reqHeight > maxAllowableGridBrushHeight)
        {
            // Simpler (and generally less efficient) version of the grid
            // Use this as fallback when texture would be too large
            ColorSwing clrd = (ColorSwing) colorDots;
            g.setColor(clrd.getColorSwing());

            double dd = 0;
            double d = 1;

            // to avoid repeated transformations
            java.util.List<Double> xPositions = new java.util.ArrayList<>();
            for (double x = cs.unmapXsnap(xmin);
                    x <= cs.unmapXsnap(xmax); x += dx) {
                xPositions.add(x);
            }

            java.util.List<Double> yPositions = new java.util.ArrayList<>();
            for (double y = cs.unmapYsnap(ymin);
                    y <= cs.unmapYsnap(ymax); y += dy) {
                yPositions.add(y);
            }

            // Pre-compute the final screen-space positions once: both
            // the immediate draw below and any later cache-hit replay
            // reuse this same array, avoiding repeated mapXr/mapYr calls.
            int nDots = xPositions.size() * yPositions.size();
            int[] xScreen = new int[nDots];
            int[] yScreen = new int[nDots];
            int idx = 0;
            for (Double x : xPositions) {
                for (Double y : yPositions) {
                    xScreen[idx] = (int)Math.round(cs.mapXr(x, y) - dd);
                    yScreen[idx] = (int)Math.round(cs.mapYr(x, y) - dd);
                    ++idx;
                }
            }

            RenderingHints savedFallback = beginFastFill();
            for (int i = 0; i < nDots; ++i) {
                g.fillRect(xScreen[i], yScreen[i], (int)d, (int)d);
            }
            endFastFill(savedFallback);

            lastGridMode = GRID_MODE_FALLBACK;
            lastXDotsScreen = xScreen;
            lastYDotsScreen = yScreen;
            lastGridSignature = signature;
            return;
        }

        // Check if we can reuse a cached texture paint
        if (shouldRecreateGridTexture(z, dx, dy, reqWidth, reqHeight)) {
            // Need to create new texture
            createGridTexture(cs, dx, dy, mul, z, colorDots);
        }

        // Apply the cached or newly created texture paint
        if (tp != null) {
            RenderingHints savedTexture = beginFastFill();
            g.setPaint(tp);
            g.fillRect(0, 0, xmax, ymax);
            endFastFill(savedTexture);
        }

        lastGridMode = GRID_MODE_TEXTURE;
        lastGridSignature = signature;
    }

    /** Clear the grid texture cache. 
        This can be called to free memory when needed.
    */
    public void clearGridCache()
    {
        gridCache.clear();
        bufferedImage = null;
        tp = null;
        // Invalidate the last-frame replay cache too, otherwise a stale
        // signature match could try to replay a texture/coordinates that
        // were just discarded.
        lastGridSignature = null;
    }

    /** Get the current size of the grid cache.
        @return the number of cached grid textures
    */
    public int getGridCacheSize()
    {
        return gridCache.size();
    }

    /** Create a polygon object, compatible with Graphics2DSwing.
        @return a polygon object (instance of PolygonSwing).
    */
    public PolygonInterface createPolygon()
    {
        return new PolygonSwing();
    }

    /** Create a shape object, compatible with Graphics2DSwing.
        @return a shape object (instance of ShapeSwing).
    */
    public ShapeInterface createShape()
    {
        return new ShapeSwing();
    }
    
    /** Create a color object, compatible with Graphics2DSwing.
        @return a color object (instance of ColorSwing).
    */
    public ColorInterface createColor()
    {
        return new ColorSwing(g.getColor());
    }

    /** Decode a raster image from its raw byte encoding.
        @param data the raw, encoded image bytes.
        @return the decoded image (instance of ImageSwing), or null if the
            data could not be decoded.
        @throws IOException if an I/O error occurs while decoding.
    */
    public ImageInterface createImage(byte[] data)
        throws IOException
    {
        BufferedImage decoded = ImageIO.read(
            new ByteArrayInputStream(data));
        if (decoded == null) {
            return null;
        }
        return new ImageSwing(decoded);
    }

    /** Draw a raster image, scaled to fit the given rectangle.
        @param img the image to be drawn.
        @param x the x coordinate of the upper left corner.
        @param y the y coordinate of the upper left corner.
        @param width the width of the destination rectangle.
        @param height the height of the destination rectangle.
    */
    public void drawImage(ImageInterface img, int x, int y,
        int width, int height)
    {
        if (!(img instanceof ImageSwing)) {
            return;
        }
        BufferedImage bi = ((ImageSwing)img).getImageSwing();
        if (bi == null) {
            return;
        }
        g.drawImage(bi, x, y, width, height, null);
    }

    /** Retrieve the current screen density in dots-per-inch.
        @return the screen resolution (density) in dots-per-inch.
    */
    public float getScreenDensity()
    {
        return Toolkit.getDefaultToolkit().getScreenResolution();
    }
}