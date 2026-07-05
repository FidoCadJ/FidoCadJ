package fidocadj.macropicker;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;

/** Extended JTree for searching node.<BR>
    Features:<BR>
    Expands or collapses all nodes on paint event if specified.<BR>
    Selects leaf cyclic.<BR>

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
public final class ExpandableJTree extends JTree
{
    private static final long serialVersionUID = 1L;
    // runOnce = true means that a change in the expansion state of the tree
    // has been requested and it should be taken into account during the next
    // painting event.
    private boolean runOnce = false;

    // direction = true means that during the next repaint the tree should be
    // expanded.
    private boolean direction = false;

    /** The creator.
    */
    public ExpandableJTree()
    {
        super();
        // Apply a correction for the text size depending on the screen
        // resolution.
        final int base=114;
        int res=Toolkit.getDefaultToolkit().getScreenResolution();
        Font standardFont=getFont();
        setFont(standardFont.deriveFont(standardFont.getSize()*res/base));
        setRowHeight(getRowHeight()*res/base);
    }

    private void fillExpandState(boolean expand)
    {
        //NOTES:
        //This only switchs expand state.
        //Actually expanding/collapsing tree is on next repaint.
        TreePath path;
        for(int row=0; row<getRowCount(); ++row) {
            path = getPathForRow(row);
            if(!getModel().isLeaf(path.getLastPathComponent())) {
                setExpandedState(path, expand);
            }
        }
    }

    /** During the next repaint of the JTree, nodes will be expanded.
    */
    public void expandOnce()
    {
        runOnce = true;
        direction = true;
    }

    /** During the next repaint of the JTree, nodes will be collapsed.
    */
    public void collapseOnce()
    {
        runOnce = true;
        direction = false;
    }

    /** Select the next leaf, i.e. the one immediately after the one which
        is currently selected.
        If this is not possible (for example because the currently selected
        leaf is the last one available), it does nothing.
    */
    public void selectNextLeaf()
    {
        int nextRow = searchNextLeaf(true);

        if(0 <= nextRow && nextRow < getRowCount()) {
            setSelectionRow(nextRow);
            scrollRowToVisible(nextRow);
        }
    }


    /** Select the previous leaf, i.e. the one immediately above the one which
        is currently selected.
        If this is not possible (for example because the currently selected
        leaf is the first one available), it does nothing.
    */
    public void selectPrevLeaf()
    {
        int nextRow = searchNextLeaf(false);

        if(0 <= nextRow && nextRow < getRowCount()) {
            setSelectionRow(nextRow);
            scrollRowToVisible(nextRow);
        }
    }

    /** Get the currently selected row in the JTree.
        If more than one selected row is present, return the index of the
        first one found.
        @return the index of the first selected row, or -1 if no selection
            is available.
    */
    private int getSelectedRow()
    {
        int selectedRow = -1;
        int[] selectedRows;

        selectedRows = getSelectionRows();

        if(selectedRows == null || selectedRows.length == 0) {
            selectedRow = -1;
        } else {
            selectedRow = selectedRows[0];
        }

        return selectedRow;
    }

    /** Search for the next leaf in a tree.
        @param searchForward true if the search is in the forward direction,
            false otherwise.
        @return the index (row number) of the next leaf, or -1 if no leaf
            has been found.
    */
    private int searchNextLeaf(boolean searchForward)
    {
        int nextRow = getSelectedRow();

        for(int i=0;i<getRowCount();i++){
            if(searchForward){
                nextRow++;
            } else {
                nextRow--;
            }

            // Circular search
            if(nextRow < 0){
                nextRow = getRowCount();
                continue;
            } else if(getRowCount() <= nextRow){
                nextRow = -1;
                continue;
            }

            if(getModel().isLeaf(getPathForRow(nextRow).
                getLastPathComponent()))
            {
                return nextRow;
            }
        }
        return -1;
    }

    /** Standard method for painting the node.
        Determines wether the nodes should be expanded or not.
        @param g the graphics context.
    */
    @Override public void paint(Graphics g)
    {
        if(runOnce) {
            fillExpandState(direction);
            runOnce = false;
        }
        super.paint(g);
    }
}
