package fidocadj.macropicker.model;

import java.util.*;

import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.*;
import javax.swing.plaf.metal.MetalIconFactory;

import fidocadj.librarymodel.LibraryModel;
import fidocadj.librarymodel.Library;
import fidocadj.librarymodel.Category;
import fidocadj.librarymodel.event.LibraryListener;
import fidocadj.librarymodel.event.AddEvent;
import fidocadj.librarymodel.event.KeyChangeEvent;
import fidocadj.librarymodel.event.RemoveEvent;
import fidocadj.librarymodel.event.RenameEvent;
import fidocadj.primitives.MacroDesc;

/** JTree model for showing macro library.
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
public class MacroTreeModel implements TreeModel,LibraryListener
{
    public static final int ROOT=0;
    public static final int LIBRARY=1;
    public static final int CATEGORY=2;
    public static final int MACRO=3;

    private RootNode rootNode;
    final private LibraryModel libraryModel;
    final private List<TreeModelListener> listeners;

    private Map<TreePath, AbstractMacroTreeNode> libraryNodeMap;

    private String filterWord;

    /** Constructor.
        @param libraryModel the library model to be associated to this class.
    */
    public MacroTreeModel(LibraryModel libraryModel)
    {
        this.libraryModel = libraryModel;
        listeners = new ArrayList<TreeModelListener>();
        createMap();
        synchronizeTree(null);
        fireChanged();
    }

    /** Set filtering word.
        @param filterWord words separated by space.
     */
    public void setFilterWord(String filterWord)
    {
        this.filterWord = filterWord;

        if(filterWord == null || filterWord.length()==0) {
            synchronizeTree(null);
            this.filterWord = null;
            fireChanged();
        } else {
            Locale lo = Locale.forLanguageTag("en");
            final String chainedWord = filterWord.toLowerCase(lo);
            synchronizeTree(new NodeFilterInterface() {
                public boolean accept(MacroTreeNode node)
                {
                    String[] words = chainedWord.trim().split(" ");
                    int matched=0;
                    for(String word:words) {
                        if(0<=node.toString().toLowerCase(lo).indexOf(word)) {
                            matched++;
                        } else if(word.length()==0) {
                            matched++;
                        }
                    }
                    return words.length==matched;
                }
            });
            fireChanged();
        }
    }

    /** Reset the current search mode.
    */
    private void resetSearchMode()
    {
        filterWord = null;
    }

    /** Check if a search is currently being made.
        @return true if a search is active.
    */
    public boolean isSearchMode()
    {
        return filterWord != null;
    }

    /** Create the map of nodes constituting the library.
    */
    private void createMap()
    {
        libraryNodeMap = new HashMap<TreePath, AbstractMacroTreeNode>();
    }

    /** Get the type of the specified node.
        @param path the node to analyze.
        @return the kind of the node: ROOT, LIBRARY, CATEGORY, MACRO or -1
            if the type could not be retrieved.
    */
    public int getNodeType(TreePath path)
    {
        Object o;

        if(path==null) { return -1; }

        o = path.getLastPathComponent();
        if(o instanceof RootNode) {
            return ROOT;
        } else if(o instanceof LibraryNode) {
            return LIBRARY;
        } else if(o instanceof CategoryNode) {
            return CATEGORY;
        } else if(o instanceof MacroNode) {
            return MACRO;
        }

        return -1;
    }

    /** Return filtering word.
        @return null or filtering word.
     */
    public String getFilterWord()
    {
        return filterWord;
    }

    /** Implements TreeModel interface.
        Add a tree model listener.
        @param l the tree model listener.
    */
    @Override public void addTreeModelListener(TreeModelListener l)
    {
        listeners.add(l);
    }

    /** Implements TreeModel interface .
        Get the child object.
        @param parent the parent object.
        @param index the index of the child.
        @return the child object retrieved.
    */
    @Override public Object getChild(Object parent, int index)
    {
        return ((TreeNode)parent).getChildAt(index);
    }

    /** Implements TreeModel interface. Get the number of children.
        @param parent the parent object.
        @return the number of the children.
    */
    @Override public int getChildCount(Object parent)
    {
        return ((TreeNode)parent).getChildCount();
    }

    /** Implements TreeModel interface. Get the index of the given child.
        @param parent the parent object.
        @param child the child to search for.
        @return the index of the child or -1 if the child has not been found.
        TODO: check if it is true that it is -1...
    */
    @Override public int getIndexOfChild(Object parent, Object child)
    {
        return ((TreeNode)parent).getIndex((TreeNode)child);
    }

    /** Implements TreeModel interface.
        Get the root node.
        @return the root node.
    */
    @Override public Object getRoot()
    {
        return rootNode;
    }

    /** Implements TreeModel interface.
        Check if the given node is a leaf.
        @param node the node to check.
        @return true if the node is a leaf (in this context, a macro).
    */
    @Override public boolean isLeaf(Object node)
    {
        return ((TreeNode)node).isLeaf();
    }

    /** Implements TreeModel interface.
        Remove the given listener.
        @param l the listener to remove.
    */
    @Override public void removeTreeModelListener(TreeModelListener l)
    {
        listeners.remove(l);
    }

    /** Implements TreeModel interface.
        TODO: improve the documentation. What is that supposed to do? Is the
        implementation complete in the code?
        @param path the path.
        @param newValue the new value.
    */
    @Override public void valueForPathChanged(TreePath path, Object newValue)
    {
        // NOP
    }

    /** Get a certain macro (leaf in this context).
        @param path the path where the macro has to be searched for.
        @return the macro.
    */
    public MacroDesc getMacro(TreePath path)
    {
        Object o;

        if(path==null) { return null; }

        o = getPathComponentAt(path,3);
        if(o instanceof MacroNode) {
            return ((MacroNode)o).getMacro();
        } else {
            return null;
        }
    }

    /** Get a certain category.
        @param path the path where the category has to be searched for.
        @return the category.
    */
    public Category getCategory(TreePath path)
    {
        Object o;

        if(path==null) { return null; }

        o = getPathComponentAt(path,2);
        if(o instanceof CategoryNode) {
            return ((CategoryNode)o).getCategory();
        } else {
            return null;
        }
    }

    /** Get a certain library.
        @param path the path where the library has to be searched for.
        @return the library.
    */
    public Library getLibrary(TreePath path)
    {
        Object o;

        if(path==null) { return null; }

        o = getPathComponentAt(path,1);
        if(o instanceof LibraryNode) {
            return ((LibraryNode)o).getLibrary();
        } else {
            return null;
        }
    }

    private Object getPathComponentAt(TreePath path,int index)
    {
        if(index <= path.getPathCount()-1) {
            return path.getPathComponent(index);
        } else {
            return null;
        }
    }

    /** Notify that the tree has changed and it requires a refresh.
    */
    private void fireChanged()
    {
        for(TreeModelListener l:listeners) {
            l.treeStructureChanged(new TreeModelEvent((Object)this,
                                   new TreePath(rootNode),null,null));
        }
    }

    private TreePath createAbsolutePath(TreeNode lastNode)
    {
        TreeNode parentNode = lastNode.getParent();
        if(parentNode==null){
            return new TreePath(lastNode);
        } else {
            return createAbsolutePath(parentNode).pathByAddingChild(lastNode);
        }
    }

    /** Called when a library node has to be renamed.
        @param e the rename event.
    */
    public void libraryNodeRenamed(RenameEvent e)
    {
        Object renamedNode = e.getRenamedNode();
        TreePath renamedPath;
        TreeNode renamedMacroTreeNode;

        if(renamedNode==null) {
            fireTreeNodeChanged(new TreePath(rootNode));
        } else {
            for(TreePath path:libraryNodeMap.keySet()){
                if(path.getLastPathComponent().equals(renamedNode)){
                    renamedMacroTreeNode = (TreeNode)libraryNodeMap.get(path);
                    renamedPath = createAbsolutePath(renamedMacroTreeNode);
                    fireTreeNodeChanged(renamedPath);
                }
            }
        }
    }

    /** Called when a library node has to be removed.
        @param e the ermove event.
    */
    public void libraryNodeRemoved(RemoveEvent e)
    {
        Object parentNode;
        TreePath parentPath;
        TreeNode parentMacroTreeNode;

        resetSearchMode();
        synchronizeTree(null);

        parentNode = e.getParentNode();
        if(parentNode==null) {
            fireTreeStructureChanged(new TreePath(rootNode));
        } else {
            for(TreePath path:libraryNodeMap.keySet()){
                if(path.getLastPathComponent().equals(parentNode)){
                    parentMacroTreeNode = (TreeNode)libraryNodeMap.get(path);
                    parentPath = createAbsolutePath(parentMacroTreeNode);
                    fireTreeStructureChanged(parentPath);
                }
            }
        }
    }

    /** Called when a library node has to be daded.
        @param e the add event.
    */
    public void libraryNodeAdded(AddEvent e)
    {
        Object parentNode;
        TreePath parentPath;
        TreeNode parentMacroTreeNode;

        resetSearchMode();
        synchronizeTree(null);

        parentNode = e.getParentNode();
        if(parentNode==null) {
            fireTreeStructureChanged(new TreePath(rootNode));
        } else {
            for(TreePath path:libraryNodeMap.keySet()){
                if(path.getLastPathComponent().equals(parentNode)){
                    parentMacroTreeNode = (TreeNode)libraryNodeMap.get(path);
                    parentPath = createAbsolutePath(parentMacroTreeNode);
                    fireTreeStructureChanged(parentPath);
                }
            }
        }
    }

    /** Called when a library node has to be changed.
        TODO: is this unimplemented?
        @param e the changed event.
    */
    public void libraryNodeKeyChanged(KeyChangeEvent e)
    {
        // Nothing to do here
    }

    /** To be called when a new library has been loaded.
    */
    public void libraryLoaded()
    {
        resetSearchMode();
        synchronizeTree(null);
        fireChanged();
    }

    private void fireTreeNodeChanged(TreePath path)
    {
        if(path!=null) {
            for(TreeModelListener l:listeners) {
                l.treeNodesChanged(new TreeModelEvent(this, path));
            }
        }
    }

    private void fireTreeStructureChanged(TreePath path)
    {
        if(path!=null){
            for(TreeModelListener l:listeners) {
                l.treeStructureChanged(new TreeModelEvent(this, path));
            }
        }
    }


    /** Performs a synchronization of the library tree with the current
        contents of the library model. It can be called when a research is
        done, to obtain the results shown in the tree.

        @param filter filtering rules to be applied.
    */
    private void synchronizeTree(NodeFilterInterface filter)
    {
        LibraryNode ln;
        CategoryNode cn;
        MacroNode mn;

        TreePath libraryPath;
        TreePath categoryPath;
        TreePath macroPath;

        // Save a copy of the current library note
        Map<TreePath,AbstractMacroTreeNode> tmpMap =libraryNodeMap;

        libraryNodeMap = new HashMap<TreePath, AbstractMacroTreeNode>();

        if(rootNode==null) {
            rootNode = new RootNode();
        }

        if(filter==null) {
            rootNode.setLabel("FidoCadJ");
        } else {
            rootNode.setLabel("Search results...");
        }

        rootNode.clearChildNodes();

        for(Library library:libraryModel.getAllLibraries()) {
            libraryPath = new TreePath(library);
            if(libraryNodeMap.containsKey(libraryPath)){
                ln = (LibraryNode)tmpMap.get(libraryPath);
                ln.clearChildNodes();
            } else {
                ln = new LibraryNode(library);
            }

            for(Category category:library.getAllCategories()) {
                if(category.isHidden()) {
                    continue;
                }
                categoryPath = libraryPath.pathByAddingChild(category);
                if(tmpMap.containsKey(categoryPath)){
                    cn = (CategoryNode)tmpMap.get(categoryPath);
                    cn.clearChildNodes();
                } else {
                    cn = new CategoryNode(category);
                }

                for(MacroDesc macro:category.getAllMacros()) {
                    macroPath = categoryPath.pathByAddingChild(macro);
                    if(tmpMap.containsKey(macroPath)){
                        mn = (MacroNode)tmpMap.get(macroPath);
                    } else {
                        mn = new MacroNode(macro);
                    }

                    if(filter!=null && !filter.accept(mn)) {
                        // If the search hasn't been successful, don't show the
                        // current macro in the category.
                        continue;
                    }
                    cn.addMacroNode(mn);
                    libraryNodeMap.put(macroPath,mn);
                }
                if(filter!=null && cn.getChildCount()==0) {
                    // If the no macros are to be shown, don't show the
                    // current category in the library.
                    continue;
                }
                ln.addCategoryNode(cn);
                libraryNodeMap.put(categoryPath,cn);
            }
            if(filter!=null && ln.getChildCount()==0) {
                // If no categories are to be shown, don't show the
                // current library.
                continue;
            }
            rootNode.addLibraryNode(ln);
            libraryNodeMap.put(libraryPath,ln);
        }

        rootNode.sortTree();
    }

    private static class RootNode extends AbstractMacroTreeNode
    {
        RootNode()
        {
            parent = null;
            label = "FidoCadJ";
            icon = MetalIconFactory.getTreeComputerIcon();
        }

        RootNode(String label)
        {
            parent=null;
            this.label=label;
            icon = MetalIconFactory.getTreeComputerIcon();
        }

        RootNode(String label,Icon icon)
        {
            parent=null;
            this.label=label;
            this.icon = icon;
        }

        public void addLibraryNode(LibraryNode node)
        {
            childNodes.add(node);
            node.setParent((TreeNode)this);
        }

        public void setLabel(String label)
        {
            this.label = label;
        }
    }

    private static class LibraryNode extends AbstractMacroTreeNode
        implements Comparable<LibraryNode>
    {
        final private Library library;

        LibraryNode(Library library)
        {
            this.library = library;
            if(library.isStdLib()) {
                icon = MetalIconFactory.getTreeHardDriveIcon();
            } else {
                icon = MetalIconFactory.getTreeFloppyDriveIcon();
            }
        }

        public Library getLibrary()
        {
            return library;
        }

        public void addCategoryNode(CategoryNode node)
        {
            childNodes.add(node);
            node.setParent((TreeNode)this);
        }

        @Override public int compareTo(LibraryNode node)
        {
            Library l1 = this.library;
            Library l2 = node.getLibrary();
            if(l1.isStdLib() == l2.isStdLib()) {
                return l1.getName().compareToIgnoreCase(l2.getName());
            } else {
                if(l1.isStdLib()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }

        /** Convert to string.
            @return the name of the node.
        */
        @Override public String toString()
        {
            return library.getName();
        }

        /** Inherit the behavior of compareTo.
        */
        @Override public boolean equals(Object node)
        {
            return node instanceof LibraryNode &&
                compareTo((LibraryNode)node)==0;
        }

        /** No implementation of the hashCode for the moment
            @return 42, because it is The Answer.
        */
        @Override public int hashCode()
        {
            assert false : "hashCode not designed";
            return 42; // any arbitrary constant will do
        }
    }

    private static class CategoryNode extends AbstractMacroTreeNode
        implements Comparable<CategoryNode>
    {
        final private Category category;

        CategoryNode(Category category)
        {
            this.category = category;
            icon = null;
        }

        public Category getCategory()
        {
            return category;
        }

        public void addMacroNode(MacroNode node)
        {
            childNodes.add(node);
            node.setParent((TreeNode)this);
        }

        @Override public int compareTo(CategoryNode node)
        {
            Category c1 = this.category;
            Category c2 = node.getCategory();
            return c1.getName().compareToIgnoreCase(c2.getName());
        }


        @Override public String toString()
        {
            return category.getName();
        }

        /** Inherit the behavior of compareTo.
        */
        @Override public boolean equals(Object node)
        {
            return node instanceof CategoryNode &&
                compareTo((CategoryNode)node)==0;
        }

        /** No implementation of the hashCode for the moment
            @return 42, because it is The Answer.
        */
        @Override public int hashCode()
        {
            assert false : "hashCode not designed";
            return 42; // any arbitrary constant will do
        }
    }

    private static class MacroNode extends AbstractMacroTreeNode
        implements Comparable<MacroNode>
    {
        final MacroDesc macro;

        MacroNode(MacroDesc macroDesc)
        {
            this.macro = macroDesc;
            icon = null;
        }

        public MacroDesc getMacro()
        {
            return macro;
        }

        @Override public boolean isLeaf()
        {
            return true;
        }

        /** Compare two nodes. The comparison is done with respect to the name
            and if the name is equal, then the key is compared too.
        */
        @Override public int compareTo(MacroNode node)
        {
            MacroDesc m1 = this.macro;
            MacroDesc m2 = node.getMacro();
            // At first, compare the two nodes using their name
            int r=m1.name.compareToIgnoreCase(m2.name);
            // If they have the same name, look at the keys.
            if(r==0) {
                r=m1.key.compareToIgnoreCase(m2.key);
            }
            return r;
        }

        @Override public String toString()
        {
            return macro.name;
        }

        /** Inherit the behavior of compareTo.
        */
        @Override public boolean equals(Object node)
        {
            return node instanceof MacroNode &&
                compareTo((MacroNode)node)==0;
        }

        /** No implementation of the hashCode for the moment
            @return 42, because it is The Answer.
        */
        @Override public int hashCode()
        {
            assert false : "hashCode not designed";
            return 42; // any arbitrary constant will do
        }
    }
}
