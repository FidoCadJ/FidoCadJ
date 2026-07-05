package fidocadj.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.event.*;

import fidocadj.globals.Globals;
import fidocadj.dialogs.controls.MinimumSizeDialog;
import fidocadj.dialogs.controls.DialogUtil;
import fidocadj.dialogs.controls.TextPopupMenu;


/** The class DialogAttachImage allows to determine which image has to be
    attached and shown as a background (for retracing/vectorization purposes).

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

    Copyright 2017-2023 by Davide Bucci
    </pre>

    @author Davide Bucci
*/

public final class DialogAttachImage extends MinimumSizeDialog
{
    private static final long serialVersionUID = 1L;
    private final JFrame parent;        // Parent window
    private final JTextField fileName;      // File name text field
    private final JTextField resolution;    // Resolution text field
    private final JTextField xcoord;    // x coordinate of the left top corner
    private final JTextField ycoord;    // y coordinate of the left top corner
    private final JTextField xsize;     // x size in mm of the image
    private final JTextField ysize;     // y size in mm of the image
    private BufferedImage img;
    private boolean isCalculating;      // A size calculation is being made.


    private static final int useResolution=0;
    private static final int useSizeX=1;
    private static final int useSizeY=2;

    private boolean attach;     // Indicates that the attach should be done
    private boolean showImage;

    /** Standard constructor.
        @param p the dialog parent
    */
    public DialogAttachImage(JFrame p)
    {
        super(500, 450, p, Globals.messages.getString("Attach_image_t"),
            true);
        parent=p;
        showImage=true;

        int ygrid=0;
        // Obtain the current content pane and create the grid layout manager
        // which will be used for putting the elements of the interface.
        GridBagLayout bgl=new GridBagLayout();
        GridBagConstraints constraints;
        Container contentPane=getContentPane();
        contentPane.setLayout(bgl);

        JLabel lblfilename=
            new JLabel(Globals.messages.getString("Image_file_attach"));

        constraints = DialogUtil.createConst(0,ygrid,1,1,100,100,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(6,6,6,6));

        contentPane.add(lblfilename, constraints);

        fileName=new JTextField(10);
        fileName.setText("");
        fileName.getDocument().addDocumentListener(new DocumentListener() {
            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void insertUpdate(DocumentEvent e)
            {
                loadImage();
            }

            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void removeUpdate(DocumentEvent e)
            {
                loadImage();
            }

            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void changedUpdate(DocumentEvent e)
            {
                loadImage();
            }
        });

        constraints = DialogUtil.createConst(1,ygrid,1,1,100,100,
            GridBagConstraints.EAST, GridBagConstraints.BOTH,
            new Insets(6,6,6,6));

        contentPane.add(fileName, constraints);

        JButton browse=new JButton(Globals.messages.getString("Browse"));
        constraints = DialogUtil.createConst(2,ygrid++,1,1,100,100,
            GridBagConstraints.CENTER, GridBagConstraints.NONE,
            new Insets(6,0,12,12));

        contentPane.add(browse, constraints);

        browse.addActionListener(createBrowseActionListener());

        JLabel lblresolution=
            new JLabel(Globals.messages.getString("Image_resolution"));

        constraints = DialogUtil.createConst(0,ygrid,1,1,100,100,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(6,6,6,6));

        contentPane.add(lblresolution, constraints);

        resolution=new JTextField(10);
        resolution.setText("200");
        // If the user changes the resolution, update the size calculation.
        resolution.getDocument().addDocumentListener(new DocumentListener() {
            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void insertUpdate(DocumentEvent e)
            {
                calculateSizeAndResolution(useResolution);
            }

            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void removeUpdate(DocumentEvent e)
            {
                calculateSizeAndResolution(useResolution);
            }

            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void changedUpdate(DocumentEvent e)
            {
                calculateSizeAndResolution(useResolution);
            }
        });

        constraints = DialogUtil.createConst(1,ygrid++,2,1,100,100,
            GridBagConstraints.EAST, GridBagConstraints.BOTH,
            new Insets(6,6,6,6));

        contentPane.add(resolution, constraints);
        JLabel lblsize=
            new JLabel(Globals.messages.getString("Image_size_mm"));

        constraints = DialogUtil.createConst(0,ygrid,1,1,100,100,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(6,6,6,6));

        contentPane.add(lblsize, constraints);

        xsize=new JTextField(5);
        xsize.setText("");
        xsize.getDocument().addDocumentListener(new DocumentListener() {
            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void insertUpdate(DocumentEvent e)
            {
                calculateSizeAndResolution(useSizeX);
            }

            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void removeUpdate(DocumentEvent e)
            {
                calculateSizeAndResolution(useSizeX);
            }

            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void changedUpdate(DocumentEvent e)
            {
                calculateSizeAndResolution(useSizeX);
            }
        });

        constraints = DialogUtil.createConst(1,ygrid,1,1,100,100,
            GridBagConstraints.EAST, GridBagConstraints.BOTH,
            new Insets(6,6,6,6));

        contentPane.add(xsize, constraints);
        ysize=new JTextField(5);
        ysize.setText("");
        ysize.getDocument().addDocumentListener(new DocumentListener() {
            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void insertUpdate(DocumentEvent e)
            {
                calculateSizeAndResolution(useSizeY);
            }

            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void removeUpdate(DocumentEvent e)
            {
                calculateSizeAndResolution(useSizeY);
            }

            /** Needed to implement the DocumentListener interface
                @param e the document event.
            */
            @Override public void changedUpdate(DocumentEvent e)
            {
                calculateSizeAndResolution(useSizeY);
            }
        });

        constraints = DialogUtil.createConst(2,ygrid++,1,1,100,100,
            GridBagConstraints.EAST, GridBagConstraints.BOTH,
            new Insets(6,6,6,6));

        contentPane.add(ysize, constraints);

        JLabel lblcoords=
            new JLabel(Globals.messages.getString("Top_left_coords"));

        constraints = DialogUtil.createConst(0,ygrid,1,1,100,100,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(6,6,6,6));

        contentPane.add(lblcoords, constraints);

        xcoord=new JTextField(5);
        xcoord.setText("0");

        constraints = DialogUtil.createConst(1,ygrid,1,1,100,100,
            GridBagConstraints.EAST, GridBagConstraints.BOTH,
            new Insets(6,6,6,6));

        contentPane.add(xcoord, constraints);
        ycoord=new JTextField(5);
        ycoord.setText("0");

        constraints = DialogUtil.createConst(2,ygrid++,1,1,100,100,
            GridBagConstraints.EAST, GridBagConstraints.BOTH,
            new Insets(6,6,6,6));

        contentPane.add(ycoord, constraints);
        // Put the No image, OK and Cancel buttons and make them active.
        JButton noImg=new JButton(Globals.messages.getString("No_img"));
        JButton ok=new JButton(Globals.messages.getString("Ok_btn"));
        JButton cancel=new JButton(Globals.messages.getString("Cancel_btn"));
        Box b=Box.createHorizontalBox();
        b.add(Box.createHorizontalGlue());
        ok.setPreferredSize(cancel.getPreferredSize());
        b.add(noImg);
        b.add(Box.createHorizontalStrut(12));
        if (Globals.okCancelWinOrder) {
            b.add(ok);
            b.add(Box.createHorizontalStrut(12));
            b.add(cancel);
        } else {
            b.add(cancel);
            b.add(Box.createHorizontalStrut(12));
            b.add(ok);
        }
        constraints = DialogUtil.createConst(1,ygrid++,3,1,0,0,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(12,40,0,0));
        contentPane.add(b, constraints);            // Add buttons
        noImg.addActionListener(new ActionListener()
            {
                @Override public void actionPerformed(ActionEvent evt)
                {
                    attach=true;
                    showImage=false;
                    fileName.setText("");
                    setVisible(false);
                }
            });
        ok.addActionListener(new ActionListener()
            {
                @Override public void actionPerformed(ActionEvent evt)
                {
                    if(validateForm()) {
                        attach=true;
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
        TextPopupMenu.addPopupToText(resolution);
        TextPopupMenu.addPopupToText(xcoord);
        TextPopupMenu.addPopupToText(ycoord);
        TextPopupMenu.addPopupToText(xsize);
        TextPopupMenu.addPopupToText(ysize);
    }

    /** Calculate the relations between size and resolution of the image.
        @param useSize if true, calculate resolution from size.
            if false, calculate size from resolution.
    */
    private void calculateSizeAndResolution(int useSize)
    {
        if(isCalculating || img==null) {
            return;
        }

        isCalculating=true;
        final double oneinch=25.4; // Conversion between inches and mm.
        switch (useSize) {
            case useResolution:
                try {
                    double res=getResolution();
                    double w=img.getWidth()/res*oneinch;
                    double h=img.getHeight()/res*oneinch;
                    xsize.setText(Globals.roundTo(w,3));
                    ysize.setText(Globals.roundTo(h,3));
                } catch (NumberFormatException eE){
                    isCalculating=false;
                    return;
                }
                break;
            case useSizeX:
                try {
                    double sizex=getSizeX();
                    double res=img.getWidth()/sizex*oneinch;
                    setResolution(res);
                    double h=img.getHeight()/res*oneinch;
                    ysize.setText(Globals.roundTo(h,3));
                } catch (NumberFormatException eE){
                    isCalculating=false;
                    return;
                }
                break;
            case useSizeY:
                try {
                    double sizey=getSizeY();
                    double res=img.getHeight()/sizey*oneinch;
                    setResolution(res);
                    double w=img.getWidth()/res*oneinch;
                    xsize.setText(Globals.roundTo(w,3));
                } catch (NumberFormatException eE){
                    isCalculating=false;
                    return;
                }
                break;
            default:
        }
        isCalculating=false;
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
                        FileDialog.LOAD);
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
                    int r = fc.showOpenDialog(null);
                    if (r == JFileChooser.APPROVE_OPTION) {
                        fileName.setText(fc.getSelectedFile().toString());
                    }
                }
                loadImage();
                calculateSizeAndResolution(useResolution);
            }
        };
    }

    /** Validate the data entered by the user.
        @return true if the data is valid.
    */
    private boolean validateForm()
    {
        try {
            img=ImageIO.read(new File(getFilename()));
            getResolution();
            getSizeX();
            getSizeY();
            getCornerX();
            getCornerY();
        } catch (NumberFormatException nN) {
            JOptionPane.showMessageDialog(parent,
                Globals.messages.getString("Format_invalid"),
                "",
                JOptionPane.INFORMATION_MESSAGE);
            return false;
        } catch (IOException eE) {
            JOptionPane.showMessageDialog(parent,
                Globals.messages.getString("Can_not_attach_image"),
                "",
                JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        return true;
    }

    /** Load the background image. Or at least try to do it!
    */
    private void loadImage()
    {
        try {
            img=ImageIO.read(new File(getFilename()));
        } catch (IOException eE) {
            System.err.println("Error: Could not load the background image");
        }
    }
    /** Indicates that the image attach should be done: the user selected
        the "ok" button.
        @return a boolean value which indicates if the attach should be done.
    */
    public boolean shouldAttach()
    {
        return attach;
    }

    /** Get the filename
        @return a string containing the file name given by the user.
    */
    public String getFilename()
    {
        return fileName.getText();
    }

    /** Set the filename
        @param s a string containing the file name given by the user.
    */
    public void setFilename(String s)
    {
        fileName.setText(s);
    }

    /** Get the resolution.
        @return the resolution in dpi of the image to be used.
    */
    public double getResolution()
    {
        return Double.parseDouble(resolution.getText());
    }

    /** Get the width of the image.
        @return the width in mm
    */
    public double getSizeX()
    {
        return Double.parseDouble(xsize.getText());
    }

    /** Get the height of the image.
        @return the height in mm
    */
    public double getSizeY()
    {
        return Double.parseDouble(ysize.getText());
    }

    /** Set the resolution
        @param r the resolution in dpi of the image to be used.
    */
    public void setResolution(double r)
    {
        resolution.setText(Globals.roundTo(r));
    }

    /** Set the coordinates of the left topmost point of the image (use
        FidoCadJ coordinates).
        @param x the x coordinate.
        @param y the y coordinate.
    */
    public void setCorner(double x, double y)
    {
        xcoord.setText(Globals.roundTo(x,3));
        ycoord.setText(Globals.roundTo(y,3));
    }

    /** Get the x coordinate of the left topmost point of the image (use
        FidoCadJ coordinates).
        @return the x coordinate.
    */
    public double getCornerX()
    {
        return Double.parseDouble(xcoord.getText());
    }

    /** Check if the image has to be shown or not.
        @return true if the image attached has to be shown.
    */
    public boolean getShowImage()
    {
        return showImage;
    }

    /** Get the currently loaded image.
        @return the image.
    */
    public BufferedImage getImage()
    {
        return img;
    }
    /** Get the y coordinate of the left topmost point of the image (use
        FidoCadJ coordinates).
        @return the y coordinate.
    */
    public double getCornerY()
    {
        return Double.parseDouble(ycoord.getText());
    }
}