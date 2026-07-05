package fidocadj.circuit.controllers;

import java.io.*;
import java.util.*;

import fidocadj.circuit.HasChangedListener;
import fidocadj.globals.FileUtils;
import fidocadj.undo.UndoState;
import fidocadj.undo.UndoManager;
import fidocadj.undo.LibraryUndoListener;
import fidocadj.undo.UndoActorListener;

/** UndoActions: perform undo operations. Since some parsing operations are
    to be done, this class requires the ParserActions controller.

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
*/

public class UndoActions implements UndoActorListener
{
    private final ParserActions pa;

    // Undo manager
    private final UndoManager um;

    // Database of the temporary directories
    private final List<String> tempDir;

    // Maximum number of levels to be retained for undo operations.
    private static final int MAX_UNDO=100;

    // A drawing modification flag. If true, there are unsaved changes.
    private boolean isModified;

    private String tempLibraryDirectory="";


    // Listeners
    private LibraryUndoListener libraryUndoListener;
    private HasChangedListener cl;

    /** Public constructor.
    @param a a parser controller (undo snapshots are kept in text format).
    */
    public UndoActions(ParserActions a)
    {
        pa=a;
        um=new UndoManager(MAX_UNDO);
        libraryUndoListener=null;
        tempDir=new Vector<String>();
        cl =null;
    }

    /** Check if undo operation is available.
        @return true if there are actions that can be undone.
    */
    public boolean canUndo()
    {
        return um.canUndo();
    }

    /** Check if redo operation is available.
        @return true if there are actions that can be redone.
    */
    public boolean canRedo()
    {
        return um.canRedo();
    }

    /** Undo the last editing action
    */
    public void undo()
    {
        UndoState r = um.undoPop();

        // Check if it is an operation involving libraries.
        if(um.isNextOperationOnALibrary() && libraryUndoListener!=null) {
            libraryUndoListener.undoLibrary(r.libraryDir);
        }

        if(!"".equals(r.text)) {
            StringBuffer s=new StringBuffer(r.text);
            pa.parseString(s);
        }
        isModified = r.isModified;
        pa.openFileName = r.fileName;

        if(cl!=null) { cl.somethingHasChanged(); }
    }

    /** Redo the last undo action
    */
    public void redo()
    {
        UndoState r = um.undoRedo();
        if(r.libraryOperation && libraryUndoListener!=null) {
            libraryUndoListener.undoLibrary(r.libraryDir);
        }

        if(!"".equals(r.text)) {
            StringBuffer s=new StringBuffer(r.text);
            pa.parseString(s);
        }

        isModified = r.isModified;
        pa.openFileName = r.fileName;

        if(cl!=null) { cl.somethingHasChanged(); }
    }


    /** Save the undo state, in the case an editing operation
        has been done on the drawing.
    */
    public void saveUndoState()
    {
        UndoState s = new UndoState();

        // In fact, the whole drawing is stored as a text.
        // In this way, we can easily store it on a string.
        s.text=pa.getText(true).toString();

        s.isModified=isModified;
        s.fileName=pa.openFileName;
        s.libraryDir=tempLibraryDirectory;
        s.libraryOperation=false;

        um.undoPush(s);
        isModified = true;
        if(cl!=null) { cl.somethingHasChanged(); }
    }

    /** Save the undo state, in the case an editing operation
        has been performed on a library.
        @param t the library directory to be used.
    */
    public void saveUndoLibrary(String t)
    {
        tempLibraryDirectory=t;
        UndoState s = new UndoState();
        s.text=pa.getText(true).toString();
        s.libraryDir=tempLibraryDirectory;
        s.isModified=isModified;
        s.fileName=pa.openFileName;
        s.libraryOperation=true;
        tempDir.add(t);
        um.undoPush(s);
    }

    /** Define a listener for a undo operation involving libraries.
    @param l the library undo listener.
    */
    public void setLibraryUndoListener(LibraryUndoListener l)
    {
        libraryUndoListener = l;
    }

    /** Determine if the drawing has been modified.
        @return the state.
    */
    public boolean getModified ()
    {
        return isModified;
    }

    /** Set the drawing modified state.
        @param s the new state to be set.
    */
    public void setModified (boolean s)
    {
        isModified = s;
        if(cl!=null) { cl.somethingHasChanged(); }
    }

    /** Set the listener of the state change.
        @param l the new listener.
    */
    public void setHasChangedListener (HasChangedListener l)
    {
        cl = l;
    }

    /** Clear all temporary files and directories created by the library undo
        system.
    */
    public void doTheDishes()
    {
        for (String fileName:tempDir)
        {
            try {
                FileUtils.deleteDirectory(new File(fileName));
            } catch (IOException eE) {
                System.out.println("Warning: "+eE);
            }
        }
    }
}