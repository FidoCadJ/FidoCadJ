package fidocadj.toolbars;


import fidocadj.FidoFrame;
import javax.swing.*;
import java.util.*;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import fidocadj.geom.ChangeCoordinatesListener;
import fidocadj.globals.Globals;
import fidocadj.layers.LayerDesc;

import fidocadj.dialogs.controls.LayerCellRenderer;
import fidocadj.dialogs.controls.LayerComboBoxEditor;


/** ToolbarZoom class

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

    Copyright 2007-2026 by Davide Bucci
    </pre>

    @author Davide Bucci
*/
public final class ToolbarZoom extends JToolBar implements ActionListener,
                                                     ChangeZoomListener,
                                                     ChangeCoordinatesListener
{
    private static final long serialVersionUID = 1L;
    private final JComboBox<String> zoom;
    private final JToggleButton showGrid;
    private final JToggleButton snapGrid;
    private final JToggleButton showLibs;
    private final JLabel coords;

    private ChangeGridState changeListener;
    private double oldzoom;
    private ChangeZoomListener notifyZoomChangeListener;
    private ZoomToFitListener actualZoomToFitListener;

    private final JComboBox<LayerDesc> layerSel;
    private ChangeSelectedLayer changeLayerListener;

    private FidoFrame fidoFrame;

    /** Standard constructor
        @param l the layer description
        @param fidoFrame the frame parent
    */
    public ToolbarZoom (List<LayerDesc> l, FidoFrame fidoFrame)
    {
        this.fidoFrame = fidoFrame;
        putClientProperty("Quaqua.ToolBar.style", "title");
        zoom = new JComboBox<String>();
        zoom.addItem("25%");
        zoom.addItem("50%");
        zoom.addItem("75%");
        zoom.addItem("100%");
        zoom.addItem("150%");
        zoom.addItem("200%");
        zoom.addItem("300%");
        zoom.addItem("400%");
        zoom.addItem("600%");
        zoom.addItem("800%");
        zoom.addItem("1000%");
        zoom.addItem("1500%");
        zoom.addItem("2000%");
        zoom.addItem("3000%");
        zoom.addItem("4000%");
        zoom.setPreferredSize(new Dimension (100,29));
        zoom.setMaximumSize(new Dimension (100,38));
        zoom.setMinimumSize(new Dimension (100,18));

        /* Commented the following line due to this remark:
            http://www.electroyou.it/phpBB2/viewtopic.php?f=4&
                t=18347&start=450#p301931
        */
        //zoom.setFocusable(false);
        JButton zoomFit;
        zoomFit=new JButton(Globals.messages.getString("Zoom_fit"));
        showGrid=new JToggleButton(Globals.messages.getString("ShowGrid"));

        snapGrid=new JToggleButton(Globals.messages.getString("SnapToGrid"));

        coords = new JLabel("");

        setBorderPainted(false);
        layerSel = new JComboBox<LayerDesc>(new Vector<LayerDesc>(l));
        layerSel.setToolTipText(
            Globals.messages.getString("tooltip_layerSel"));
        layerSel.setRenderer(new LayerCellRenderer());
        layerSel.setEditor(new LayerComboBoxEditor(layerSel, fidoFrame));
        layerSel.setEditable(true);
        layerSel.setPreferredSize(new Dimension(150, 12));
        changeListener=null;

        showLibs=new JToggleButton(Globals.messages.getString("Libs"));

        // MacOSX Quaqua information
        zoomFit.putClientProperty("Quaqua.Button.style","toggleWest");
        showGrid.putClientProperty("Quaqua.Button.style","toggleCenter");
        snapGrid.putClientProperty("Quaqua.Button.style","toggleCenter");
        showLibs.putClientProperty("Quaqua.Button.style","toggleEast");

        // VAqua7 information
        String style="recessed";  // order: recessed, textured,
        //  segmentedCapsule, segmentedRoundRect, segmented, segmentedTextured

        zoomFit.putClientProperty("JButton.buttonType",style);
        zoomFit.putClientProperty("JButton.segmentPosition","first");

        showGrid.putClientProperty("JButton.buttonType",style);
        showGrid.putClientProperty("JButton.segmentPosition","middle");

        snapGrid.putClientProperty("JButton.buttonType",style);
        snapGrid.putClientProperty("JButton.segmentPosition","middle");

        showLibs.putClientProperty("JButton.buttonType",style);
        showLibs.putClientProperty("JButton.segmentPosition","last");

        zoom.addActionListener(this);
        zoomFit.addActionListener(this);
        showGrid.addActionListener(this);
        snapGrid.addActionListener(this);
        showLibs.addActionListener(this);
        layerSel.addActionListener(new ActionListener()
        {
            @Override public void actionPerformed(ActionEvent evt)
            {
                if (layerSel.getSelectedIndex()>=0 && changeListener!=null) {
                    changeLayerListener.changeSelectedLayer(
                        layerSel.getSelectedIndex());
                }
            }
        });

        changeListener=null;
        add(zoom);
        add(zoomFit);
        add(showGrid);
        add(snapGrid);
        add(showLibs);
        add(layerSel);
        add(Box.createGlue());
        add(coords);
        add(Box.createGlue());

        coords.setPreferredSize(new Dimension (300,28));
        coords.setMinimumSize(new Dimension (300,18));
        coords.setMaximumSize(new Dimension (300,38));
        setFloatable(true);
        setRollover(false);
        zoom.setEditable(true);
        showGrid.setSelected(true);
        snapGrid.setSelected(true);
        showLibs.setSelected(true);
        changeCoordinates(0, 0);
    }

    /** Add a layer listener (object implementing the ChangeSelectionListener
        interface) whose change method will be called when the current
        layer should be changed.
        @param c the change selection layer listener.
    */
    public void addLayerListener(ChangeSelectedLayer c)
    {
        changeLayerListener=c;
    }

    /** Add a grid state listener whose methods will be called when the current
        grid state should be changed.
        @param c the wanted grid state
    */
    public void addGridStateListener(ChangeGridState c)
    {
        changeListener=c;
    }

    /** Add a zoom change listener to be called when the zoom is changed by the
        user with the combo box slide.
        @param c the new listener.
    */
    public void addChangeZoomListener(ChangeZoomListener c)
    {
        notifyZoomChangeListener=c;
    }

    /** Add a zoom to fit listener to be called when the user wants to fit
        @param c the new listener.
    */
    public void addZoomToFitListener(ZoomToFitListener c)
    {
        actualZoomToFitListener=c;
    }

    /** The event listener to be called when the buttons are pressed, or the
        zoom setup is changed.
        @param evt the event to be processed.
    */
    @Override public void actionPerformed(ActionEvent evt)
    {
        String s = evt.getActionCommand();

        // Buttons
        if(s.equals(Globals.messages.getString("ShowGrid"))) {
            // Toggle grid visibility
            if(changeListener!=null) {
                changeListener.setGridVisibility(showGrid.isSelected());
            }
        } else if(s.equals(Globals.messages.getString("SnapToGrid"))) {
            // Toggle snap to grid
            if(changeListener!=null) {
                changeListener.setSnapState(snapGrid.isSelected());
            }
        } else if(s.equals(Globals.messages.getString("Zoom_fit"))) {
            // Zoom to fit
            if(actualZoomToFitListener!=null) {
                actualZoomToFitListener.zoomToFit();
            }
        } else if(evt.getSource() instanceof JComboBox) {
            // ComboBox: the only one is about the zoom settings.
            handleZoomChangeEvents(evt);
        } else if(s.equals(Globals.messages.getString("Libs"))) {
            // Toggle library visibility
            actualZoomToFitListener.showLibs(showLibs.isSelected());
        }
    }

    /** Set the current state of the button which controls the visibility of
        the library tree. This method is useful when the state is changed
        elsewhere and one needs to update the visible appearance of the button
        to follow the change.
        @param s the true if the libs are visible.
    */
    public void setShowLibsState(boolean s)
    {
        showLibs.setSelected(s);
    }

    /** Handle events of zoom change from the combo box.
        @param evt the event object.
    */
    private void handleZoomChangeEvents(ActionEvent evt)
    {
        if (notifyZoomChangeListener!=null) {
            try {
                String s=(String)zoom.getSelectedItem();
                // The percent symbol should be eliminated.
                s=s.replace('%',' ').trim();
                //System.out.println ("New zoom: "+s);
                double z=Double.parseDouble(s);
                // Very important: if I remove that, CPU goes to 100%
                // since this is called continuously!
                if(z==oldzoom) {
                    return;
                }
                oldzoom=z;
                if(Globals.minZoomFactor<=z && z<=Globals.maxZoomFactor) {
                    notifyZoomChangeListener.changeZoom(z/100);
                }
            } catch (NumberFormatException ee) {
                System.out.println("Exception while changing the zoom. "+
                    evt);
            }
        }
    }

    /** Change the zoom level and show it in the combo box
        @param z the new zoom level to be considered
    */
    public void changeZoom(double z)
    {
        zoom.setSelectedItem(""+((int)(z*100))+"%");
    }

    /** Change the cursor coordinates to be shown
        @param x the x value of the cursor coordinates (logical units)
        @param y the y value of the cursor coordinates (logical units)
    */
    public void changeCoordinates(float x, float y)
    {
        float xum=x*127;
        float yum=y*127;

        float xmm=xum/1000;
        float ymm=yum/1000;

        Color c1=UIManager.getColor("Label.foreground");
        Color c2=UIManager.getColor("Label.background");
        if(c1!=null && c2!=null) {
            coords.setOpaque(false);
            coords.setForeground(c1);
            coords.setBackground(c2);
        }
        coords.setText(""+x+"; "+y+ " ("+xmm+" mm; "+ymm+" mm)");
    }

    /** Change the strict compatibility mode with the old FidoCAD.
        @param strict true if a strict compatibility mode should be active.
    */
    public void changeStrict(boolean strict)
    {
        // Does nothing.
    }

    /** Change the state of the show libs toggle button.
        @param s the state of the button.
    */
    public void setShowLibs(boolean s)
    {
        showLibs.setSelected(s);
    }

    /** Change the state of the show grid toggle button.
        @param s the state of the button.
    */
    public void setShowGrid(boolean s)
    {
        showGrid.setSelected(s);
    }

    /** Change the state of the show grid toggle button.
        @param s the state of the button.
    */
    public void setSnapGrid(boolean s)
    {
        snapGrid.setSelected(s);
    }

    /** Change the infos.
        @param s the string to be shown.
    */
    public void changeInfos(String s)
    {
        // Ensure that we will be able to restore colors back!
        Color c1=UIManager.getColor("Label.background");
        Color c2=UIManager.getColor("Label.foreground");
        if(c1!=null && c2!=null) {
            coords.setOpaque(true);
            coords.setForeground(Color.WHITE);
            coords.setBackground(Color.GREEN.darker().darker());
        }
        coords.setText(s);
    }
}