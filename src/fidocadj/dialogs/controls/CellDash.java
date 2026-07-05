package fidocadj.dialogs.controls;

import java.io.*;
import java.awt.*;
import javax.swing.*;

import fidocadj.globals.Globals;

/** The class CellArrow is a simple panel showing the dash style
    characteristics.
    To be used with ArrowCellRenderer.

    @author Davide Bucci

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
    </pre>

    Copyright 2009-2023 by Davide Bucci
*/
public class CellDash extends JPanel
{
    private static final long serialVersionUID = 1L;
    private final boolean isSelected;
    private final DashInfo dash;
    private final JList list;


    /** Constructor. The user should provide the list in which the element is
        used, information about the dashing style as well as the selection
        state

        @param la the dashing style to be used
        @param l the JList in which the element is used
        @param is the selection state which will be used for the background
    */
    CellDash(DashInfo la,JList l, boolean is)
    {
        dash=la;
        list=l;
        isSelected=is;
        setPreferredSize(new Dimension(50,18));
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

    /** This routine is called by the callback system when there is the need
        to draw on the screen the element.
        @param g the graphic context on which to draw.
    */
    @Override public void paintComponent(Graphics g)
    {
        // Show the dashing styles in a list.
        g.setColor(isSelected ? list.getSelectionBackground():
                                list.getBackground());

        // We draw the background with the correct color depending wether
        // the element is selected or not.
        g.fillRect(0,0, getWidth(), getHeight());

        g.setColor(isSelected ? list.getSelectionForeground():
                                list.getForeground());

        // We then proceed by drawing an horisontal line showing the dashing
        // style corresponding to the element

        // Maybe just applyStroke of the GraphicsSwing object can be enough?
        BasicStroke dashed = new BasicStroke(1,
                                          BasicStroke.CAP_BUTT,
                                          BasicStroke.JOIN_MITER,
                                          10.0f, Globals.dash[dash.style],
                                          0.0f);

        ((Graphics2D) g).setStroke(dashed);
        g.drawLine(getWidth()/3, getHeight()/2,2*getWidth()/3, getHeight()/2);
    }
}