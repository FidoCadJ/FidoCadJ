package fidocadj.dialogs;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import fidocadj.globals.Globals;
import fidocadj.graphic.DimensionG;
import fidocadj.graphic.PointG;
import fidocadj.dialogs.controls.MinimumSizeDialog;
import fidocadj.dialogs.controls.DialogUtil;
import fidocadj.circuit.model.DrawingModel;
import fidocadj.geom.DrawingSize;
import fidocadj.circuit.views.Export;
import fidocadj.dialogs.controls.TextPopupMenu;

/** Choose file format, size and options for the "Copy as image".
    This dialog is a stripped-down version of the DialogExport one.

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

    Copyright 2020-2023 by Davide Bucci
    </pre>

    @author Davide Bucci
*/
public final class DialogCopyAsImage extends MinimumSizeDialog implements
    ActionListener
{
    private static final long serialVersionUID = 1L;
    private boolean copy;     // Indicates that the copy should be done

    private static final double EPS=1E-5;   // Resolution comparison precision

    // Swing elements
    private JComboBox<String> resolution;       // Resolution combo box
    private JCheckBox antiAlias_CB;             // AntiAlias checkbox
    private JCheckBox blackWhite_CB;            // Black and white checkbox
    private final JTabbedPane tabsPane;         // Tab panel for res/size exp.
    private JTextField xsizePixel;              // The x size of the image
    private JTextField ysizePixel;              // The y size of the image
    private final DrawingModel dm;              // The drawing to be exported
    private final DimensionG dim;               // The drawing size in l.u.

    private JLabel expectedSize;                // The calculated size

    /** Constructor: it needs the parent frame.
        @param p the dialog's parent.
        @param c the drawing model containing the drawing to be exported.
    */
    public DialogCopyAsImage (JFrame p, DrawingModel c)
    {
        super(450,400,p,Globals.messages.getString("Circ_exp_t"), true);
        // Ensure that under MacOSX >= 10.5 Leopard, this dialog will appear
        // as a document modal sheet (NOTE: it does not work)
        dm=c;
        getRootPane().putClientProperty("apple.awt.documentModalSheet",
                Boolean.TRUE);

        // Calculate the size of the drawing in logic units:
        PointG o=new PointG();
        dim = DrawingSize.getImageSize(dm,1.0,true,o);

        addComponentListener(this);
        copy=false;

        // Obtain the current content pane and create the grid layout manager
        // which will be used for putting the elements of the interface.
        GridBagLayout bgl=new GridBagLayout();
        GridBagConstraints constraints;
        Container contentPane=getContentPane();

        contentPane.setLayout(bgl);

        constraints = DialogUtil.createConst(1,0,1,1,0,0,
            GridBagConstraints.EAST, GridBagConstraints.BOTH,
            new Insets(12,40,0,0));

        JPanel panel = createResolutionBasedExportPanel();

        constraints.gridx=0;
        constraints.gridy=0;
        constraints.weightx=100;
        constraints.weighty=100;

        constraints.gridy=0;
        constraints.gridwidth=4;
        constraints.gridheight=2;
        constraints.anchor=GridBagConstraints.EAST;
        constraints.fill=GridBagConstraints.BOTH;
        constraints.insets=new Insets(20,20,20,20);

        tabsPane = new JTabbedPane();
        tabsPane.addTab(Globals.messages.getString("res_export"), panel);

        JPanel sizepanel = createSizeBasedExportPanel();
        tabsPane.addTab(Globals.messages.getString("size_export"), sizepanel);

        contentPane.add(tabsPane, constraints);

        constraints.gridx=0;
        constraints.gridy=2;
        constraints.gridwidth=4;
        constraints.gridheight=1;
        constraints.anchor=GridBagConstraints.EAST;
        constraints.fill=GridBagConstraints.HORIZONTAL;

        constraints.insets=new Insets(20,20,20,20);

        JPanel commonPane = createCommonInterfaceElements();
        contentPane.add(commonPane, constraints);

        constraints.gridx=0;
        constraints.gridy=3;
        constraints.gridwidth=4;
        constraints.gridheight=1;
        constraints.anchor=GridBagConstraints.EAST;
        constraints.insets=new Insets(20,20,20,20);

        // Put the OK and Cancel buttons and make them active.
        Box b=Box.createHorizontalBox();
        b.add(Box.createHorizontalGlue());

        JButton ok=new JButton(Globals.messages.getString("Ok_btn"));
        JButton cancel=new JButton(Globals.messages.getString("Cancel_btn"));

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
        contentPane.add(b, constraints);            // Add cancel button

        ok.addActionListener(new ActionListener()
        {
            @Override public void actionPerformed(ActionEvent evt)
            {
                int selection;
                selection=JOptionPane.OK_OPTION;
                if (selection==JOptionPane.OK_OPTION) {
                    copy=true;
                    setVisible(false);
                }
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

        TextPopupMenu.addPopupToText(xsizePixel);
        TextPopupMenu.addPopupToText(ysizePixel);
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

    /** Indicates that the copy should be done: the user selected the "ok"
        button.

        @return a boolean value which indicates if the copy should be done.
    */
    public boolean shouldExport()
    {
        return copy;
    }

    /** Indicates that the antiAlias should be activated.
        @return a boolean value which indicates if the anti alias should be
                activated.
    */
    public boolean getAntiAlias()
    {
        return antiAlias_CB.isSelected();
    }

    /** Indicates that the black and white copy should be activated.

        @return a boolean value which indicates if the copy should be
            done in black and white.
    */
    public boolean getBlackWhite()
    {
        return blackWhite_CB.isSelected();
    }

    /** Indicates if the copy should be size-based or resolution-based.
        @return true if the copy is size-based (i.e. the image size in pixels
            should be given) or false if the copy is resolution-based (i.e.
            the image size is calculated from the typographical resolution.
    */
    public boolean getResolutionBasedExport()
    {
        return tabsPane.getSelectedIndex()==0;
    }

    /** Set if the copy should be size-based or resolution-based.
        @param s true if the copy is size-based (i.e. the image size in pixels
            should be given) or false if the copy is resolution-based (i.e.
            the image size is calculated from the typographical resolution.
    */
    public void setResolutionBasedExport(boolean s)
    {
        tabsPane.setSelectedIndex(s?0:1);
    }

    /** Sets the actual anti alias state
        @param a a boolean which indicates the default anti alias state
    */
    public void setAntiAlias(boolean a)
    {
        antiAlias_CB.setSelected(a);
    }

    /** Sets the actual black and white state
        @param a a boolean which indicates the default black and white state
    */
    public void setBlackWhite(boolean a)
    {
        blackWhite_CB.setSelected(a);
    }

    /** Set the default unit per pixel value
        @param d the default unit per pixel value
    */
    public void setUnitPerPixel(double d)
    {
        int index=0;

        if (Math.abs(d-0.36)<EPS) { index=0; }
        if (Math.abs(d-0.75)<EPS) { index=1; }
        if (Math.abs(d-1.50)<EPS) { index=2; }
        if (Math.abs(d-3.00)<EPS) { index=3; }
        if (Math.abs(d-6.00)<EPS) { index=4; }
        if (Math.abs(d-9.00)<EPS) { index=5; }
        if (Math.abs(d-12.0)<EPS) { index=6; }

        resolution.setSelectedIndex(index);
    }


    /** Set the x size of the picture.
        @param xs the wanted x size.
    */
    public void setXsizeInPixels(int xs)
    {
        xsizePixel.setText(""+xs);
    }

    /** If the resolution is size-based, get the x size of the picture.
        @return the wanted x size.
    */
    public int getXsizeInPixels()
    {
        return Integer.parseInt(xsizePixel.getText());
    }

    /** Set the y size of the picture.
        @param ys the wanted y size.
    */
    public void setYsizeInPixels(int ys)
    {
        ysizePixel.setText(""+ys);
    }

    /** If the resolution is size-based, get the y size of the picture.
        @return the wanted y size.
    */
    public int getYsizeInPixels()
    {
        return Integer.parseInt(ysizePixel.getText());
    }

    /** Get the default unit per pixel value
        @return the unit per pixel value
    */
    public double getUnitPerPixel()
    {
        int index=resolution.getSelectedIndex();
        switch(index) {
            case 0:
                return 0.36;  // 72/200
            case 1:
                return 0.75;  // 150/200
            case 2:
                return 1.50;  // 300/200
            case 3:
                return 3.00;  // 600/200
            case 4:
                return 6.00;  // 1200/200
            case 5:
                return 9.00;  // 1800/200
            case 6:
                return 12.0;  // 2400/200
            default:
                return 0.36;  // Not recognized.
        }
    }

    /** Create a JPanel containing all the interface elements needed
        for a size-based copy of bitmap images.
        @return the created panel.
    */
    private JPanel createSizeBasedExportPanel()
    {
        JPanel panel = new JPanel();
        GridBagLayout bgl=new GridBagLayout();
        GridBagConstraints constraints=new GridBagConstraints();
        panel.setLayout(bgl);
        JLabel xSizeLabel=new
            JLabel(Globals.messages.getString("ctrl_x_radius"));

        constraints = DialogUtil.createConst(1,0,1,1,0,0,
            GridBagConstraints.EAST, GridBagConstraints.BOTH,
            new Insets(6,40,6,6));
        panel.add(xSizeLabel, constraints);

        xsizePixel=new JTextField();
        constraints = DialogUtil.createConst(2,0,1,1,100,100,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(6,0,0,0));
        panel.add(xsizePixel, constraints);

        JLabel ySizeLabel=new
            JLabel(Globals.messages.getString("ctrl_y_radius"));

        constraints = DialogUtil.createConst(1,1,1,1,0,0,
            GridBagConstraints.EAST, GridBagConstraints.BOTH,
            new Insets(6,40,6,6));
        panel.add(ySizeLabel, constraints);

        ysizePixel=new JTextField();
        constraints = DialogUtil.createConst(2,1,1,1,100,100,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(6,0,0,0));
        panel.add(ysizePixel, constraints);

        return panel;
    }

    /** Create a JPanel containing the interface elements needed for
        the configuration of copy operation.
        @return the created panel.
    */
    private JPanel createResolutionBasedExportPanel()
    {
        JPanel panel = new JPanel();

        GridBagLayout bgl=new GridBagLayout();
        GridBagConstraints constraints=new GridBagConstraints();
        panel.setLayout(bgl);

        JLabel resolutionLabel=new
            JLabel(Globals.messages.getString("Resolution"));

        constraints = DialogUtil.createConst(1,0,1,1,0,0,
            GridBagConstraints.EAST, GridBagConstraints.BOTH,
            new Insets(6,40,6,6));

        panel.add(resolutionLabel, constraints);

        resolution = createResolutionComboBox();

        constraints = DialogUtil.createConst(2,0,1,1,100,100,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(6,0,0,0));

        panel.add(resolution, constraints);

        expectedSize= new JLabel("x");

        constraints = DialogUtil.createConst(2,1,1,1,100,100,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(6,0,0,0));

        panel.add(expectedSize, constraints);

        return panel;
    }

    private JPanel createCommonInterfaceElements()
    {
        JPanel panel=new JPanel();

        GridBagLayout bgl=new GridBagLayout();
        GridBagConstraints constraints=new GridBagConstraints();
        panel.setLayout(bgl);
        antiAlias_CB=new JCheckBox(Globals.messages.getString("Anti_aliasing"));

        constraints = DialogUtil.createConst(2,1,1,1,100,100,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
            new Insets(6,0,0,0));

        panel.add(antiAlias_CB, constraints);     // Add antialias cb

        blackWhite_CB=new JCheckBox(Globals.messages.getString("B_W"));

        constraints = DialogUtil.createConst(2,2,1,1,100,100,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(6,0,0,0));

        panel.add(blackWhite_CB, constraints);        // Add black/white cb

        return panel;
    }


    /** Create a JComboBox containing the resolutions, described as strings.
        @return the JComboBox created.
    */
    private JComboBox<String> createResolutionComboBox()
    {
        JComboBox<String> res=new JComboBox<String>();
        res.addItem("72x72 dpi");
        res.addItem("150x150 dpi");
        res.addItem("300x300 dpi");
        res.addItem("600x600 dpi");
        res.addItem("1200x1200 dpi");
        res.addItem("1800x1800 dpi");
        res.addItem("2400x2400 dpi");
        res.addActionListener (new ActionListener () {
            @Override public void actionPerformed(ActionEvent e)
            {
                int width = (int)(
                    (dim.width+Export.exportBorder)*getUnitPerPixel());
                int height = (int)(
                    (dim.height+Export.exportBorder)*getUnitPerPixel());
                expectedSize.setText(""+width+" x "+height+" pixels");
            }
        });
        return res;
    }

    /** Event handling routine for the user interface.
        For the moment, it does not do much, except setting the enabled
        state of buttons and elements of the UI, depending on which kind
        of copy is being done.
        @param evt the event to be processed.
    */
    @Override public void actionPerformed(ActionEvent evt)
    {
        // It is a bitmap based copy
        xsizePixel.setEnabled(true);
        ysizePixel.setEnabled(true);
        resolution.setEnabled(true);     // Resolution combo box
        antiAlias_CB.setEnabled(true);   // AntiAlias checkbox
        blackWhite_CB.setEnabled(true);  // Black and white checkbox
    }
}
