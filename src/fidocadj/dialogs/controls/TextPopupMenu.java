package fidocadj.dialogs.controls;

import fidocadj.globals.Globals;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.text.JTextComponent;

/**
 A class that creates a popup menu with Cut, Copy, Paste,
 and Select All options for any JTextComponent (e.g., JTextArea, JTextField).

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
public class TextPopupMenu extends JPopupMenu
{
    private static final long serialVersionUID = 1L;

    private TextPopupMenu()
    {
        JMenuItem cutMenuItem = new JMenuItem(
                Globals.messages.getString("Cut"));
        cutMenuItem.setIcon(Globals.loadIcon("/icons/menu_icons/cut.png"));

        JMenuItem copyMenuItem = new JMenuItem(
                Globals.messages.getString("Copy"));
        copyMenuItem.setIcon(Globals.loadIcon("/icons/menu_icons/copy.png"));

        JMenuItem pasteMenuItem = new JMenuItem(
                Globals.messages.getString("Paste"));
        pasteMenuItem.setIcon(Globals.loadIcon("/icons/menu_icons/paste.png"));

        JMenuItem selectAllMenuItem = new JMenuItem(
                Globals.messages.getString("SelectAll"));
        selectAllMenuItem.setIcon(
                Globals.loadIcon("/icons/menu_icons/select_all.png"));

        add(copyMenuItem);
        add(cutMenuItem);
        addSeparator();
        add(pasteMenuItem);
        addSeparator();
        add(selectAllMenuItem);

        cutMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JTextComponent tc = (JTextComponent) getInvoker();
                tc.cut();
            }
        });

        copyMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JTextComponent tc = (JTextComponent) getInvoker();
                tc.copy();
            }
        });

        pasteMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JTextComponent tc = (JTextComponent) getInvoker();
                tc.paste();
            }
        });

        selectAllMenuItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JTextComponent tc = (JTextComponent) getInvoker();
                tc.selectAll();
            }
        });
    }

    /**
     Adds the TextPopupMenu to a specified JTextComponent.

     @param textComponent the JTextComponent to which the popup menu will be
         added
     */
    public static void addPopupToText(JTextComponent textComponent)
    {
        TextPopupMenu popupMenu = new TextPopupMenu();
        textComponent.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            public void mouseReleased(MouseEvent e)
            {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }

            private void showMenu(MouseEvent e)
            {
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }
}
