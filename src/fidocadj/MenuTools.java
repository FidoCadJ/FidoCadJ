package fidocadj;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.Base64;

import fidocadj.globals.Globals;
import fidocadj.circuit.controllers.SelectionActions;
import fidocadj.circuit.controllers.ElementsEdtActions;
import fidocadj.circuit.controllers.ParserActions;
import fidocadj.circuit.controllers.CopyPasteActions;
import fidocadj.circuit.controllers.EditorActions;
import fidocadj.circuit.ImageAsCanvas;
import fidocadj.circuit.CircuitPanel;
import fidocadj.circuit.model.DrawingModel;
import fidocadj.dialogs.DialogAttachImage;
import fidocadj.dialogs.DialogAbout;
import fidocadj.dialogs.DialogLayer;
import fidocadj.dialogs.DialogCircuitCode;
import fidocadj.clipboard.TextTransfer;
import fidocadj.geom.ChangeCoordinatesListener;
import fidocadj.geom.MapCoordinates;
import fidocadj.primitives.PrimitiveImage;

/**
 * MenuTools - Main menu creation and handling class for FidoCadJ.
 *
 * <p>This class is responsible for creating and managing all menus in the
 * FidoCadJ application. It provides methods to create menus, handle menu
 * events, and dynamically update menu item states based on application state.
 * </p>
 * <pre>
 * This file is part of FidoCadJ.
 *
 * FidoCadJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FidoCadJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FidoCadJ. If not,
 * @see <a href=http://www.gnu.org/licenses/>http://www.gnu.org/licenses/</a>.
 *
 * Copyright 2015-2025 by Davide Bucci, Manuel Finessi
 * </pre>
 *
 * @author Davide Bucci
 */
public class MenuTools implements MenuListener
{
    // Constants
    private static final String MENU_ICONS_PATH = "/icons/menu_icons/";

    // View menu items
    private JCheckBoxMenuItem libs = new JCheckBoxMenuItem();

    // Menu references
    private JMenu editMenu;

    // Circuit panel reference for dynamic updates
    private CircuitPanel circuitPanel;

    // Menu items holder for organized access
    private final MenuItemsHolder menuItems = new MenuItemsHolder();

    // Action handlers map for action dispatching
    private final Map<String, MenuActionHandler>
            actionHandlers = new HashMap<>();

    private static class MenuItemsHolder
    {
        // Edit menu items
        JMenuItem editUndo;
        JMenuItem editRedo;
        JMenuItem editCut;
        JMenuItem editCopy;
        JMenuItem editCopySplit;
        JMenuItem editCopyImage;
        JMenuItem editPaste;
        JMenuItem editDuplicate;
        JMenuItem editMove;
        JMenuItem editRotate;
        JMenuItem editMirror;

        // Alignment menu items
        JMenuItem alignLeftSelected;
        JMenuItem alignRightSelected;
        JMenuItem alignTopSelected;
        JMenuItem alignBottomSelected;
        JMenuItem alignHorizontalCenterSelected;
        JMenuItem alignVerticalCenterSelected;

        // Distribution menu items
        JMenuItem distributeHorizontallySelected;
        JMenuItem distributeVerticallySelected;

        /**
         * Get all selection-dependent menu items as an array.
         * These items should be enabled only when something is selected.
         *
         * @return Array of menu items that depend on selection state
         */
        JMenuItem[] getSelectionDependentItems()
        {
            return new JMenuItem[] {
                editCut, editCopy, editCopySplit, editCopyImage,
                editDuplicate, editMove, editRotate, editMirror,
                alignLeftSelected, alignRightSelected, alignTopSelected,
                alignBottomSelected, alignHorizontalCenterSelected,
                alignVerticalCenterSelected, distributeHorizontallySelected,
                distributeVerticallySelected
            };
        }
    }

    /**
     * Functional interface for menu action handlers.
     * Allows lambda expressions to be used for handling menu actions.
     */
    @FunctionalInterface
    private interface MenuActionHandler
    {
        /**
         * Handle a menu action.
         *
         * @param fidoFrame The main application frame
         * @param coordL The coordinate listener for status messages
         */
        void handle(FidoFrame fidoFrame, ChangeCoordinatesListener coordL);
    }

    /**
     * Constructor - initializes action handlers.
     */
    public MenuTools()
    {
        initializeActionHandlers();
    }

    /**
     * Create all the menus and associate to them all the needed listeners.
     *
     * @param al the action listener to associate to the menu elements
     * @return the complete menu bar
     */
    public JMenuBar defineMenuBar(ActionListener al)
    {
        JMenuBar menuBar = new JMenuBar();

        menuBar.add(createFileMenu(al));
        menuBar.add(createEditMenu(al));
        menuBar.add(createViewMenu(al));
        menuBar.add(createCircuitMenu(al));

        // On a MacOSX system, this menu is associated to preferences menu
        // in the application menu. We do not need to show it in bar.
        // This needs the AppleSpecific extensions to be active.
        JMenu about = createAboutMenu(al);
        if (!Globals.desktopInt.getHandleAbout()) {
            menuBar.add(about);
        }

        return menuBar;
    }

    /**
     * Set the circuit panel reference for dynamic menu updates.
     * This reference is used to query selection state and
     * undo/redo availability.
     *
     * @param cp the CircuitPanel instance
     */
    public void setCircuitPanel(CircuitPanel cp)
    {
        this.circuitPanel = cp;
    }

    /**
     * MenuListener implementation - called when a menu is selected.
     * Updates menu items state based on current selection and undo/redo status.
     *
     * @param evt the menu event object
     */
    @Override
    public void menuSelected(MenuEvent evt)
    {
        // Update Edit menu items when the menu is opened
        if (evt.getSource() == editMenu) {
            updateEditMenuState();
        }
    }

    /**
     * MenuListener implementation - called when a menu is deselected.
     *
     * @param evt the menu event object
     */
    @Override
    public void menuDeselected(MenuEvent evt)
    {
        // Does nothing
    }

    /**
     * MenuListener implementation - called when a menu is canceled.
     *
     * @param evt the menu event object
     */
    @Override
    public void menuCanceled(MenuEvent evt)
    {
        // Does nothing
    }

    /**
     * Update the state of Edit menu items based on current selection
     * and undo/redo availability. This method is called when the Edit menu
     * is opened to ensure all items reflect the current application state.
     */
    private void updateEditMenuState()
    {
        if (circuitPanel == null) {
            // If no circuit panel, disable all editing operations
            disableAllEditItems();
            return;
        }

        try {
            SelectionActions selectionActions =
                    circuitPanel.getSelectionActions();

            boolean somethingSelected = false;

            if (selectionActions != null) {
                somethingSelected =
                        selectionActions.getFirstSelectedPrimitive() != null;
            }

            // Enable/disable selection-dependent items
            updateSelectionDependentItems(somethingSelected);

            // Check clipboard for paste operation
            updatePasteItemState();

            // Check undo/redo availability
            updateUndoRedoItemsState();

        } catch (Exception e) {
            e.printStackTrace();
            // If any error occurs, disable all items for safety
            disableAllEditItems();
        }
    }

    /**
     * Update the enabled state of all selection-dependent menu items.
     *
     * @param enabled true if items should be enabled, false otherwise
     */
    private void updateSelectionDependentItems(boolean enabled)
    {
        for (JMenuItem item : menuItems.getSelectionDependentItems()) {
            if (item != null) {
                item.setEnabled(enabled);
            }
        }
    }

    /**
     * Update the paste menu item state based on clipboard content.
     * Checks if there is valid content in the clipboard that can be pasted.
     */
    private void updatePasteItemState()
    {
        try {
            TextTransfer textTransfer = new TextTransfer();
            String clipboardContent = textTransfer.getClipboardContents();
            boolean pasteEnabled = clipboardContent != null &&
                               !"".equals(clipboardContent);
            menuItems.editPaste.setEnabled(pasteEnabled);
        } catch (Exception e) {
            menuItems.editPaste.setEnabled(false);
        }
    }

    /**
     * Update undo and redo menu items state based on availability.
     * Queries the undo actions controller to determine if undo/redo
     * are possible.
     */
    private void updateUndoRedoItemsState()
    {
        if (circuitPanel.getUndoActions() != null) {
            try {
                boolean undoEnabled = circuitPanel.getUndoActions().canUndo();
                boolean redoEnabled = circuitPanel.getUndoActions().canRedo();
                menuItems.editUndo.setEnabled(undoEnabled);
                menuItems.editRedo.setEnabled(redoEnabled);
            } catch (Exception e) {
                e.printStackTrace();
                menuItems.editUndo.setEnabled(false);
                menuItems.editRedo.setEnabled(false);
            }
        } else {
            menuItems.editUndo.setEnabled(false);
            menuItems.editRedo.setEnabled(false);
        }
    }

    /**
     * Disable all edit menu items (safety fallback).
     * This method is called when an error occurs or when there's no circuit
     * panel.
     */
    private void disableAllEditItems()
    {
        if (menuItems.editUndo != null) menuItems.editUndo.setEnabled(false);
        if (menuItems.editRedo != null) menuItems.editRedo.setEnabled(false);
        if (menuItems.editPaste != null) menuItems.editPaste.setEnabled(false);

        // Disable all selection-dependent items
        updateSelectionDependentItems(false);
    }

    /**
     * Create the main File menu with all file operations.
     *
     * @param al the action listener to associate to the menu items
     * @return the File menu
     */
    private JMenu createFileMenu(ActionListener al)
    {
        JMenu fileMenu = new JMenu(Globals.messages.getString("File"));

        // Create menu items using builder pattern for consistency
        JMenuItem fileNew = createMenuItem("New", "new.png")
            .withShortcut(KeyEvent.VK_N, Globals.shortcutKey)
            .build();

        JMenuItem fileOpen = createMenuItem("Open", "open.png")
            .withShortcut(KeyEvent.VK_O, Globals.shortcutKey)
            .build();

        JMenuItem fileSave = createMenuItem("Save", "save.png")
            .withShortcut(KeyEvent.VK_S, Globals.shortcutKey)
            .build();

        JMenuItem fileSaveName = createMenuItem("SaveName", "save_name.png")
            .withShortcut(KeyEvent.VK_S,
                Globals.shortcutKey | InputEvent.SHIFT_DOWN_MASK)
            .build();

        JMenuItem fileSaveNameSplit = createMenuItem("Save_split",
            "save_split.png").build();

        JMenuItem fileExport = createMenuItem("Export", "export.png")
            .withShortcut(KeyEvent.VK_E, Globals.shortcutKey)
            .build();

        JMenuItem filePrint = createMenuItem("Print", "print.png")
            .withShortcut(KeyEvent.VK_P, Globals.shortcutKey)
            .build();

        JMenuItem fileClose = createMenuItem("Close", "close.png")
            .withShortcut(KeyEvent.VK_W, Globals.shortcutKey)
            .build();

        JMenuItem options = createMenuItem("Circ_opt", "options.png").build();

        // Add action listeners
        fileNew.addActionListener(al);
        fileOpen.addActionListener(al);
        fileSave.addActionListener(al);
        fileSaveName.addActionListener(al);
        fileSaveNameSplit.addActionListener(al);
        fileExport.addActionListener(al);
        filePrint.addActionListener(al);
        fileClose.addActionListener(al);
        options.addActionListener(al);

        // Add items to menu
        fileMenu.add(fileNew);
        fileMenu.add(fileOpen);
        fileMenu.add(fileSave);
        fileMenu.add(fileSaveName);
        fileMenu.addSeparator();
        fileMenu.add(fileSaveNameSplit);
        fileMenu.addSeparator();
        fileMenu.add(fileExport);
        fileMenu.add(filePrint);
        fileMenu.addSeparator();

        // On a MacOSX system, options is associated to preferences menu
        // in the application menu. We do not need to show it in File.
        // This needs the AppleSpecific extensions to be active.
        if (!Globals.desktopInt.getHandlePreferences()) {
            fileMenu.add(options);
            fileMenu.addSeparator();
        }

        fileMenu.add(fileClose);

        return fileMenu;
    }

    /**
     * Create the Edit main menu with all editing operations.
     * This includes undo/redo, cut/copy/paste, transformations,
     * and alignment tools.
     *
     * @param al the action listener to associate to the menu items
     * @return the Edit menu
     */
    private JMenu createEditMenu(ActionListener al)
    {
        editMenu = new JMenu(Globals.messages.getString("Edit_menu"));

        // Add popup menu listener to update menu state when opened
        editMenu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
                updateEditMenuState();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
                // Does nothing
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e)
            {
                // Does nothing
            }
        });

        // Create undo/redo items
        menuItems.editUndo = createMenuItem("Undo", "undo.png")
            .withShortcut(KeyEvent.VK_Z, Globals.shortcutKey)
            .build();

        menuItems.editRedo = createMenuItem("Redo", "redo.png")
            .withShortcut(KeyEvent.VK_Z,
                Globals.shortcutKey | InputEvent.SHIFT_DOWN_MASK)
            .build();

        // Create clipboard operation items
        menuItems.editCut = createMenuItem("Cut", "cut.png")
            .withShortcut(KeyEvent.VK_X, Globals.shortcutKey)
            .build();

        menuItems.editCopy = createMenuItem("Copy", "copy.png")
            .withShortcut(KeyEvent.VK_C, Globals.shortcutKey)
            .build();

        menuItems.editCopySplit =
                createMenuItem("Copy_split", "copy_split.png")
            .withShortcut(KeyEvent.VK_M, Globals.shortcutKey)
            .build();

        menuItems.editCopyImage =
                createMenuItem("Copy_as_image", "copy_image.png")
            .withShortcut(KeyEvent.VK_I, Globals.shortcutKey)
            .build();

        menuItems.editPaste = createMenuItem("Paste", "paste.png")
            .withShortcut(KeyEvent.VK_V, Globals.shortcutKey)
            .build();

        JMenuItem clipboardCircuit = createMenuItem("DefineClipboard",
            "paste_new.png").build();

        JMenuItem editSelectAll = createMenuItem("SelectAll", "select_all.png")
            .withShortcut(KeyEvent.VK_A, Globals.shortcutKey)
            .build();

        menuItems.editDuplicate = createMenuItem("Duplicate", "duplicate.png")
            .withShortcut(KeyEvent.VK_D, Globals.shortcutKey)
            .build();

        // Create transformation items
        menuItems.editMove = createMenuItem("Move", "move.png")
            .withShortcut("M")
            .build();

        menuItems.editRotate = createMenuItem("Rotate", "rotate.png")
            .withShortcut("R")
            .build();

        menuItems.editMirror = createMenuItem("Mirror_E", "mirror.png")
            .withShortcut("S")
            .build();

        // Create alignment items
        createAlignmentMenuItems();

        // Create distribution items
        createDistributionMenuItems();

        // Add action listeners
        menuItems.editUndo.addActionListener(al);
        menuItems.editRedo.addActionListener(al);
        menuItems.editCut.addActionListener(al);
        menuItems.editCopy.addActionListener(al);
        menuItems.editCopySplit.addActionListener(al);
        menuItems.editCopyImage.addActionListener(al);
        menuItems.editPaste.addActionListener(al);
        editSelectAll.addActionListener(al);
        menuItems.editDuplicate.addActionListener(al);
        menuItems.editMove.addActionListener(al);
        menuItems.editRotate.addActionListener(al);
        menuItems.editMirror.addActionListener(al);
        clipboardCircuit.addActionListener(al);

        // Add action listeners for alignment and distribution items
        addAlignmentActionListeners(al);
        addDistributionActionListeners(al);

        // Build menu structure
        editMenu.add(menuItems.editUndo);
        editMenu.add(menuItems.editRedo);
        editMenu.addSeparator();
        editMenu.add(menuItems.editCut);
        editMenu.add(menuItems.editCopy);
        editMenu.add(menuItems.editCopySplit);
        editMenu.add(menuItems.editCopyImage);
        editMenu.add(menuItems.editPaste);
        editMenu.add(clipboardCircuit);
        editMenu.add(menuItems.editDuplicate);
        editMenu.addSeparator();
        editMenu.add(editSelectAll);
        editMenu.addSeparator();
        editMenu.add(menuItems.editMove);
        editMenu.add(menuItems.editRotate);
        editMenu.add(menuItems.editMirror);

        // Add alignment and distribution items
        addAlignmentItemsToMenu(editMenu);
        addDistributionItemsToMenu(editMenu);

        return editMenu;
    }

    /**
     * Create all alignment menu items.
     * These items allow aligning selected elements to various positions.
     */
    private void createAlignmentMenuItems()
    {
        menuItems.alignLeftSelected = createMenuItem("alignLeftSelected",
            "align_left.png").build();

        menuItems.alignRightSelected = createMenuItem("alignRightSelected",
            "align_right.png").build();

        menuItems.alignTopSelected = createMenuItem("alignTopSelected",
            "align_top.png").build();

        menuItems.alignBottomSelected = createMenuItem("alignBottomSelected",
            "align_bottom.png").build();

        menuItems.alignHorizontalCenterSelected = createMenuItem(
            "alignHorizontalCenterSelected",
            "align_horizontal_center.png").build();

        menuItems.alignVerticalCenterSelected = createMenuItem(
            "alignVerticalCenterSelected", "align_vertical_center.png").build();
    }

    /**
     * Create all distribution menu items.
     * These items allow distributing selected elements evenly.
     */
    private void createDistributionMenuItems()
    {
        menuItems.distributeHorizontallySelected = createMenuItem(
            "distributeHorizontallySelected",
            "horizonta_distribute.png").build();

        menuItems.distributeVerticallySelected = createMenuItem(
            "distributeVerticallySelected", "vertical_distribute.png").build();
    }

    /**
     * Add action listeners to all alignment menu items.
     *
     * @param al the action listener to add
     */
    private void addAlignmentActionListeners(ActionListener al)
    {
        menuItems.alignLeftSelected.addActionListener(al);
        menuItems.alignRightSelected.addActionListener(al);
        menuItems.alignTopSelected.addActionListener(al);
        menuItems.alignBottomSelected.addActionListener(al);
        menuItems.alignHorizontalCenterSelected.addActionListener(al);
        menuItems.alignVerticalCenterSelected.addActionListener(al);
    }

    /**
     * Add action listeners to all distribution menu items.
     *
     * @param al the action listener to add
     */
    private void addDistributionActionListeners(ActionListener al)
    {
        menuItems.distributeHorizontallySelected.addActionListener(al);
        menuItems.distributeVerticallySelected.addActionListener(al);
    }

    /**
     * Add alignment menu items to the Edit menu.
     *
     * @param menu the menu to add items to
     */
    private void addAlignmentItemsToMenu(JMenu menu)
    {
        menu.addSeparator();
        menu.add(menuItems.alignLeftSelected);
        menu.add(menuItems.alignRightSelected);
        menu.add(menuItems.alignTopSelected);
        menu.add(menuItems.alignBottomSelected);
        menu.add(menuItems.alignHorizontalCenterSelected);
        menu.add(menuItems.alignVerticalCenterSelected);
    }

    /**
     * Add distribution menu items to the Edit menu.
     *
     * @param menu the menu to add items to
     */
    private void addDistributionItemsToMenu(JMenu menu)
    {
        menu.addSeparator();
        menu.add(menuItems.distributeHorizontallySelected);
        menu.add(menuItems.distributeVerticallySelected);
    }

    /**
     * Create the main View menu with display and layer options.
     *
     * @param al the action listener to associate to the menu items
     * @return the View menu
     */
    private JMenu createViewMenu(ActionListener al)
    {
        JMenu viewMenu = new JMenu(Globals.messages.getString("View"));

        JMenuItem layerOptions = createMenuItem("Layer_opt", "layers.png")
            .withShortcut(KeyEvent.VK_L, Globals.shortcutKey)
            .build();

        JMenuItem attachImage = createMenuItem("Attach_image_menu",
            "back_image.png").build();

        layerOptions.addActionListener(al);
        attachImage.addActionListener(al);

        viewMenu.add(layerOptions);
        viewMenu.add(attachImage);
        viewMenu.addSeparator();

        libs = new JCheckBoxMenuItem(Globals.messages.getString("Libs"));
        libs.setIcon(Globals.loadIcon(MENU_ICONS_PATH + "libs.png"));
        libs.addActionListener(al);
        viewMenu.add(libs);

        return viewMenu;
    }

    /**
     * Create the main Circuit menu with circuit-specific operations.
     *
     * @param al the action listener to associate to the menu items
     * @return the Circuit menu
     */
    private JMenu createCircuitMenu(ActionListener al)
    {
        JMenu circuitMenu = new JMenu(Globals.messages.getString("Circuit"));

        JMenuItem defineCircuit = createMenuItem("Define", "code.png")
            .withShortcut(KeyEvent.VK_G, Globals.shortcutKey)
            .build();

        JMenuItem updateLibraries = createMenuItem("LibraryUpdate",
            "lib_update.png")
            .withShortcut(KeyEvent.VK_U, Globals.shortcutKey)
            .build();

        JMenuItem insertImage = createMenuItem("Insert_image_menu",
            "back_image.png").build();

        defineCircuit.addActionListener(al);
        updateLibraries.addActionListener(al);
        insertImage.addActionListener(al);

        circuitMenu.add(defineCircuit);
        circuitMenu.add(updateLibraries);
        circuitMenu.addSeparator();
        circuitMenu.add(insertImage);

        return circuitMenu;
    }

    /**
     * Create the main About menu.
     *
     * @param al the action listener to associate to the menu items
     * @return the About menu
     */
    private JMenu createAboutMenu(ActionListener al)
    {
        JMenu about = new JMenu(Globals.messages.getString("About"));

        JMenuItem aboutMenu = createMenuItem("About_menu", "info.png").build();
        aboutMenu.addActionListener(al);
        about.add(aboutMenu);

        return about;
    }

    /**
     * Change the state of the show libs toggle menu item.
     *
     * @param s the state of the item
     */
    public void setShowLibsState(boolean s)
    {
        libs.setState(s);
    }

    /**
     * Initialize all action handlers using a map-based approach.
     */
    private void initializeActionHandlers()
    {
        // File operations
        registerFileActionHandlers();

        // Edit operations
        registerEditActionHandlers();

        // View operations
        registerViewActionHandlers();

        // Circuit operations
        registerCircuitActionHandlers();

        // Alignment operations
        registerAlignmentActionHandlers();

        // Distribution operations
        registerDistributionActionHandlers();
    }

    /**
     * Register all file-related action handlers.
     */
    private void registerFileActionHandlers()
    {
        actionHandlers.put(Globals.messages.getString("New"),
            (frame, coordL) -> frame.createNewInstance());

        actionHandlers.put(Globals.messages.getString("Open"),
            (frame, coordL) -> handleOpenFile(frame));

        actionHandlers.put(Globals.messages.getString("Save"),
            (frame, coordL) -> frame.getFileTools().save(false));

        actionHandlers.put(Globals.messages.getString("SaveName"),
            (frame, coordL) -> frame.getFileTools().saveWithName(false));

        actionHandlers.put(Globals.messages.getString("Save_split"),
            (frame, coordL) -> frame.getFileTools().saveWithName(true));

        actionHandlers.put(Globals.messages.getString("Export"),
            (frame, coordL) -> handleExport(frame, coordL));

        actionHandlers.put(Globals.messages.getString("Print"),
            (frame, coordL) -> handlePrint(frame));

        actionHandlers.put(Globals.messages.getString("Close"),
            (frame, coordL) -> handleClose(frame));

        actionHandlers.put(Globals.messages.getString("Circ_opt"),
            (frame, coordL) -> frame.showPrefs());
    }

    /**
     * Register all edit-related action handlers.
     */
    private void registerEditActionHandlers()
    {
        actionHandlers.put(Globals.messages.getString("Undo"),
            (frame, coordL) -> handleUndo(frame));

        actionHandlers.put(Globals.messages.getString("Redo"),
            (frame, coordL) -> handleRedo(frame));

        actionHandlers.put(Globals.messages.getString("Cut"),
            (frame, coordL) -> handleCut(frame));

        actionHandlers.put(Globals.messages.getString("Copy"),
            (frame, coordL) -> handleCopy(frame, false));

        actionHandlers.put(Globals.messages.getString("Copy_split"),
            (frame, coordL) -> handleCopy(frame, true));

        actionHandlers.put(Globals.messages.getString("Copy_as_image"),
            (frame, coordL) -> handleCopyAsImage(frame));

        actionHandlers.put(Globals.messages.getString("Paste"),
            (frame, coordL) -> handlePaste(frame));

        actionHandlers.put(Globals.messages.getString("DefineClipboard"),
            (frame, coordL) -> handleDefineClipboard(frame));

        actionHandlers.put(Globals.messages.getString("SelectAll"),
            (frame, coordL) -> handleSelectAll(frame));

        actionHandlers.put(Globals.messages.getString("Duplicate"),
            (frame, coordL) -> handleDuplicate(frame));

        actionHandlers.put(Globals.messages.getString("Move"),
            (frame, coordL) -> handleMove(frame));

        actionHandlers.put(Globals.messages.getString("Rotate"),
            (frame, coordL) -> handleRotate(frame));

        actionHandlers.put(Globals.messages.getString("Mirror_E"),
            (frame, coordL) -> handleMirror(frame));
    }

    /**
     * Register all view-related action handlers.
     */
    private void registerViewActionHandlers()
    {
        actionHandlers.put(Globals.messages.getString("Layer_opt"),
            (frame, coordL) -> handleLayerOptions(frame));

        actionHandlers.put(Globals.messages.getString("Libs"),
            (frame, coordL) -> handleShowLibs(frame));

        actionHandlers.put(Globals.messages.getString("Attach_image_menu"),
            (frame, coordL) -> handleAttachImage(frame));
    }

    /**
     * Register all circuit-related action handlers.
     */
    private void registerCircuitActionHandlers()
    {
        actionHandlers.put(Globals.messages.getString("Define"),
            (frame, coordL) -> handleDefineCircuit(frame));

        actionHandlers.put(Globals.messages.getString("LibraryUpdate"),
            (frame, coordL) -> handleUpdateLibraries(frame));

        actionHandlers.put(Globals.messages.getString("Insert_image_menu"),
            (frame, coordL) -> handleInsertImage(frame));

        actionHandlers.put(Globals.messages.getString("About_menu"),
            (frame, coordL) -> handleAbout(frame));
    }

    /**
     * Register all alignment-related action handlers.
     */
    private void registerAlignmentActionHandlers()
    {
        actionHandlers.put(
                Globals.messages.getString("alignLeftSelected"),
                (frame, coordL) -> handleAlignment(frame,
                frame.getCircuitPanel()
                        .getEditorActions()::alignLeftSelected));

        actionHandlers.put(
                Globals.messages.getString("alignRightSelected"),
                (frame, coordL) -> handleAlignment(frame,
                frame.getCircuitPanel()
                        .getEditorActions()::alignRightSelected));

        actionHandlers.put(
                Globals.messages.getString("alignTopSelected"),
                (frame, coordL) -> handleAlignment(frame,
                frame.getCircuitPanel()
                        .getEditorActions()::alignTopSelected));

        actionHandlers.put(
                Globals.messages.getString("alignBottomSelected"),
                (frame, coordL) -> handleAlignment(frame,
                frame.getCircuitPanel()
                        .getEditorActions()::alignBottomSelected));

        actionHandlers.put(
                Globals.messages.getString("alignHorizontalCenterSelected"),
                (frame, coordL) -> handleAlignment(frame,
                frame.getCircuitPanel()
                        .getEditorActions()::alignHorizontalCenterSelected));

        actionHandlers.put(
                Globals.messages.getString("alignVerticalCenterSelected"),
                (frame, coordL) -> handleAlignment(frame,
                frame.getCircuitPanel()
                        .getEditorActions()::alignVerticalCenterSelected));
    }

    /**
     * Register all distribution-related action handlers.
     */
    private void registerDistributionActionHandlers()
    {
        actionHandlers.put(
                Globals.messages.getString("distributeHorizontallySelected"),
                (frame, coordL) -> handleAlignment(frame,
                frame.getCircuitPanel()
                        .getEditorActions()::distributeHorizontallySelected));

        actionHandlers.put(
                Globals.messages.getString("distributeVerticallySelected"),
                (frame, coordL) -> handleAlignment(frame,
                frame.getCircuitPanel()
                        .getEditorActions()::distributeVerticallySelected));
    }

    /**
     * Process menu action events by dispatching to appropriate handlers.
     * This is the main entry point for handling all menu actions.
     *
     * @param evt the action event
     * @param fidoFrame the main frame in which the menu is present
     * @param coordL the coordinate listener to show messages if needed
     */
    public void processMenuActions(ActionEvent evt, FidoFrame fidoFrame,
        ChangeCoordinatesListener coordL)
    {
        String action = evt.getActionCommand();
        MenuActionHandler handler = actionHandlers.get(action);

        if (handler != null) {
            handler.handle(fidoFrame, coordL);
        }
    }

    // ========================================================================
    // Handler methods for specific actions
    // ========================================================================

    /**
     * Handle opening a file.
     *
     * @param frame the main application frame
     */
    private void handleOpenFile(FidoFrame frame)
    {
        OpenFile openf = new OpenFile();
        openf.setParam(frame);

        /*  TODO:
            The following code would require a thread safe implementation
            of some of the inner classes (such as CircuitModel), which was
            indeed not the case... Now, yes!
        */
        SwingUtilities.invokeLater(openf);
    }

    /**
     * Handle export operation.
     *
     * @param frame the main application frame
     * @param coordL the coordinate listener
     */
    private void handleExport(FidoFrame frame, ChangeCoordinatesListener coordL)
    {
        ExportTools exportTools = frame.getExportTools();
        exportTools.setCoordinateListener(coordL);
        exportTools.launchExport(frame, frame.getCircuitPanel(),
                frame.getFileTools().getOpenFileDirectory());
    }

    /**
     * Handle print operation.
     *
     * @param frame the main application frame
     */
    private void handlePrint(FidoFrame frame)
    {
        PrintTools printTools = frame.getPrintTools();
        CircuitPanel circuitPanel = frame.getCircuitPanel();
        printTools.associateToCircuitPanel(circuitPanel);
        printTools.printDrawing(frame);
    }

    /**
     * Handle window close operation.
     *
     * @param frame the main application frame
     */
    private void handleClose(FidoFrame frame)
    {
        if (!frame.getFileTools().checkIfToBeSaved()) {
            return;
        }
        frame.closeThisFrame();
    }

    /**
     * Handle undo operation.
     *
     * @param frame the main application frame
     */
    private void handleUndo(FidoFrame frame)
    {
        frame.getCircuitPanel().getUndoActions().undo();
        frame.repaint();
    }

    /**
     * Handle redo operation.
     *
     * @param frame the main application frame
     */
    private void handleRedo(FidoFrame frame)
    {
        frame.getCircuitPanel().getUndoActions().redo();
        frame.repaint();
    }

    /**
     * Handle cut operation.
     *
     * @param frame the main application frame
     */
    private void handleCut(FidoFrame frame)
    {
        CircuitPanel circuitPanel = frame.getCircuitPanel();
        CopyPasteActions copyPasteActions = circuitPanel.getCopyPasteActions();
        EditorActions editorActions = circuitPanel.getEditorActions();

        copyPasteActions.copySelected(!circuitPanel.extStrict, false);
        editorActions.deleteAllSelected(true);
        frame.repaint();
    }

    /**
     * Handle copy operation.
     *
     * @param frame the main application frame
     * @param split whether to split non-standard macros
     */
    private void handleCopy(FidoFrame frame, boolean split)
    {
        CircuitPanel circuitPanel = frame.getCircuitPanel();
        CopyPasteActions copyPasteActions = circuitPanel.getCopyPasteActions();
        copyPasteActions.copySelected(!circuitPanel.extStrict, split);
    }

    /**
     * Handle copy as image operation.
     * Display a dialog similar to the Export menu and create an image
     * that is stored in the clipboard, using a bitmap or vector format.
     *
     * @param frame the main application frame
     */
    private void handleCopyAsImage(FidoFrame frame)
    {
        ExportTools exportTools = frame.getExportTools();
        exportTools.exportAsCopiedImage(frame, frame.getCircuitPanel());
    }

    /**
     * Handle paste operation.
     *
     * @param frame the main application frame
     */
    private void handlePaste(FidoFrame frame)
    {
        CircuitPanel circuitPanel = frame.getCircuitPanel();
        CopyPasteActions copyPasteActions = circuitPanel.getCopyPasteActions();

        copyPasteActions.paste(
                circuitPanel.getMapCoordinates().getXGridStep(),
                circuitPanel.getMapCoordinates().getYGridStep());
        frame.repaint();
    }

    /**
     * Handle define clipboard operation.
     * Paste clipboard content as a new circuit.
     *
     * @param frame the main application frame
     */
    private void handleDefineClipboard(FidoFrame frame)
    {
        CircuitPanel circuitPanel = frame.getCircuitPanel();
        ParserActions parserActions = circuitPanel.getParserActions();

        TextTransfer textTransfer = new TextTransfer();

        if (circuitPanel.getUndoActions().getModified()) {
            frame.createNewInstance();
        }

        parserActions.parseString(
            new StringBuffer(textTransfer.getClipboardContents()));
        frame.repaint();
    }

    /**
     * Handle select all operation.
     *
     * @param frame the main application frame
     */
    private void handleSelectAll(FidoFrame frame)
    {
        CircuitPanel circuitPanel = frame.getCircuitPanel();
        SelectionActions selectionActions = circuitPanel.getSelectionActions();

        selectionActions.setSelectionAll(true);
        // Even if the drawing is not changed, a repaint operation is
        // needed since all selected elements are rendered in green.
        frame.repaint();
    }

    /**
     * Handle duplicate operation.
     *
     * @param frame the main application frame
     */
    private void handleDuplicate(FidoFrame frame)
    {
        CircuitPanel circuitPanel = frame.getCircuitPanel();
        CopyPasteActions copyPasteActions = circuitPanel.getCopyPasteActions();

        copyPasteActions.copySelected(!circuitPanel.extStrict, false);
        copyPasteActions.paste(
                circuitPanel.getMapCoordinates().getXGridStep(),
                circuitPanel.getMapCoordinates().getYGridStep());
        frame.repaint();
    }

    /**
     * Handle move operation.
     * Start moving selected elements with Move command.
     *
     * @param frame the main application frame
     */
    private void handleMove(FidoFrame frame)
    {
        CircuitPanel circuitPanel = frame.getCircuitPanel();
        SelectionActions selectionActions = circuitPanel.getSelectionActions();

        if (selectionActions.getFirstSelectedPrimitive() != null) {
            circuitPanel.getContinuosMoveActions().startMovingSelected(
                        circuitPanel.getMapCoordinates());
            frame.repaint();
        }
    }

    /**
     * Handle rotate operation.
     * 90 degrees rotation of all selected elements.
     *
     * @param frame the main application frame
     */
    private void handleRotate(FidoFrame frame)
    {
        CircuitPanel circuitPanel = frame.getCircuitPanel();
        EditorActions editorActions = circuitPanel.getEditorActions();
        ElementsEdtActions elementsEdtActions =
                circuitPanel.getContinuosMoveActions();

        if (elementsEdtActions.isEnteringMacro()) {
            elementsEdtActions.rotateMacro();
        } else {
            editorActions.rotateAllSelected();
        }
        frame.repaint();
    }

    /**
     * Handle mirror operation.
     * Mirror all the selected elements.
     *
     * @param frame the main application frame
     */
    private void handleMirror(FidoFrame frame)
    {
        CircuitPanel circuitPanel = frame.getCircuitPanel();
        EditorActions editorActions = circuitPanel.getEditorActions();
        ElementsEdtActions elementsEdtActions =
                circuitPanel.getContinuosMoveActions();

        if (elementsEdtActions.isEnteringMacro()) {
            elementsEdtActions.mirrorMacro();
        } else {
            editorActions.mirrorAllSelected();
        }
        frame.repaint();
    }

    /**
     * Handle alignment operations using a functional approach.
     *
     * @param frame the main application frame
     * @param alignmentAction the alignment action to execute
     */
    private void handleAlignment(FidoFrame frame, Runnable alignmentAction)
    {
        alignmentAction.run();
        frame.repaint();
    }

    /**
     * Handle layer options dialog.
     *
     * @param frame the main application frame
     */
    private void handleLayerOptions(FidoFrame frame)
    {
        CircuitPanel circuitPanel = frame.getCircuitPanel();

        DialogLayer layerDialog = new DialogLayer(frame,
                circuitPanel.getDrawingModel().getLayers());
        layerDialog.setVisible(true);

        // It is important that we force a complete recalculation of
        // all details in the drawing, otherwise the buffered setup
        // will not be responsive to the changes in the layer editing.
        circuitPanel.getDrawingModel().setChanged(true);
        frame.repaint();
    }

    /**
     * Handle show/hide libraries operation.
     *
     * @param frame the main application frame
     */
    private void handleShowLibs(FidoFrame frame)
    {
        frame.showLibs(!frame.areLibsVisible());
        libs.setState(frame.areLibsVisible());
    }

    /**
     * Handle attach image dialog.
     * Show the attach image dialog and process the result.
     *
     * @param frame the main application frame
     */
    private void handleAttachImage(FidoFrame frame)
    {
        ImageAsCanvas ii = frame.getCircuitPanel().getAttachedImage();
        DialogAttachImage di = new DialogAttachImage(frame);

        di.setFilename(ii.getFilename());
        di.setCorner(ii.getCornerX(), ii.getCornerY());
        di.setResolution(ii.getResolution());
        di.setVisible(true);

        if (di.shouldAttach()) {
            try {
                if (di.getShowImage()) {
                    ii.loadImage(di.getFilename());
                } else {
                    ii.removeImage();
                }
                ii.setResolution(di.getResolution());
                ii.setCorner(di.getCornerX(), di.getCornerY());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame,
                    Globals.messages.getString("Can_not_attach_image"),
                    "",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Handle inserting a new embedded image primitive.
     * Opens a file chooser, reads the selected image file, and adds it
     * to the drawing as a new selected, movable/resizable "IM" primitive
     * centered in the currently visible area.
     *
     * @param frame the main application frame
     */
    void handleInsertImage(FidoFrame frame)
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter(
            Globals.messages.getString("Image_files"),
            "png", "jpg", "jpeg", "gif", "bmp"));
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fc.getSelectedFile();

        byte[] bytes;
        BufferedImage img;
        try {
            bytes = Files.readAllBytes(file.toPath());
            img = ImageIO.read(file);
        } catch (IOException eE) {
            img = null;
            bytes = null;
        }
        if (img == null || bytes == null) {
            JOptionPane.showMessageDialog(frame,
                Globals.messages.getString("Can_not_attach_image"),
                "",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        CircuitPanel circuitPanel = frame.getCircuitPanel();
        DrawingModel dmp = circuitPanel.getDrawingModel();
        MapCoordinates cs = circuitPanel.getMapCoordinates();

        float cx;
        float cy;
        if (circuitPanel.father != null) {
            Point viewPos = circuitPanel.father.getViewport()
                .getViewPosition();
            Dimension viewSize = circuitPanel.father.getViewport()
                .getExtentSize();
            cx = cs.unmapXnosnap(viewPos.x + viewSize.width/2);
            cy = cs.unmapYnosnap(viewPos.y + viewSize.height/2);
        } else {
            cx = 100;
            cy = 100;
        }

        // Scale the image's natural pixel size down to a reasonable
        // logical size (about 200 logical units on the longest side).
        double scale = 200.0/Math.max(img.getWidth(), img.getHeight());
        float halfW = (float)(img.getWidth()*scale/2.0);
        float halfH = (float)(img.getHeight()*scale/2.0);

        String base64 = Base64.getEncoder().encodeToString(bytes);
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String ext = dot>=0 ? name.substring(dot+1).toLowerCase() : "png";
        String mime = "jpg".equals(ext) ? "jpeg" : ext;

        circuitPanel.getSelectionActions().setSelectionAll(false);
        PrimitiveImage p = new PrimitiveImage(cx-halfW, cy-halfH,
            cx+halfW, cy+halfH, mime, base64, 1.0f, false,
            circuitPanel.getCurrentLayer(), dmp.getTextFont(),
            dmp.getTextFontSize());
        dmp.addPrimitive(p, true, circuitPanel.getUndoActions());
        p.setSelected(true);

        frame.repaint();
    }

    /**
     * Handle define circuit dialog.
     * Edit the FidoCadJ code of the drawing.
     *
     * @param frame the main application frame
     */
    private void handleDefineCircuit(FidoFrame frame)
    {
        CircuitPanel circuitPanel = frame.getCircuitPanel();
        ParserActions parserActions = circuitPanel.getParserActions();

        DialogCircuitCode circuitDialog = new DialogCircuitCode(frame,
            parserActions.getText(!circuitPanel.extStrict).toString());
        circuitDialog.setVisible(true);

        parserActions.parseString(
                new StringBuffer(circuitDialog.getStringCircuit()));
        circuitPanel.getUndoActions().saveUndoState();
        frame.repaint();
    }

    /**
     * Handle library update operation.
     * Update libraries and refresh the display.
     *
     * @param frame the main application frame
     */
    private void handleUpdateLibraries(FidoFrame frame)
    {
        frame.loadLibraries();
        frame.setVisible(true);
    }

    /**
     * Handle about dialog.
     * Show the about menu.
     *
     * @param frame the main application frame
     */
    private void handleAbout(FidoFrame frame)
    {
        DialogAbout d = new DialogAbout(frame);
        d.setVisible(true);
    }

    // ========================================================================
    // Menu Item Builder - Helper class for creating menu items
    // ========================================================================

    /**
     * Builder class for creating menu items
     */
    private static class MenuItemBuilder
    {
        private String textKey;
        private String iconName;
        private KeyStroke accelerator;

        /**
         * Create a builder with the specified text key.
         *
         * @param textKey the resource key for the menu item text
         */
        MenuItemBuilder(String textKey)
        {
            this.textKey = textKey;
        }

        /**
         * Set the icon for this menu item.
         *
         * @param iconName the icon filename (without path)
         * @return this builder for method chaining
         */
        MenuItemBuilder withIcon(String iconName)
        {
            this.iconName = iconName;
            return this;
        }

        /**
         * Set a keyboard shortcut for this menu item.
         *
         * @param key the key code
         * @param modifiers the modifier keys
         * @return this builder for method chaining
         */
        MenuItemBuilder withShortcut(int key, int modifiers)
        {
            this.accelerator = KeyStroke.getKeyStroke(key, modifiers);
            return this;
        }

        /**
         * Set a simple key shortcut (single letter, no modifiers).
         *
         * @param key the key string
         * @return this builder for method chaining
         */
        MenuItemBuilder withShortcut(String key)
        {
            this.accelerator = KeyStroke.getKeyStroke(key);
            return this;
        }

        /**
         * Build the menu item with all specified properties.
         *
         * @return the constructed JMenuItem
         */
        JMenuItem build()
        {
            JMenuItem item = new JMenuItem(Globals.messages.getString(textKey));

            if (iconName != null) {
                item.setIcon(Globals.loadIcon(MENU_ICONS_PATH + iconName));
            }

            if (accelerator != null) {
                item.setAccelerator(accelerator);
            }

            return item;
        }
    }

    /**
     * Factory method to create a menu item builder.
     *
     * @param textKey the resource key for the menu item text
     * @param iconName the icon filename (without path)
     * @return a new MenuItemBuilder
     */
    private static MenuItemBuilder createMenuItem(String textKey,
            String iconName)
    {
        return new MenuItemBuilder(textKey).withIcon(iconName);
    }
}