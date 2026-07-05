package fidocadj.dialogs.controls;


import fidocadj.circuit.CircuitPanel;
import fidocadj.globals.Globals;

import java.io.*;
import java.awt.*;

/** The class OriginCircuitPanel extends the CircuitPanel class by adding
    coordinate axis which can be moved.

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

    Copyright 2012-2026 Phylum2, Davide Bucci
    </pre>
    @author Phylum2, Davide Bucci

*/
public final class OriginCircuitPanel extends CircuitPanel
{
    private static final long serialVersionUID = 1L;
    final float dash1[] = {2.0f};
    final BasicStroke dashed = new BasicStroke(1.0f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER,
                            1.0f, dash1, 1.0f);

    // x and y coordinates of the origin in pixel.
    private int dx = 20;
    private int dy = 20;

    // x and y coordinates of the origin in logical units.
    // TODO: improve data encapsulation (these should be private).
    private float xl=5;
    private float yl=5;

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

    /** Get the x coordinate of the origin in pixels.
     * @return the x coordinate of the origin in pixel.
    */
    public int getDx()
    {
        return dx;
    }

    /** Get the y coordinate of the origin in pixels.
     * @return the y coordinate of the origin in pixel.
    */
    public int getDy()
    {
        return dy;
    }

    /** Get the x coordinates of the origin in logical units.
     * @return the x coordinate of the origin in logical units.
    */
    public float getLx()
    {
        return xl;
    }

    /** Get the y coordinates of the origin in logical units.
     * @return the y coordinate of the origin in logical units.
    */
    public float getLy()
    {
        return yl;
    }

    /** Set the x coordinates of the origin in logical units.
     * @param xl coordinates of the origin in logical units.
    */
    public void setLx(float xl)
    {
        this.xl = xl;
    }

    /** Set the y coordinates of the origin in logical units.
     * @param yl coordinates of the origin in logical units.
    */
    public void setLy(float yl)
    {
        this.yl = yl;
    }


    /** Put the origin in the 10,10 logical coordinates.
    */
    public void resetOrigin()
    {
        xl=getMapCoordinates().unmapXsnap(10);
        yl=getMapCoordinates().unmapYsnap(10);

        dx=getMapCoordinates().mapXi(xl,yl,false);
        dy=getMapCoordinates().mapYi(xl,yl,false);
    }

    /** Set the new x coordinate of the origin.
        @param dx the new x coordinates in pixels.
    */
    public void setDx(int dx)
    {
        if (dx < 0 || dx>getWidth()) {
            return;
        }
        this.dx = dx;
    }

    /** Set the new y coordinate of the origin.
        @param dy the new y coordinates in pixels.
    */
    public void setDy(int dy)
    {
        if (dy<0 || dy>getHeight()) {
            return;
        }
        this.dy = dy;
    }

    /** Constructor.
        @param isEditable true if the panel should be editable.
    */
    public OriginCircuitPanel(boolean isEditable)
    {
        super(isEditable);
    }

    /** Show a red cross with dashed line and write "origin" near to the
        center. This should suggest to the user that it is worth clicking
        in the origin panel (some users reported they did not see the
        cross alone in a first instance).
    */
    @Override
    public void paintComponent (Graphics g)
    {
        super.paintComponent(g);
        Color c = g.getColor();
        Graphics2D g2 = (Graphics2D) g;
        g.setColor(Color.red);
        Stroke t=g2.getStroke();
        g2.setStroke(dashed);
        // Show the origin of axes (red cross)
        g.drawLine(dx, 0, dx, getHeight()); // y
        g.drawLine(0, dy, getWidth(), dy); // x

        Font f=new Font("Helvetica",0,12);
        FontMetrics fm = g.getFontMetrics(f);
        int h = fm.getAscent();
        int th = h+fm.getDescent();

        g.drawString(Globals.messages.getString("Origin"),
            dx+5, dy+th+2);
        g.setColor(c);
        g2.setStroke(t);
    }
}
