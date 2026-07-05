package fidocadj.dialogs.settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import fidocadj.dialogs.controls.DialogUtil;
import fidocadj.dialogs.controls.TextPopupMenu;
import fidocadj.globals.Globals;
import fidocadj.globals.SettingsManager;

/**
 PanelGeneralSettings is responsible for managing the general settings
 of the application, such as the library directory and toolbar settings.
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
public final class PanelGeneralSettings extends JPanel implements SettingsPanel
{
    private static final long serialVersionUID = 1L;
    private JTextField libDirectoryField;
    private JCheckBox textToolbarCheckBox;
    private JCheckBox smallIconsCheckBox;
    private JCheckBox extStrict_CB;

    /**
     Constructor for PanelGeneralSettings.
     */
    public PanelGeneralSettings()
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

        // Label for the library directory setting
        JLabel libDirLabel = new JLabel(Globals.messages.getString("lib_dir"));
        constraints = DialogUtil.createConst(0, 0, 4, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(20, 40, 6, 0));
        add(libDirLabel, constraints);

        // Text field for the library directory path
        libDirectoryField = new JTextField(20);
        TextPopupMenu.addPopupToText(libDirectoryField);
        constraints = DialogUtil.createConst(0, 1, 3, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 40, 6, 20));
        add(libDirectoryField, constraints);

        // Button to browse for a library directory
        JButton browseButton = new
            JButton(Globals.messages.getString("Browse"));
        constraints = DialogUtil.createConst(3, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(0, 0, 6, 20));
        add(browseButton, constraints);

        // Label for restart information
        JLabel restw = new JLabel(Globals.messages.getString("restart_info"));
        restw.setForeground(Color.red.brighter());
        constraints = DialogUtil.createConst(0, 2, 4, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(35, 40, 6, 0));
        add(restw, constraints);

        // Checkbox to toggle text in the toolbar
        textToolbarCheckBox = new JCheckBox(Globals.messages.getString(
                "TextToolbar"));
        constraints = DialogUtil.createConst(0, 3, 4, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 40, 6, 0));
        add(textToolbarCheckBox, constraints);

        // Checkbox to toggle small icons in the toolbar
        smallIconsCheckBox = new JCheckBox(Globals.messages.getString(
                "SmallIcons"));
        constraints = DialogUtil.createConst(0, 4, 4, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(10, 40, 6, 0));
        add(smallIconsCheckBox, constraints);

        // Checkbox for strict FidoCAD compatibility
        extStrict_CB = new
            JCheckBox(Globals.messages.getString("strict_FC_comp"));
        constraints = DialogUtil.createConst(0, 5, 4, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(25, 40, 6, 0));
        add(extStrict_CB, constraints);

        // Spacer to push all controls to the top
        constraints = DialogUtil.createConst(0, 6, 4, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0));
        add(Box.createGlue(), constraints);

        // Action listener for the browse button to select a directory
        browseButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser fileChooser = new JFileChooser(
                        libDirectoryField.getText());
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnValue = fileChooser.showOpenDialog(
                        PanelGeneralSettings.this);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    libDirectoryField.setText(
                            fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
    }

    /**
     Load settings from SettingsManager and populate the UI components.
     */
    @Override
    public void loadSettings()
    {
        libDirectoryField.setText(SettingsManager.get("DIR_LIBS", ""));
        textToolbarCheckBox.setSelected(SettingsManager.get("TEXT_TOOLBAR",
                "true").equals("true"));
        smallIconsCheckBox.setSelected(SettingsManager.get("SMALL_ICON_TOOLBAR",
                "false").equals("true"));
        extStrict_CB.setSelected(SettingsManager.get("STRICT_COMPATIBILITY",
                "false").equals("true"));
    }

    /**
     Save the settings from the UI components into SettingsManager.
     */
    @Override
    public void saveSettings()
    {
        SettingsManager.put("DIR_LIBS", libDirectoryField.getText());
        SettingsManager.put("TEXT_TOOLBAR",
                textToolbarCheckBox.isSelected() ? "true" : "false");
        SettingsManager.put("SMALL_ICON_TOOLBAR",
                smallIconsCheckBox.isSelected() ? "true" : "false");
        SettingsManager.put("STRICT_COMPATIBILITY",
                extStrict_CB.isSelected() ? "true" : "false");
    }
}
