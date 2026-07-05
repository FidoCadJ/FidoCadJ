package fidocadj.dialogs.controls;

import javax.swing.*;
import java.awt.*;

import fidocadj.layers.LayerDesc;
import fidocadj.graphic.swing.ColorSwing;
import fidocadj.globals.Globals;

/**
 * LayerRenderer.java
 *
 * This class defines a custom renderer for displaying layers in a JList
 * within FidoCadJ.
 * It handles the visual representation of each layer, including its color,
 * visibility, and name.
 *
 * <pre>
 * This file is part of FidoCadJ.
 *
 * FidoCadJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FidoCadJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FidoCadJ. If not,
 * @see<a href=http://www.gnu.org/licenses/>http://www.gnu.org/licenses/</a>.
 *
 * Copyright 2015-2025 by Davide Bucci
 * </pre>
 *
 * @author Manuel Finessi
 */
public final class LayerCellRenderer extends JPanel implements
                                                ListCellRenderer<LayerDesc>
{
    private static final long serialVersionUID = 1L;
    private final JLabel colorLabel;
    private final JLabel visibilityLabel;
    private final JLabel lockLabel;
    private final JLabel nameLabel;
    private final Icon visibleIcon;
    private final Icon invisibleIcon;
    private final Icon lockedIcon;
    private final Icon unlockedIcon;
    private final int iconSize = 20;

    /**
     * Constructs a LayerRenderer.
     */
    public LayerCellRenderer()
    {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        colorLabel = new JLabel();
        visibilityLabel = new JLabel();
        lockLabel = new JLabel();
        nameLabel = new JLabel();

        visibleIcon = Globals.loadIcon("/icons/layer-on.png");
        invisibleIcon = Globals.loadIcon("/icons/layer-off.png");
        lockedIcon = Globals.loadIcon("/icons/layer-locked.png");
        unlockedIcon = Globals.loadIcon("/icons/layer-unlocked.png");

        add(colorLabel);
        add(visibilityLabel);
        add(lockLabel);
        add(nameLabel);
    }

    /**
     * Configures the renderer component for each cell in the JList.
     *
     * @param list the JList we're painting.
     * @param layer the layer to be rendered.
     * @param index the index of the cell being drawn.
     * @param isSelected true if the specified cell is currently selected.
     * @param cellHasFocus true if the cell has focus.
     * @return the component used to render the value.
     */
    @Override
    public Component getListCellRendererComponent(
            JList<? extends LayerDesc> list, LayerDesc layer,
            int index, boolean isSelected, boolean cellHasFocus)
    {
        colorLabel.setOpaque(true);

        ColorSwing color=(ColorSwing) layer.getColor();

        colorLabel.setBackground(color.getColorSwing());
        colorLabel.setPreferredSize(new Dimension(25, iconSize));

        visibilityLabel.setIcon(
                layer.isVisible() ? visibleIcon : invisibleIcon);

        visibilityLabel.setPreferredSize(new Dimension(iconSize, iconSize));

        lockLabel.setIcon(
                layer.isLocked() ? lockedIcon : unlockedIcon);

        lockLabel.setPreferredSize(new Dimension(iconSize, iconSize));

        nameLabel.setText(layer.getDescription());

        if (!layer.isVisible())
            nameLabel.setForeground(SystemColor.textInactiveText);

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        nameLabel.setForeground(getForeground());


        return this;
    }
}
