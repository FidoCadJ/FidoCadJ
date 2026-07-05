package fidocadj.macropicker;

import java.awt.event.*;

import java.io.*;
import javax.swing.*;
import javax.swing.event.*;

import fidocadj.globals.Globals;
import fidocadj.librarymodel.Library;
import fidocadj.librarymodel.Category;
import fidocadj.primitives.MacroDesc;

/** PopupMenu for MacroTree.<BR>
    This class checks the appropriate menu state for items by
    OperationPermission class of MacroTree.

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

    Copyright 2014-2023 Kohta Ozaki, Davide Bucci
    </pre>

    @author Kohta Ozaki, Davide Bucci
*/
public class MacroTreePopupMenu extends JPopupMenu implements
            ChangeListener
{
    private static final long serialVersionUID = 1L;
    final private MacroTree macroTree;

    // Element employed to check the kind of actions which can be done on
    // an element (permissions).
    final private OperationPermissions permission;

    final private JMenuItem copyMenu;
    final private JMenuItem pasteMenu;
    final private JMenuItem renameMenu;
    final private JMenuItem removeMenu;
    final private JMenuItem renkeyMenu;

    /** Create the popupmenu.
        @param macroTree the tree on which the menu has to be associated.
    */
    public MacroTreePopupMenu(MacroTree macroTree)
    {
        this.macroTree = macroTree;
        permission = macroTree.getOperationPermission();

        copyMenu = new JMenuItem(Globals.messages.getString("Copy"));
        copyMenu.setIcon(Globals.loadIcon("/icons/menu_icons/copy.png"));

        pasteMenu = new JMenuItem(Globals.messages.getString("Paste"));
        pasteMenu.setIcon(Globals.loadIcon("/icons/menu_icons/paste.png"));

        removeMenu = new JMenuItem(Globals.messages.getString("Delete"));
        removeMenu.setIcon(Globals.loadIcon("/icons/menu_icons/delete.png"));

        renameMenu = new JMenuItem(Globals.messages.getString("Rename"));
        renameMenu.setIcon(Globals.loadIcon("/icons/menu_icons/rename.png"));

        renkeyMenu = new JMenuItem(Globals.messages.getString("RenKey"));
        renkeyMenu.setIcon(
                Globals.loadIcon("/icons/menu_icons/change_key.png"));

        this.add(copyMenu);
        this.add(pasteMenu);
        this.add(removeMenu);
        this.add(renameMenu);
        this.add(renkeyMenu);

        copyMenu.addActionListener(createCopyActionListener());
        pasteMenu.addActionListener(createPasteActionListener());
        removeMenu.addActionListener(createRemoveActionListener());
        renameMenu.addActionListener(createRenameActionListener());
        renkeyMenu.addActionListener(createRenkeyActionListener());
    }

    /** By implementing writeObject method,
    // we can prevent
    // subclass from serialization
    */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        throw new NotSerializableException();
    }

    /* By implementing readObject method,
    // we can prevent
    // subclass from de-serialization
    */
    private void readObject(ObjectInputStream in) throws IOException
    {
        throw new NotSerializableException();
    }

    /** Update all the "enabled" states of the menu items, depending on which
        element is currently selected.
        @param e the event change object.
    */
    @Override public void stateChanged(ChangeEvent e)
    {
        copyMenu.setEnabled(permission.isCopyAvailable());
        pasteMenu.setEnabled(permission.isPasteAvailable());
        removeMenu.setEnabled(permission.isRemoveAvailable());
        renameMenu.setEnabled(permission.isRenameAvailable());
        renkeyMenu.setEnabled(permission.isRenKeyAvailable());
    }

    /** Create an action listener associated to the menu, reacting to
        the different elements presented. This action listener is associated
        to the renaming action.
        @return the ActionListener created by the routine.
    */
    private ActionListener createRenameActionListener()
    {
        final MacroTree mt = macroTree;
        return new ActionListener(){
            @Override public void actionPerformed(ActionEvent e)
            {
                switch(mt.getSelectedType()) {
                    case MacroTree.MACRO:
                        MacroDesc m = mt.getSelectedMacro();
                        if(m!=null){
                            mt.rename(m);
                        }
                        break;
                    case MacroTree.CATEGORY:
                        Category c = mt.getSelectedCategory();
                        if(c!=null){
                            mt.rename(c);
                        }
                        break;
                    case MacroTree.LIBRARY:
                        Library l = mt.getSelectedLibrary();
                        if(l!=null){
                            mt.rename(l);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    /** Create an action listener associated to the menu, reacting to
        the different elements presented. This action listener is associated
        to the delete/remove action on an element.
        @return the ActionListener created by the routine.
    */
    private ActionListener createRemoveActionListener()
    {
        final MacroTree mt = macroTree;
        return new ActionListener(){
            @Override public void actionPerformed(ActionEvent e)
            {
                switch(mt.getSelectedType()) {
                    case MacroTree.MACRO:
                        MacroDesc m = mt.getSelectedMacro();
                        if(m!=null){
                            mt.remove(m);
                        }
                        break;
                    case MacroTree.CATEGORY:
                        Category c = mt.getSelectedCategory();
                        if(c!=null){
                            mt.remove(c);
                        }
                        break;
                    case MacroTree.LIBRARY:
                        Library l = mt.getSelectedLibrary();
                        if(l!=null){
                            mt.remove(l);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }
    /** Create an action listener associated to the menu, reacting to
        the different elements presented. This action listener is associated
        to the action of changing the key of a macro. It does not have any
        effect on categories or libraries since there is no key associated to
        them.
        @return the ActionListener created by the routine.
    */
    private ActionListener createRenkeyActionListener()
    {
        final MacroTree mt = macroTree;
        return new ActionListener(){
            @Override public void actionPerformed(ActionEvent e)
            {
                switch(mt.getSelectedType()) {
                    case MacroTree.MACRO:
                        MacroDesc m = mt.getSelectedMacro();
                        if(m!=null){
                            mt.changeKey(m);
                        }
                        break;
                    case MacroTree.CATEGORY:
                        //NOP
                        break;
                    case MacroTree.LIBRARY:
                        //NOP
                        break;
                    default:
                        break;
                }
            }
        };
    }

    /** Create an action listener associated to the menu, reacting to
        the different elements presented. This action listener is associated
        to the copy action.
        @return the ActionListener created by the routine.
    */
    private ActionListener createCopyActionListener()
    {
        final MacroTree mt = macroTree;
        return new ActionListener(){
            @Override public void actionPerformed(ActionEvent e)
            {
                mt.setSelectedNodeToCopyTarget();
            }
        };
    }

    /** Create an action listener associated to the menu, reacting to
        the different elements presented. This action listener is associated
        to the paste action.
        @return the ActionListener created by the routine.
    */
    private ActionListener createPasteActionListener()
    {
        final MacroTree mt = macroTree;
        return new ActionListener(){
            @Override public void actionPerformed(ActionEvent e)
            {
                mt.pasteIntoSelectedNode();
            }
        };
    }
}

