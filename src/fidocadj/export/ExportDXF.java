package fidocadj.export;

import java.util.*;
import java.io.*;

import fidocadj.globals.Globals;
import fidocadj.layers.LayerDesc;
import fidocadj.primitives.Arrow;
import fidocadj.primitives.MacroDesc;
import fidocadj.graphic.DimensionG;
import fidocadj.graphic.ColorInterface;
import fidocadj.graphic.PointDouble;

/** Export in the DXF (Drawing Exchange Format) ASCII format, targeting
    AutoCAD Release 12 (AC1009). R12 is chosen deliberately over newer
    variants: it needs no object handles, no BLOCK_RECORD table and no
    true-color group codes, which keeps a hand-written (no external
    library) implementation far less likely to produce a file that looks
    structurally valid but is silently rejected or misrendered by real
    CAD software.

    Known limitations of this implementation (documented rather than
    guessed away):
    <ul>
    <li>Filled shapes are only supported where DXF R12 offers a trivial
    native fill: triangles/quadrilaterals (via SOLID) and circles/PCB
    pads/connection dots (via a zero-length wide POLYLINE). Filled
    polygons with more than 4 vertices and filled non-circular ellipses
    are exported as outlines only; a real HATCH-based fill was judged too
    high-risk to hand-write correctly for a first version.</li>
    <li>Text is always drawn with a generic "STANDARD"/"STANDARD_ITALIC"
    style: R12's TEXT entity has no free font-name field of its own (only
    a STYLE reference), so the FidoCadJ font actually selected by the user
    is not reproduced. Bold is not reproduced either (no clean R12
    approximation without a real bold font file).</li>
    <li>The per-entity dash "phase" (offset into the pattern) has no DXF
    R12 equivalent and is ignored; only the pattern shape itself is
    reproduced.</li>
    <li>Layer colors are approximated to the nearest entry of a
    representative (not the full official 256-entry) AutoCAD Color Index
    palette, since R12 has no true-color support.</li>
    </ul>

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
public final class ExportDXF implements ExportInterface
{
    private final FileWriter fstream;
    private BufferedWriter out;
    private List<LayerDesc> layerV;
    private DimensionG totalSize;

    // One FidoCadJ logical unit is 1/200 inch (0.127 mm) at magnitude 1.
    private static final double MM_PER_UNIT = 25.4/200.0;

    private static final int BEZIER_STEPS = 20;
    private static final int ELLIPSE_STEPS = 36;

    private int[] layerAci;
    private String[] layerName;

    // Dash pattern segment lengths in mm (signed: positive = pen down,
    // negative = gap), indexed [dashStyle][segment]. Index 0 (solid) is
    // never populated/used.
    private double[][] dashSegmentsMm;

    private float dashPhase;

    // The layer of the primitive currently being exported, so that
    // exportArrow (which the interface does not pass a layer to) knows
    // which DXF layer to draw arrowheads on.
    private int currentLayer;

    // Approximate AutoCAD Color Index (ACI) palette. DXF R12 has no
    // true-color support, so layer colors must be approximated by the
    // nearest indexed color. This is a representative subset (primary/
    // secondary hues plus a grayscale ramp), not the complete official
    // 256-entry table, chosen to give a reasonable nearest-color match
    // without claiming byte-exact fidelity to AutoCAD's full palette.
    private static final int[][] ACI_RGB = {
        {1,255,0,0},     {2,255,255,0},   {3,0,255,0},     {4,0,255,255},
        {5,0,0,255},     {6,255,0,255},   {7,255,255,255}, {7,0,0,0},
        {8,65,65,65},    {9,128,128,128},
        {10,255,128,128},{30,255,128,0},  {40,255,200,0},
        {60,128,255,0},  {90,0,255,128},  {130,0,200,255},
        {150,0,128,255}, {170,128,0,255}, {200,255,0,128},
        {250,51,51,51},  {251,80,80,80},  {252,110,110,110},
        {253,150,150,150},{254,190,190,190},{255,230,230,230}
    };

    /** Constructor.
        @param f the File object in which the export should be done.
        @throws IOException if the file can not be accessed.
    */
    public ExportDXF(File f) throws IOException
    {
        fstream = new FileWriter(f);
    }

    /** {@inheritDoc} */
    public void setDashUnit(double u)
    {
        dashSegmentsMm = new double[Globals.dashNumber][];
        dashSegmentsMm[0] = new double[0];
        for (int i = 1; i < Globals.dashNumber; ++i) {
            double[] seg = new double[Globals.dash[i].length];
            for (int j = 0; j < seg.length; ++j) {
                double len = Globals.dash[i][j] * u * MM_PER_UNIT;
                // FidoCadJ's dash arrays alternate "pen down" (even
                // index) and "pen up"/gap (odd index) segment lengths,
                // starting with a pen-down segment: the same convention
                // DXF LTYPE uses for the sign of its length values.
                seg[j] = (j % 2 == 0) ? len : -len;
            }
            dashSegmentsMm[i] = seg;
        }
    }

    /** {@inheritDoc} */
    public void setDashPhase(float p)
    {
        // Stored for completeness, but DXF R12 has no per-entity group
        // code for a dash-pattern phase/offset: only the pattern shape
        // itself (declared once in the LTYPE table) is reproduced. This
        // is a cosmetic-only limitation.
        dashPhase = p;
    }

    /** {@inheritDoc} */
    public void exportStart(DimensionG totalSizeP, List<LayerDesc> la,
        int grid)
        throws IOException
    {
        totalSize = totalSizeP;
        layerV = la;
        out = new BufferedWriter(fstream);

        buildLayerTables();

        writeHeader();
        writeTables();
        out.write("0\nSECTION\n2\nBLOCKS\n0\nENDSEC\n");
        out.write("0\nSECTION\n2\nENTITIES\n");
    }

    /** {@inheritDoc} */
    public void exportEnd()
        throws IOException
    {
        out.write("0\nENDSEC\n0\nEOF\n");
        out.close();
    }

    /** {@inheritDoc} */
    public void exportAdvText(int x, int y, double sizex, double sizey,
        String fontname, boolean isBold, boolean isMirrored, boolean isItalic,
        int orientation, int layer, String text)
        throws IOException
    {
        currentLayer = layer;

        double heightMm = sizey * MM_PER_UNIT;
        // sizex/sizey == 7/12 is considered the "normal" aspect ratio
        // (matching the convention used by ExportEPS.exportAdvText).
        double widthFactor = (sizex/sizey) / (7.0/12.0);
        if (widthFactor < 0.1) {
            widthFactor = 0.1;
        }
        if (widthFactor > 10.0) {
            widthFactor = 10.0;
        }

        // exportAdvText's (x,y) is the top-left corner of the text box;
        // DXF TEXT's insertion point is the baseline, so shift down by
        // the same empirical 0.8*height offset ExportEPS uses.
        double baseX = x;
        double baseY = y + 0.8*sizey;

        // Unlike EPS/PGF/PDF, a DXF TEXT entity is not subject to any
        // global coordinate transform (there is no CTM concept): dxfY()
        // only relocates the insertion point, it does not affect how
        // the glyphs themselves are drawn. So, exactly as in
        // ExportSVG.exportAdvText (which needs no Y-flip at all and
        // still never flips the Y scale), no "upside-down" compensation
        // is needed here either -- group 71 bit 2 (backward) is set
        // only when the primitive itself is mirrored, bit 4 is never
        // used.
        int mirrorFlags = isMirrored ? 2 : 0;

        String style = isItalic ? "STANDARD_ITALIC" : "STANDARD";

        writeText(baseX, baseY, heightMm, widthFactor, -orientation,
            mirrorFlags, style, layer, text);
    }

    /** {@inheritDoc} */
    public void exportBezier(int x1, int y1, int x2, int y2, int x3, int y3,
        int x4, int y4, int layer, boolean arrowStart, boolean arrowEnd,
        int arrowStyle, int arrowLength, int arrowHalfWidth, int dashStyle,
        double strokeWidth)
        throws IOException
    {
        currentLayer = layer;

        double xa=x1, ya=y1, xd=x4, yd=y4;
        if (arrowStart) {
            PointPr p = exportArrow(x1, y1, x2, y2, arrowLength,
                arrowHalfWidth, arrowStyle);
            if (arrowLength>0) { xa=p.x; ya=p.y; }
        }
        if (arrowEnd) {
            PointPr p = exportArrow(x4, y4, x3, y3, arrowLength,
                arrowHalfWidth, arrowStyle);
            if (arrowLength>0) { xd=p.x; yd=p.y; }
        }

        // DXF R12 has no native Bezier/spline entity: sample the cubic
        // in Bernstein form and connect the samples as a polyline.
        double[] xs = new double[BEZIER_STEPS+1];
        double[] ys = new double[BEZIER_STEPS+1];
        for (int i=0; i<=BEZIER_STEPS; ++i) {
            double t = (double)i/BEZIER_STEPS;
            double mt = 1-t;
            double a = mt*mt*mt;
            double b = 3*mt*mt*t;
            double c = 3*mt*t*t;
            double d = t*t*t;
            xs[i] = a*xa + b*x2 + c*x3 + d*xd;
            ys[i] = a*ya + b*y2 + c*y3 + d*yd;
        }
        writePolyline(xs, ys, xs.length, false, layer, dashStyle, -1);
    }

    /** {@inheritDoc} */
    public void exportConnection(int x, int y, int layer, double size)
        throws IOException
    {
        currentLayer = layer;
        writeFilledDot(x, y, size, layer);
    }

    /** {@inheritDoc} */
    public void exportLine(double x1, double y1, double x2, double y2,
        int layer, boolean arrowStart, boolean arrowEnd, int arrowStyle,
        int arrowLength, int arrowHalfWidth, int dashStyle,
        double strokeWidth)
        throws IOException
    {
        currentLayer = layer;

        double xs=x1, ys=y1, xe=x2, ye=y2;
        if (arrowStart) {
            PointPr p = exportArrow(x1, y1, x2, y2, arrowLength,
                arrowHalfWidth, arrowStyle);
            if (arrowLength>0) { xs=p.x; ys=p.y; }
        }
        if (arrowEnd) {
            PointPr p = exportArrow(x2, y2, x1, y1, arrowLength,
                arrowHalfWidth, arrowStyle);
            if (arrowLength>0) { xe=p.x; ye=p.y; }
        }
        writeLine(xs, ys, xe, ye, layer, dashStyle);
    }

    /** {@inheritDoc} */
    public boolean exportMacro(int x, int y, boolean isMirrored,
        int orientation, String macroName, String macroDesc,
        String name, int xn, int yn, String value, int xv, int yv,
        String font, int fontSize, Map<String, MacroDesc> m)
        throws IOException
    {
        // The macro is expanded into its constituent primitives by the
        // caller, exactly as every other exporter in this project does.
        return false;
    }

    /** {@inheritDoc} */
    public void exportOval(int x1, int y1, int x2, int y2, boolean isFilled,
        int layer, int dashStyle, double strokeWidth)
        throws IOException
    {
        currentLayer = layer;

        double cx=(x1+x2)/2.0, cy=(y1+y2)/2.0;
        double rx=Math.abs(x2-x1)/2.0, ry=Math.abs(y2-y1)/2.0;
        if (rx<=0 || ry<=0) {
            return;
        }
        boolean circular = Math.abs(rx-ry) < 1e-6;

        if (isFilled && circular) {
            // A circle can be filled exactly with the same zero-length
            // wide-polyline trick used for PCB pads/connection dots.
            writeFilledDot(cx, cy, 2*rx, layer);
        } else if (circular) {
            writeCircle(cx, cy, rx, layer, dashStyle);
        } else {
            double[] xs = new double[ELLIPSE_STEPS];
            double[] ys = new double[ELLIPSE_STEPS];
            for (int i=0; i<ELLIPSE_STEPS; ++i) {
                double t = 2*Math.PI*i/ELLIPSE_STEPS;
                xs[i] = cx + rx*Math.cos(t);
                ys[i] = cy + ry*Math.sin(t);
            }
            // Filled non-circular ellipses are exported as an outline
            // only: DXF R12 has no filled-ellipse primitive short of a
            // HATCH entity, which is out of scope for this version.
            writePolyline(xs, ys, ELLIPSE_STEPS, true, layer, dashStyle, -1);
        }
    }

    /** {@inheritDoc} */
    public void exportPCBLine(int x1, int y1, int x2, int y2, int width,
        int layer)
        throws IOException
    {
        currentLayer = layer;
        double[] xs = {x1, x2};
        double[] ys = {y1, y2};
        // Unlike exportLine, the width here is semantic (a trace's
        // copper width), so it is preserved via a constant-width
        // polyline instead of being dropped.
        writePolyline(xs, ys, 2, false, layer, 0, width);
    }

    /** {@inheritDoc} */
    public void exportPCBPad(int x, int y, int style, int six, int siy,
        int indiam, int layer, boolean onlyHole)
        throws IOException
    {
        currentLayer = layer;

        if (onlyHole) {
            writeCircle(x, y, indiam/2.0, layer, 0);
            return;
        }

        switch (style) {
            case 1: // Square pad
            case 2: // Rounded square pad (corner rounding not modeled)
                {
                    double hx=six/2.0, hy=siy/2.0;
                    double[] xs = {x-hx, x+hx, x+hx, x-hx};
                    double[] ys = {y-hy, y-hy, y+hy, y+hy};
                    writeSolid(xs, ys, 4, layer);
                }
                break;
            case 0: // Oval pad
            default:
                if (six==siy) {
                    writeFilledDot(x, y, six, layer);
                } else {
                    double rx=six/2.0, ry=siy/2.0;
                    double[] xs = new double[ELLIPSE_STEPS];
                    double[] ys = new double[ELLIPSE_STEPS];
                    for (int i=0; i<ELLIPSE_STEPS; ++i) {
                        double t = 2*Math.PI*i/ELLIPSE_STEPS;
                        xs[i] = x + rx*Math.cos(t);
                        ys[i] = y + ry*Math.sin(t);
                    }
                    writePolyline(xs, ys, ELLIPSE_STEPS, true, layer, 0, -1);
                }
                break;
        }
    }

    /** {@inheritDoc} */
    public void exportPolygon(PointDouble[] vertices, int nVertices,
        boolean isFilled, int layer, int dashStyle, double strokeWidth)
        throws IOException
    {
        currentLayer = layer;

        double[] xs = new double[nVertices];
        double[] ys = new double[nVertices];
        for (int i=0; i<nVertices; ++i) {
            xs[i] = vertices[i].x;
            ys[i] = vertices[i].y;
        }

        if (isFilled && nVertices>=3 && nVertices<=4) {
            writeSolid(xs, ys, nVertices, layer);
        } else {
            // Filled polygons with more than 4 vertices are exported as
            // an outline only (see class documentation).
            writePolyline(xs, ys, nVertices, true, layer, dashStyle, -1);
        }
    }

    /** {@inheritDoc} */
    public boolean exportCurve(PointDouble[] vertices, int nVertices,
        boolean isFilled, boolean isClosed, int layer,
        boolean arrowStart, boolean arrowEnd, int arrowStyle,
        int arrowLength, int arrowHalfWidth, int dashStyle,
        double strokeWidth)
        throws IOException
    {
        // The curve is flattened into a polygon by the caller, exactly
        // as every other exporter in this project does.
        return false;
    }

    /** {@inheritDoc} */
    public void exportRectangle(int x1, int y1, int x2, int y2,
        boolean isFilled, int layer, int dashStyle, double strokeWidth)
        throws IOException
    {
        currentLayer = layer;

        double[] xs = {x1, x2, x2, x1};
        double[] ys = {y1, y1, y2, y2};
        if (isFilled) {
            writeSolid(xs, ys, 4, layer);
        } else {
            writePolyline(xs, ys, 4, true, layer, dashStyle, -1);
        }
    }

    /** {@inheritDoc} */
    public void exportImage(int x1, int y1, int x2, int y2, int layer,
        double opacity, boolean blackAndWhite, String mimeType,
        String base64Data)
        throws IOException
    {
        // DXF R12 has no raster-image entity that this hand-written
        // exporter can embed safely: draw a dashed placeholder rectangle
        // with a "[immagine]" label instead (see class documentation).
        currentLayer = layer;

        double[] xs = {x1, x2, x2, x1};
        double[] ys = {y1, y1, y2, y2};
        writePolyline(xs, ys, 4, true, layer, 1, -1);

        double cx = (x1+x2)/2.0;
        double cy = (y1+y2)/2.0;
        double heightFido = Math.abs(y2-y1)*0.1;
        if (heightFido<=0) {
            heightFido = 10;
        }
        writeText(cx, cy+heightFido/2, heightFido*MM_PER_UNIT, 1.0, 0.0,
            0, "STANDARD", layer, "[immagine]");
    }

    /** {@inheritDoc} */
    public PointPr exportArrow(double x, double y, double xc, double yc,
        double l, double h, int style)
        throws IOException
    {
        // Trigonometry identical to the other exporters (see e.g.
        // ExportPGF.exportArrow): compute the direction from (xc,yc) to
        // (x,y), then place the two "wing" points of the arrowhead.
        double alpha;
        if (x==xc) {
            alpha = Math.PI/2.0 + (y-yc<0?0:Math.PI);
        } else {
            alpha = Math.atan((y-yc)/(x-xc));
        }
        alpha += x-xc>0?0:Math.PI;

        double x0 = x - l*Math.cos(alpha);
        double y0 = y - l*Math.sin(alpha);
        double x1 = x0 - h*Math.sin(alpha);
        double y1 = y0 + h*Math.cos(alpha);
        double x2 = x0 + h*Math.sin(alpha);
        double y2 = y0 - h*Math.cos(alpha);

        // Arrowheads are always drawn with a continuous (non-dashed)
        // outline, regardless of the owning line/curve's dash style.
        if ((style & Arrow.flagEmpty) == 0) {
            double[] xs = {x, x1, x2};
            double[] ys = {y, y1, y2};
            writeSolid(xs, ys, 3, currentLayer);
        } else {
            double[] xsA = {x, x1};
            double[] ysA = {y, y1};
            writePolyline(xsA, ysA, 2, false, currentLayer, 0, -1);
            double[] xsB = {x, x2};
            double[] ysB = {y, y2};
            writePolyline(xsB, ysB, 2, false, currentLayer, 0, -1);
        }

        if ((style & Arrow.flagLimiter) != 0) {
            double x3 = x - h*Math.sin(alpha);
            double y3 = y + h*Math.cos(alpha);
            double x4 = x + h*Math.sin(alpha);
            double y4 = y - h*Math.cos(alpha);
            double[] xs = {x3, x4};
            double[] ys = {y3, y4};
            writePolyline(xs, ys, 2, false, currentLayer, 0, -1);
        }

        return new PointPr(x0, y0);
    }

    // ------------------------------------------------------------------
    // DXF section writers
    // ------------------------------------------------------------------

    private void writeHeader() throws IOException
    {
        double wMm = totalSize.width*MM_PER_UNIT;
        double hMm = totalSize.height*MM_PER_UNIT;

        // Group code 999 is a plain comment: valid anywhere in a DXF
        // file and ignored by every reader, used here only for
        // provenance (matching the "Created by FidoCadJ..." banner
        // every other exporter writes).
        gc(999, "Created by FidoCadJ ver. "+Globals.version
            +", DXF export filter");

        out.write("0\nSECTION\n2\nHEADER\n");
        gc(9, "$ACADVER");
        gc(1, "AC1009");
        gc(9, "$INSBASE");
        gc(10, 0.0); gc(20, 0.0); gc(30, 0.0);
        gc(9, "$EXTMIN");
        gc(10, 0.0); gc(20, 0.0); gc(30, 0.0);
        gc(9, "$EXTMAX");
        gc(10, wMm); gc(20, hMm); gc(30, 0.0);
        out.write("0\nENDSEC\n");
    }

    private void writeTables() throws IOException
    {
        out.write("0\nSECTION\n2\nTABLES\n");

        // LTYPE table: CONTINUOUS plus one custom entry per FidoCadJ
        // dash style (indices 1..Globals.dashNumber-1).
        out.write("0\nTABLE\n2\nLTYPE\n");
        gc(70, Globals.dashNumber);
        out.write("0\nLTYPE\n2\nCONTINUOUS\n70\n0\n3\nSolid line\n"
            +"72\n65\n73\n0\n40\n0.0\n");
        for (int i=1; i<Globals.dashNumber; ++i) {
            double[] seg = dashSegmentsMm[i];
            double total = 0;
            for (double s : seg) {
                total += Math.abs(s);
            }
            out.write("0\nLTYPE\n2\nFCJDASH"+i+"\n70\n0\n"
                +"3\nFidoCadJ dash style "+i+"\n");
            gc(72, 65);
            gc(73, seg.length);
            gc(40, total);
            for (double s : seg) {
                gc(49, s);
            }
        }
        out.write("0\nENDTAB\n");

        // LAYER table.
        out.write("0\nTABLE\n2\nLAYER\n");
        gc(70, layerV.size());
        for (int i=0; i<layerV.size(); ++i) {
            out.write("0\nLAYER\n");
            gc(2, layerName[i]);
            gc(70, 0);
            gc(62, layerAci[i]);
            gc(6, "CONTINUOUS");
        }
        out.write("0\nENDTAB\n");

        // STYLE table: a generic style plus an obliqued variant used for
        // italic text (see the class documentation for why bold/font
        // name are not reproduced).
        out.write("0\nTABLE\n2\nSTYLE\n");
        gc(70, 2);
        writeStyle("STANDARD", 0.0);
        writeStyle("STANDARD_ITALIC", 15.0);
        out.write("0\nENDTAB\n");

        out.write("0\nENDSEC\n");
    }

    private void writeStyle(String name, double obliqueAngle)
        throws IOException
    {
        out.write("0\nSTYLE\n");
        gc(2, name);
        gc(70, 0);
        gc(40, 0.0);
        gc(41, 1.0);
        gc(50, obliqueAngle);
        gc(71, 0);
        gc(42, 2.5);
        gc(3, "txt");
        gc(4, "");
    }

    private void buildLayerTables()
    {
        int n = layerV.size();
        layerAci = new int[n];
        layerName = new String[n];
        for (int i=0; i<n; ++i) {
            LayerDesc l = layerV.get(i);
            ColorInterface c = l.getColor();
            int aci = nearestAci(c.getRed(), c.getGreen(), c.getBlue());
            if (aci < 1) {
                aci = 1;
            }
            // A negative LAYER color value is the standard DXF
            // convention for "this layer is off".
            layerAci[i] = l.isVisible() ? aci : -aci;
            layerName[i] = sanitizeLayerName(i, l.getDescription());
        }
    }

    private static String sanitizeLayerName(int index, String description)
    {
        StringBuilder sb = new StringBuilder();
        String d = description==null ? "" : description;
        for (int i=0; i<d.length() && sb.length()<24; ++i) {
            char ch = d.charAt(i);
            if ((ch>='A' && ch<='Z') || (ch>='a' && ch<='z')
                || (ch>='0' && ch<='9') || ch=='_' || ch=='$' || ch=='-')
            {
                sb.append(ch);
            } else {
                sb.append('_');
            }
        }
        return String.format(Locale.ROOT, "L%02d_%s", index, sb.toString());
    }

    private static int nearestAci(int r, int g, int b)
    {
        int best = 7;
        long bestDist = Long.MAX_VALUE;
        for (int[] e : ACI_RGB) {
            long dr = e[1]-r, dg = e[2]-g, db = e[3]-b;
            long dist = dr*dr + dg*dg + db*db;
            if (dist < bestDist) {
                bestDist = dist;
                best = e[0];
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Entity writers
    // ------------------------------------------------------------------

    private void writeLayerAndLinetype(int layer, int dashStyle)
        throws IOException
    {
        gc(8, layerName[layer]);
        if (dashStyle != 0) {
            gc(6, "FCJDASH"+dashStyle);
        }
    }

    private void writeLine(double x1, double y1, double x2, double y2,
        int layer, int dashStyle) throws IOException
    {
        out.write("0\nLINE\n");
        writeLayerAndLinetype(layer, dashStyle);
        gc(10, dxfX(x1)); gc(20, dxfY(y1)); gc(30, 0.0);
        gc(11, dxfX(x2)); gc(21, dxfY(y2)); gc(31, 0.0);
    }

    private void writeCircle(double cx, double cy, double rFido, int layer,
        int dashStyle) throws IOException
    {
        out.write("0\nCIRCLE\n");
        writeLayerAndLinetype(layer, dashStyle);
        gc(10, dxfX(cx)); gc(20, dxfY(cy)); gc(30, 0.0);
        gc(40, rFido*MM_PER_UNIT);
    }

    /** Write a POLYLINE/VERTEX.../SEQEND entity.
        @param xs x coordinates, in FidoCadJ logical units.
        @param ys y coordinates, in FidoCadJ logical units.
        @param n number of vertices to write.
        @param closed true if the polyline should be closed.
        @param layer the DXF layer index.
        @param dashStyle the FidoCadJ dash style (0 = solid).
        @param widthFido constant width in FidoCadJ logical units, or a
            non-positive value to omit the width (hairline).
    */
    private void writePolyline(double[] xs, double[] ys, int n,
        boolean closed, int layer, int dashStyle, double widthFido)
        throws IOException
    {
        out.write("0\nPOLYLINE\n");
        writeLayerAndLinetype(layer, dashStyle);
        gc(66, 1);
        gc(70, closed ? 1 : 0);
        if (widthFido > 0) {
            double wmm = widthFido*MM_PER_UNIT;
            gc(40, wmm);
            gc(41, wmm);
        }
        for (int i=0; i<n; ++i) {
            out.write("0\nVERTEX\n");
            gc(8, layerName[layer]);
            gc(10, dxfX(xs[i])); gc(20, dxfY(ys[i])); gc(30, 0.0);
        }
        out.write("0\nSEQEND\n");
    }

    private void writeFilledDot(double x, double y, double diameterFido,
        int layer) throws IOException
    {
        // A POLYLINE whose two vertices coincide, given a constant
        // width equal to the wanted diameter, renders as a filled disc
        // -- the same trick real PCB/CAM tools use for round pads.
        double[] xs = {x, x};
        double[] ys = {y, y};
        writePolyline(xs, ys, 2, false, layer, 0, diameterFido);
    }

    /** Write a SOLID entity (a filled triangle or quadrilateral).
        @param xs x coordinates, in FidoCadJ logical units (3 or 4).
        @param ys y coordinates, in FidoCadJ logical units (3 or 4).
        @param n number of points (3 or 4).
        @param layer the DXF layer index.
    */
    private void writeSolid(double[] xs, double[] ys, int n, int layer)
        throws IOException
    {
        out.write("0\nSOLID\n");
        gc(8, layerName[layer]);
        // DXF SOLID takes 4 corners but the 3rd/4th are given in
        // "crossed" order (P1,P2,P4,P3), not simple polygon winding
        // order, or the fill renders as a bowtie. A triangle is
        // represented by repeating the 3rd point as the 4th.
        int i2 = n>=4 ? 3 : 2;
        gc(10, dxfX(xs[0])); gc(20, dxfY(ys[0])); gc(30, 0.0);
        gc(11, dxfX(xs[1])); gc(21, dxfY(ys[1])); gc(31, 0.0);
        gc(12, dxfX(xs[i2])); gc(22, dxfY(ys[i2])); gc(32, 0.0);
        gc(13, dxfX(xs[2])); gc(23, dxfY(ys[2])); gc(33, 0.0);
    }

    private void writeText(double xFido, double yFido, double heightMm,
        double widthFactor, double rotationDeg, int mirrorFlags,
        String style, int layer, String text) throws IOException
    {
        out.write("0\nTEXT\n");
        gc(8, layerName[layer]);
        gc(10, dxfX(xFido)); gc(20, dxfY(yFido)); gc(30, 0.0);
        gc(40, heightMm);
        gc(1, sanitizeText(text));
        gc(7, style);
        if (rotationDeg != 0.0) {
            gc(50, rotationDeg);
        }
        if (Math.abs(widthFactor-1.0) > 1e-6) {
            gc(41, widthFactor);
        }
        gc(71, mirrorFlags);
    }

    private static String sanitizeText(String s)
    {
        if (s==null) {
            return "";
        }
        // A DXF TEXT entity is single-line: a literal newline inside a
        // group value would corrupt the strict alternating-line format.
        return s.replace("\r", " ").replace("\n", " ");
    }

    // ------------------------------------------------------------------
    // Coordinate mapping and low-level group-code output
    // ------------------------------------------------------------------

    private double dxfX(double xFido)
    {
        return xFido * MM_PER_UNIT;
    }

    private double dxfY(double yFido)
    {
        // DXF, like Postscript/PDF, is Y-up; FidoCadJ/MapCoordinates is
        // Y-down, so every Y coordinate is flipped against the drawing
        // height.
        return (totalSize.height - yFido) * MM_PER_UNIT;
    }

    private void gc(int code, String value) throws IOException
    {
        out.write(code+"\n"+value+"\n");
    }

    private void gc(int code, int value) throws IOException
    {
        out.write(code+"\n"+value+"\n");
    }

    private void gc(int code, double value) throws IOException
    {
        out.write(code+"\n"+fmt(value)+"\n");
    }

    private static String fmt(double v)
    {
        // DXF requires a '.' decimal separator regardless of the JVM's
        // default locale.
        return String.format(Locale.ROOT, "%.4f", v);
    }
}
