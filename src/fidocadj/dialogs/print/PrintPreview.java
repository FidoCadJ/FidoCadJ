package fidocadj.dialogs.print;

import java.io.*;
import java.awt.*;
import java.awt.print.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import javax.swing.border.EtchedBorder;     // NOPMD (bug in PMD 7.0?)
import javax.swing.BorderFactory;           // NOPMD (bug in PMD 7.0?)

import fidocadj.circuit.CircuitPanel;
import fidocadj.PrintTools;

/** Shows a print preview.

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
public final class PrintPreview extends CircuitPanel
    implements ComponentListener
{
    private static final long serialVersionUID = 1L;
    private final PageFormat pageDescription;
    private double topMargin;
    private double bottomMargin;
    private double leftMargin;
    private double rightMargin;
    private BufferedImage pageImage;
    private final PrintTools printObject;
    private final DialogPrint dialog;
    private int currentPage;
    private double oldBaseline;

    /** Constructor.
        @param isEditable true if the panel should be editable.
        @param p the PageFormat description.
        @param ddp the DialogPrint object to communicate with.
    */
    public PrintPreview(boolean isEditable, PageFormat p, DialogPrint ddp)
    {
        super(isEditable);
        pageDescription=p;
        currentPage=0;
        dialog=ddp;
        setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        setGridVisibility(false);
        addComponentListener(this);
        int width=200;
        int height=320;
        pageImage = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_RGB);
        printObject=new PrintTools();
        printObject.associateToCircuitPanel(this);
        printObject.setShowMargins(true);

        Graphics2D g2=pageImage.createGraphics();
        g2.setColor(Color.white);
        g2.fillRect(0,0,width,height);
        g2.scale(1.0/160,1.0/160);
        try {
            printObject.print(g2, pageDescription, 0);
        } catch (PrinterException pe)
        {
            System.err.println("Some problem here!");
        }
    }

    /** By implementing writeObject method,
    // we can prevent
    // subclass from serialization
    */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        throw new NotSerializableException();
    }

    /* By implementing readObject method,
    // we can prevent
    // subclass from de-serialization
    */
    private void readObject(ObjectInputStream in) throws IOException
    {
        throw new NotSerializableException();
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
        topMargin=tm;
        bottomMargin=bm;
        leftMargin=lm;
        rightMargin=rm;
    }

    /** Set the current page to be printed.
        @param p the page to be printed.
        @return the number of the selected page (it may differ from the one
            which is passed as an argument, since a sanity check is done).
    */
    public int setCurrentPage(int p)
    {
        pageImage = new BufferedImage(10, 10,
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g2=pageImage.createGraphics();

        try {
            if (printObject.print(g2, pageDescription, p)
                ==Printable.PAGE_EXISTS)
            {
                currentPage=p;
            } else {
                currentPage=0;
            }
        } catch (PrinterException pe) {
            currentPage=0;
        }
        return currentPage;
    }

    /** Show the margins.
        @param g the graphic context where to draw.
    */
    @Override
    public void paintComponent(Graphics g)
    {
        getDrawingModel().setChanged(true); // Needed?
        Color c = g.getColor();
        Graphics2D g2d = (Graphics2D) g;

        int shadowShiftX=4;
        int shadowShiftY=4;

        double baseline=getWidth()*0.6;
        if(Math.abs(oldBaseline-baseline)>1e5) {   /// TODO check -> 1e-5 !!!
            updatePreview();
        }
        double ratio=pageDescription.getHeight()/pageDescription.getWidth();

        if(dialog.getLandscape()) {
            baseline=getWidth()*0.8;
            pageDescription.setOrientation(PageFormat.LANDSCAPE);
        } else {
            pageDescription.setOrientation(PageFormat.PORTRAIT);
        }
        // Draw the background.
        g2d.setColor(getBackground());
        g2d.fillRect(0,0,getWidth(), getHeight());

        // Draw the shadow of the page.
        g2d.setColor(Color.gray.darker());
        g2d.fillRect((int)Math.round(getWidth()/2.0-baseline/2.0)+shadowShiftX,
            (int)Math.round(getHeight()/2.0-baseline*ratio/2.0)+shadowShiftY,
            (int)Math.round(baseline),
            (int)Math.round(baseline*ratio));
        // Draw the image containing the preview.
        g2d.drawImage(pageImage,
            (int)Math.round(getWidth()/2.0-baseline/2.0),
            (int)Math.round(getHeight()/2.0-baseline*ratio/2.0),
            null);

        // Draw the contour of the page.
        g2d.setColor(Color.black);
        g2d.drawRect((int)Math.round(getWidth()/2.0-baseline/2.0),
            (int)Math.round(getHeight()/2.0-baseline*ratio/2.0),
            (int)Math.round(baseline)-1,
            (int)Math.round(baseline*ratio));

        g2d.setColor(c);
    }

    /** Called when the panel is resized.
        TODO: this is not very memory efficient, since an image is created
        each time the panel is resized.
        @param e the event descriptor.
    */
    @Override public void componentResized(ComponentEvent e)
    {
        updatePreview();
    }

    /** Update the printing preview by calculating again the image
        containing it. It will be shown at the following repaint
        operation.
    */
    public void updatePreview()
    {
        printObject.configurePrinting(dialog, pageDescription, false);

        double baseline=getWidth()*0.6;
        double pageWidth=pageDescription.getWidth();
        double pageHeight=pageDescription.getHeight();
        double ratio=pageHeight/pageWidth;

        if(dialog.getLandscape()) {
            baseline=getWidth()*0.8;
            pageDescription.setOrientation(PageFormat.LANDSCAPE);
        } else {
            pageDescription.setOrientation(PageFormat.PORTRAIT);
        }

        int width=(int)baseline;
        int height=(int)Math.round(baseline*ratio);

        if(width<1) {
            width=1;
        }
        if(height<1) {
            height=1;
        }

        pageImage = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d=pageImage.createGraphics();
        AffineTransform oldTransform = g2d.getTransform();

        // Activate anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.white);
        g2d.fillRect(0,0,width,height);

        g2d.scale(width/pageWidth,
            height/pageHeight);

        try {
            printObject.setMargins(topMargin, bottomMargin,
                leftMargin, rightMargin);
            printObject.print(g2d, pageDescription, currentPage);
        } catch (PrinterException pe)
        {
            System.err.println("Some problem here!");
        }
        g2d.setTransform(oldTransform);
        oldBaseline=baseline;
    }

    /** Get the total number of pages in the preview.
        @return the number of pages.
    */
    public int getTotalNumberOfPages()
    {
        int numpages=0;
        pageImage = new BufferedImage(10, 10,
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g2=pageImage.createGraphics();

        try {
            while(printObject.print(g2, pageDescription, numpages)
                ==Printable.PAGE_EXISTS)
            {
                ++numpages;
            }
        } catch (PrinterException pe) {
            System.err.println("Some problems when trying to print.");
        }

        return numpages;
    }

    /** Called when the panel is hidden.
        @param e the event descriptor.
    */
    @Override public void componentHidden(ComponentEvent e)
    {
        // Nothing to do here
    }

    /** Called when the panel is moved.
        @param e the event descriptor.
    */
    @Override public void componentMoved(ComponentEvent e)
    {
        // Nothing to do here
    }

    /** Called when the panel is shown.
        @param e the event descriptor.
    */
    @Override public void componentShown(ComponentEvent e)
    {
        // Nothing to do here
    }
}
