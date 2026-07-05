package fidocadj;

import javax.swing.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.awt.print.*;
import java.awt.*;
import java.util.*;
import java.awt.geom.*;

import fidocadj.circuit.CircuitPanel;
import fidocadj.dialogs.print.DialogPrint;
import fidocadj.globals.Globals;
import fidocadj.geom.MapCoordinates;
import fidocadj.geom.DrawingSize;
import fidocadj.graphic.DimensionG;
import fidocadj.graphic.PointG;
import fidocadj.graphic.swing.Graphics2DSwing;
import fidocadj.graphic.swing.ColorSwing;
import fidocadj.layers.LayerDesc;

/** PrintTools.java

    Class performing interface operations for launching print operations.
    It also reads and stores preferences.

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

    Copyright 2015-2023 by Davide Bucci
    </pre>

    @author Davide Bucci
*/

public class PrintTools implements Printable
{
    // Settings related to the printing modes.
    private boolean printMirror;        // Mirror the printed output.
    private boolean printFitToPage;     // Fit to the page.
    private boolean printLandscape;     // Put the page in the landscape mode.
    private boolean printBlackWhite;    // Black and white.
    private int currentLayerSelected;   // Layer to print. If negative=all.

    // Margins
    private double topMargin=-1;
    private double bottomMargin=-1;
    private double leftMargin=-1;
    private double rightMargin=-1;

    private CircuitPanel cc;

    private boolean showMargins;

    private final static double MULT=16.0;       // Multiplying 72dp times MULT
    private final static double INCH=2.54;  // in cm
    private final static double NATIVERES=72.0;  // in dpi

    /** Standard constructor.
    */
    public PrintTools()
    {
        // some standard configurations
        printMirror = false;
        printFitToPage = false;
        printLandscape = false;
        adjustMargins(PrinterJob.getPrinterJob().defaultPage());
        showMargins = false;
        currentLayerSelected=-1;
    }

    /** If the values of the margins are negative, they will be adjusted
        in such a way that the printable area will be covered in the most
        efficient way.
    */
    private void adjustMargins(PageFormat pf)
    {
        final double correction=0.01;
        // Start of the imageable region, in centimeters.
        if(leftMargin<0.0) {
            leftMargin=pf.getImageableX()/NATIVERES*INCH+correction;
        }
        if(topMargin<0.0) {
            topMargin=pf.getImageableY()/NATIVERES*INCH+correction;
        }
        if(rightMargin<0.0) {
            rightMargin=(pf.getWidth()-pf.getImageableX()
                -pf.getImageableWidth())/NATIVERES*INCH+correction;
        }
        if(bottomMargin<0.0) {
            bottomMargin=(pf.getHeight()-pf.getImageableY()
                -pf.getImageableHeight())/NATIVERES*INCH+correction;
        }
    }

    /** Determine if the margins should be shown or not.
        @param sm true if the margins should be shown (for example, in a
            print preview operation).
    */
    public void setShowMargins(boolean sm)
    {
        showMargins=sm;
    }

    /** Set the size of the margins, in centimeters. The orientation of those
        margins should correspond to the page in the portrait orientation.
        @param tm top margin.
        @param bm bottom margin.
        @param lm left margin.
        @param rm right margin.
    */
    public void setMargins(double tm, double bm, double lm, double rm)
    {
        if(tm>0.0) { topMargin=tm; }
        if(bm>0.0) { bottomMargin=bm; }
        if(lm>0.0) { leftMargin=lm; }
        if(rm>0.0) { rightMargin=rm; }
    }
    /** Associate to a given CircuitPanel containing the circuit to be printed.
        @param rCC the CircuitPanel containing the drawing to be exported.
    */
    public void associateToCircuitPanel(CircuitPanel rCC)
    {
        cc=rCC;
    }

    /** Show a dialog for printing the current drawing.
        @param fff the parent frame which will be used for dialogs and message
            boxes.
    */
    public void printDrawing(JFrame fff)
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pp = job.defaultPage();

        DialogPrint dp=new DialogPrint(fff, cc.getDrawingModel(), pp);
        dp.setMirror(printMirror);
        dp.setFit(printFitToPage);
        dp.setBW(printBlackWhite);
        dp.setLandscape(printLandscape);

        dp.setMaxMargins(pp.getWidth()/NATIVERES*INCH,
            pp.getHeight()/NATIVERES*INCH);
        dp.setMargins(topMargin, bottomMargin, leftMargin, rightMargin);

        boolean noexit;
        do {
            // Show the (modal) dialog.
            dp.setVisible(true);
            noexit=configurePrinting(dp, pp,true);
            if (!dp.shouldPrint()) {
                return;
            }
        } while (noexit);
        try {
            // Launch printing.
            executePrinting(job);
        } catch (PrinterException ex) {
            // The job did not successfully complete
            JOptionPane.showMessageDialog(fff,
                Globals.messages.getString("Print_uncomplete"));
        }
    }

    /** Configure the printing object by reading the current settings of
        the DialogPrint employed for the user interaction.
        @param dp the printing dialog.
        @param pp the standard page format.
        @param checkMarginSize if true, the size of the margins is checked
            and an error message is issued in case of problems.
        @return true if the printing operation should not be done and the
            dialog should be shown again.
    */
    public boolean configurePrinting(DialogPrint dp, PageFormat pp,
        boolean checkMarginSize)
    {
        boolean noexit=false;

        if (!dp.shouldPrint() && checkMarginSize) {
            return true;
        }

        // Get some information about the printing options.
        printMirror = dp.getMirror();
        printFitToPage = dp.getFit();
        printLandscape = dp.getLandscape();
        printBlackWhite=dp.getBW();

        try {
            topMargin=dp.getTMargin();
            bottomMargin=dp.getBMargin();
            leftMargin=dp.getLMargin();
            rightMargin=dp.getRMargin();
        } catch (NumberFormatException n) {
            System.out.println(
                Globals.messages.getString("Format_invalid"));
        }

        if(checkMarginSize && (topMargin/INCH*NATIVERES<pp.getImageableY() ||
            bottomMargin/INCH*NATIVERES<pp.getHeight()
                -pp.getImageableHeight()-pp.getImageableY() ||
            leftMargin/INCH*NATIVERES<pp.getImageableX() ||
            rightMargin/INCH*NATIVERES<pp.getWidth()
                -pp.getImageableWidth()-pp.getImageableX()))
        {
            int answer = JOptionPane.showConfirmDialog(dp,
                Globals.messages.getString("Print_outside_regions"),
                "",JOptionPane.YES_NO_OPTION);
            if(answer!= JOptionPane.YES_OPTION) {
                noexit=true;
            }
        }
        currentLayerSelected=dp.getSingleLayerToPrint();
        // Deselect all elements.
        cc.getSelectionActions().setSelectionAll(false);
        return noexit;
    }

    /** Low level printing operations.
        @param job the current printing job.
    */
    private void executePrinting(PrinterJob job)
        throws PrinterException
    {
        job.setPrintable(this);
        if (job.printDialog()) {
            PrintRequestAttributeSet aset = new
                HashPrintRequestAttributeSet();
            // Set the correct printing orientation.
            if (printLandscape) {
                aset.add(OrientationRequested.LANDSCAPE);
            } else {
                aset.add(OrientationRequested.PORTRAIT);
            }
            job.print(aset);
        }
    }

    /** The printing interface (prints one page).
        @param g the graphic context.
        @param pf the page format.
        @param page the page number.
        @return PAGE_EXISTS if the page has to be printed.
        @throws PrinterException if a printing error occurs.
    */
    @Override public int print(Graphics g, PageFormat pf, int page) throws
        PrinterException
    {
        // This is not a "real" margin, but just a tiny amount which ensures
        // that even when the calculations are rounded up, the printout does
        // not span erroneously over multiple pages.
        int security=5;

        // This might be explained as follows:
        // 1 - The Java printing system normally works with an internal
        // resolution which is 72 dpi (probably inspired by Postscript).
        // 2 - To have a sufficient resolution, this is increased by 16 times,
        // by using the scale method of the graphic object associated to the
        // printer. This gives a 72 dpi * 16=1152 dpi resolution.
        // 3 - The 0.127 mm pitch used in FidoCadJ corresponds to a 200 dpi
        // resolution. Calculating 1152 dpi / 200 dpi gives the 5.76 constant
        // for the zoom.

        double xscale = 1.0/MULT; // Set 1152 logical units for an inch
        double yscale = 1.0/MULT; // as the standard resolution is 72
        double zoom = NATIVERES*MULT/200.0;// in a 1152 dpi resolution is 1:1
        double shownWidth;        // Printed region (taking into account
        double shownHeight;       // margins).

        Graphics2D g2d = (Graphics2D)g;
        AffineTransform oldTransform = g2d.getTransform();

        // Mark with a light red the unprintable area of the sheet.
        if(showMargins) {
            g2d.setColor(new Color(255,200,200));
            g2d.fillRect(0,0, (int)pf.getImageableX(), (int)pf.getHeight());
            g2d.fillRect(0,0, (int)pf.getWidth(), (int)pf.getImageableY());
            g2d.fillRect((int)(pf.getImageableX()+pf.getImageableWidth()),
                0, (int)pf.getImageableX(), (int)pf.getHeight());
            g2d.fillRect(0, (int)(pf.getImageableY()+pf.getImageableHeight()),
                (int)pf.getWidth(), (int)(pf.getHeight()-
                    pf.getImageableHeight()-pf.getImageableY()));
        }

        // User (0,0) is typically outside the imageable area, so we must
        // translate by the X and Y values in the PageFormat to avoid clipping,
        // taking into account the margins which are needed.
        if (printMirror) {
            g2d.translate(pf.getWidth()-rightMargin/INCH*NATIVERES,
                topMargin/INCH*NATIVERES);
            g2d.scale(-xscale,yscale);
        } else {
            g2d.translate(leftMargin/INCH*NATIVERES, topMargin/INCH*NATIVERES);
            g2d.scale(xscale,yscale);
        }

        shownWidth=(pf.getWidth()-
            (leftMargin+rightMargin)/INCH*NATIVERES)*MULT;
        shownHeight=(pf.getHeight()-
            (topMargin+bottomMargin)/INCH*NATIVERES)*MULT;

        // The current margins are shown with a dashed black line.
        Rectangle2D.Double border = new Rectangle2D.Double(0, 0,
                shownWidth-2*security, shownHeight-2*security);
        if(showMargins) {
            float dashBorder[] = {150.0f};
            BasicStroke dashed = new BasicStroke(50.0f,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER,
                        10.0f, dashBorder, 0.0f);
            g2d.setStroke(dashed);
            g2d.setColor(Color.black);
            g2d.draw(border);
        }
        // Clip the drawing inside the borders.
        g2d.clip(border);
        MapCoordinates m;

        // Perform an adjustement if we need to fit the drawing to the page.
        if (printFitToPage) {
            MapCoordinates n = DrawingSize.calculateZoomToFit(
                cc.getDrawingModel(),
                (int)((pf.getWidth()-(leftMargin+rightMargin)
                    /INCH*NATIVERES)*MULT)
                    -2*security,
                (int)((pf.getHeight()-(topMargin+bottomMargin)
                    /INCH*NATIVERES)*MULT)
                    -2*security,
                true);
            zoom=n.getXMagnitude();
        }
        m=new MapCoordinates();
        m.setMagnitudes(zoom, zoom);

        PointG o=new PointG(0,0);

        DimensionG dim = DrawingSize.getImageSize(
            cc.getDrawingModel(), zoom, true, o);
        int imageWidth = dim.width;
        int imageHeight = dim.height;

        // Calculate how many pages are needed in the horisontal and in the
        // vertical dimensions. The printout will be organized as a mosaic.
        int npagesx = (int)Math.ceil(imageWidth/shownWidth);
        int npagesy = (int)Math.ceil(imageHeight/shownHeight);
        // Calculate the total number of pages.
        int npages=npagesx*npagesy;

        // Current pages of the mosaic.
        int pagex=page % npagesx;
        int pagey=page / npagesx;

        if(printFitToPage) {
            g2d.translate(-o.x,-o.y);
        }

        // Check if printing is finished.
        if(page>=npages) {
            g2d.setTransform(oldTransform);
            return NO_SUCH_PAGE;
        }
        // Check if we need more than one page
        if (page>0) {
            g2d.translate(-(shownWidth*pagex),0);
            g2d.translate(0,-(shownHeight*pagey));
        }

        java.util.List<LayerDesc> ol=cc.getDrawingModel().getLayers();
        // Check if only one layer should be printed.
        if(currentLayerSelected>=0) {
            cc.getDrawingModel().setDrawOnlyLayer(currentLayerSelected);
        }
        // Check if the drawing should be black and white
        if(printBlackWhite) {
            java.util.List<LayerDesc> v=new Vector<LayerDesc>();

            // Here we create an alternative array of layers in
            // which all colors are pitch black. This may be
            // useful for PCB's.

            for (int i=0; i<LayerDesc.MAX_LAYERS;++i) {
                v.add(new LayerDesc(new ColorSwing(Color.black),
                    (ol.get(i)).isVisible(),
                     "B/W",(ol.get(i)).getAlpha()));
            }
            cc.getDrawingModel().setLayers(v);
        }
        Graphics2DSwing graphicSwing = new Graphics2DSwing(g2d);
        // This is important for taking into account the dashing size
        graphicSwing.setZoom(m.getXMagnitude());
        // Now we perform our rendering
        cc.drawingAgent.draw(graphicSwing, m);
        if(currentLayerSelected>=0) {
            cc.getDrawingModel().setDrawOnlyPads(true);
            cc.drawingAgent.draw(new Graphics2DSwing(g2d), m);
            cc.getDrawingModel().setDrawOnlyPads(false);
            cc.getDrawingModel().setDrawOnlyLayer(-1);
        }
        cc.getDrawingModel().setLayers(ol);
        g2d.setTransform(oldTransform);
        /* tell the caller that this page is part of the printed document */
        return PAGE_EXISTS;
    }
}
