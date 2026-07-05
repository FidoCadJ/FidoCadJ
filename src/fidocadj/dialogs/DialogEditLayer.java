package fidocadj.dialogs;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import fidocadj.globals.Globals;
import fidocadj.layers.LayerDesc;
import fidocadj.graphic.swing.ColorSwing;
import fidocadj.dialogs.controls.MinimumSizeDialog;
import fidocadj.dialogs.controls.DialogUtil;
import fidocadj.dialogs.controls.TextPopupMenu;

/** The class DialogEditLayer allows to choose the style, visibility and
    description of the current layer.

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
public final class DialogEditLayer extends MinimumSizeDialog
{
    private static final long serialVersionUID = 1L;
    static final int ALPHA_MIN = 0;
    static final int ALPHA_MAX = 100;

    private final JColorChooser tcc;
    private final JCheckBox visibility;
    private final JCheckBox locked;
    private final JTextField description;
    private final JSlider opacity;
    private boolean active;             // true if the user selected ok
    private final LayerDesc ll;

    /** Standard constructor.
        @param parent the dialog parent
        @param l a LayerDesc containing the layer's attributes
    */
    public DialogEditLayer (JFrame parent, LayerDesc l)
    {
        super(500, 450, parent, Globals.messages.getString("Layer_options")+
            l.getDescription(), true);

        ll = l;
        addComponentListener(this);
        active=false;

        Container contentPane=getContentPane();

        GridBagLayout bgl=new GridBagLayout();
        contentPane.setLayout(bgl);

        GridBagConstraints constraints = DialogUtil.createConst(0,0,3,1,100,100,
            GridBagConstraints.CENTER, GridBagConstraints.NONE,
            new Insets(20,20,6,20));
        ColorSwing c = (ColorSwing) l.getColor();
        tcc = new JColorChooser(c.getColorSwing());
        contentPane.add(tcc, constraints);

        JLabel descrLabel=new JLabel(Globals.messages.getString("Description"));
        constraints = DialogUtil.createConst(1,1,1,1,100,100,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(0,20,0,0));

        contentPane.add(descrLabel, constraints);

        description=new JTextField();
        TextPopupMenu.addPopupToText(description);
        description.setText(l.getDescription());
        constraints = DialogUtil.createConst(2,1,1,1,100,0,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
            new Insets(0,0,0,120));

        contentPane.add(description, constraints);

        JLabel opacityLbl=new JLabel(Globals.messages.getString("Opacity"));
        constraints = DialogUtil.createConst(1,3,1,1,100,0,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(0,0,0,20));

        contentPane.add(opacityLbl, constraints);


        opacity = new JSlider(JSlider.HORIZONTAL,
                                      ALPHA_MIN, ALPHA_MAX,
                                      Math.round(l.getAlpha()*100.0f));

        //Turn on labels at major tick marks.
        opacity.setMajorTickSpacing(20);
        opacity.setMinorTickSpacing(1);
        opacity.setPaintTicks(true);
        opacity.setPaintLabels(true);

        constraints = DialogUtil.createConst(2,3,1,1,100,0,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
            new Insets(0,0,0,120));

        contentPane.add(opacity, constraints);

        visibility=new JCheckBox(Globals.messages.getString("IsVisible"));
        visibility.setSelected(l.isVisible());

        constraints = DialogUtil.createConst(2,4,1,1,100,0,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
            new Insets(0,0,0,120));

        contentPane.add(visibility, constraints);

        locked=new JCheckBox(Globals.messages.getString("IsLocked"));
        locked.setSelected(l.isLocked());

        constraints = DialogUtil.createConst(2,5,1,1,100,0,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
            new Insets(0,0,20,120));

        contentPane.add(locked, constraints);

        JButton ok = new JButton(Globals.messages.getString("Ok_btn"));
        JButton cancel = new JButton(Globals.messages.getString("Cancel_btn"));

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

        constraints = DialogUtil.createConst(0,6,3,1,100,0,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
            new Insets(0,20,20,20));

        contentPane.add(b, constraints);

        ok.addActionListener(new ActionListener()
        {
            @Override public void actionPerformed(ActionEvent evt)
            {
                active=true;
                setVisible(false);
            }
        });
        getRootPane().setDefaultButton(ok);

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

    /** Get the layer description as specified in the layer edit dialog
    */
    public void acceptLayer()
    {
        // It is important that here we use exactly the same layer which has
        // been specified at the beginning. In this way, every reference to
        // that layer will be modified.

        ll.setVisible(visibility.isSelected());
        ll.setLocked(locked.isSelected());
        ll.setDescription(description.getText());
        ll.setColor(new ColorSwing(tcc.getColor()));
        ll.setAlpha(opacity.getValue()/100.0f);
        ll.setModified(true);
    }

    /** Determine if the user selected the Ok button.
        @return true if the user has quit the dialog box with the Ok button.
    */

    public boolean getActive()
    {
        return active;
    }
}