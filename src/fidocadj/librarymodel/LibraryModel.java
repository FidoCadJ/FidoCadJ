package fidocadj.librarymodel;

import java.util.*;
import java.io.FileNotFoundException;
import java.io.IOException;

import fidocadj.undo.UndoActorListener;
import fidocadj.circuit.model.DrawingModel;
import fidocadj.globals.LibUtils;
import fidocadj.librarymodel.event.KeyChangeEvent;
import fidocadj.librarymodel.event.LibraryListener;
import fidocadj.librarymodel.event.RemoveEvent;
import fidocadj.librarymodel.event.AddEvent;
import fidocadj.librarymodel.event.RenameEvent;
import fidocadj.primitives.MacroDesc;

// TODO: comment public methods
// NOTE: This model has no adding macro method.

/** Model class for macro operation.

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

   Copyright 2014-2023 Kohta Ozaki - Davide Bucci
*/
public class LibraryModel
{
    final private List<LibraryListener> libraryListeners;
    final private DrawingModel drawingModel;
    final private List<Library> libraries;

    // Bridge for existing system.
    private Map<String,MacroDesc> masterLibrary;

    private UndoActorListener undoActorListener;

    /**
        Costructor.
        @param drawingModel DrawingModel instance to fetch macros.
    */
    public LibraryModel(DrawingModel drawingModel)
    {
        this.drawingModel = drawingModel;
        libraryListeners = new ArrayList<LibraryListener>();
        libraries = new ArrayList<Library>();
        updateLibraries();
    }

    /**
        Adds LibraryListener.
        @param listener LibraryLisner.
    */
    public void addLibraryListener(LibraryListener listener)
    {
        libraryListeners.add(listener);
    }

    /**
        Removes LibraryListener.
        @param listener LibraryListener.
    */
    public void removeLibraryListener(LibraryListener listener)
    {
        libraryListeners.remove(listener);
    }

    /**
        Removes category from library.
        Notices LibraryListeners after removed.
        @param category Category to remove.
        @throws IllegalLibraryAccessException If access standard library.
    */
    public void remove(Category category)
        throws IllegalLibraryAccessException
    {
        Library parentLibrary;

        if(category==null) {
            return;
        }

        parentLibrary = category.getParentLibrary();

        if(parentLibrary.isStdLib()) {
            throw new IllegalLibraryAccessException(
                "A category in standard library can't be removed.");
        }

        parentLibrary.removeCategory(category);
        synchronizeMasterLibrary();
        save();
        saveLibraryState();
        fireRemoved(parentLibrary,category);
    }

    /**
        Removes library and deletes file.
        Notices LibraryListeners after removed.
        @param library Library to remove.
        @throws IllegalLibraryAccessException If access standard library.
    */
    public void remove(Library library)
        throws IllegalLibraryAccessException
    {
        // NOTE: We must consider this method contains deleting file.

        if(library==null) {
            return;
        }

        if(library.isStdLib()) {
            throw new IllegalLibraryAccessException(
                "A standard library can't be removed.");
        }

        libraries.remove(library);
        synchronizeMasterLibrary();
        try{
            LibUtils.deleteLib(library.getFilename());
        } catch (FileNotFoundException e){
            System.out.println("library not found:"+library.getFilename());
        } catch (IOException e) {
            System.out.println("Exception: "+e);
        }
        saveLibraryState();
        fireRemoved(null,library);
    }

    /**
        Removes macro from library.
        Notices LibraryListeners after removed.
        @param macro MacroDesc to remove.
        @throws IllegalLibraryAccessException If access standard library.
    */
    public void remove(MacroDesc macro)
        throws IllegalLibraryAccessException
    {
        Category category;

        if(macro==null) {
            return;
        }

        category = (Category)getParentNode(macro);

        if(category==null) {
            throw new IllegalLibraryAccessException("It's a wondering macro.");
        }
        if(category.getParentLibrary().isStdLib()) {
            throw new IllegalLibraryAccessException(
                "A standard library can't be removed.");
        }

        category.removeMacro(macro);
        synchronizeMasterLibrary();
        save();
        saveLibraryState();
        fireRemoved(category,macro);
    }

    /**
        Renames macro.
        Notices LibraryListeners after renamed.
        @param macro MacroDesc to rename.
        @param newName New macro name.
        @throws IllegalLibraryAccessException If access standard library.
        @throws IllegalNameException If new name is invalid.
    */
    public void rename(MacroDesc macro,String newName)
        throws IllegalNameException, IllegalLibraryAccessException
    {
        if(macro==null) {
            return;
        }

        String oldName = macro.name;

        //validation
        if(newName==null || newName.length()==0) {
            throw new IllegalNameException("Name length must not be zero.");
        }

        if(isStdLib(macro)) {
            throw new IllegalLibraryAccessException(
                "A macro in standard library can't be renamed.");
        }

        // TODO:validation
        // macro.isValidName(newName);

        macro.name = newName;

        save();
        saveLibraryState();
        fireRenamed(getParentNode(macro),macro,oldName);
    }

    /**
        Renames category.
        Notices LibraryListeners after renamed.
        @param category Category to rename.
        @param newName New category name.
        @throws IllegalLibraryAccessException If access standard library.
        @throws IllegalNameException If new name is invalid.
    */
    public void rename(Category category,String newName)
        throws IllegalNameException,IllegalLibraryAccessException
    {
        String oldName = category.getName();

        //validation
        if(newName==null || newName.length()==0) {
            throw new IllegalNameException("Name length must not be zero.");
        }

        if(category.getParentLibrary().isStdLib()) {
            throw new IllegalLibraryAccessException(
                 "A category in standard library can't be renamed.");
        }

        if(!Category.isValidName(newName)) {
            throw new IllegalNameException("invalid name");
        }

        category.setName(newName);
        synchronizeMacros(category.getParentLibrary());
        save();
        saveLibraryState();
        fireRenamed(getParentNode(category),category,oldName);
    }

    /**
        Renames library.
        Notices LibraryListeners after renamed.
        @param library Library to rename.
        @param newName New library name.
        @throws IllegalLibraryAccessException If access standard library.
        @throws IllegalNameException If new name is invalid.
    */
    public void rename(Library library,String newName)
        throws IllegalNameException,IllegalLibraryAccessException
    {
        String oldName = library.getName();

        //validation
        if(newName==null || newName.length()==0) {
            throw new IllegalNameException("Name length must not be zero.");
        }

        if(library.isStdLib()) {
            throw new IllegalLibraryAccessException(
                "A standard library can't be renamed.");
        }

        if(!Library.isValidName(newName)) {
            throw new IllegalNameException("invalid name");
        }

        library.setName(newName);
        synchronizeMacros(library);
        synchronizeMasterLibrary();
        save();
        saveLibraryState();
        fireRenamed(null,library,oldName);
    }

    /**
        Copies macro into category.
        Notices LibraryListeners after copied.
        @param macro target macro.
        @param destCategory destination category.
    */
    public void copy(MacroDesc macro, Category destCategory)
    {
        //TODO: Standard library check.
        MacroDesc newMacro;
        System.out.println("copy:"+macro+destCategory);

        newMacro = copyMacro(macro,destCategory);
        synchronizeMacros(destCategory.getParentLibrary());
        synchronizeMasterLibrary();
        save();
        saveLibraryState();
        fireAdded(destCategory,newMacro);
    }

    /**
        Utility function.
     */
    private MacroDesc copyMacro(MacroDesc macro, Category destCategory)
    {
        MacroDesc newMacro;
        String newPlainKey;
        int retry;

        if(macro==null || destCategory==null) {
            return null;
        }

        newMacro = cloneMacro(macro);
        newPlainKey = createRandomMacroKey();
        for(retry=20; 0<retry; retry--) {
            if(!destCategory.getParentLibrary().containsMacroKey(newPlainKey)) {
                break;
            }
        }
        if(retry<0) {
            throw new RuntimeException("Key generation failed.");
        }
        newMacro.key = createMacroKey(destCategory.getParentLibrary().
                                      getFilename(),newPlainKey);
        destCategory.addMacro(newMacro);

        return newMacro;
    }

    /**
        Copies category into library.
        Notices LibraryListeners after copied.
        @param category target category.
        @param destLibrary destination library.
    */
    public void copy(Category category, Library destLibrary)
    {
        //TODO: Standard library check.
        Category newCategory;

        if(category==null || destLibrary==null) {
            return;
        }

        newCategory = new Category(category.getName(),
                                   category.getParentLibrary(),
                                   false);

        for(MacroDesc macro:category.getAllMacros()) {
            copyMacro(macro,newCategory);
        }
        destLibrary.addCategory(newCategory);

        synchronizeMacros(destLibrary);
        synchronizeMasterLibrary();
        save();
        saveLibraryState();
        fireAdded(destLibrary, newCategory);
    }

    /**
        Changes macro key.
        Notices LibraryListeners after changed.
        @param macro MacroDesc to change key.
        @param newKey New macro key without library prefix.
        @throws IllegalLibraryAccessException If access standard library.
        @throws IllegalKeyException If new key is invalid.
    */
    public void changeKey(MacroDesc macro,String newKey)
        throws IllegalKeyException,IllegalLibraryAccessException
    {
        String oldKey;
        Category category;

        if(macro==null || newKey.length()==0) {
            throw new IllegalKeyException("Name length must not be zero.");
        }

        if(isStdLib(macro)) {
            throw new IllegalLibraryAccessException(
                "A macro in standard library can't be renamed.");
        }

        // key validation

        // key exists check
        category = (Category)getParentNode(macro);
        if(category.getParentLibrary().containsMacroKey(newKey)) {
            throw new IllegalKeyException("New key already exists.");
        }

        oldKey = getPlainMacroKey(macro);
        macro.key = createMacroKey(macro.filename,newKey);
        save();
        saveLibraryState();
        fireKeyChanged(getParentNode(macro),macro,oldKey);
    }

    /**
        Utility function.
    */
    private MacroDesc cloneMacro(MacroDesc macro)
    {
        return new MacroDesc(macro.key,
           macro.name,
           macro.description,
           macro.category,
           macro.library,
           macro.filename);
    }

    /**
        Returns macro key without library prefix.
        @param macro macro.
        @return String plain key.
    */
    public static String getPlainMacroKey(MacroDesc macro)
    {
        String[] parted;

        if(macro==null) {
            return null;
        }
        parted = macro.key.split("\\.");

        if(1<parted.length) {
            return parted[1];
        } else {
            return parted[0];
        }
    }

    /**
        Returns new macro key.
        @return String plain key.
    */
    public static String createRandomMacroKey()
    {
        long t=System.nanoTime();
        long h=0;
        for(int i=0; t>0; ++i) {
            t>>=i*8;
            h^=t & 0xFF;
        }

        return String.valueOf(h);
    }

    /**
        Returns identifiable macro key.
        @param fileName filename of library.
        @param key plain key.
        @return String key.
    */
    public static String createMacroKey(String fileName,String key)
    {
        String macroKey = fileName+"."+key;
        return macroKey.toLowerCase(Locale.forLanguageTag("en"));
    }

    /**
        Synchronizes MacroDesc's properties with library.
     */
    private void synchronizeMacros(Library library)
    {
        String plainKey;

        if(library.isStdLib()) {
            return;
        }
        for(Category category:library.getAllCategories()) {
            for(MacroDesc m:category.getAllMacros()) {
                m.category = category.getName();
                m.library = library.getName();
                m.filename = library.getFilename();
                plainKey = getPlainMacroKey(m);
                m.key = createMacroKey(library.getFilename(),plainKey);
            }
        }
    }

    /**
        Sets UndoActorListener.
        @param undoActorListener UndoActorListener.
    */
    public void setUndoActorListener(UndoActorListener undoActorListener)
    {
        this.undoActorListener = undoActorListener;
    }

    /**
        Returns true if macro is in standard library.
        This method will be removed in the future.
        @param macro MacroDesc
    */
    private boolean isStdLib(MacroDesc macro)
    {
        // An alternative way to see if a macro is standard or not
        // is to extract the prefix from the key and to see if the
        // prefix is "" or the one of the standard libraries.

        for(Library l:getAllLibraries()) {
            if(l.isStdLib()) {
                for(Category c:l.getAllCategories()) {
                    for(MacroDesc m:c.getAllMacros()) {
                        if(macro.equals(m)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void fireAdded(Object parentNode,Object addedNode)
    {
        for(LibraryListener l:libraryListeners) {
            l.libraryNodeAdded(new AddEvent(parentNode,addedNode));
        }
    }

    private void fireRenamed(Object parentNode,Object renamedNode,
                             String oldName)
    {
        for(LibraryListener l:libraryListeners) {
            l.libraryNodeRenamed(new RenameEvent(parentNode,renamedNode,
                                                 oldName));
        }
    }

    private void fireRemoved(Object parentNode,Object removedNode)
    {
        for(LibraryListener l:libraryListeners) {
            l.libraryNodeRemoved(new RemoveEvent(parentNode,removedNode));
        }
    }

    private void fireKeyChanged(Object parentNode,Object changedNode,
                                String oldKey)
    {
        for(LibraryListener l:libraryListeners) {
            l.libraryNodeKeyChanged(new KeyChangeEvent(parentNode,changedNode,
                                    oldKey));
        }
    }

    // NOTE: This implementation is incorrect. (DB why? equals may fail?)
    private Object getParentNode(Object node)
    {
        for(Library l:getAllLibraries()) {
            if(node.equals(l)) {
                return null;        // A library does not have any parent!
            }
            for(Category c:l.getAllCategories()) {
                if(node.equals(c)) {
                    return l;
                }
                for(MacroDesc m:c.getAllMacros()) {
                    if(node.equals(m)) {
                        return c;
                    }
                }
            }
        }
        return null;    // Node not found
    }

    /**
        Returns MacroDesc map.
        @return Map composed of String key and MacroDesc from parser.
    */
    public Map<String,MacroDesc> getAllMacros()
    {
        return masterLibrary;
    }

    /**
        Returns Libraries as list.
        @return List of Library objects.
    */
    public List<Library> getAllLibraries()
    {
        return libraries;
    }

    /**
        Saves library to file.
    */
    public void save()
    {
        //TODO: throw necessary exceptions.
        for(Library library:libraries){
            try{
                if(!library.isStdLib()){
                    LibUtils.save(masterLibrary,
                        LibUtils.getLibPath(library.getFilename()),
                        library.getName().trim(), library.getFilename());
                }
            } catch (FileNotFoundException e) {
                System.out.println("Error accessing to the file.");
            }
        }
    }

    /**
        Saves library state for undo.
    */
    public void saveLibraryState()
    {
        try {
            LibUtils.saveLibraryState(undoActorListener);
        } catch (IOException e) {
            System.out.println("Exception: "+e);
        }
    }

    /**
        Not implemented.
    */
    public void undoLibrary()
    {
        // TODO: this should undo the last library operation.

    }

    /**
        Updates library.
    */
    public void forceUpdate()
    {
        updateLibraries();
        fireChanged();
    }

    private void fireChanged()
    {
        for(LibraryListener l:libraryListeners) {
            l.libraryLoaded();
        }
    }

    /**
        Bridges existing components.
        This method will be removed in the future.
    */
    private void synchronizeMasterLibrary()
    {
        masterLibrary.clear();
        for(Library library:libraries) {
            for(Category category:library.getAllCategories()) {
                for(MacroDesc macro:category.getAllMacros()) {
                    masterLibrary.put(macro.key,macro);
                }
            }
        }
    }

    private void updateLibraries()
    {
        Library library;
        Category category;
        boolean catIsHidden;
        String key;
        Map<String,Library> tmpLibraryMap = new HashMap<String,Library>();
        masterLibrary = drawingModel.getLibrary();
        libraries.clear();

        for(MacroDesc md:masterLibrary.values()) {
            cleanMacro(md);
            key = md.filename + "/" + md.library;
            if(tmpLibraryMap.containsKey(key)) {
                library = tmpLibraryMap.get(key);
            } else {
                library = new
                Library(md.library,md.filename,LibUtils.isStdLib(md));
                tmpLibraryMap.put(key,library);
                libraries.add(library);
            }
            if(library.getCategory(md.category)==null) {
                catIsHidden = "hidden".equals(md.category);
                category = new Category(md.category,library,catIsHidden);
                library.addCategory(category);
            } else {
                category = library.getCategory(md.category);
            }
            category.addMacro(md);
        }
        fireChanged();
    }

    private void cleanMacro(MacroDesc macro)
    {
        macro.name = macro.name.trim();
    }

    /**
        Exception for library operation error.
    */
    public class LibraryException extends Exception
    {
        private static final long serialVersionUID = 1L;
        LibraryException(String message)
        {
            super(message);
        }
    }

    /**
        Exception for an illegal name (when searching, etc...)
    */
    public class IllegalNameException extends LibraryException
    {
        private static final long serialVersionUID = 1L;
        IllegalNameException(String message)
        {
            super(message);
        }
    }

    /**
        Exception for an illegal library access (i.e. non existant,
        most of the times).
    */
    public class IllegalLibraryAccessException extends LibraryException
    {
        private static final long serialVersionUID = 1L;
        IllegalLibraryAccessException(String message)
        {
            super(message);
        }
    }

    /**
        Exception for an illegal key in a library.
    */
    public class IllegalKeyException extends LibraryException
    {
        private static final long serialVersionUID = 1L;
        IllegalKeyException(String message)
        {
            super(message);
        }
    }
}
