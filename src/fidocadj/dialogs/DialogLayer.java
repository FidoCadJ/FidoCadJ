package fidocadj.dialogs;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import fidocadj.globals.Globals;
import fidocadj.layers.LayerDesc;
import fidocadj.dialogs.controls.MinimumSizeDialog;
import fidocadj.dialogs.controls.LayerCellRenderer;
import fidocadj.dialogs.controls.DialogUtil;


/** List and choose the layer to be edited.
    The class dialogLayer allows to choose which layers should be displayed,
    on which color and characteristics.

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
public final class DialogLayer extends MinimumSizeDialog
    implements ComponentListener
{
    private static final long serialVersionUID = 1L;
    private final java.util.List<LayerDesc> layers;
    public JList<LayerDesc> layerList;

    /** Constructor.
        @param parent the dialog parent
        @param l a LayerDesc vector containing the layers' attributes
    */
    public DialogLayer (JFrame parent, java.util.List<LayerDesc> l)
    {
        super(400,350, parent,
            Globals.messages.getString("Layer_editor"), true);
        DialogUtil.center(this, .40,.40,400,350);

        addComponentListener(this);
        layers=l;

        // Ensure that under MacOSX >= 10.5 Leopard, this dialog will appear
        // as a document modal sheet

        getRootPane().putClientProperty("apple.awt.documentModalSheet",
                Boolean.TRUE);
        GridBagLayout bgl=new GridBagLayout();
        GridBagConstraints constraints;
        Container contentPane=getContentPane();
        contentPane.setLayout(bgl);

        layerList = new JList<LayerDesc>(new Vector<LayerDesc>(layers));
        JScrollPane sl=new JScrollPane(layerList);

        layerList.setCellRenderer(new LayerCellRenderer());

        constraints = DialogUtil.createConst(0,0,1,1,100,100,
            GridBagConstraints.EAST, GridBagConstraints.BOTH,
            new Insets(20,20,6,20));

        contentPane.add(sl, constraints);

        JButton ok = new JButton(Globals.messages.getString("Ok_btn"));
        JButton cancel = new JButton(Globals.messages.getString("Cancel_btn"));
        JButton edit = new JButton(Globals.messages.getString("Edit"));

        // Put the OK and Cancel buttons and make them active.
        Box b=Box.createHorizontalBox();
        b.add(edit);
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

        constraints = DialogUtil.createConst(0,1,1,1,100,0,
            GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
            new Insets(0,20,20,20));

        contentPane.add(b,constraints);         // Add cancel button
        layerList.addMouseListener(new ActionDClick(this));

        edit.addActionListener(new ActionListener()
        {
            @Override public void actionPerformed(ActionEvent evt)
            {
                activateLayerEditor(layerList.getSelectedIndex());
            }
        });

        ok.addActionListener(new ActionListener()
        {
            @Override public void actionPerformed(ActionEvent evt)
            {
                setVisible(false);
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

    /** Check if the layer index is non negative and then show the dialog for
        the editing of the selected layer.
        @param index the index of the layer to be modified.
    */
    public void activateLayerEditor(int index)
    {
        if (index>=0) {
            DialogEditLayer del=new DialogEditLayer(null,
                layers.get(layerList.getSelectedIndex()));
            del.setVisible(true);
            if (del.getActive()){
                del.acceptLayer();
                repaint();
            }
        } else {
            JOptionPane.showMessageDialog(null,
                Globals.messages.getString("Warning_select_layer"),
                Globals.messages.getString("Layer_editor"),
                JOptionPane.INFORMATION_MESSAGE, null);
        }
    }
}

/** ActionDClick is a class which activates the layer editor when the user
    double clicks into the layerList JList object contained in the
    DialogLayer object. If in Java there was a "friend" class specificator
    like the one in C++, I would probably define this class as a friend of
    DialogLayer, thus avoiding of having to make layerList public.

*/
class ActionDClick extends MouseAdapter
{
    private final DialogLayer dl;

    /** Method handling a double click event.
        @param i the DialogLayer object which has been clicked.
    */
    public ActionDClick(DialogLayer i)
    {
        dl = i;
    }
    /** Handle a click of the mouse.
        @param e the mouse event object.
    */
    @Override public void mouseClicked(MouseEvent e)
    {
        if(e.getClickCount() == 2){
            int t = dl.layerList.locationToIndex(e.getPoint());
            dl.layerList.ensureIndexIsVisible(t);
            dl.activateLayerEditor(t);
        }
    }
}
