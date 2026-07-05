package fidocadj.dialogs.controls;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

/**
 Implements a dialog to display error messages with the ability to copy text.
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

 Copyright 2015-2024 by Davide Bucci, Manuel Finessi
 </pre>
 */
public class ErrorDialog extends MinimumSizeDialog
{
    private static final long serialVersionUID = 1L;

    // Static configuration for the dialog
    private static final int MIN_WIDTH = 450;
    private static final int MIN_HEIGHT = 300;
    private static final String DIALOG_TITLE = "An unexpected error occurred";
    private static final boolean IS_MODAL = true;

    private JTextArea textArea;

    /**
     Constructor.

     @param parent the parent frame.
     @param errorMessage the error message to display.
     */
    public ErrorDialog(JFrame parent, String errorMessage)
    {
        super(MIN_WIDTH, MIN_HEIGHT, parent, DIALOG_TITLE, IS_MODAL);
        initComponents(errorMessage);
        setLocationRelativeTo(parent);
    }

    /**
     Initialize the components of the dialog.

     @param errorMessage the error message to display.
     */
    private void initComponents(String errorMessage)
    {
        setLayout(new BorderLayout());

        // Create a JTextArea to display the error message
        textArea = new JTextArea();
        TextPopupMenu.addPopupToText(textArea);
        textArea.setText(errorMessage);
        textArea.setEditable(false); // Make it read-only
        textArea.setLineWrap(false); // Disable line wrapping
        textArea.setWrapStyleWord(false);

        // Add the JTextArea to a JScrollPane with scrollbars as needed
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Add the scroll pane to the dialog
        add(scrollPane, BorderLayout.CENTER);

        // Create a panel to hold the buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        // Add the "Copy" button
        JButton copyButton = new JButton("Copy Error");
        copyButton.setPreferredSize(new Dimension(
                copyButton.getPreferredSize().width, 50));
        copyButton.addActionListener(e -> copyToClipboard());
        buttonPanel.add(copyButton);

        // Add the "Close" button
        JButton closeButton = new JButton("Close");
        closeButton.setPreferredSize(new Dimension(
                closeButton.getPreferredSize().width, 50));
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        // Add the button panel to the dialog
        add(buttonPanel, BorderLayout.SOUTH);

        // Set default close operation and pack the dialog
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
    }

    /**
     Copies the error message to the system clipboard.
     */
    private void copyToClipboard()
    {
        String text = textArea.getText();
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    /**
     Set the error message to display.

     @param errorMessage the error message to display.
     */
    public void setErrorMessage(String errorMessage)
    {
        textArea.setText(errorMessage);
    }
}
