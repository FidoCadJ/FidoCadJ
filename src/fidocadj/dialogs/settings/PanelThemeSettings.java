package fidocadj.dialogs.settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import fidocadj.dialogs.controls.ColorPicker;
import fidocadj.dialogs.controls.DialogUtil;
import fidocadj.dialogs.controls.TextPopupMenu;
import fidocadj.globals.Globals;
import fidocadj.globals.SettingsManager;

/**
 PanelThemeSettings is responsible for managing the theme and color settings
 in the application. It allows the user to select between predefined themes
 (light, dark) or a custom theme and customize various colors used in the
 application's interface.
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
public class PanelThemeSettings extends JPanel implements SettingsPanel
{
    private static final long serialVersionUID = 1L;
    private JRadioButton lightThemeRadioButton;
    private JRadioButton darkThemeRadioButton;
    private JCheckBox personalizedThemeCheckBox;
    private JTextField customThemePathField;
    private JButton browseCustomThemeButton;
    private JCheckBox enableThemesSupportCheckBox;

    private ColorPicker backgroundColorPicker;
    private ColorPicker gridDotsColorPicker;
    private ColorPicker gridLinesColorPicker;
    private ColorPicker selectionRightToLeftColorPicker;
    private ColorPicker selectionLeftToRightColorPicker;
    private ColorPicker selectedElementsColorPicker;

    /**
     Constructor for PanelThemeSettings.
     */
    public PanelThemeSettings()
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

        // Label for restart information
        JLabel restw = new JLabel(Globals.messages.getString("restart_info"));
        restw.setForeground(Color.red.brighter());

        // Set the constraints to center the label horizontally
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(5, 0, 0, 0);

        add(restw, constraints);

        // Groupbox "Theme Management"
        JPanel themeManagementPanel = new JPanel();
        themeManagementPanel.setBorder(
                BorderFactory.createTitledBorder(
                        Globals.messages.getString("Theme_groupBox")));
        themeManagementPanel.setLayout(new GridBagLayout());

        // Checkbox to enable custom themes support
        enableThemesSupportCheckBox = new JCheckBox(
                Globals.messages.getString("Theme_enableSupport"));
        enableThemesSupportCheckBox.setOpaque(false);
        constraints = DialogUtil.createConst(0, 1, 3, 1, 100, 100,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(6, 6, 6, 6));
        themeManagementPanel.add(enableThemesSupportCheckBox, constraints);

        // Radio buttons for theme selection
        lightThemeRadioButton = new JRadioButton(
                Globals.messages.getString("FlatLaf_light"));
        darkThemeRadioButton = new JRadioButton(
                Globals.messages.getString("FlatLaf_dark"));

        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightThemeRadioButton);
        themeGroup.add(darkThemeRadioButton);

        // Set "Tema light" as the default selected option
        lightThemeRadioButton.setSelected(true);

        constraints = DialogUtil.createConst(0, 2, 1, 1, 100, 100,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        themeManagementPanel.add(lightThemeRadioButton, constraints);

        constraints = DialogUtil.createConst(0, 3, 1, 1, 100, 100,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        themeManagementPanel.add(darkThemeRadioButton, constraints);

        // Label explaining the custom theme is based on the selected theme
        JLabel customThemeInfoLabel = new JLabel(
                Globals.messages.getString("Custom_theme_info"));
        customThemeInfoLabel.setForeground(Color.decode("#404040"));

        constraints = DialogUtil.createConst(1, 3, 2, 1, 100, 100,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(6, 6, 6, 6));
        themeManagementPanel.add(customThemeInfoLabel, constraints);

        // Checkbox and TextBox for custom theme file
        personalizedThemeCheckBox = new JCheckBox(
                Globals.messages.getString("FlatLaf_custom"));
        customThemePathField = new JTextField(20);
        TextPopupMenu.addPopupToText(customThemePathField);
        browseCustomThemeButton = new JButton(
                Globals.messages.getString("Browse"));

        browseCustomThemeButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                fileChooser.setFileFilter(
                        new javax.swing.filechooser.FileNameExtensionFilter(
                                "Properties Files (*.properties)",
                                "properties"));

                int returnValue = fileChooser.showOpenDialog(
                        PanelThemeSettings.this);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    customThemePathField.setText(
                            fileChooser.getSelectedFile().getPath());
                }
            }
        });

        personalizedThemeCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                boolean isSelected = personalizedThemeCheckBox.isSelected();
                customThemePathField.setEnabled(isSelected);
                browseCustomThemeButton.setEnabled(isSelected);
            }
        });

        constraints = DialogUtil.createConst(0, 4, 1, 1, 100, 100,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        themeManagementPanel.add(personalizedThemeCheckBox, constraints);

        constraints = DialogUtil.createConst(1, 4, 1, 1, 100, 100,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(6, 6, 6, 6));
        themeManagementPanel.add(customThemePathField, constraints);

        constraints = DialogUtil.createConst(2, 4, 1, 1, 100, 100,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        themeManagementPanel.add(browseCustomThemeButton, constraints);

        // Enable/Disable Controls based on Custom Themes
        enableThemesSupportCheckBox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                boolean isEnabled = enableThemesSupportCheckBox.isSelected();
                lightThemeRadioButton.setEnabled(isEnabled);
                darkThemeRadioButton.setEnabled(isEnabled);
                personalizedThemeCheckBox.setEnabled(isEnabled);
                customThemePathField.setEnabled(
                        isEnabled && personalizedThemeCheckBox.isSelected());
                browseCustomThemeButton.setEnabled(
                        isEnabled && personalizedThemeCheckBox.isSelected());
            }
        });

        // Groupbox "Color Management"
        JPanel colorManagementPanel = new JPanel();
        colorManagementPanel.setBorder(
                BorderFactory.createTitledBorder(
                        Globals.messages.getString("Colors_management")));
        colorManagementPanel.setLayout(new GridBagLayout());

        // Column 1: Long text labels
        JLabel backgroundColorLabel = new JLabel(
                Globals.messages.getString("Circuit_backgroud"));
        constraints = DialogUtil.createConst(0, 0, 1, 1, 100, 100,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        colorManagementPanel.add(backgroundColorLabel, constraints);

        backgroundColorPicker = new ColorPicker(30, 20, Color.WHITE);
        constraints = DialogUtil.createConst(1, 0, 1, 1, 100, 100,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        colorManagementPanel.add(backgroundColorPicker, constraints);

        JLabel selectionRightToLeftColorLabel = new JLabel(
                Globals.messages.getString("Select_RL_color"));
        constraints = DialogUtil.createConst(0, 1, 1, 1, 100, 100,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        colorManagementPanel.add(selectionRightToLeftColorLabel, constraints);

        selectionRightToLeftColorPicker = new ColorPicker(30, 20, Color.BLUE);
        constraints = DialogUtil.createConst(1, 1, 1, 1, 100, 100,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        colorManagementPanel.add(selectionRightToLeftColorPicker, constraints);

        JLabel selectionLeftToRightColorLabel = new JLabel(
                Globals.messages.getString("Select_LR_color"));
        constraints = DialogUtil.createConst(0, 2, 1, 1, 100, 100,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        colorManagementPanel.add(selectionLeftToRightColorLabel, constraints);

        selectionLeftToRightColorPicker = new ColorPicker(30, 20, Color.GREEN);
        constraints = DialogUtil.createConst(1, 2, 1, 1, 100, 100,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        colorManagementPanel.add(selectionLeftToRightColorPicker, constraints);

        JLabel selectedElementsColorLabel = new JLabel(
                Globals.messages.getString("Select_color"));
        constraints = DialogUtil.createConst(2, 2, 1, 1, 100, 100,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        colorManagementPanel.add(selectedElementsColorLabel, constraints);

        selectedElementsColorPicker = new ColorPicker(30, 20, Color.YELLOW);
        constraints = DialogUtil.createConst(3, 2, 1, 1, 100, 100,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        colorManagementPanel.add(selectedElementsColorPicker, constraints);

        // Column 2: Short text labels
        JLabel gridDotsColorLabel = new JLabel(
                Globals.messages.getString("Grid_dots_color"));
        constraints = DialogUtil.createConst(2, 0, 1, 1, 100, 100,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        colorManagementPanel.add(gridDotsColorLabel, constraints);

        gridDotsColorPicker = new ColorPicker(30, 20, Color.GRAY);
        constraints = DialogUtil.createConst(3, 0, 1, 1, 100, 100,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        colorManagementPanel.add(gridDotsColorPicker, constraints);

        JLabel gridLinesColorLabel = new JLabel(
                Globals.messages.getString("Grid_lines_color"));
        constraints = DialogUtil.createConst(2, 1, 1, 1, 100, 100,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        colorManagementPanel.add(gridLinesColorLabel, constraints);

        gridLinesColorPicker = new ColorPicker(30, 20, Color.LIGHT_GRAY);
        constraints = DialogUtil.createConst(3, 1, 1, 1, 100, 100,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(6, 6, 6, 6));
        colorManagementPanel.add(gridLinesColorPicker, constraints);

        // Adding both GroupBoxes to the main panel
        constraints = DialogUtil.createConst(0, 1, 1, 1, 100, 100,
                GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                new Insets(10, 10, 10, 10));
        add(themeManagementPanel, constraints);

        constraints = DialogUtil.createConst(0, 2, 1, 1, 100, 100,
                GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,
                new Insets(10, 10, 10, 10));
        add(colorManagementPanel, constraints);
    }

    @Override
    public void loadSettings()
    {
        String theme = SettingsManager.get("THEME", "light");
        if ("dark".equals(theme)) {
            darkThemeRadioButton.setSelected(true);
        } else {
            lightThemeRadioButton.setSelected(true);
        }

        customThemePathField.setText(
                SettingsManager.get("CUSTOM_THEME_PATH", ""));
        boolean enableCustomThemes = SettingsManager.get("ENABLE_CUSTOM_THEMES",
                Globals.isFlatLafAvailable() ? "true" : "false")
                .equals("true");
        enableThemesSupportCheckBox.setSelected(enableCustomThemes);

        boolean isCustomThemeEnabled = SettingsManager.get("PERSONALIZED_THEME",
                                                        "false").equals("true");
        personalizedThemeCheckBox.setSelected(isCustomThemeEnabled);
        customThemePathField.setEnabled(isCustomThemeEnabled);
        browseCustomThemeButton.setEnabled(isCustomThemeEnabled);

        backgroundColorPicker.setColor(Color.decode(SettingsManager.get(
                "BACKGROUND_COLOR", "#FFFFFF")));
        gridDotsColorPicker.setColor(Color.decode(SettingsManager.get(
                "GRID_DOTS_COLOR", "#000000")));
        gridLinesColorPicker.setColor(Color.decode(SettingsManager.get(
                "GRID_LINES_COLOR", "#D3D3D3")));
        selectionRightToLeftColorPicker.setColor(Color.decode(
                SettingsManager.get("SELECTION_RTL_COLOR", "#0000FF")));
        selectionLeftToRightColorPicker.setColor(Color.decode(
                SettingsManager.get("SELECTION_LTR_COLOR", "#008000")));
        selectedElementsColorPicker.setColor(Color.decode(SettingsManager.get(
                "SELECTED_ELEMENTS_COLOR", "#00FF00")));
    }

    @Override
    public void saveSettings()
    {
        String theme = lightThemeRadioButton.isSelected() ? "light" : "dark";
        SettingsManager.put("THEME", theme);
        SettingsManager.put("CUSTOM_THEME_PATH",
            customThemePathField.getText());
        SettingsManager.put("ENABLE_CUSTOM_THEMES",
                enableThemesSupportCheckBox.isSelected() ? "true" : "false");
        SettingsManager.put("PERSONALIZED_THEME",
                personalizedThemeCheckBox.isSelected() ? "true" : "false");

        SettingsManager.put("BACKGROUND_COLOR", String.format("#%06X",
                (0xFFFFFF & backgroundColorPicker.getColor().getRGB())));
        SettingsManager.put("GRID_DOTS_COLOR", String.format("#%06X",
                (0xFFFFFF & gridDotsColorPicker.getColor().getRGB())));
        SettingsManager.put("GRID_LINES_COLOR", String.format("#%06X",
                (0xFFFFFF & gridLinesColorPicker.getColor().getRGB())));
        SettingsManager.put("SELECTION_RTL_COLOR", String.format("#%06X",
                (0xFFFFFF &
                    selectionRightToLeftColorPicker.getColor().getRGB())));
        SettingsManager.put("SELECTION_LTR_COLOR", String.format("#%06X",
                (0xFFFFFF &
                    selectionLeftToRightColorPicker.getColor().getRGB())));
        SettingsManager.put("SELECTED_ELEMENTS_COLOR", String.format("#%06X",
                (0xFFFFFF & selectedElementsColorPicker.getColor().getRGB())));
    }
}
