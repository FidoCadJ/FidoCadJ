package fidocadj.dialogs;

import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;

import fidocadj.dialogs.controls.LayerInfo;
import fidocadj.dialogs.controls.OSKeybPanel;
import fidocadj.dialogs.controls.ParameterDescription;
import fidocadj.dialogs.controls.LayerCellRenderer;
import fidocadj.dialogs.controls.DashInfo;
import fidocadj.dialogs.controls.DashCellRenderer;
import fidocadj.dialogs.controls.ArrowInfo;
import fidocadj.dialogs.controls.ArrowCellRenderer;
import fidocadj.dialogs.controls.OSKeybPanel.KEYBMODES;
import fidocadj.dialogs.controls.DialogUtil;
import fidocadj.dialogs.controls.PadSelector;
import fidocadj.dialogs.controls.TextPopupMenu;

import fidocadj.globals.Globals;
import fidocadj.layers.LayerDesc;
import fidocadj.graphic.PointG;
import fidocadj.graphic.FontG;

/**
    Allows to create a generic dialog, capable of displaying and let the user
    modify the parameters of a graphic primitive. The idea is that the dialog
    uses a ParameterDescripion vector which contains all the elements, their
    description as well as the type. Depending on the contents of the array, the
    window will be created automagically.

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
 */
public final class DialogParameters extends JDialog
{
    private static final long serialVersionUID = 1L;
    //private int minWidth = 450;
    //private int minHeight = 350;
    private static final int MAX = 20;

    // Maximum number of user interface elements of the same type present
    // in the dialog window.
    private static final int MAX_ELEMENTS = 20;
    public boolean active; // true if the user selected Ok

    // Text box array and counter
    private final JTextField jtf[];
    private int tc; // NOPMD this field can NOT be final! It is a counter.

    // Check box array and counter
    private final JCheckBox jcb[];
    private int cc; // NOPMD this field can NOT be final! It is a counter.

    private final JComboBox jco[];
    private int co; // NOPMD this field can NOT be final! It is a counter.

    private final java.util.List<ParameterDescription> v;

    private PadSelector pads;

    OSKeybPanel keyb1;
    OSKeybPanel keyb2;
    JTabbedPane keyb = new JTabbedPane();

    /**
       Programmatically build a dialog frame containing the appropriate
       elements, in order to let the user modify the characteristics of a
       graphic primitive.

        @param parent the parent frame useful for creating the dialog.
        @param vec a ParameterDescription array containing the value and the
                description of each parameter that should be edited by the
                user.
        @param strict true if a strict compatibility with FidoCAD is required
        @param layers a vector containing the layers
     */
    // Here some legacy code makes use of generics. They are tested, so
    // there is no risk of an actual error, but Java issues a warning.
    @SuppressWarnings("unchecked")
    public DialogParameters(final JFrame parent,
            java.util.List<ParameterDescription> vec,
            boolean strict, java.util.List<LayerDesc> layers)
    {
        super(parent, Globals.messages.getString("Param_opt"), true);

        keyb1 = new OSKeybPanel(KEYBMODES.GREEK);
        keyb2 = new OSKeybPanel(KEYBMODES.MISC);
        JPanel hints = new JPanel();
        JTextArea hintsL = new JTextArea(
            Globals.messages.getString("text_hints"),6,40);
        TextPopupMenu.addPopupToText(hintsL);
        hintsL.setLineWrap(true);
        hintsL.setWrapStyleWord(true);
        hintsL.setEditable(false);
        hintsL.setOpaque(false);
        hints.add(hintsL);
        keyb1.setField(this);
        keyb2.setField(this);
        keyb.addTab(Globals.messages.getString("param_greek"), keyb1);
        keyb.addTab(Globals.messages.getString("param_misc"), keyb2);
        keyb.addTab(Globals.messages.getString("param_hints"), hints);
        keyb.setVisible(false);
        v = vec;

        // We create dynamically all the needed elements.
        // For this reason, we work on arrays of the potentially useful Swing
        // objects.

        jtf = new JTextField[MAX_ELEMENTS];
        jcb = new JCheckBox[MAX_ELEMENTS];
        jco = new JComboBox[MAX_ELEMENTS];

        pads = new PadSelector(0);

        active = false;

        GridBagLayout bgl = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        Container contentPane = getContentPane();
        contentPane.setLayout(bgl);
        boolean extStrict = strict;

        int top = 0;

        JLabel lab;

        tc = 0;
        cc = 0;
        co = 0;

        // We process all parameter passed. Depending on its type, a
        // corresponding interface element will be created.
        // A symmetrical operation is done when validating parameters.

        int ycount = 0;
        for (ParameterDescription pd : v) {
            // We do not need to store label objects, since we do not need
            // to retrieve data from them.
            lab = new JLabel(pd.description);
            constraints.weightx = 100;
            constraints.weighty = 100;
            constraints.gridx = 1;
            constraints.gridy = ycount;
            constraints.gridwidth = 1;
            constraints.gridheight = 1;

            // The first element needs a little bit more space at the top.
            if (ycount == 0) {
                top = 10;
            } else {
                top = 5;
            }

            // Here, we configure the grid layout.
            constraints.insets = new Insets(top, 20, 0, 6);

            constraints.fill = GridBagConstraints.VERTICAL;
            constraints.anchor = GridBagConstraints.EAST;
            lab.setEnabled(!(pd.isExtension && extStrict));

            if (!(pd.parameter instanceof Boolean)) {
                contentPane.add(lab, constraints);
            }
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(top, 0, 0, 0);
            constraints.fill = GridBagConstraints.HORIZONTAL;

            // Now, depending on the type of parameter we create interface
            // elements and we populate the dialog.
            if (pd.parameter instanceof PointG) {
                jtf[tc] = new JTextField(10);
                TextPopupMenu.addPopupToText(jtf[tc]);
                jtf[tc].setPreferredSize(new Dimension(150,20));
                jtf[tc].setText("" + ((PointG) pd.parameter).x);
                constraints.weightx = 100;
                constraints.weighty = 100;
                constraints.gridx = 2;
                constraints.gridy = ycount;
                constraints.gridwidth = 1;
                constraints.gridheight = 1;
                // Disable FidoCadJ extensions in the strict compatibility mode
                jtf[tc].setEnabled(!(pd.isExtension && extStrict));

                contentPane.add(jtf[tc++], constraints);

                jtf[tc] = new JTextField(10);
                TextPopupMenu.addPopupToText(jtf[tc]);
                jtf[tc].setPreferredSize(new Dimension(150,20));
                jtf[tc].setText("" + ((PointG) pd.parameter).y);
                constraints.weightx = 100;
                constraints.weighty = 100;
                constraints.gridx = 3;
                constraints.gridy = ycount;
                constraints.gridwidth = 1;
                constraints.gridheight = 1;
                constraints.insets = new Insets(top, 6, 0, 20);
                constraints.fill = GridBagConstraints.HORIZONTAL;
                jtf[tc].setEnabled(!(pd.isExtension && extStrict));
                contentPane.add(jtf[tc++], constraints);
            } else if (pd.parameter instanceof String) {
                jtf[tc] = new JTextField(24);
                TextPopupMenu.addPopupToText(jtf[tc]);
                jtf[tc].setPreferredSize(new Dimension(150,20));
                jtf[tc].setText((String) pd.parameter);
                // If we have a String text field in the first position, its
                // contents should be evidenced, since it is supposed to be
                // the most important field (e.g. for the AdvText primitive)
                if (ycount == 0) {
                    jtf[tc].selectAll();
                }
                constraints.weightx = 100;
                constraints.weighty = 100;
                constraints.gridx = 2;
                constraints.gridy = ycount;
                constraints.gridwidth = 2;
                constraints.gridheight = 1;
                constraints.insets = new Insets(top, 0, 0, 20);
                constraints.fill = GridBagConstraints.HORIZONTAL;
                jtf[tc].setEnabled(!(pd.isExtension && extStrict));
                contentPane.add(jtf[tc++], constraints);
            } else if (pd.parameter instanceof Boolean) {
                jcb[cc] = new JCheckBox(pd.description);
                jcb[cc].setSelected(((Boolean) pd.parameter).booleanValue());
                constraints.weightx = 100;
                constraints.weighty = 100;
                constraints.gridx = 2;
                constraints.gridy = ycount;
                constraints.gridwidth = 2;
                constraints.gridheight = 1;
                constraints.insets = new Insets(top, 0, 0, 20);
                constraints.fill = GridBagConstraints.HORIZONTAL;
                jcb[cc].setEnabled(!(pd.isExtension && extStrict));
                contentPane.add(jcb[cc++], constraints);
            } else if (pd.parameter instanceof Integer) {
                constraints.weightx = 100;
                constraints.weighty = 100;
                constraints.gridx = 2;
                constraints.gridy = ycount;
                constraints.gridwidth = 2;
                constraints.gridheight = 1;
                constraints.insets = new Insets(top, 0, 0, 20);
                constraints.fill = GridBagConstraints.HORIZONTAL;
                pads.setEnabled(!(pd.isExtension && extStrict));

                if (pd.description.equals(
                        Globals.messages.getString("ctrl_pad_style")))
                {
                    pads.setSelectedIndex((Integer) pd.parameter);
                    contentPane.add(pads, constraints);
                } else {
                    jtf[tc] = new JTextField(24);
                    TextPopupMenu.addPopupToText(jtf[tc]);
                    jtf[tc].setPreferredSize(new Dimension(150, 20));
                    jtf[tc].setText(((Integer) pd.parameter).toString());
                    contentPane.add(jtf[tc++], constraints);
                }

            } else if (pd.parameter instanceof Float) {
                jtf[tc] = new JTextField(24);
                TextPopupMenu.addPopupToText(jtf[tc]);
                jtf[tc].setPreferredSize(new Dimension(150,20));
                jtf[tc].setText(""+pd.parameter);
                constraints.weightx = 100;
                constraints.weighty = 100;
                constraints.gridx = 2;
                constraints.gridy = ycount;
                constraints.gridwidth = 2;
                constraints.gridheight = 1;
                constraints.insets = new Insets(top, 0, 0, 20);
                constraints.fill = GridBagConstraints.HORIZONTAL;
                jtf[tc].setEnabled(!(pd.isExtension && extStrict));
                contentPane.add(jtf[tc++], constraints);
            } else if (pd.parameter instanceof FontG) {
                jco[co] = new JComboBox();
                String selectedFont = ((FontG) pd.parameter).getFamily();
                loadFontsInBackground((JComboBox<String>) jco[co],
                                                            selectedFont);

                constraints.weightx = 100;
                constraints.weighty = 100;
                constraints.gridx = 2;
                constraints.gridy = ycount;
                constraints.gridwidth = 2;
                constraints.gridheight = 1;
                constraints.insets = new Insets(top, 0, 0, 20);
                constraints.fill = GridBagConstraints.HORIZONTAL;
                jco[co].setEnabled(!(pd.isExtension && extStrict));
                contentPane.add(jco[co++], constraints);
            } else if (pd.parameter instanceof LayerInfo) {
                jco[co] = new JComboBox(new Vector<LayerDesc>(layers));
                jco[co].setSelectedIndex(((LayerInfo) pd.parameter).getLayer());
                jco[co].setRenderer(new LayerCellRenderer());

                constraints.weightx = 100;
                constraints.weighty = 100;
                constraints.gridx = 2;
                constraints.gridy = ycount;
                constraints.gridwidth = 2;
                constraints.gridheight = 1;
                constraints.insets = new Insets(top, 0, 0, 20);
                constraints.fill = GridBagConstraints.HORIZONTAL;
                jco[co].setEnabled(!(pd.isExtension && extStrict));
                contentPane.add(jco[co++], constraints);

            } else if (pd.parameter instanceof ArrowInfo) {
                jco[co] = new JComboBox<ArrowInfo>();
                jco[co].addItem(new ArrowInfo(0));
                jco[co].addItem(new ArrowInfo(1));
                jco[co].addItem(new ArrowInfo(2));
                jco[co].addItem(new ArrowInfo(3));

                jco[co].setSelectedIndex(((ArrowInfo) pd.parameter).style);
                jco[co].setRenderer(new ArrowCellRenderer());

                constraints.weightx = 100;
                constraints.weighty = 100;
                constraints.gridx = 2;
                constraints.gridy = ycount;
                constraints.gridwidth = 2;
                constraints.gridheight = 1;
                constraints.insets = new Insets(top, 0, 0, 20);
                constraints.fill = GridBagConstraints.HORIZONTAL;
                jco[co].setEnabled(!(pd.isExtension && extStrict));
                contentPane.add(jco[co++], constraints);

            } else if (pd.parameter instanceof DashInfo) {

                jco[co] = new JComboBox<DashInfo>();

                for (int k = 0; k < Globals.dashNumber; ++k) {
                    jco[co].addItem(new DashInfo(k));
                }

                jco[co].setSelectedIndex(((DashInfo) pd.parameter).style);
                jco[co].setRenderer(new DashCellRenderer());

                constraints.weightx = 100;
                constraints.weighty = 100;
                constraints.gridx = 2;
                constraints.gridy = ycount;
                constraints.gridwidth = 2;
                constraints.gridheight = 1;
                constraints.insets = new Insets(top, 0, 0, 20);
                constraints.fill = GridBagConstraints.HORIZONTAL;
                jco[co].setEnabled(!(pd.isExtension && extStrict));
                contentPane.add(jco[co++], constraints);
            }
            ++ycount;
            if (ycount >= MAX) {
                break;
            }
        }
        // Put the OK and Cancel buttons and make them active.
        JButton ok = new JButton(Globals.messages.getString("Ok_btn"));
        JButton cancel = new JButton(Globals.messages.getString("Cancel_btn"));
        JButton keybd = new JButton("\u00B6\u2211\u221A");// phylum
        keybd.setFocusable(false);
        keybd.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e)
            {
                // If at this point, the keyboard is not visible, this means
                // that it will become visible in a while. It is better to
                // resize first and then show up the keyboard.
                /*if (keyb.isVisible()) {
                    minWidth = 400;
                    minHeight = 350;
                } else {
                    minWidth = 400;
                    minHeight = 500;
                }*/
                keyb.setVisible(!keyb.isVisible());
                pack();
            }
        });
        ++ycount;
        constraints.gridx = 0;
        constraints.gridy = ycount++;
        constraints.gridwidth = 4;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(6, 20, 20, 20);

        // Put the OK and Cancel buttons and make them active.
        Box b = Box.createHorizontalBox();
        b.add(keybd); // phylum
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
        contentPane.add(b, constraints);

        constraints.gridx = 0;
        constraints.gridy = ycount;
        constraints.gridwidth = 4;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(6, 20, 20, 20);

        contentPane.add(keyb, constraints);

        ok.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent evt)
            {
                try {
                    int ycount=0;
                    //ParameterDescription pd;
                    tc = 0;
                    cc = 0;
                    co = 0;

                    // Here we read all the contents of the interface and we
                    // update the contents of the parameter description array.

                    for (ParameterDescription pd: v) {
                        ++ycount;
                        if (ycount >= MAX) {
                            break;
                        }
                        if (pd.parameter instanceof PointG) {
                            ((PointG) pd.parameter).x = Float
                                    .parseFloat(jtf[tc++].getText());
                            ((PointG) pd.parameter).y = Float
                                    .parseFloat(jtf[tc++].getText());
                        } else if (pd.parameter instanceof String) {
                            pd.parameter = jtf[tc++].getText();
                        } else if (pd.parameter instanceof Boolean) {
                            pd.parameter = Boolean.valueOf(
                                jcb[cc++].isSelected());
                        } else if (pd.parameter instanceof Integer) {
                            if (pd.description.equals(
                                Globals.messages.getString("ctrl_pad_style")))
                            {
                                pd.parameter = pads.getSelectedIndex();
                            } else {
                                pd.parameter = Integer.valueOf(Integer
                                    .parseInt(jtf[tc++].getText()));
                            }
                        } else if (pd.parameter instanceof Float) {
                            pd.parameter = Float.valueOf(Float
                                    .parseFloat(jtf[tc++].getText()));
                        } else if (pd.parameter instanceof FontG) {
                            pd.parameter = new FontG((String) jco[co++]
                                    .getSelectedItem());
                        } else if (pd.parameter instanceof LayerInfo) {
                            pd.parameter = new LayerInfo(jco[co++]
                                    .getSelectedIndex());
                        } else if (pd.parameter instanceof ArrowInfo) {
                            pd.parameter = new ArrowInfo(jco[co++]
                                    .getSelectedIndex());
                        } else if (pd.parameter instanceof DashInfo) {
                            pd.parameter = new DashInfo(jco[co++]
                                    .getSelectedIndex());
                        }
                    }
                } catch (NumberFormatException eE) {
                    // Error detected. Probably, the user has entered an
                    // invalid string when FidoCadJ was expecting a numerical
                    // input.
                    JOptionPane.showMessageDialog(parent,
                            Globals.messages.getString("Format_invalid")+
                            " ("+eE.getMessage()+")", "",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                active = true;
                setVisible(false);
                keyb.setVisible(false);
            }
        });

        // Here is an action in which the dialog is closed
        AbstractAction cancelAction = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
                keyb.setVisible(false);
            }
        };
        cancel.addActionListener(cancelAction);
        DialogUtil.addCancelEscape(this, cancelAction);

        this.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e)
            {
                keyb.setVisible(false);
            }
        });

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

    /**
        Get a ParameterDescription vector describing the characteristics
        modified by the user.
        @return a ParameterDescription vector describing each parameter.
     */
    public java.util.List<ParameterDescription> getCharacteristics()
    {
        return v;
    }

    /**
     Load the list of available fonts in the background to avoid UI freezing.
     The fonts are loaded into the provided JComboBox.

     @param comboBox The JComboBox to populate with the available font families.
     @param selectedFont The font family name to select in the comboBox.
     */
    private void loadFontsInBackground(JComboBox<String> comboBox,
            String selectedFont)
    {
        new Thread(() -> {
            GraphicsEnvironment gE =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] fontFamilies = gE.getAvailableFontFamilyNames();
            SwingUtilities.invokeLater(() -> {
                for (String font : fontFamilies) {
                    comboBox.addItem(font);
                    if (font.equals(selectedFont)) {
                        comboBox.setSelectedItem(font);
                    }
                }
            });
        }).start();
    }
}
