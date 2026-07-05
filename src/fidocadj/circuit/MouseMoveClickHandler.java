package fidocadj.circuit;

import java.awt.*;
import java.awt.event.*;

import fidocadj.globals.Globals;
import fidocadj.circuit.controllers.ElementsEdtActions;
import fidocadj.circuit.controllers.ContinuosMoveActions;
import fidocadj.circuit.controllers.EditorActions;
import fidocadj.circuit.controllers.HandleActions;
import fidocadj.timer.MyTimer;
import fidocadj.geom.MapCoordinates;
import fidocadj.globals.OSValidator;


/** Handle the mouse click operations, as well as some dragging actions.
    <pre>
    cp file is part of FidoCadJ.

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
public class MouseMoveClickHandler implements MouseMotionListener,
                                             MouseListener
{
    private final CircuitPanel circuitPanel;
    private final EditorActions editorActions;
    private final ContinuosMoveActions continuosMoveActions;
    private final HandleActions handleActions;

    // Record time for mouse down handle event in selection.
    private double record_c;

    // Record time for click up event in selection.
    private double record_d;

    /** Constructor. Create a MouseMoveClickHandler and associates it to the
        provided handler.
        @param p the CircuitPanel.
    */
    public MouseMoveClickHandler(CircuitPanel p)
    {
        circuitPanel=p;
        editorActions=circuitPanel.getEditorActions();
        continuosMoveActions=circuitPanel.getContinuosMoveActions();
        handleActions=circuitPanel.getHandleActions();
        record_c=record_d=1e100;
    }

    /** Called when the mouse is clicked inside the control
        0.23.2: the Java click event is a bit too much restrictive. The mouse
        need to be hold still during the click. This is apparently a problem for
        a number of user. I have thus decided to use the mouse release event
        instead of the complete click.
        @param evt the MouseEvent to handle.
    */
    @Override public void mouseClicked(MouseEvent evt)
    {
        circuitPanel.requestFocusInWindow();
    }

    /** Handle the mouse movements when editing a graphic primitive.
        This procedure is important since it is used to show interactively
        to the user which element is being modified.
        @param evt the MouseEvent to handle.
    */
    @Override public void mouseMoved(MouseEvent evt)
    {
        int xa=evt.getX();
        int ya=evt.getY();

        boolean toggle = getToggle(evt);

        // Handle Move command mode - update positions with center anchoring
        if (continuosMoveActions.isMovingSelected()) {
            continuosMoveActions.updateMovePositions(
                    circuitPanel.getMapCoordinates().unmapXsnap(xa),
                    circuitPanel.getMapCoordinates().unmapYsnap(ya));
            circuitPanel.repaint();
            return;
        }

        // Normal continuous move (for drawing new primitives)
        if (continuosMoveActions.continuosMove(
                circuitPanel.getMapCoordinates(), xa, ya, toggle))
        {
            circuitPanel.repaint();
        }
    }

    /** Check if the "toggle" keyboard button is pressed during the mouse
        operation. Toggle may be Control or Meta, depending on the operating
        system.
        @param evt the MouseEvent.
        @return true if the toggle should be active.
    */
    private boolean getToggle(MouseEvent evt)
    {
        if(Globals.useMetaForMultipleSelection) {
            return evt.isMetaDown();
        } else {
            return evt.isControlDown();
        }
    }

    /** Mouse interface: start of the dragging operations.
        @param evt the MouseEvent to handle
    */
    @Override public void mousePressed(MouseEvent evt)
    {
        MyTimer mt = new MyTimer();

        int px=evt.getX();
        int py=evt.getY();

        // Handle Move command mode - just consume the event
        if (continuosMoveActions.isMovingSelected()) {
            return;
        }

        circuitPanel.getRuler().setActive(false);
        circuitPanel.getRuler().setRulerStart(px, py);
        circuitPanel.getRuler().setRulerEnd(px, py);
        boolean toggle = getToggle(evt);

        if(continuosMoveActions.actionSelected ==
                ElementsEdtActions.SELECTION &&
                (evt.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK)==0 &&
                !evt.isShiftDown())
        {
            handleActions.dragHandleStart(px, py,
                    editorActions.getSelectionTolerance(),
                    toggle, circuitPanel.getMapCoordinates());
        } else if(continuosMoveActions.actionSelected ==
                    ElementsEdtActions.SELECTION &&
                    circuitPanel.isRulerEnabled())
        {
            // Right click during selection
            circuitPanel.getRuler().setActive(true);
        }

        if(circuitPanel.isProfiling()) {
            double elapsed=mt.getElapsed();
            if(elapsed<record_c) {
                record_c=elapsed;
            }
            System.out.println("MP: Time elapsed: "+elapsed+
                "; record: "+record_c+" ms");
        }
    }

    /** Dragging event with the mouse.
        @param evt the MouseEvent to handle
    */
    @Override public void mouseDragged(MouseEvent evt)
    {
        MyTimer mt = new MyTimer();
        int px=evt.getX();
        int py=evt.getY();

        // Handle Move command mode - update positions during drag
        if (continuosMoveActions.isMovingSelected()) {
            continuosMoveActions.updateMovePositions(
                    circuitPanel.getMapCoordinates().unmapXsnap(px),
                    circuitPanel.getMapCoordinates().unmapYsnap(py));
            circuitPanel.repaint();
            return;
        }

        // Handle the ruler. Basically, we just save the coordinates and
        // we launch a repaint which will be done as soon as possible.
        // No graphical elements are drawn outside a repaint.
        if(circuitPanel.isRulerEnabled() &&
            ((evt.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK)!=0 ||
            evt.isShiftDown()))
        {
            circuitPanel.getRuler().setRulerEnd(px, py);
            circuitPanel.repaint();
            return;
        }

        if (continuosMoveActions.actionSelected == ElementsEdtActions.HAND) {
            return;
        }

        handleActions.dragHandleDrag(
                circuitPanel, px, py, circuitPanel.getMapCoordinates(),
                (evt.getModifiersEx() & ActionEvent.CTRL_MASK)==
                ActionEvent.CTRL_MASK);
        // A little profiling if necessary. I noticed that time needed for
        // handling clicks is not negligible in large drawings, hence the
        // need of controlling it.
        if(circuitPanel.isProfiling()) {
            double elapsed=mt.getElapsed();
            if(elapsed<record_d) {
                record_d=elapsed;
            }
            System.out.println("MD: Time elapsed: "+elapsed+
                "; record: "+record_d+" ms");
        }
    }

    /** Mouse release event.
        @param evt the MouseEvent to handle
    */
    @Override public void mouseReleased(MouseEvent evt)
    {
        MyTimer mt = new MyTimer();
        int px=evt.getX();
        int py=evt.getY();
        MapCoordinates cs=circuitPanel.getMapCoordinates();

        // Handle Move command mode - confirm move on release
        if (continuosMoveActions.isMovingSelected()) {
            if (evt.getButton() == MouseEvent.BUTTON1) {
                continuosMoveActions.confirmMove();
                circuitPanel.repaint();
            }
            return;
        }

        boolean button3 = false;
        boolean toggle = getToggle(evt);

        // Key bindings are a little different with macOS.
        if(OSValidator.isMac()) {
            if(evt.getButton()==MouseEvent.BUTTON3) {
                button3=true;
            } else if(evt.getButton()==MouseEvent.BUTTON1 &&
                evt.isControlDown())
            {
                button3=true;
            }
        } else if (evt.getButton() == MouseEvent.BUTTON3) {
            button3 = true;
        }

        // If we are in the selection state, either we are ending the editing
        // of an element (and thus the dragging of a handle) or we are
        // making a click.
        if(continuosMoveActions.actionSelected==ElementsEdtActions.SELECTION) {
            if(circuitPanel.getRuler().getRulerStartX()!=px ||
                circuitPanel.getRuler().getRulerStartY()!=py) // NOPMD
            {
                handleActions.dragHandleEnd(circuitPanel, px, py, toggle, cs);
            } else {
                circuitPanel.getRuler().setActive(false);
                circuitPanel.requestFocusInWindow();

                continuosMoveActions.handleClick(cs,evt.getX(), evt.getY(),
                    button3, toggle, evt.getClickCount() >= 2);
            }
            circuitPanel.repaint();
        } else {
            circuitPanel.requestFocusInWindow();
            if(continuosMoveActions.handleClick(cs,evt.getX(), evt.getY(),
                button3, toggle, evt.getClickCount() >= 2))
            {
                circuitPanel.repaint();
            }
        }

        // Having an idea of the release time is useful for the optimization
        // of the click event handling. The most time-consuming operation
        // which is done in this phase is finding the closest component to
        // the mouse pointer and eventually selecting it.

        if(circuitPanel.isProfiling()) {
            double elapsed=mt.getElapsed();
            if(elapsed<record_d) {
                record_d=elapsed;
            }
            System.out.println("MR: Time elapsed: "+elapsed+
                "; record: "+record_d+" ms");
        }
    }

    /** The mouse pointer enters into the control. This method changes the
        cursor associated to it.
        @param evt the MouseEvent to handle
    */
    @Override public void mouseEntered(MouseEvent evt)
    {
        selectCursor();
    }

    /**
        Define the icon used for the mouse cursor, depending on the current
        editing action.
    */
    public void selectCursor()
    {
        switch(continuosMoveActions.actionSelected) {
            case ElementsEdtActions.NONE:
            case ElementsEdtActions.ZOOM:
            case ElementsEdtActions.HAND:
                circuitPanel.setCursor(
                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                break;
            case ElementsEdtActions.LINE:
            case ElementsEdtActions.TEXT:
            case ElementsEdtActions.BEZIER:
            case ElementsEdtActions.POLYGON:
            case ElementsEdtActions.COMPLEXCURVE:
            case ElementsEdtActions.ELLIPSE:
            case ElementsEdtActions.RECTANGLE:
            case ElementsEdtActions.CONNECTION:
            case ElementsEdtActions.PCB_LINE:
            case ElementsEdtActions.PCB_PAD:
                circuitPanel.setCursor(Cursor.getPredefinedCursor(
                        Cursor.CROSSHAIR_CURSOR));
                break;
            case ElementsEdtActions.SELECTION:
            default:
                circuitPanel.setCursor(Cursor.getPredefinedCursor(
                        Cursor.DEFAULT_CURSOR));
                break;
        }
    }

    /** The mouse pointer has exited the control. This method changes the
        cursor associated and restores the default one.
        @param evt the MouseEvent to handle
    */
    @Override public void mouseExited(MouseEvent evt)
    {
        circuitPanel.setCursor(
                Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        if(continuosMoveActions.successiveMove) {
            continuosMoveActions.successiveMove = false;
            circuitPanel.repaint();
        }
    }
}
