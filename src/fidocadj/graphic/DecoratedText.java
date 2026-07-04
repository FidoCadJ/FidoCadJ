package fidocadj.graphic;

/** Decorated text is a class that provides advanced text functions.
    It is possible to do things as follows:

    I_dsat

    R^2

    V^2e

    x^2^3_-3_-4

    to indicate indices or exponents. The command _ indicates that the next
    character will be an index. The command ^ indicates that the next character
    is an exponent. If more of one character must be put, put them in braces.
    Use \_ to enter a bar and \^ to enter a caret.

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

    Copyright 2020-2023 by Davide Bucci
    </pre>
*/
public class DecoratedText
{
    private TextInterface g;
    private String btoken;
    private String bstr;
    private int currentIndex;
    private int lastIndex;
    private int exponentLevel;

    final static int CHUNK = 0;
    final static int INDEX = 1;
    final static int EXPONENT = 2;
    final static int END = 3;

    /** The creator.
        @param g the graphic object where to draw.
    */
    public DecoratedText(TextInterface g)
    {
        this.g=g;
    }

    /** Get the width of the given string with the current font.
        @param s the string to be used.
        @return the width of the string, in pixels.
    */
    public int getStringWidth(String s)
    {
        return g.getStringWidth(s);
    }

    private int getToken()
    {
        StringBuffer processToken;
        if(currentIndex >= lastIndex) {
            return END;
        }
        char c=bstr.charAt(currentIndex);
        char cp=0;
        if(currentIndex < lastIndex-1) {
            cp=bstr.charAt(currentIndex+1);
        }

        if(c=='\\') {
            c=cp;
            ++currentIndex;
        } else if(c=='_') {
            ++currentIndex;
            return INDEX;
        } else if (c=='^') {
            ++currentIndex;
            return EXPONENT;
        }
        processToken=new StringBuffer();
        while(true) {
            processToken.append(c);
            ++currentIndex;

            if(currentIndex>=lastIndex) {
                break;
            }
            c=bstr.charAt(currentIndex);
            if(c=='_' || c=='^' || c=='\\') {
                break;
            }
        }
        btoken=processToken.toString();
        return CHUNK;
    }

    private void resetTokenization(String s)
    {
        bstr=s;
        currentIndex=0;
        exponentLevel=0;
        lastIndex=s.length();
    }

    private float getSizeMultLevel()
    {
        switch((int)Math.abs(exponentLevel)){
            case 0:
                return 1f;
            case 1:
                return 0.8f;
            case 2:
                return 0.7f;
            case 3:
                return 0.6f;
            default:
                return 0.5f;
        }
    }

    /** Get the actual width of a decorated string (with indices and exponents).
        This method calculates the width by simulating the exact same parsing
        and rendering logic used by drawString, ensuring the bounding box
        matches the actual rendered text.
        @param str the string to measure (may contain ^ and _ commands).
        @return the actual rendered width of the string, in pixels.
    */
    public int getDecoratedStringWidth(String str)
    {
        resetTokenization(str);
        int totalWidth = 0;
        double originalFontSize = g.getFontSize();
        int t;

        while((t = getToken()) != END) {
            switch(t) {
                case CHUNK:
                    g.setFontSize(originalFontSize * getSizeMultLevel());
                    totalWidth += g.getStringWidth(btoken);
                    break;

                case EXPONENT:
                    ++exponentLevel;
                    break;

                case INDEX:
                    --exponentLevel;
                    break;

                case END:
                    break;

                default:
                    break;
            }
        }
        g.setFontSize(originalFontSize);

        return totalWidth;
    }

    /** Compute how far a decorated string's glyphs stray from the plain
        text baseline box because of nested subscript/superscript
        shifting, so that a hit-test/selection bounding box can be
        expanded to actually cover them (see issue #197: clicking on a
        deeply-nested subscript/superscript character selected the wrong
        primitive because the selection box was not compensating for the
        vertical shift, only for the width).
        This mirrors the exact shift formula used by {@link #drawString},
        given the same base font size that will be passed to
        {@link fidocadj.graphic.TextInterface#setFontSize(double)} /
        the font passed to <code>setFont</code> before drawing.
        Note: unlike {@link #getDecoratedStringWidth}, this method takes
        the base font size explicitly rather than reading it from
        <code>g.getFontSize()</code>, since some TextInterface
        implementations (e.g. the headless one used for hit-testing) do
        not keep getFontSize() in sync with the font last passed to
        setFont(), only with setFontSize().
        @param str the string to measure (may contain ^ and _ commands).
        @param baseFontSize the font size (same unit/scale as the one
            passed to setFont() before this string was/will be drawn).
        @param baseAscent the ascent of the base (unshifted, level 0) font,
            in the same unit the caller wants the result expressed in.
        @param baseDescent the descent of the base (unshifted, level 0)
            font, in the same unit as baseAscent.
        @return a two-element array {top, bottom}, both relative to the
            unshifted baseline (top is typically negative, i.e. above the
            baseline; bottom is typically positive, i.e. below it) and
            expressed in the same unit as baseAscent/baseDescent.
    */
    public int[] getDecoratedVerticalExtent(String str,
        double baseFontSize, double baseAscent, double baseDescent)
    {
        resetTokenization(str);
        int top = 0;
        int bottom = 0;
        int t;

        while((t = getToken()) != END) {
            switch(t) {
                case CHUNK:
                    float mult = getSizeMultLevel();
                    // drawString() draws this chunk at baseline
                    // "y - shift" (see below): the chunk's own top/bottom,
                    // relative to the *outer* baseline, are therefore
                    // offset by "-shift", not "+shift".
                    double shift = exponentLevel*baseFontSize*mult*0.5;
                    int chunkTop =
                        (int)Math.round(-shift - baseAscent*mult);
                    int chunkBottom =
                        (int)Math.round(-shift + baseDescent*mult);
                    if (chunkTop < top) { top = chunkTop; }
                    if (chunkBottom > bottom) { bottom = chunkBottom; }
                    break;

                case EXPONENT:
                    ++exponentLevel;
                    break;

                case INDEX:
                    --exponentLevel;
                    break;

                case END:
                    break;

                default:
                    break;
            }
        }

        return new int[]{top, bottom};
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
        /*
            [FIDOCAD]
            FJC A 0.35
            TY 1 2 4 3 0 0 0 Helvetica 1^2^3^4^5^6^7^8^9
        */
        resetTokenization(str);
        int xc=x;
        double fontSize=g.getFontSize();
        int t;
        while((t=getToken())!=END) {
            switch(t) {
                case CHUNK:
                    g.setFontSize(fontSize*getSizeMultLevel());
                    // Font size is given in points, i.e. 1/72 of an inch.
                    // FidoCadJ has a 200 dpi internal resolution.
                    g.drawString(btoken, xc, y-(int)Math.round(
                            exponentLevel*fontSize*getSizeMultLevel()*0.5));
                    xc+=g.getStringWidth(btoken);
                    break;
                case EXPONENT:
                    ++exponentLevel;
                    break;
                case INDEX:
                    --exponentLevel;
                    break;
                case END:
                    break;
                default:
            }
        }
    }
}
