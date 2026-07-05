package fidocadj.dialogs.settings;

import javax.swing.*;
import java.awt.*;

import fidocadj.dialogs.controls.DialogUtil;
import fidocadj.dialogs.controls.TextPopupMenu;
import fidocadj.globals.Globals;
import fidocadj.globals.SettingsManager;

/**
 PanelDrawingSettings is responsible for managing the drawing settings
 of the application, such as grid width, connection size, and stroke size.
 It implements the SettingsPanel interface to load and save settings.

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
public class PanelDrawingSettings extends JPanel implements SettingsPanel
{
    private static final long serialVersionUID = 1L;
    private JTextField gridWidthField;
    private JTextField connectionSizeField;
    private JTextField strokeSizeStraightField;
    private JComboBox<String> comboFont;
    private JTextField macroSizeField;
    private JCheckBox antiAliasCheckBox;
    private JCheckBox shiftCPCheckBox;
    private JCheckBox profileCheckBox;
    private JCheckBox zoomKeyCheckBox;

    /**
     Constructor for PanelDrawingSettings.
     */
    public PanelDrawingSettings()
    {
        setupUI();
        loadSettings(); // Load settings during initialization
    }

    /**
     Set up the UI components and layout.
     */
    @Override
    public void setupUI()
    {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        int col2Width = 125;

        // Optional: Profiler checkbox (only for beta versions)
        if (Globals.isBeta) {
            profileCheckBox = new JCheckBox(
                    Globals.messages.getString("Profile"));
            constraints = DialogUtil.createConst(0, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE,
                    new Insets(12, 6, 6, 6));
            add(Box.createHorizontalGlue(), constraints); // placeholder

            constraints = DialogUtil.createConst(1, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(12, 6, 6, 6));
            add(profileCheckBox, constraints);
        }

        // Label and text field for grid width setting
        JLabel gridWidthLabel = new JLabel(Globals.messages.getString(
                "Grid_width"));
        constraints = DialogUtil.createConst(0, 1, 1, 1, 1.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(12, 6, 6, 6));
        add(gridWidthLabel, constraints);

        gridWidthField = new JTextField(10);
        TextPopupMenu.addPopupToText(gridWidthField);
        gridWidthField.setPreferredSize(new Dimension(col2Width,
                gridWidthField.getPreferredSize().height));
        constraints = DialogUtil.createConst(1, 1, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(12, 6, 6, 6));
        add(gridWidthField, constraints);

        // Label and text field for connection size setting
        JLabel connectionSizeLabel = new JLabel(Globals.messages.getString(
                "connection_size"));
        constraints = DialogUtil.createConst(0, 2, 1, 1, 1.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(8, 6, 6, 6));
        add(connectionSizeLabel, constraints);

        connectionSizeField = new JTextField(10);
        TextPopupMenu.addPopupToText(connectionSizeField);
        connectionSizeField.setPreferredSize(new Dimension(col2Width,
                connectionSizeField.getPreferredSize().height));
        constraints = DialogUtil.createConst(1, 2, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(8, 6, 6, 6));
        add(connectionSizeField, constraints);

        // Label and text field for stroke size setting
        JLabel strokeSizeStraightLabel = new JLabel(Globals.messages.getString(
                "stroke_size_straight"));
        constraints = DialogUtil.createConst(0, 3, 1, 1, 1.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(8, 6, 6, 6));
        add(strokeSizeStraightLabel, constraints);

        strokeSizeStraightField = new JTextField(10);
        TextPopupMenu.addPopupToText(strokeSizeStraightField);
        strokeSizeStraightField.setPreferredSize(new Dimension(col2Width,
                strokeSizeStraightField.getPreferredSize().height));
        constraints = DialogUtil.createConst(1, 3, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(8, 6, 6, 6));
        add(strokeSizeStraightField, constraints);

        // Label and combo box for macro font selection
        JLabel macroFontLabel = new JLabel(Globals.messages.getString(
                "macrofont"));
        constraints = DialogUtil.createConst(0, 4, 1, 1, 1.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(8, 6, 6, 6));
        add(macroFontLabel, constraints);

        comboFont = new JComboBox<>();
        comboFont.setPreferredSize(new Dimension(col2Width,
                comboFont.getPreferredSize().height));
        constraints = DialogUtil.createConst(1, 4, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(8, 6, 6, 6));
        add(comboFont, constraints);

        // Load font names in a separate thread
        loadFontsInBackground();

        // Label and text field for macro size
        JLabel macroSizeLabel = new JLabel(Globals.messages.getString(
                "macroSize"));
        constraints = DialogUtil.createConst(0, 5, 1, 1, 1.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(8, 6, 6, 6));
        add(macroSizeLabel, constraints);

        macroSizeField = new JTextField(10);
        TextPopupMenu.addPopupToText(macroSizeField);
        macroSizeField.setPreferredSize(new Dimension(col2Width,
                macroSizeField.getPreferredSize().height));
        constraints = DialogUtil.createConst(1, 5, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(8, 6, 6, 6));
        add(macroSizeField, constraints);

        // Checkbox for anti-aliasing
        antiAliasCheckBox = new
            JCheckBox(Globals.messages.getString("Anti_al"));
        constraints = DialogUtil.createConst(1, 6, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(8, 6, 6, 6));
        add(antiAliasCheckBox, constraints);

        // Checkbox for shift copy/paste
        shiftCPCheckBox = new JCheckBox(Globals.messages.getString("Shift_cp"));
        constraints = DialogUtil.createConst(1, 7, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(8, 6, 6, 6));
        add(shiftCPCheckBox, constraints);

        // Checkbox for Zoom Key
        zoomKeyCheckBox = new JCheckBox(Globals.messages.getString("Zoom_key"));
        constraints = DialogUtil.createConst(1, 8, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(8, 6, 6, 6));
        add(zoomKeyCheckBox, constraints);

        // Spacer to push all components to the top
        constraints = DialogUtil.createConst(0, 9, 2, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0));
        add(Box.createGlue(), constraints);
    }

    /**
     Load the list of available fonts in the background to avoid UI freezing.
     */
    private void loadFontsInBackground()
    {
        new Thread(() -> {
            GraphicsEnvironment gE =
                            GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] fontFamilies = gE.getAvailableFontFamilyNames();
            SwingUtilities.invokeLater(() -> {
                for (String font : fontFamilies) {
                    comboFont.addItem(font);
                }
                // Set default font after loading
                comboFont.setSelectedItem(SettingsManager.get("MACRO_FONT",
                        "Monospaced"));
            });
        }).start();
    }

    /**
     Load settings from SettingsManager and populate the UI components.
     */
    @Override
    public void loadSettings()
    {
        if (Globals.isBeta) {
            profileCheckBox.setSelected(SettingsManager.get("PROFILE_TIME",
                    "false").equals("true"));
        }
        gridWidthField.setText(SettingsManager.get("GRID_SIZE", "5"));
        connectionSizeField.setText(
                SettingsManager.get("CONNECTION_SIZE", "2.0"));
        strokeSizeStraightField.setText(SettingsManager.get(
                "STROKE_SIZE_STRAIGHT", "0.5"));
        macroSizeField.setText(SettingsManager.get("MACRO_SIZE", "3"));
        antiAliasCheckBox.setSelected(
                SettingsManager.get("ANTIALIAS", "false").equals("true"));
        shiftCPCheckBox.setSelected(
                SettingsManager.get("SHIFT_CP", "false").equals("true"));
        zoomKeyCheckBox.setSelected(
                SettingsManager.get("ZOOM_KEY", "false").equals("true"));
    }

    /**
     Save the settings from the UI components into SettingsManager.
     */
    @Override
    public void saveSettings()
    {
        if (Globals.isBeta) {
            SettingsManager.put("PROFILE_TIME",
                    profileCheckBox.isSelected() ? "true" : "false");
        }
        SettingsManager.put("GRID_SIZE", gridWidthField.getText());
        SettingsManager.put("CONNECTION_SIZE", connectionSizeField.getText());
        SettingsManager.put("STROKE_SIZE_STRAIGHT",
                strokeSizeStraightField.getText());
        SettingsManager.put("MACRO_FONT", (String) comboFont.getSelectedItem());
        SettingsManager.put("MACRO_SIZE", macroSizeField.getText());
        SettingsManager.put("ANTIALIAS",
                antiAliasCheckBox.isSelected() ? "true" : "false");
        SettingsManager.put("SHIFT_CP",
                shiftCPCheckBox.isSelected() ? "true" : "false");
        SettingsManager.put("ZOOM_KEY",
                zoomKeyCheckBox.isSelected() ? "true" : "false");
    }
}
