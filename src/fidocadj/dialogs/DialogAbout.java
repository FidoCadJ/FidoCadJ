package fidocadj.dialogs;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.net.*;

import fidocadj.globals.Globals;
import fidocadj.dialogs.controls.MinimumSizeDialog;
import fidocadj.dialogs.controls.BareBonesBrowserLaunch;
import fidocadj.dialogs.controls.DialogUtil;

/**
    Shows a rather standard "About" dialog. Nothing more exotic than showing
    the nice icon of the program, its name as well as three lines of
    description.

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

    Copyright 2007-2023 by Davide Bucci
    </pre>
    @author Davide Bucci
*/
public final class DialogAbout extends MinimumSizeDialog
{
    private static final long serialVersionUID = 1L;
    /** Standard constructor: it needs the parent frame.
        @param parent the dialog's parent
    */
    public DialogAbout (JFrame parent)
    {
        super(300, 200, parent, "", true);
        DialogUtil.center(this, .30,.35,350,300);
        setResizable(false);
        addComponentListener(this);

        // Shows the icon of the program and then three lines read from the
        // resources which describe the software and give the credits.

        GridBagLayout bgl=new GridBagLayout();
        GridBagConstraints constraints=new GridBagConstraints();
        Container contentPane=getContentPane();
        contentPane.setLayout(bgl);

        URL url=DialogAbout.class.getResource(
            "/icons/icona_fidocadj_128x128.png");
        JLabel icon=new JLabel("");
        constraints.weightx=100;
        constraints.weighty=100;
        constraints.gridx=0;
        constraints.gridy=0;
        constraints.gridwidth=1;
        constraints.gridheight=1;
        constraints.anchor=GridBagConstraints.CENTER;
        constraints.insets=new Insets(10,20,0,20);

        if (url != null) { icon.setIcon(new ImageIcon(url)); }
        contentPane.add(icon, constraints);

        JLabel programName=new JLabel("FidoCadJ");

        Font f=new Font("Lucida Grande",Font.BOLD,18);

        programName.setFont(f);
        constraints.gridx=0;
        constraints.gridy=1;
        constraints.gridwidth=1;
        constraints.gridheight=1;
        constraints.anchor=GridBagConstraints.CENTER;
        constraints.insets=new Insets(0,20,0,20);
        contentPane.add(programName, constraints);


        JLabel programVersion=new JLabel(
            Globals.messages.getString("Version")+Globals.version);
        constraints.gridx=0;
        constraints.gridy=2;
        constraints.gridwidth=1;
        constraints.gridheight=1;
        constraints.anchor=GridBagConstraints.CENTER;
        contentPane.add(programVersion, constraints);

        JLabel programDescription1=new JLabel(
            Globals.messages.getString("programDescription1"));
        constraints.gridx=0;
        constraints.gridy=3;
        constraints.gridwidth=1;
        constraints.gridheight=1;
        constraints.anchor=GridBagConstraints.CENTER;
        contentPane.add(programDescription1, constraints);

        JLabel programDescription2=new JLabel(
            Globals.messages.getString("programDescription2"));
        constraints.gridx=0;
        constraints.gridy=4;
        constraints.gridwidth=1;
        constraints.gridheight=1;
        constraints.anchor=GridBagConstraints.CENTER;
        constraints.insets=new Insets(0,20,20,20);

        contentPane.add(programDescription2, constraints);

        JLabel programDescription3=new JLabel(
            Globals.messages.getString("programDescription3"));
        constraints.gridx=0;
        constraints.gridy=5;
        constraints.gridwidth=1;
        constraints.gridheight=1;
        constraints.anchor=GridBagConstraints.CENTER;
        constraints.insets=new Insets(0,20,20,20);

        contentPane.add(programDescription3, constraints);


        class OpenUrlAction implements ActionListener
        {
            @Override public void actionPerformed(ActionEvent e)
            {
                BareBonesBrowserLaunch.openURL(
                    "http://fidocadj.github.io/FidoCadJ/");

            // The following code works only in Java above v. 1.6 and
            // the minimum requirements for FidoCadJ are Java 1.9
            // UPDATE: this has changed and we may consider employing a more
            // standard code now.
            /*  if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(uri);
                    } catch (IOException err) { /* TODO: error handling * }
                 } else { /* TODO: error handling * }*/
            }
        }

        JButton link=new JButton(
            "<HTML> <a href=\"http://fidocadj.github.io/FidoCadJ/\">"+
            "http://fidocadj.github.io/FidoCadJ/</a></HTML>");
        constraints.gridx=0;
        constraints.gridy=6;
        constraints.gridwidth=1;
        constraints.gridheight=2;
        constraints.anchor=GridBagConstraints.CENTER;
        constraints.insets=new Insets(0,20,20,20);

        link.setBorderPainted(false);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));

        link.addActionListener(new OpenUrlAction());
        contentPane.add(link, constraints);
        pack();
    }
}