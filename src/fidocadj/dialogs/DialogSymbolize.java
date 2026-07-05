package fidocadj.dialogs;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.LinkedList;
import java.util.Locale;

import javax.swing.*;
import javax.swing.event.*;

import fidocadj.dialogs.controls.OriginCircuitPanel;
import fidocadj.circuit.CircuitPanel;
import fidocadj.circuit.controllers.EditorActions;
import fidocadj.circuit.controllers.ParserActions;
import fidocadj.circuit.controllers.SelectionActions;
import fidocadj.circuit.model.DrawingModel;
import fidocadj.geom.DrawingSize;
import fidocadj.geom.MapCoordinates;
import fidocadj.globals.Globals;
import fidocadj.globals.LibUtils;
import fidocadj.primitives.GraphicPrimitive;
import fidocadj.primitives.MacroDesc;
import fidocadj.dialogs.controls.MinimumSizeDialog;
import fidocadj.dialogs.controls.DialogUtil;
import fidocadj.dialogs.controls.TextPopupMenu;

/** Choose file format, size and options of the graphic exporting.

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

    Copyright 2012-2026 Phylum2, Davide Bucci
    </pre>
    @author Phylum2, Davide Bucci

    */

public final class DialogSymbolize extends MinimumSizeDialog
{
    private static final long serialVersionUID = 1L;
    final private JPanel parent;
    private DrawingModel drawingModel;

    // Swing elements
    private JComboBox<String> libFilename;
    private JTextField libName;
    private JTextField name;
    private JTextField key;
    private JComboBox<String> group;
    private JCheckBox snapToGrid;

    OriginCircuitPanel cpanel = new OriginCircuitPanel(false);

    /** Gets the library to be created or modified.
        @return the given library (string description).
    */
    public String getLibraryName()
    {
        String s=libName.getText();
        return s.trim();
    }
    /** Gets the name of the macro to be created
        @return the name
    */
    public String getMacroName()
    {
        return name.getText();
    }

    /** Gets the prefix (filename) of the macro to be created
        @return the filename/prefix
    */
    public String getPrefix()
    {
        return libFilename.getEditor().getItem().toString();
    }

    /** Gets the group of the macro to be created
        @return the name of the group
    */
    public String getGroup()
    {
        return group.getEditor().getItem().toString();
    }

    /** List all the libraries available which are not standard in the
        libFilename combo box.
        If there are no non standard libraries, suggest a default name.
    */
    private void enumLibs()
    {
        libFilename.removeAllItems();
        List<String> lst = new LinkedList<String>();
        Map<String,MacroDesc> m=cpanel.getDrawingModel().getLibrary();

        for (Entry<String,MacroDesc> e : m.entrySet()) {
            MacroDesc md = e.getValue();
            // Add only non standard libs.
            if(!lst.contains(md.filename) &&
                !LibUtils.isStdLib(md))
            {
                libFilename.addItem(md.filename);
                lst.add(md.filename);
            }
        }
        if (((DefaultComboBoxModel) libFilename.getModel()).getSize() == 0) {
            libFilename.addItem("user_lib");
        }
        libFilename.setEditable(true);
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

    /** Create the GUI for the dialog.
        @return the panel containing the user interface.
    */
    private JPanel createInterfacePanel()
    {
        JPanel panel = new JPanel();

        GridBagLayout bgl=new GridBagLayout();
        GridBagConstraints constraints=new GridBagConstraints();
        panel.setLayout(bgl);

        JLabel libraryLabel=new
            JLabel(Globals.messages.getString("Library_file"));

        constraints = DialogUtil.createConst(1,0,1,1,0.01,0.01,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(0,0,0,6));

        panel.add(libraryLabel, constraints);

        libFilename=new JComboBox<String>();

        constraints = DialogUtil.createConst(2,0,1,1,0.01,0.01,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(0,0,0,0));

        panel.add(libFilename, constraints);

        JLabel libraryNameLabel=new
            JLabel(Globals.messages.getString("Library_name"));

        constraints = DialogUtil.createConst(1,1,1,1,0.01,0.01,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(8,0,0,6));

        panel.add(libraryNameLabel, constraints);

        libName = new JTextField();
        TextPopupMenu.addPopupToText(libName);
        constraints = DialogUtil.createConst(2,1,1,1,0.01,0.01,
            GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
            new Insets(8,0,0,0));

        panel.add(libName, constraints);

        MouseAdapter ma = new MouseAdapter() {
            boolean grid = false;

            /** Mouse click: either show the grid (button 3), or move the
                origin.
            */
            @Override public void mouseReleased(MouseEvent e)
            {
                // Toggle grid visibility, via the secondary mouse button
                if (e.getButton()==MouseEvent.BUTTON3) {
                    grid = !grid;
                    cpanel.setGridVisibility(grid);
                    cpanel.repaint();
                } else {
                    mouseDragged(e);
                }
            }

            /** Drag the origin of axes using the mouse.
            */
            @Override public void mouseDragged(MouseEvent evt)
            {
                int x=evt.getX();
                int y=evt.getY();

                if(snapToGrid.isSelected()) {
                    cpanel.setLx(cpanel.getMapCoordinates().unmapXsnap(x));
                    cpanel.setLy(cpanel.getMapCoordinates().unmapYsnap(y));
                } else {
                    cpanel.setLx(cpanel.getMapCoordinates().unmapXnosnap(x));
                    cpanel.setLy(cpanel.getMapCoordinates().unmapYnosnap(y));
                }
                x=cpanel.getMapCoordinates().mapXi(
                    cpanel.getLx(),cpanel.getLy(),false);
                y=cpanel.getMapCoordinates().mapYi(
                    cpanel.getLx(),cpanel.getLy(),false);
                cpanel.setDx(x);
                cpanel.setDy(y);
                cpanel.repaint();
            }
        };

        // Make sort that the user can move the origin both by clicking
        // and by dragging the mouse pointer.
        cpanel.addMouseListener(ma);
        cpanel.addMouseMotionListener(ma);

        // Reasonable size
        cpanel.setSize(256, 256);
        cpanel.setPreferredSize(new Dimension(256, 256));
        cpanel.add(Box.createVerticalStrut(256));
        cpanel.add(Box.createHorizontalStrut(256));

        cpanel.getDrawingModel().setLayers(drawingModel.getLayers());
        cpanel.getDrawingModel().setLibrary(drawingModel.getLibrary());
        enumLibs();
        cpanel.setAntiAlias(true);
        cpanel.profileTime = false;
        MacroDesc macro = buildMacro("temp","temp","temp","temp", "temp",
            new Point(100,100));

        cpanel.setBorder(BorderFactory.createEtchedBorder());

        // Set the current objects in the preview panel.
        cpanel.getParserActions().addString(
            new StringBuffer(macro.description), false);
        // Calculate an optimum preview size in order to show all elements.
        MapCoordinates m =
                DrawingSize.calculateZoomToFit(cpanel.getDrawingModel(),
                cpanel.getSize().width*80/100, cpanel.getSize().height*80/100,
                true);
        m.setXCenter(-m.getXCenter()+10);
        m.setYCenter(-m.getYCenter()+10);
        cpanel.setMapCoordinates(m);
        cpanel.resetOrigin();

        constraints = DialogUtil.createConst(3,0,8,8,0.01,0.01,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(0,6,6,6));

        panel.add(cpanel, constraints);

        JLabel groupLabel=new
            JLabel(Globals.messages.getString("Group")); //

        constraints = DialogUtil.createConst(1,3,1,1,0.01,0.01,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(8,0,0,6));
        panel.add(groupLabel, constraints);

        group=new JComboBox<String>();
        listGroups();
        if (group.getItemCount()==0) {
            group.addItem(Globals.messages.getString("Group").
                toLowerCase(Locale.getDefault()));
        }
        group.setEditable(true);

        libFilename.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0)
            {
                listGroups();
                generateUniqueKey();
            }
        });

        libFilename.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                listGroups();
                generateUniqueKey();
            }
        });

        constraints = DialogUtil.createConst(2,3,1,1,0.01,0.01,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(8,0,0,0));
        panel.add(group, constraints);

        JLabel nameLabel=new
            JLabel(Globals.messages.getString("Name")); //

        constraints = DialogUtil.createConst(1,4,1,1,0.01,0.01,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(8,0,0,6));
        panel.add(nameLabel, constraints);

        name=new JTextField();
        TextPopupMenu.addPopupToText(name);
        name.setText(
            Globals.messages.getString("Name").
                toLowerCase(Locale.getDefault()));
        constraints = DialogUtil.createConst(2,4,1,1,0.01,0.01,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(8,0,0,0));

        panel.add(name, constraints);

        JLabel nameLabel1=new
                JLabel(Globals.messages.getString("Key")); //

        constraints = DialogUtil.createConst(1,5,1,1,0.01,0.01,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(8,0,0,6));
        panel.add(nameLabel1, constraints);

        key=new JTextField();
        TextPopupMenu.addPopupToText(key);

        constraints = DialogUtil.createConst(2,5,1,1,0.01,0.01,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(8,0,0,0));

        panel.add(key, constraints);

        key.getDocument().addDocumentListener(new DocumentListener() {
            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void insertUpdate(DocumentEvent e)
            {
                showValidity();
            }

            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void removeUpdate(DocumentEvent e)
            {
                showValidity();
            }

            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void changedUpdate(DocumentEvent e)
            {
                showValidity();
            }

            /** Change the background color of the field depending on the
                validity of the key currently defined.
            */
            public void showValidity()
            {
                if (isKeyInvalid()) {
                    key.setBackground(Color.RED.darker());
                    key.setForeground(Color.WHITE);
                } else {
                    Color c1 = UIManager.getColor("TextField.background");
                    Color c2 = UIManager.getColor("TextField.foreground");
                    if (c1 != null && c2 != null) {
                        key.setBackground(c1);
                        key.setForeground(c2);
                    } else {
                        key.setBackground(Color.GREEN.darker().darker());
                        key.setForeground(Color.WHITE);
                    }
                }
            }
        });

        generateUniqueKey();

        snapToGrid = new JCheckBox(
                Globals.messages.getString("SnapToGridOrigin"));
        snapToGrid.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        constraints = DialogUtil.createConst(1, 6, 2, 1, 1.0, 0.1,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(8, 0, 0, 0));
        panel.add(snapToGrid, constraints);

        // Keep in mind the last edited library and group
        if (Globals.lastCLib!=null) {
            libFilename.setSelectedItem(Globals.lastCLib);
        }
        if (Globals.lastCGrp!=null) {
            group.setSelectedItem(Globals.lastCGrp);
        }

        libFilename.getEditor().selectAll();
        return panel;
    }

    /** Obtain all the groups in a given library and put them in the
        group list.
    */
    private void listGroups()
    {
        // Obtain all the groups in a given library.
        List<String> l = LibUtils.enumGroups(drawingModel.getLibrary(),
            libFilename.getEditor().getItem().toString());

        // Update the group list.
        group.removeAllItems();
        for (String s : l) {
            group.addItem(s);
        }
        libName.setText(LibUtils.getLibName(drawingModel.getLibrary(),
            libFilename.getEditor().getItem().toString()));
    }

    /** Standard constructor.
        @param circuitPanel the panel containing a complete editor.
        @param p the model
    */
    public DialogSymbolize (CircuitPanel circuitPanel, DrawingModel p)
    {
        super(600,430,(JFrame)null,
            Globals.messages.getString("SaveSymbol"), true);
        parent = circuitPanel;
        addComponentListener(this);

        // Obtain the current content pane and create the grid layout manager
        // which will be used for putting the elements of the interface.
        GridBagLayout bgl=new GridBagLayout();
        GridBagConstraints constraints;
        Container contentPane=getContentPane();

        contentPane.setLayout(bgl);

        constraints = DialogUtil.createConst(2,0,1,1,100,100,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
            new Insets(12,0,0,20));

        setCircuit(p);

        JPanel panel = createInterfacePanel();

        JButton ok=new JButton(Globals.messages.getString("Ok_btn"));
        JButton cancel=new JButton(Globals.messages.getString("Cancel_btn"));

        constraints.gridx=0;
        constraints.gridy=1;
        constraints.gridwidth=4;
        constraints.gridheight=1;
        constraints.anchor=GridBagConstraints.EAST;
        constraints.insets=new Insets(20,20,20,20);

        contentPane.add(panel, constraints);

        constraints.gridx=0;
        constraints.gridy=2;
        constraints.gridwidth=4;
        constraints.gridheight=1;
        constraints.anchor=GridBagConstraints.EAST;
        constraints.insets=new Insets(20,20,20,20);

        // Put the OK and Cancel buttons and make them active.
        Box b=Box.createHorizontalBox();
        b.add(Box.createHorizontalGlue());
        ok.setPreferredSize(cancel.getPreferredSize());

        if (Globals.okCancelWinOrder) {
            b.add(ok);
            b.add(Box.createHorizontalStrut(12));
            b.add(cancel);
        } else {
            b.add(cancel);
            b.add(Box.createHorizontalStrut(12));
            b.add(ok);
        }
        // Add the OK/cancel buttons in the correct order
        contentPane.add(b, constraints);

        ok.addActionListener(new ActionListener()
        {
            @Override public void actionPerformed(ActionEvent evt)
            {
                // Check if there is a valid key available. We can not continue
                // without a key!
                Map<String, MacroDesc> libref = drawingModel.getLibrary();
                String k = key.getText().trim();
                String lk = getPrefix().trim()+"."+k;
                boolean val  = LibUtils.checkKey(libref, getPrefix().trim(),lk);

                if (isKeyAbsent()) {
                    JOptionPane.showMessageDialog(null,
                        Globals.messages.getString("InvKey"),
                        Globals.messages.getString("Symbolize"),
                        JOptionPane.ERROR_MESSAGE);
                    key.requestFocus();
                    return;

                } else if(isKeyDuplicate()) {
                    JOptionPane.showMessageDialog(null,
                        Globals.messages.getString("DupKey"),
                        Globals.messages.getString("Symbolize"),
                        JOptionPane.ERROR_MESSAGE);
                    key.requestFocus();
                    return;
                } else if(isKeyContainingInvalidChars()) {
                    JOptionPane.showMessageDialog(null,
                        Globals.messages.getString("SpaceKey"),
                        Globals.messages.getString("Symbolize"),
                        JOptionPane.ERROR_MESSAGE);
                    key.requestFocus();
                    return;
                }
                Point p = new Point(
                    Math.round(200-cpanel.getLx()),
                    Math.round(200-cpanel.getLy()));
                MacroDesc macro = buildMacro(getMacroName().trim(),
                    key.getText().trim(),getLibraryName().trim(),
                    getGroup().trim(), getPrefix().trim(),p);

                drawingModel.getLibrary().put(macro.key, macro); // add to lib

                // Save the new symbol in the current libFilename
                try {
                    LibUtils.save(drawingModel.getLibrary(),
                        LibUtils.getLibPath(getPrefix()).trim(),
                        getLibraryName(), getPrefix());
                } catch (FileNotFoundException fF) {
                    JOptionPane.showMessageDialog(null,
                        Globals.messages.getString("DirNotFound"),
                        Globals.messages.getString("Symbolize"),
                        JOptionPane.ERROR_MESSAGE);
                }

                setVisible(false);
                ((JFrame)Globals.activeWindow).repaint();

                // Update libs
                updateTreeLib();

                Globals.lastCLib = getLibraryName();
                Globals.lastCGrp = getGroup();
            }
        });
        cancel.addActionListener(new ActionListener()
        {
            @Override public void actionPerformed(ActionEvent evt)
            {
                setVisible(false);
            }
        });

        // Here is an action in which the dialog is closed
        AbstractAction cancelAction = new AbstractAction ()
        {
            @Override public void actionPerformed (ActionEvent e)
            {
                setVisible(false);
            }
        };
        DialogUtil.addCancelEscape (this, cancelAction);
        pack();
        DialogUtil.center(this);
        getRootPane().setDefaultButton(ok);
    }

    /** Generates a unique key based on the selected library.
    */
    private void generateUniqueKey()
    {
        long t = System.nanoTime();
        long h = 0;
        for (int i = 0; t > 0; ++i) {
            t >>= i * 8;
            h ^= t & 0xFF;
        }

        String baseKey = String.valueOf(h);
        Map<String, MacroDesc> libref = drawingModel.getLibrary();
        String lk = getPrefix().trim()+".";

        while (LibUtils.checkKey(libref, getPrefix().trim(),lk+baseKey)) {
            baseKey = String.valueOf(h++);
        }
        key.setText(baseKey);
    }

    /** Check if the current key is duplicate or not.
        @return true if a duplicate exists in the library.
    */
    private boolean isKeyDuplicate()
    {
        Map<String, MacroDesc> libref = drawingModel.getLibrary();
        String k = key.getText().trim();
        String lk = getPrefix().trim()+"."+k;
        return LibUtils.checkKey(libref, getPrefix().trim(),lk);
    }

    /** Check if the current key is containing invalid characters.
        @return true the current key is containing invalid chars.
    */
    private boolean isKeyContainingInvalidChars()
    {
        // Currently, the only recognized invalid chars are spaces and dots.
        return key.getText().contains(" ") || key.getText().contains(".");
    }

    /** Check if the current key is specified.
        @return true the current key is not empty
    */
    private boolean isKeyAbsent()
    {
        return key.getText().length()<1;
    }

    /** Check if the key is valid or not.
    */
    private boolean isKeyInvalid()
    {
        return isKeyAbsent() || isKeyContainingInvalidChars()
            || isKeyDuplicate();
    }

    protected MacroDesc buildMacro(String myname, String mykey, String mylib,
            String mygrp, String myprefix, Point origin)
    {
        StringBuilder ss = new StringBuilder();
        SelectionActions sa = new SelectionActions(drawingModel);
        new EditorActions(drawingModel, sa, null);

        // Check if there is anything selected.
        if (sa.getFirstSelectedPrimitive() == null) {
            return null;
        }

        // Move the selected primitives around the origin just
        // determined and add them to the macro description contained
        // in ss.

        DrawingModel ps = new DrawingModel();
        try {
            ps.setLibrary(drawingModel.getLibrary());
            ParserActions pa = new ParserActions(ps);

            for (GraphicPrimitive g : drawingModel.getPrimitiveVector()) {
                if (g.isSelected()) {
                    pa.addString(new StringBuffer(g.toString(true)), true);
                }
            }
            //sa.setSelectionAll(true);
        } catch (Exception e){
            e.printStackTrace();
        }

        for (GraphicPrimitive psp : ps.getPrimitiveVector()) {
            if (!psp.isSelected()) {
                continue;
            }
            psp.movePrimitive(origin.x, origin.y);
            ss.append(psp.toString(true));
        }

        parent.repaint();
        String desc = ss.toString();
        return new MacroDesc(myprefix+"."+mykey, myname, desc, mygrp,
            mylib, myprefix);
    }

    /** Update all the libs shown in the tree.
    */
    protected void updateTreeLib()
    {
        // TODO:
        // This is a tricky code. What if a new menu option is added?
        // This would be better, but there is something to solve about access.
        // ((FidoFrame)Globals.activeWindow).loadLibraries();

        Container cc;
        cc = (JFrame)Globals.activeWindow;

        ((AbstractButton) ((JFrame) cc).getJMenuBar()
            .getMenu(3).getSubElements()[0].getSubElements()[1]).doClick();

    }

    /** Sets the drawing database on which to work
        @param p the database
    */
    public void setCircuit(DrawingModel p)
    {
        this.drawingModel = p;
    }
}