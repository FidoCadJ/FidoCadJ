package fidocadj.dialogs.controls;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 ColorPicker.java

 ColorPicker defines a custom control for pick a color..
 from JColorChooser.

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
 @see<a href=http://www.gnu.org/licenses/>http://www.gnu.org/licenses/</a>.

 Copyright 2015-2024 by Davide Bucci, Manuel Finessi
 </pre>
 */
public class ColorPicker extends JPanel
{
    private static final long serialVersionUID = 1L;
    private Color color;
    private final Border border =
                    BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

    /**
     Constructs a ColorPicker with specified..
     initial dimensions and color.

     @param initialWidth the initial width
     @param initialHeight the initial height
     @param initialColor the initial color
     */
    public ColorPicker(int initialWidth, int initialHeight,
            Color initialColor)
    {
        this.color = initialColor;
        setPreferredSize(new Dimension(initialWidth, initialHeight));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Add a mouse listener to handle clicks
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                Color newColor = JColorChooser.showDialog(null, "", color);
                if (newColor != null) {
                    setColor(newColor);
                }
            }
        });
    }

    /**
     Gets the current color

     @return the current color
     */
    public Color getColor()
    {
        return color;
    }

    /**
     Sets a new color

     @param newColor the new color to set
     */
    public void setColor(Color newColor)
    {
        this.color = newColor;
        repaint();
    }

    /**
     Paints the component by filling it with the current color
     and drawing the etched border around it.

     @param g the Graphics object to protect
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(color);
        g2.fillRect(0, 0, getWidth(), getHeight());
        border.paintBorder(this, g2, 0, 0, getWidth(), getHeight());
    }
}
