package fidocadj.macropicker.model;

import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;

/** Abstract class for MacroTreeNode.
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
public abstract class AbstractMacroTreeNode implements MacroTreeNode
{
    protected Vector<AbstractMacroTreeNode> childNodes; // NOPMD
    protected TreeNode parent;
    protected String label;         // The description of the node
    protected Icon icon;

    /** Standard constructor.
    */
    AbstractMacroTreeNode()
    {
        childNodes = new Vector<AbstractMacroTreeNode>();
        label="";
    }

    /** Implements TreeNode interface.
        Get the children nodes.
        @return the child nodes elements.
    */
    public Enumeration<AbstractMacroTreeNode> children()
    {
        return childNodes.elements();
    }

    /** Implements TreeNode interface
        @return true if children are allowed.
    */
    public boolean getAllowsChildren()
    {
        return true;
    }

    /** Implements TreeNode interface.
        Retrieve a particular child.
        @param childIndex the index of the child
        @return the node containing the child.
    */
    public TreeNode getChildAt(int childIndex)
    {
        return (TreeNode)childNodes.get(childIndex);
    }

    /** Implements TreeNode interface
        @return the number of children.
    */
    public int getChildCount()
    {
        return childNodes.size();
    }

    /** Implements TreeNode interface.
        Get the index of a particular node.
        @param node the node to be searched for.
        @return the index of the node or -1 if the node has not been found.
    */
    public int getIndex(TreeNode node)
    {
        return childNodes.indexOf(node);
    }

    /** Implements TreeNode interface. Get the parent node.
        @return the parent node.
    */
    public TreeNode getParent()
    {
        return parent;
    }

    /** Set the parent node.
        @param parent the parent node.
    */
    public void setParent(TreeNode parent)
    {
        this.parent = parent;
    }

    /** Implements TreeNode interface.
        Check if it is a leaf.
        @return boolean false default.
     */
    public boolean isLeaf()
    {
        return false;
    }

    /** Sort recursively.
    */
    public void sortTree()
    {
        Collections.sort(childNodes, new Comparator<AbstractMacroTreeNode>(){
                @Override public int compare(AbstractMacroTreeNode o1,
                    AbstractMacroTreeNode o2)
                {
                    return o1.toString().compareTo(o2.toString());
                }
            });
        for(Object n:childNodes) {
            ((AbstractMacroTreeNode)n).sortTree();
        }
    }

    /** Remove a child node.
        @param node the node to be removed.
    */
    public void removeChildNode(TreeNode node)
    {
        childNodes.remove(node);
    }

    /** Get the index of the provided node.
        @param node the node to be searched for.
        @return the node index, or -1 if the node has not been found.
    */
    public int getIndexAt(TreeNode node)
    {
        return childNodes.indexOf(node);
    }

    /** Get the icon associated to the tree.
        @return the icon.
    */
    public Icon getIcon()
    {
        return icon;
    }

    /** Get a string describing the tree.
        @return the description (the label).
    */
    @Override public String toString()
    {
        return label;
    }

    /** Clear (remove) all child nodes.
    */
    public void clearChildNodes()
    {
        childNodes.clear();
    }
}
