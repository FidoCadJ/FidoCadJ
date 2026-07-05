package fidocadj.dialogs.settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import fidocadj.dialogs.controls.MinimumSizeDialog;
import fidocadj.globals.Globals;


/**
 DialogSettings is the main dialog that contains various settings panels.
 It allows the user to configure general settings, drawing settings,
 PCB settings, and theme settings in the application.

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

 Copyright 2008-2024 by Davide Bucci, Manuel Finessi
 </pre>
 */

public class DialogSettings extends MinimumSizeDialog
{
    private static final long serialVersionUID = 1L;
    private final PanelGeneralSettings panelGeneralSettings;
    private final PanelDrawingSettings panelDrawingSettings;
    private final PanelPCBSettings panelPCBSettings;
    private final PanelThemeSettings panelThemeSettings;

    /**
     Constructor for DialogOptions.

     @param parentFrame the parent frame.
     */
    public DialogSettings(JFrame parentFrame)
    {
        super(600, 450, parentFrame, Globals.messages.getString("Cir_opt_t"),
                true);

        // Initialize the panels
        panelGeneralSettings = new PanelGeneralSettings();
        panelDrawingSettings = new PanelDrawingSettings();
        panelPCBSettings = new PanelPCBSettings();
        panelThemeSettings = new PanelThemeSettings();

        setupUI();
    }

    /**
     Set up the UI components and layout of the dialog.
     */
    private void setupUI()
    {
        setLayout(new BorderLayout());

        // Create the tabbed pane and add the panels
        JTabbedPane tabsPane = new JTabbedPane();
        tabsPane.addTab(Globals.messages.getString("Restart"),
                panelGeneralSettings);
        tabsPane.addTab(Globals.messages.getString("Drawing"),
                panelDrawingSettings);
        tabsPane.addTab(Globals.messages.getString("PCBsizes"),
            panelPCBSettings);
        tabsPane.addTab(Globals.messages.getString("Theme_management"),
                panelThemeSettings);

        add(tabsPane, BorderLayout.CENTER);

        // Create the OK and Cancel buttons
        JPanel buttonsPanel = new JPanel();
        JButton okButton = new JButton(Globals.messages.getString("Ok_btn"));
        JButton cancelButton = new JButton(Globals.messages.getString(
                "Cancel_btn"));

        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        add(buttonsPanel, BorderLayout.SOUTH);

        // Set button actions
        okButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                saveSettings();
                setVisible(false);
            }
        });

        cancelButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
            }
        });

        pack();
        setLocationRelativeTo(getParent());
    }

    /**
     Save settings by calling the saveSettings method on each panel.
     */
    private void saveSettings()
    {
        panelGeneralSettings.saveSettings();
        panelDrawingSettings.saveSettings();
        panelPCBSettings.saveSettings();
        panelThemeSettings.saveSettings();
    }

    /**
     Show the dialog and load the settings to display current values.
     */
    public void showDialog()
    {
        panelGeneralSettings.loadSettings();
        panelDrawingSettings.loadSettings();
        panelPCBSettings.loadSettings();
        panelThemeSettings.loadSettings();

        setVisible(true);
    }
}
