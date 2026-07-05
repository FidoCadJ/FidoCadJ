package fidocadj.dialogs.controls;

import javax.swing.*;
import java.awt.*;

/**
    PadSelector is a custom control that extends JPanel and contains three ..
    radio buttons.
    Each radio button has an associated image to its right. Only one button ..
    can be selected at a time.
    The selected button can be set via the constructor or later via a method,
    and the currently selected button index can be retrieved.

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

    Copyright 2007-2024 by Davide Bucci, Manuel Finessi
    </pre>
 */
public final class PadSelector extends JPanel
{
    private static final long serialVersionUID = 1L;

    private final JRadioButton radioButton1;
    private final JRadioButton radioButton2;
    private final JRadioButton radioButton3;
    private final ButtonGroup buttonGroup;

    /**
     Constructs a PadSelector with three radio buttons.
     The button corresponding to the index passed as an argument is selected.

     @param selectedIndex The index of the button to be selected
     (0 for the first, 1 for the second, 2 for the third).
     */
    public PadSelector(int selectedIndex)
    {
        // Initialize the radio buttons
        radioButton1 = new JRadioButton();
        radioButton2 = new JRadioButton();
        radioButton3 = new JRadioButton();

        // Create labels with images
        JLabel label1 = new JLabel(new ImageIcon(getClass().getResource(
                "/icons/pad_0.png")));
        JLabel label2 = new JLabel(new ImageIcon(getClass().getResource(
                "/icons/pad_1.png")));
        JLabel label3 = new JLabel(new ImageIcon(getClass().getResource(
                "/icons/pad_2.png")));

        // Group the radio buttons to ensure only one can be selected at a time
        buttonGroup = new ButtonGroup();
        buttonGroup.add(radioButton1);
        buttonGroup.add(radioButton2);
        buttonGroup.add(radioButton3);

        // Set layout for the entire panel
        setLayout(new GridLayout(1, 3));

        // Create a panel for each radio button and its corresponding label
        JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Add radio button and label pairs to their respective panels
        panel1.add(radioButton1);
        panel1.add(label1);

        panel2.add(radioButton2);
        panel2.add(label2);

        panel3.add(radioButton3);
        panel3.add(label3);

        // Add the panels to the main panel
        add(panel1);
        add(panel2);
        add(panel3);

        setSelectedIndex(selectedIndex);
    }

    /**
     Sets the selected radio button based on the given index.

     @param index The index of the button to select
     (0 for the first, 1 for the second, 2 for the third).
     */
    public void setSelectedIndex(int index)
    {
        switch (index) {
            case 0 -> radioButton1.setSelected(true);
            case 1 -> radioButton2.setSelected(true);
            case 2 -> radioButton3.setSelected(true);
            default -> radioButton1.setSelected(true);
        }
    }

    /**
     Returns the index of the currently selected radio button.

     @return The index of the selected button
     (0 for the first, 1 for the second, 2 for the third).
     */
    public int getSelectedIndex()
    {
        if (radioButton1.isSelected()) {
            return 0;
        } else {
            if (radioButton2.isSelected()) {
                return 1;
            } else {
                if (radioButton3.isSelected()) {
                    return 2;
                }
            }
        }
        return 0;
    }

}
