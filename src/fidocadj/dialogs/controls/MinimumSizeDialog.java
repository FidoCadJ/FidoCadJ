package fidocadj.dialogs.controls;

import javax.swing.*;
import java.awt.event.*;

/** Implements a dialog having a minimum size.
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
public class MinimumSizeDialog extends JDialog implements ComponentListener
{
    private static final long serialVersionUID = 1L;
    // The minimum size in pixels.
    private final int minWidth;
    private final int minHeight;

    /** Constructor.
        @param minW minimum width in pixels.
        @param minH minimum height in pixels.
        @param parent the parent frame.
        @param title the tile of the dialog.
        @param modal true if it is a modal dialog, false otherwise.
    */
    public MinimumSizeDialog(
        int minW, int minH,
        JFrame parent, String title,
        boolean modal)
    {
        super(parent, title, modal);
        minWidth=minW;
        minHeight=minH;
    }

    /** Required for the implementation of the ComponentListener interface.
        In this case, prevents from resizing the dialog in a size which is
        too small.
        @param e the component event which happened.
    */
    @Override public void componentResized(ComponentEvent e)
    {
        int width = getWidth();
        int height = getHeight();

        boolean resize = false;
        if (width < minWidth) {
            resize = true;
            width = minWidth;
        }
        if (height < minHeight) {
            resize = true;
            height = minHeight;
        }
        if (resize) {
            setSize(width, height);
        }
    }

    /** Required for the implementation of the ComponentListener interface.
        @param e the component event which happened.
    */
    @Override public void componentMoved(ComponentEvent e)
    {
        // Nothing to do
    }

    /** Required for the implementation of the ComponentListener interface.
        @param e the component event which happened.
    */
    @Override public void componentShown(ComponentEvent e)
    {
        // Nothing to do
    }

    /** Required for the implementation of the ComponentListener interface.
        @param e the component event which happened.
    */
    @Override public void componentHidden(ComponentEvent e)
    {
        // Nothing to do
    }
}