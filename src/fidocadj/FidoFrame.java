package fidocadj;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowFocusListener;

import javax.swing.*;

import java.io.*;
import java.util.*;
import java.net.*;

import fidocadj.dialogs.controls.DialogUtil;
import fidocadj.dialogs.settings.DialogSettings;
import fidocadj.dialogs.DialogAbout;
import fidocadj.geom.DrawingSize;
import fidocadj.geom.MapCoordinates;
import fidocadj.globals.Globals;
import fidocadj.globals.AccessResources;
import fidocadj.globals.Utf8ResourceBundle;
import fidocadj.globals.LibUtils;
import fidocadj.globals.SettingsManager;
import fidocadj.circuit.HasChangedListener;
import fidocadj.circuit.CircuitPanel;
import fidocadj.circuit.controllers.CopyPasteActions;
import fidocadj.circuit.controllers.AddElements;
import fidocadj.circuit.controllers.ParserActions;
import fidocadj.circuit.controllers.ElementsEdtActions;
import fidocadj.globals.OSValidator;
import fidocadj.toolbars.ToolbarZoom;
import fidocadj.toolbars.ToolbarTools;
import fidocadj.toolbars.ZoomToFitListener;
import fidocadj.toolbars.InsertImageListener;
import fidocadj.macropicker.MacroTree;
import fidocadj.librarymodel.LibraryModel;
import fidocadj.layermodel.LayerModel;
import fidocadj.layers.StandardLayers;
import fidocadj.layers.LayerDesc;
import fidocadj.librarymodel.utils.CircuitPanelUpdater;
import fidocadj.librarymodel.utils.LibraryUndoExecutor;

/** FidoFrame.java

The class describing the main frame in which FidoCadJ runs.

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

    Copyright 2008-2026 by Davide Bucci
    </pre>

    The FidoFrame class describes a frame which is used to trace schematics
    and printed circuit boards.

    @author Davide Bucci
*/

public final class FidoFrame extends JFrame implements
                                            ActionListener,
                                            ZoomToFitListener,
                                            InsertImageListener,
                                            HasChangedListener,
                                            WindowFocusListener
{
    // Interface elements parts of FidoFrame

    // The circuit panel...
    private CircuitPanel circuitPanel;
    // ... which is contained in a scroll pane.
    private JScrollPane scrollPane;
    // ... which at its turn is in a split pane.
    private JSplitPane splitPane;
    // Macro picker component
    MacroTree macroLib;

    // Macro library model
    private LibraryModel libraryModel;

    // Objects which regroup a certain number of actions somewhat related
    // to the FidoFrame object in different domains.
    final private ExportTools exportTools;
    final private PrintTools printTools;
    final private MenuTools menuTools;
    //final private DragDropTools dt;
    final private FileTools fileTools;

    // Libraries properties
    private String libDirectory;

    // Toolbar properties
    // The toolbar dedicated to the available tools (the first one under
    // thewindow title).
    private ToolbarTools toolBar;
    // The second toolbar dedicated to the zoom factors and other niceties
    // (the second one under the window title).
    ToolbarZoom toolZoom;

    // Text description under icons
    private boolean textToolbar;
    // Small (16x16 pixel) icons instead of standard (32x32 pixel)
    private boolean smallIconsToolbar;

    // Locale settings
    private Locale currentLocale;
    // Runs as an application or an applet.
    private boolean runsAsApplication;

    /** The standard constructor: create the frame elements and set up all
        variables. Note that the constructor itself is not sufficient for
        using the frame. You need to call the init procedure after you have
        set the configuration variables available for FidoFrame.

        @param appl should be true if FidoCadJ is run as a stand alone
            application or false if it is run as an applet. In this case, some
            local settings are not accessed because they would raise an
            exception.
        @param loc the locale which should be used. If it is null, the current
            locale is automatically determined and FidoCadJ will try to use
            it for its user interface.
    */
    public FidoFrame (boolean appl, Locale loc)
    {
        super("FidoCadJ "+Globals.version);
        runsAsApplication = appl;

        currentLocale = registerLocale(loc);

        getRootPane().putClientProperty("Aqua.windowStyle", "combinedToolBar");

        prepareLanguageResources();
        Globals.configureInterfaceDetailsFromPlatform(InputEvent.META_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK);

        DialogUtil.center(this, .75,.75,800,500);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // We need to keep track of the number of open windows. If the last
        // one is closed, we exit from the program.

        ++Globals.openWindowsNumber;
        Globals.openWindows.add(this);

        setIconForApplication();

        if (!runsAsApplication) {
            // If we can not access to the preferences, we inizialize those
            // configuration variables with default values.
            libDirectory = System.getProperty("user.home");
            smallIconsToolbar = false;
            textToolbar = true;
        }
        exportTools = new ExportTools();
        printTools = new PrintTools();
        menuTools = new MenuTools();
        new DragDropTools(this);
        fileTools = new FileTools(this);

        readPreferences();
        // In practice, we need to restore the size of the open current window
        // only for the first window.
        if (Globals.openWindowsNumber==1) {
            restorePosition();
        }
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

    /** Store location & size of UI.
        vaguely based on: http://stackoverflow.com/questions/7777640/\
            best-practice-for-setting-jframe-locations
    */
    public void savePosition()
    {
        if (!runsAsApplication) {
            return;
        }

        int state=getExtendedState();
        // restore the frame from 'full screen' first!
        setExtendedState(NORMAL);
        Rectangle r = getBounds();
        int x = (int)r.getX();
        int y = (int)r.getY();
        int w = (int)r.getWidth();
        int h = (int)r.getHeight();

        SettingsManager.put("FRAME_POSITION_X", "" + x);
        SettingsManager.put("FRAME_POSITION_Y", "" + y);
        SettingsManager.put("FRAME_WIDTH", "" + w);
        SettingsManager.put("FRAME_HEIGHT", "" + h);
        SettingsManager.put("FRAME_STATE", ""+state);
    }

    /** Restore location & size of UI
    */
    public void restorePosition()
    {
        if (!runsAsApplication) {
            return;
        }

        try{
            int x = Integer.parseInt(
                    SettingsManager.get("FRAME_POSITION_X","no"));
            int y = Integer.parseInt(
                    SettingsManager.get("FRAME_POSITION_Y","no"));
            int w = Integer.parseInt(
                    SettingsManager.get("FRAME_WIDTH","no"));
            int h = Integer.parseInt(
                    SettingsManager.get("FRAME_HEIGHT","no"));
            int state=Integer.parseInt(
                    SettingsManager.get("FRAME_STATE","no"));

            if((state & MAXIMIZED_HORIZ)!=0 ||
                (state & MAXIMIZED_VERT)!=0)
            {
                setExtendedState(state);
            } else {
                Rectangle r = new Rectangle(x,y,w,h);
                setBounds(r);
            }
        } catch (NumberFormatException eE) {
            System.out.println("Choosing default values for frame size");
        }
    }

    /** Obtain the language resources associated to the current locale.
        If the current locale is available, then load the appropriate
        LanguageResources file. If the current locale is not found, the
        en-US language resources file is employed, thus showing an interface
        in American English.
    */
    private void prepareLanguageResources()
    {
        try {
            // Try to load the messages resources with the current locale
            Globals.messages = new
                AccessResources (Utf8ResourceBundle.getBundle("MessagesBundle",
               currentLocale));
        } catch(MissingResourceException mre) {
            try {
                // If it does not work, try to use the standard English
                Globals.messages = new
                AccessResources (ResourceBundle.getBundle("MessagesBundle",
                    Locale.forLanguageTag("en-US")));
                System.out.println("No locale available, sorry... "+
                    "interface will be in English");
            } catch(MissingResourceException mre1) {
                // Give up!!!
                JOptionPane.showMessageDialog(null,
                    "Unable to find any language localization files: " + mre1);
                System.exit(1);
            }
        }
    }

    /** Retrieve the program icon and associate it to the window.
    */
    private void setIconForApplication()
    {
        URL url=DialogAbout.class.getResource(
            "/icons/icona_fidocadj_128x128.png");

        if (url == null) {
            System.err.println("Could not retrieve the FidoCadJ icon!");
        } else {
            Image icon = Toolkit.getDefaultToolkit().getImage(url);
            setIconImage(icon);
        }
    }

    /** Check if a locale has been specified. If not, get the operating
        system's locale and employ this as the current locale.
        @param loc the desired locale, or null if the system one has to be
            employed.
    */
    private Locale registerLocale(Locale loc)
    {
        String systemLanguage = Locale.getDefault().getLanguage();
        Locale newLocale;

        if(loc==null) {
            // Make sort that only the language is used for the current
            newLocale = Locale.forLanguageTag(systemLanguage);
        } else {

            newLocale = loc;
            if(!loc.getLanguage().equals(systemLanguage)) {
                System.out.println("Forcing the locale to be: " +loc+
                    " instead of: "+systemLanguage);
            }
        }
        return newLocale;
    }

    /** Get the locale employed by this instance.
        @return the current locale.
    */
    @Override public Locale getLocale()
    {
        return currentLocale;
    }

    /** Get the ExportTools object (containing the code related to interface
        for exporting files).
        @return the ExportTools object.
    */
    public ExportTools getExportTools()
    {
        return exportTools;
    }

    /** Get the PrintTools object (containing the code related to interface
        for printing drawings).
        @return the PrintTools object.
    */
    public PrintTools getPrintTools()
    {
        return printTools;
    }

    /** Get the FileTools object (containing the code related to interface
        for loading and saving drawings).
        @return the FileTools object.
    */
    public FileTools getFileTools()
    {
        return fileTools;
    }

    /** Read the preferences settings (mainly at startup or when a new
        editing window is created.
        If no preferences settings are accessible, does nothing.
    */
    public void readPreferences()
    {
        // The library directory
        libDirectory = SettingsManager.getString("DIR_LIBS", "");

        // The icon size
        String defaultSize = "false";
        smallIconsToolbar = SettingsManager.getBoolean("SMALL_ICON_TOOLBAR",
                Boolean.parseBoolean(defaultSize));
        textToolbar = SettingsManager.getBoolean("TEXT_TOOLBAR", true);

        // Read export preferences
        exportTools.readPrefs();
        // Read file preferences
        fileTools.readPrefs();

        // Element sizes
        Globals.lineWidth =
                SettingsManager.getDouble("STROKE_SIZE_STRAIGHT", 0.5);
        Globals.lineWidthCircles =
                SettingsManager.getDouble("STROKE_SIZE_OVAL", 0.35);
        Globals.diameterConnection =
                SettingsManager.getDouble("CONNECTION_SIZE", 2.0);
    }

    /** Load the saved configuration for the grid.
    */
    public void readGridSettings()
    {
        circuitPanel.getMapCoordinates().setXGridStep(
                Integer.parseInt(SettingsManager.get("GRID_SIZE", "5")));

        circuitPanel.getMapCoordinates().setYGridStep(
                Integer.parseInt(SettingsManager.get("GRID_SIZE", "5")));
    }

    /** Load the saved configuration for the drawing primitives and zoom.
    */
    public void readDrawingSettings()
    {
        CopyPasteActions cpa = circuitPanel.getCopyPasteActions();

        // Shift elements when copy/pasting them.
        cpa.setShiftCopyPaste("true".
            equals(SettingsManager.get("SHIFT_CP","true")));
        AddElements ae=circuitPanel.getContinuosMoveActions().getAddElements();

        // Default PCB sizes (pad/line)
        ae.pcbPadSizeX=Integer.parseInt(
                SettingsManager.get("PCB_pad_sizex","10"));
        ae.pcbPadSizeY=Integer.parseInt(
                SettingsManager.get("PCB_pad_sizey","10"));
        ae.pcbPadStyle=Integer.parseInt(
                SettingsManager.get("PCB_pad_style","0"));
        ae.pcbPadDrill=Integer.parseInt(
                SettingsManager.get("PCB_pad_drill","5"));
        ae.pcbThickness=Integer.parseInt(
                SettingsManager.get("PCB_thickness","5"));

        circuitPanel.setBackground(Color.decode(SettingsManager.get(
                "BACKGROUND_COLOR", "#FFFFFF")));

        circuitPanel.setDotsGridColor(Color.decode(SettingsManager.get(
                "GRID_DOTS_COLOR", "#000000")));

        circuitPanel.setLinesGridColor(Color.decode(SettingsManager.get(
                "GRID_LINES_COLOR", "#D3D3D3")));

        circuitPanel.setLeftToRightColor(Color.decode(
                SettingsManager.get("SELECTION_LTR_COLOR", "#008000")));

        circuitPanel.setRightToLeftColor(Color.decode(
                SettingsManager.get("SELECTION_RTL_COLOR", "#0000FF")));

        circuitPanel.setSelectedColor(Color.decode(
                SettingsManager.get("SELECTED_ELEMENTS_COLOR", "#00FF00")));

        MapCoordinates mc=circuitPanel.getMapCoordinates();
        double z=Double.parseDouble(SettingsManager.get("CURRENT_ZOOM","4.0"));
        mc.setMagnitudes(z,z);

        circuitPanel.setEnableKeyForZoom(
                SettingsManager.getBoolean("ZOOM_KEY", true));
    }

    /** Load the standard libraries according to the locale.
    */
    public void loadLibraries()
    {
        // Check if we are using the english libraries. Basically, since the
        // only other language available other than english is italian, I
        // suppose that people are less uncomfortable with the current Internet
        // standard...

        boolean englishLibraries = !currentLocale.getLanguage().equals(
                Locale.forLanguageTag("it").getLanguage());

        // This is useful if this is not the first time that libraries are
        // being loaded.
        circuitPanel.getDrawingModel().resetLibrary();
        ParserActions pa=circuitPanel.getParserActions();

        if(runsAsApplication) {
            FidoMain.readLibrariesProbeDirectory(circuitPanel.getDrawingModel(),
                englishLibraries, libDirectory);
        } else {
            // This code is useful when FidoCadJ is used whithout having access
            // to the user file system, for example because it is run as an
            // applet. In this case, the only accesses will be internal to
            // the jar file in order to respect security restrictions.
            if(englishLibraries) {
                // Read the english version of the libraries
                pa.loadLibraryInJar(FidoFrame.class.getResource(
                    "lib/IHRAM_en.FCL"), "ihram");
                pa.loadLibraryInJar(FidoFrame.class.getResource(
                    "lib/FCDstdlib_en.fcl"), "");
                pa.loadLibraryInJar(FidoFrame.class.getResource(
                    "lib/PCB_en.fcl"), "pcb");
                pa.loadLibraryInJar(FidoFrame.class.getResource(
                    "lib/elettrotecnica_en.fcl"), "elettrotecnica");
                pa.loadLibraryInJar(FidoFrame.class.getResource(
                    "lib/EY_Libraries.fcl"), "EY_Libraries");
            } else {
                // Read the italian version of the libraries
                pa.loadLibraryInJar(FidoFrame.class.getResource(
                    "lib/IHRAM.FCL"), "ihram");
                pa.loadLibraryInJar(FidoFrame.class.getResource(
                    "lib/FCDstdlib.fcl"), "");
                pa.loadLibraryInJar(FidoFrame.class.getResource(
                    "lib/PCB.fcl"), "pcb");
                pa.loadLibraryInJar(FidoFrame.class.getResource(
                    "lib/elettrotecnica.fcl"), "elettrotecnica");
                pa.loadLibraryInJar(FidoFrame.class.getResource(
                    "lib/EY_Libraries.fcl"), "EY_Libraries");
            }
        }
        libraryModel.forceUpdate();
    }

    /** Perform some initialization tasks: in particular, it reads the library
        directory and it creates the user interface.
    */
    public void init()
    {
        Container contentPane=getContentPane();
        circuitPanel=new CircuitPanel(true);
        circuitPanel.getParserActions().openFileName = "";
        // Useful for automatic scrolling in panning mode.
        ScrollGestureRecognizer sgr;

        // If FidoCadJ runs as a standalone application, we must read the
        // content of the current library directory.
        // at the same time, we see if we should maintain a strict FidoCad
        // compatibility.
        if (runsAsApplication)  {
            circuitPanel.getDrawingModel().setTextFont(
                    SettingsManager.getString("MACRO_FONT",
                    Globals.defaultTextFont),
                    SettingsManager.getInt("MACRO_SIZE", 3),
                    circuitPanel.getUndoActions());

            readGridSettings();
            readDrawingSettings();
        }
        circuitPanel.setStrictCompatibility(false);

        // Here we set the approximate size of the control at startup. This is
        // useful, since the scroll panel (created just below) use those
        // settings for specifying the behaviour of scroll bars.

        circuitPanel.setPreferredSize(new Dimension(1000,1000));
        scrollPane= new JScrollPane((Component)circuitPanel);
        circuitPanel.father=scrollPane;

        scrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        if (runsAsApplication) {
            sgr = new ScrollGestureRecognizer();
            circuitPanel.addScrollGestureSelectionListener(sgr);
            sgr.getInstance();
        }

        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);

        circuitPanel.profileTime=false;
        circuitPanel.setAntiAlias(true);

        // Create the layer vector. Basically, this is a rather standard
        // attribution in which only the first layers are attributed to
        // something that is circuit-related.
        // I followed the FidoCAD tradition on this.
        java.util.List<LayerDesc> layerDesc=
            StandardLayers.createStandardLayers();
        circuitPanel.getDrawingModel().setLayers(layerDesc);

        toolBar = new ToolbarTools(textToolbar,smallIconsToolbar);
        toolZoom = new ToolbarZoom(layerDesc, this);

        toolBar.addSelectionListener(circuitPanel);
        toolBar.addInsertImageListener(this);
        toolZoom.addLayerListener(circuitPanel);

        toolZoom.addGridStateListener(circuitPanel);
        toolZoom.addZoomToFitListener(this);

        circuitPanel.addChangeZoomListener(toolZoom);
        circuitPanel.addChangeSelectionListener(toolBar);
        circuitPanel.addChangeCoordinatesListener(toolZoom);

        toolZoom.addChangeZoomListener(circuitPanel);

        Box b=Box.createVerticalBox();

        b.add(toolBar);
        b.add(toolZoom);

        toolZoom.setFloatable(false);
        toolZoom.setRollover(false);

        JMenuBar menuBar=menuTools.defineMenuBar(this);
        setJMenuBar(menuBar);
        menuTools.setCircuitPanel(circuitPanel);

        // The initial state is the selection one.
        circuitPanel.setSelectionState(ElementsEdtActions.SELECTION, "");

        contentPane.add(b,"North");

        libraryModel = new LibraryModel(circuitPanel.getDrawingModel());
        LayerModel layerModel = new LayerModel(circuitPanel.getDrawingModel());
        macroLib = new MacroTree(libraryModel,layerModel);
        macroLib.setSelectionListener(circuitPanel);

        libraryModel.setUndoActorListener(circuitPanel.getUndoActions());
        libraryModel.addLibraryListener(new CircuitPanelUpdater(this));
        circuitPanel.getUndoActions().setLibraryUndoListener(
                                   new LibraryUndoExecutor(this,libraryModel));

        try {
            LibUtils.saveLibraryState(circuitPanel.getUndoActions());
        } catch (IOException e) {
            System.out.println("Exception: "+e);
        }

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Useful for Quaqua with MacOSX.
        //splitPane.putClientProperty("Quaqua.SplitPane.style","bar");
        splitPane.putClientProperty("JSplitPane.style","thick");

        Dimension windowSize = getSize();
        circuitPanel.setPreferredSize(
                new Dimension(windowSize.width*85/100,100));

        splitPane.setTopComponent(scrollPane);
        macroLib.setPreferredSize(new Dimension(450,200));
        splitPane.setBottomComponent(macroLib);
        splitPane.setResizeWeight(.8);

        contentPane.add(splitPane,"Center");

        circuitPanel.getUndoActions().setHasChangedListener(this);

        circuitPanel.setFocusable(true);
        scrollPane.setFocusable(true);

        /*  Add a window listener to close the application when the frame is
            closed. This behavior is platform dependent, for example a
            Macintosh application can be made run without a visible frame.
            There would anyway the need to customize the menu bar, in order
            to allow the user to open a new FidoFrame, when it has been
            closed once. The easiest solution to implement is therefore to
            make the application close when the user closes the last frame.
        */
        addWindowListener(new WindowAdapter()
        {
            @Override public void windowClosing(WindowEvent e)
            {
                if(!fileTools.checkIfToBeSaved()) {
                    return;
                }
                closeThisFrame();
            }
        });

        addWindowFocusListener(this);
        Globals.activeWindow=this;

        // This is WAY too invasive!!!
        circuitPanel.getUndoActions().setModified(false);
        if(runsAsApplication) {
            // Show the library tab or not.
            boolean s="true".equals(SettingsManager.get("SHOW_LIBS","true"));
            showLibs(s);
            s="true".equals(SettingsManager.get("SHOW_GRID","true"));
            circuitPanel.setGridVisibility(s);
            toolZoom.setShowGrid(s);
            s="true".equals(SettingsManager.get("SNAP_GRID","true"));
            circuitPanel.setSnapState(s);
            toolZoom.setSnapGrid(s);
        }
    }

    /** Procedure to close the current frame, check if there are other open
        frames, and exit the program if there are no frames remaining.
        Ensure that the configuration settings are properly saved.
    */
    public void closeThisFrame()
    {
        setVisible(false);
        circuitPanel.getUndoActions().doTheDishes();
        savePosition();
        // Save the zoom factor. There is no reason to save the other
        // coordinate mapping data as this will be used for an empty drawing.
        MapCoordinates mc=circuitPanel.getMapCoordinates();
        SettingsManager.put("CURRENT_ZOOM",""+mc.getXMagnitude());
        if(areLibsVisible()) {
            SettingsManager.put("SHOW_LIBS","true");
        } else {
            SettingsManager.put("SHOW_LIBS","false");
        }

        if(circuitPanel.getGridVisibility()) {
            SettingsManager.put("SHOW_GRID","true");
        } else {
            SettingsManager.put("SHOW_GRID","false");
        }

        if(circuitPanel.getSnapState()) {
            SettingsManager.put("SNAP_GRID","true");
        } else {
            SettingsManager.put("SNAP_GRID","false");
        }

        dispose();
        Globals.openWindows.remove(this);
        --Globals.openWindowsNumber;
        if (Globals.openWindowsNumber<1 && runsAsApplication) {
            System.exit(0);
        }
    }

    /** The action listener. Recognize menu events and behaves consequently.
        @param evt the event to be processed.
    */
    @Override public void actionPerformed(ActionEvent evt)
    {
        // Recognize and handle menu events
        if(evt.getSource() instanceof JMenuItem) {
            menuTools.processMenuActions(evt, this, toolZoom);
        }
    }

    /** Create a new instance of the window.
        @return the created instance
    */
    public FidoFrame createNewInstance()
    {
        FidoFrame popFrame=new FidoFrame(runsAsApplication, currentLocale);
        popFrame.setBounds(getX()+30, getY()+30, popFrame.getWidth(),
            popFrame.getHeight());

        popFrame.init();

        popFrame.loadLibraries();
        popFrame.setExtendedState(getExtendedState());

        popFrame.setVisible(true);

        return popFrame;
    }

    /**
     Show the FidoCadJ preferences panel
     */
    public void showPrefs()
    {
        String oldDirectory = libDirectory;
        CopyPasteActions cpa = circuitPanel.getCopyPasteActions();
        ElementsEdtActions eea = circuitPanel.getContinuosMoveActions();
        AddElements ae = eea.getAddElements();

        // Create and display the settings dialog window
        DialogSettings options = new DialogSettings(this);
        options.showDialog();

        // Update properties based on the new settings
        circuitPanel.setBackground(Color.decode(SettingsManager.get(
                "BACKGROUND_COLOR", "#FFFFFF")));

        circuitPanel.setDotsGridColor(Color.decode(SettingsManager.get(
                "GRID_DOTS_COLOR", "#000000")));

        circuitPanel.setLinesGridColor(Color.decode(SettingsManager.get(
                "GRID_LINES_COLOR", "#D3D3D3")));

        circuitPanel.setLeftToRightColor(Color.decode(
                SettingsManager.get("SELECTION_LTR_COLOR", "#008000")));

        circuitPanel.setRightToLeftColor(Color.decode(
                SettingsManager.get("SELECTION_RTL_COLOR", "#0000FF")));

        circuitPanel.setSelectedColor(Color.decode(
                SettingsManager.get("SELECTED_ELEMENTS_COLOR", "#00FF00")));

        circuitPanel.profileTime = SettingsManager.getBoolean("PROFILE_TIME",
                circuitPanel.profileTime);
        circuitPanel.setAntiAlias(SettingsManager.getBoolean("ANTIALIAS",
                circuitPanel.getAntiAlias()));
        textToolbar = SettingsManager.getBoolean("TEXT_TOOLBAR", textToolbar);
        smallIconsToolbar = SettingsManager.getBoolean("SMALL_ICON_TOOLBAR",
                smallIconsToolbar);

        circuitPanel.getMapCoordinates().setMagnitudes(
                SettingsManager.getDouble("ZOOM_VALUE",
                        circuitPanel.getMapCoordinates().getXMagnitude()),
                SettingsManager.getDouble("ZOOM_VALUE",
                        circuitPanel.getMapCoordinates().getYMagnitude()));
        circuitPanel.getMapCoordinates().setXGridStep(SettingsManager.getInt(
                "GRID_SIZE", circuitPanel.getMapCoordinates().getXGridStep()));
        circuitPanel.getMapCoordinates().setYGridStep(SettingsManager.getInt(
                "GRID_SIZE", circuitPanel.getMapCoordinates().getYGridStep()));

        ae.setPcbThickness((float)SettingsManager.getDouble("PCB_LINEWIDTH",
                ae.getPcbThickness()));
        ae.setPcbPadSizeX((float)SettingsManager.getDouble("PCB_PAD_WIDTH",
                ae.getPcbPadSizeX()));
        ae.setPcbPadSizeY((float)SettingsManager.getDouble("PCB_PAD_HEIGHT",
                ae.getPcbPadSizeY()));
        ae.setPcbPadDrill((float)SettingsManager.getDouble("PCB_PAD_DRILL",
                ae.getPcbPadDrill()));

        circuitPanel.getDrawingModel().setTextFont(SettingsManager.getString(
                "MACRO_FONT", circuitPanel.getDrawingModel().getTextFont()),
                (float)SettingsManager.getDouble("MACRO_SIZE",
                        circuitPanel.getDrawingModel().getTextFontSize()),
                circuitPanel.getUndoActions());

        circuitPanel.setStrictCompatibility(SettingsManager.getBoolean(
                "STRICT_COMPATIBILITY", circuitPanel.getStrictCompatibility()));
        toolBar.setStrictCompatibility(circuitPanel.getStrictCompatibility());
        cpa.setShiftCopyPaste(SettingsManager.getBoolean("SHIFT_CP",
                cpa.getShiftCopyPaste()));

        libDirectory = SettingsManager.getString("DIR_LIBS", libDirectory);

        Globals.lineWidth = SettingsManager.getDouble(
                    "STROKE_SIZE_STRAIGHT", Globals.lineWidth);
        Globals.lineWidthCircles = SettingsManager.getDouble(
                    "STROKE_SIZE_OVAL", Globals.lineWidthCircles);
        Globals.diameterConnection = SettingsManager.getDouble(
                    "CONNECTION_SIZE", Globals.diameterConnection);

        circuitPanel.setEnableKeyForZoom(
                SettingsManager.getBoolean("ZOOM_KEY", true));

        if (!libDirectory.equals(oldDirectory)) {
            loadLibraries();
            setVisible(true);
        }
        repaint();
    }

    /** Called when the user clicks the "insert image" toolbar button.
        Reuses the same file-chooser-then-place logic as the "Insert
        image" menu item.
    */
    public void insertImage()
    {
        menuTools.handleInsertImage(this);
    }

    /** Set the current zoom to fit
    */
    public void zoomToFit()
    {
        // If the drawing is empty, there's no need to go further.
        if(circuitPanel.getDrawingModel().isEmpty()) {
            return;
        }
        //double oldz=cc.getMapCoordinates().getXMagnitude();

        // Check for ghost primitives (hidden outside the drawing area)
        if (circuitPanel.checkGhostPrimitives()) {
            int response = JOptionPane.showConfirmDialog(this,
                    Globals.messages.getString("GhostPrimitivesFound") + "\n"
                    + Globals.messages.getString(
                            "GhostPrimitivesTranslatePrompt"),
                    Globals.messages.getString("GhostPrimitivesTitle"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (response == JOptionPane.YES_OPTION) {
                // Normalize the coordinates if user clicks 'Yes'
                circuitPanel.normalizeCoordinates();
            }
        }

        // We calculate the zoom to fit factor here.
        MapCoordinates m=DrawingSize.calculateZoomToFit(
                circuitPanel.getDrawingModel(),
                scrollPane.getViewport().getExtentSize().width-35,
                scrollPane.getViewport().getExtentSize().height-35,
                true);

        double z=m.getXMagnitude();

        // We apply the zoom factor to the coordinate transform
        circuitPanel.getMapCoordinates().setMagnitudes(z, z);

        // We make the scroll pane show the interesting part of
        // the drawing.
        Rectangle r= new Rectangle((int)m.getXCenter(), (int)m.getYCenter(),
            scrollPane.getViewport().getExtentSize().width,
            scrollPane.getViewport().getExtentSize().height);

        circuitPanel.updateSizeOfScrollBars(r);
    }

    /** We notify the user that something has changed by putting an asterisk
        in the file name.
        We also show here in the titlebar the (eventually shortened) file name
        of the drawing being modified or shown.
    */
    public void somethingHasChanged()
    {
        if (OSValidator.isMac()) {

            // Apparently, this does not work as expected in MacOSX 10.4 Tiger.
            // Those are MacOSX >= 10.5 Leopard features.
            // We thus leave also the classic Window-based asterisk when
            // the file has been modified.

            getRootPane().putClientProperty("Window.documentModified",
                Boolean.valueOf(circuitPanel.getUndoActions().getModified()));

            toolBar.setTitle(Globals.prettifyPath(
                    circuitPanel.getParserActions().openFileName,45));
        } else {
            setTitle("FidoCadJ "+Globals.version+" "+
                Globals.prettifyPath(
                        circuitPanel.getParserActions().openFileName,45)+
                        (circuitPanel.getUndoActions().getModified()?" *":""));

        }
    }

    /** The current window has gained focus.
        @param e the window event.
    */
    @Override public void windowGainedFocus(WindowEvent e)
    {
        Globals.activeWindow = this;
        // This should fix #182
        circuitPanel.requestFocusInWindow();
    }

    /** The current window has lost focus.
        @param e the window event.
    */
    @Override public void windowLostFocus(WindowEvent e)
    {
        // Nothing to do
    }

    /** Control the presence of the libraries and the preview on the right
        of the frame.
        @param s true if the libs have to be shown.
    */
    public void showLibs(boolean s)
    {
        splitPane.setBottomComponent(s?macroLib:null);
        toolZoom.setShowLibsState(areLibsVisible());
        menuTools.setShowLibsState(areLibsVisible());
        if(s) {
            splitPane.setDividerLocation(0.75);
        }
        splitPane.revalidate();
    }

    /** Determine if the libraries are visible or not.
        @return true if the libs are visible.
    */
    public boolean areLibsVisible()
    {
        return splitPane.getBottomComponent()!=null;
    }

    /**
     Get the current CircuitPanel object.

     @return the current CircuitPanel object.
     */
    public CircuitPanel getCircuitPanel()
    {
        return circuitPanel;
    }

    /**
     Get the current library directory path.

     @return the current library directory path.
     */
    public String getLibDirectory()
    {
        return libDirectory;
    }

    /**
     Set the library directory path.

     @param libDirectory the directory path to set.
     */
    public void setLibDirectory(String libDirectory)
    {
        this.libDirectory = libDirectory;
    }

    /**
     Set the current locale settings.

     @param currentLocale the Locale object to set.
     */
    public void setCurrentLocale(Locale currentLocale)
    {
        this.currentLocale = currentLocale;
    }

    /**
     Get the current locale settings.

     @return the current Locale object.
     */
    public Locale getCurrentLocale()
    {
        return currentLocale;
    }

    /**
     Set whether the program runs as an application.

     @param runsAsApplication true if it runs as an application,
                              false if it runs as an applet.
     */
    public void setRunsAsApplication(boolean runsAsApplication)
    {
        this.runsAsApplication = runsAsApplication;
    }

    /**
     Check if the program runs as an application.

     @return true if it runs as an application, false if it runs as an applet.
     */
    public boolean getRunsAsApplication()
    {
        return runsAsApplication;
    }
}
