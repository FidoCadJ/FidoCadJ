package fidocadj.dialogs;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

import fidocadj.globals.Globals;
import fidocadj.graphic.DimensionG;
import fidocadj.graphic.PointG;
import fidocadj.dialogs.controls.MinimumSizeDialog;
import fidocadj.dialogs.controls.DialogUtil;
import fidocadj.circuit.model.DrawingModel;
import fidocadj.geom.DrawingSize;
import fidocadj.circuit.views.Export;
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

    Copyright 2007-2023 by Davide Bucci
    </pre>

    @author Davide Bucci
*/
public final class DialogExport extends MinimumSizeDialog
    implements ActionListener
{
    private static final long serialVersionUID = 1L;
    private final JFrame parent;

    private boolean export;     // Indicates that the export should be done

    private static final int PNG_INDEX=0;       // Combo list index: png format
    private static final int JPG_INDEX=1;       // Combo list index: jpg format
    private static final int SVG_INDEX=2;       // Combo list index: svg format
    private static final int EPS_INDEX=3;       // Combo list index: eps format
    private static final int PGF_INDEX=4;       // Combo list index: pgf format
    private static final int PDF_INDEX=5;       // Combo list index: pgf format
    private static final int SCR_INDEX=6;       // idem: Eagle scr format
    private static final int PCB_INDEX=7;       // idem: gEDA pcb-rnd format
    private static final int DXF_INDEX=8;       // idem: DXF format

    private static final double EPS=1E-5;   // Resolution comparison precision

    // Swing elements
    private JComboBox<String> resolution;       // Resolution combo box
    private JCheckBox antiAlias_CB;             // AntiAlias checkbox
    private JCheckBox blackWhite_CB;            // Black and white checkbox
    private final JComboBox<String> fileFormat; // File format combo box
    private JTextField fileName;                // File name text field
    private final JTabbedPane tabsPane;         // Tab panel for res/size exp.
    private JTextField multiplySizes;           // Size mult. for vector exp.
    private JTextField xsizePixel;              // The x size of the image
    private JTextField ysizePixel;              // The y size of the image
    private final DrawingModel dm;              // The drawing to be exported
    private final DimensionG dim;               // The drawing size in l.u.
    private JCheckBox splitLayers_CB;           // The split layers c.b.

    private JLabel expectedSize;                // The calculated size

    /** Constructor: it needs the parent frame.
        @param p the dialog's parent.
        @param c the drawing model containing the drawing to be exported.
    */
    public DialogExport (JFrame p, DrawingModel c)
    {
        super(450,400,p,Globals.messages.getString("Circ_exp_t"), true);
        // Ensure that under MacOSX >= 10.5 Leopard, this dialog will appear
        // as a document modal sheet
        dm=c;
        getRootPane().putClientProperty("apple.awt.documentModalSheet",
                Boolean.TRUE);

        // Calculate the size of the drawing in logic units:
        PointG o=new PointG();
        dim = DrawingSize.getImageSize(dm,1.0,true,o);

        addComponentListener(this);
        export=false;
        parent=p;

        // Obtain the current content pane and create the grid layout manager
        // which will be used for putting the elements of the interface.
        GridBagLayout bgl=new GridBagLayout();
        GridBagConstraints constraints;
        Container contentPane=getContentPane();

        contentPane.setLayout(bgl);

        // The first thing we need to put is the combobox describing the file
        // format to be used. This is important, since part of the remaining
        // dialog should be changed depending on the format chosen.
        JLabel fileFormatLabel=new
            JLabel(Globals.messages.getString("File_format"));

        constraints = DialogUtil.createConst(1,0,1,1,0,0,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(12,40,0,0));

        contentPane.add(fileFormatLabel, constraints);

        fileFormat=new JComboBox<String>();
        fileFormat.addItem("PNG (Bitmap)");
        fileFormat.addItem("JPG (Bitmap)");
        fileFormat.addItem("SVG (Vector, Scalable Vector Graphic)");
        fileFormat.addItem("EPS (Vector, Encapsulated Postscript)");
        fileFormat.addItem("PGF (Vector, PGF packet for LaTeX)");
        fileFormat.addItem("PDF (Vector, Portable Document File)");
        fileFormat.addItem("CadSoft Eagle SCR (Script)");
        fileFormat.addItem("gEDA PCB, pcb-rnd (.pcb) file");
        fileFormat.addItem("DXF (Vector, AutoCAD Drawing Exchange Format)");

        fileFormat.setSelectedIndex(0);

        constraints = DialogUtil.createConst(2,0,1,1,100,100,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
            new Insets(12,0,0,20));
        contentPane.add(fileFormat, constraints);

        // We need to track when the user changes the file format since some
        // options will be made available or not depending if the file format
        // chosen is vector based or bitmap based.

        fileFormat.addActionListener(this);

        JPanel panel = createResolutionBasedExportPanel();

        JButton ok=new JButton(Globals.messages.getString("Ok_btn"));
        JButton cancel=new JButton(Globals.messages.getString("Cancel_btn"));

        constraints.gridx=0;
        constraints.gridy=1;
        constraints.gridwidth=4;
        constraints.gridheight=1;
        constraints.anchor=GridBagConstraints.EAST;
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
                // Check if the magnification factor is correct.
                double mult = Double.parseDouble(multiplySizes.getText());
                if(multiplySizes.isEnabled() && (mult<0.01 || mult>100)) {
                    export=false;
                    JOptionPane.showMessageDialog(null,
                        Globals.messages.getString("Warning_mul"),
                        Globals.messages.getString("Warning"),
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if("".equals(fileName.getText().trim())){
                    export=false;
                    JOptionPane.showMessageDialog(null,
                        Globals.messages.getString("Warning_noname"),
                        Globals.messages.getString("Warning"),
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                selection=JOptionPane.OK_OPTION;
                if (selection==JOptionPane.OK_OPTION) {
                    export=true;
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

        TextPopupMenu.addPopupToText(fileName);
        TextPopupMenu.addPopupToText(multiplySizes);
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

    /** Indicates that the export should be done: the user selected the "ok"
        button.

        @return a boolean value which indicates if the export should be done.
    */
    public boolean shouldExport()
    {
        return export;
    }

    /** Indicates that the antiAlias should be activated.
        @return a boolean value which indicates if the anti alias should be
                activated.
    */
    public boolean getAntiAlias()
    {
        return antiAlias_CB.isSelected();
    }

    /** Indicates that the black and white export should be activated.

        @return a boolean value which indicates if the export should be
            done in black and white.
    */
    public boolean getBlackWhite()
    {
        return blackWhite_CB.isSelected();
    }

    /** Indicates that the layers should be split into different files.

        @return a boolean value which indicates if the layers should be split.
    */
    public boolean getSplitLayers()
    {
        return splitLayers_CB.isSelected();
    }

    /** Indicates that the layers should be split into different files.

        @param s a boolean value which indicates if the layers should be split.
    */
    public void setSplitLayers(boolean s)
    {
        splitLayers_CB.setSelected(s);
    }

    /** Indicates if the export should be size-based or resolution-based.
        @return true if the export is size-based (i.e. the image size in pixels
            should be given) or false if the export is resolution-based (i.e.
            the image size is calculated from the typographical resolution.
    */
    public boolean getResolutionBasedExport()
    {
        return tabsPane.getSelectedIndex()==0;
    }

    /** Set if the export should be size-based or resolution-based.
        @param s true if the export is size-based (i.e. the image size in pixels
            should be given) or false if the export is resolution-based (i.e.
            the image size is calculated from the typographical resolution.
    */
    public void setResolutionBasedExport(boolean s)
    {
        tabsPane.setSelectedIndex(s?0:1);
    }
    /** Indicates which export format has been selected.
        @return a string describing the image format (e.g. "png", "jpg").
    */
    public String getFormat()
    {
        switch(fileFormat.getSelectedIndex()){
            case PNG_INDEX:
                return "png";
            case JPG_INDEX:
                return "jpg";
            case SVG_INDEX:
                return "svg";
            case EPS_INDEX:
                return "eps";
            case PGF_INDEX:
                return "pgf";
            case PDF_INDEX:
                return "pdf";
            case SCR_INDEX:
                return "scr";
            case PCB_INDEX:
                return "pcb";
            case DXF_INDEX:
                return "dxf";

            default:
                System.out.println (
                    "dialogExport.getExportFormat Warning:"+
                    " file format set to png");
                return "png";
        }
    }

    /** @return a string containing the file name given by the user
    */
    public String getFilename()
    {
        return fileName.getText();
    }

    /** Sets the actual file name

        @param f a string containing the default file name
    */
    public void setFilename(String f)
    {
        fileName.setText(f);
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

    /** Set the magnification factor for vector format export
        @param d the default unit per pixel value
    */
    public void setMagnification(double d)
    {
        multiplySizes.setText(""+d);
    }

    /** Get the magnification factor for vector format export
        @return the unit per pixel value
    */
    public double getMagnification()
    {
        return Double.parseDouble(multiplySizes.getText());
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

    /** Sets the default export format.
        @param s The export format. If the format string is not
            recognized (valid strings are {"png"|"jpg"|"svg"|"eps"|"pgf"|
            "pdf"|"scr"|"pcb"|"dxf"}), use the png format.
    */
    public void setFormat(String s)
    {
        if ("png".equals(s)) {
            fileFormat.setSelectedIndex(PNG_INDEX);
        } else if ("jpg".equals(s)) {
            fileFormat.setSelectedIndex(JPG_INDEX);
        } else if ("svg".equals(s)) {
            fileFormat.setSelectedIndex(SVG_INDEX);
        } else if ("eps".equals(s)) {
            fileFormat.setSelectedIndex(EPS_INDEX);
        } else if ("pgf".equals(s)) {
            fileFormat.setSelectedIndex(PGF_INDEX);
        } else if ("pdf".equals(s)) {
            fileFormat.setSelectedIndex(PDF_INDEX);
        } else if ("scr".equals(s)) {
            fileFormat.setSelectedIndex(SCR_INDEX);
        } else if ("pcb".equals(s)) {
            fileFormat.setSelectedIndex(PCB_INDEX);
        } else if ("dxf".equals(s)) {
            fileFormat.setSelectedIndex(DXF_INDEX);
        } else {
            fileFormat.setSelectedIndex(PNG_INDEX);
        }
    }

    /** Create a JPanel containing all the interface elements needed
        for a size-based export of bitmap images.
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
        the configuration of export operation.
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
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
            new Insets(6,0,0,0));

        panel.add(blackWhite_CB, constraints);        // Add black/white cb

        JLabel multiplySizesLabel=new
            JLabel(Globals.messages.getString("Multiply_sizes"));

        constraints = DialogUtil.createConst(1,3,1,1,0,0,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(6,40,0,0));
        panel.add(multiplySizesLabel, constraints);

        multiplySizes=new JTextField();
        constraints = DialogUtil.createConst(2,3,1,1,100,100,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(6,0,0,0));
        panel.add(multiplySizes, constraints);

        splitLayers_CB= new JCheckBox(
            Globals.messages.getString("Split_layers_multiple_files"));

        constraints = DialogUtil.createConst(2,4,1,1,100,100,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
            new Insets(6,0,0,0));

        panel.add(splitLayers_CB, constraints);        // Add split layers cb

        JLabel fileNameLabel=new
            JLabel(Globals.messages.getString("File_name"));

        constraints = DialogUtil.createConst(1,5,1,1,0,0,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(6,40,12,0));
        panel.add(fileNameLabel, constraints);

        fileName=new JTextField();
        constraints = DialogUtil.createConst(2,5,1,1,100,100,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(6,0,12,0));

        panel.add(fileName, constraints);

        // See request #3526600
        // fileName.setEditable(false);

        JButton browse=new JButton(Globals.messages.getString("Browse"));
        constraints = DialogUtil.createConst(3,5,1,1,0,0,
            GridBagConstraints.CENTER, GridBagConstraints.NONE,
            new Insets(6,0,12,12));

        panel.add(browse, constraints);

        browse.addActionListener(createBrowseActionListener());
        return panel;
    }

    /** Create an action listener which handle clicking on the 'browse' button.
        @return the ActionListener
    */
    private ActionListener createBrowseActionListener()
    {
        return new ActionListener()
        {
            @Override public void actionPerformed(ActionEvent evt)
            {
                // Open the browser in order to let the user select the file
                // name on which export
                if(Globals.useNativeFileDialogs) {
                    // Native file dialog
                    FileDialog fd = new FileDialog(parent,
                        Globals.messages.getString("Select_file_export"),
                        FileDialog.SAVE);
                    String filen;

                    // Set defaults and make visible.
                    fd.setDirectory(new File(fileName.getText()).getPath());
                    fd.setVisible(true);

                    // The user has selected a file.
                    if(fd.getFile() != null) {
                        filen=Globals.createCompleteFileName(
                            fd.getDirectory(),
                            fd.getFile());
                        fileName.setText(filen);
                    }
                } else {
                    // Swing file dialog
                    JFileChooser fc = new JFileChooser(
                        new File(fileName.getText()).getPath());
                    int r = fc.showSaveDialog(null);
                    if (r == JFileChooser.APPROVE_OPTION) {
                        fileName.setText(fc.getSelectedFile().toString());
                    }
                }
            }
        };
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
        of export is being done.
        @param evt the event to be processed.
    */
    @Override public void actionPerformed(ActionEvent evt)
    {
        int idx=fileFormat.getSelectedIndex();

        // Once the index of the selected item is obtained, we proceed
        // by checking if it is a bitmap format
        if(idx==0 || idx == 1) {
            // It is a bitmap based export
            xsizePixel.setEnabled(true);
            ysizePixel.setEnabled(true);
            resolution.setEnabled(true);     // Resolution combo box
            antiAlias_CB.setEnabled(true);   // AntiAlias checkbox
            blackWhite_CB.setEnabled(true);  // Black and white checkbox
            multiplySizes.setEnabled(false); // Size multiplications
            multiplySizes.setText("1.0");
        } else {
            // It is a vector based export
            xsizePixel.setEnabled(false);
            ysizePixel.setEnabled(false);
            expectedSize.setText("");        // Don't show the size in pixels
            resolution.setEnabled(false);    // Resolution combo box
            antiAlias_CB.setEnabled(false);  // AntiAlias checkbox
            blackWhite_CB.setEnabled(true);  // Black and white checkbox
            multiplySizes.setEnabled(true);  // Size multiplications
        }
    }
}
