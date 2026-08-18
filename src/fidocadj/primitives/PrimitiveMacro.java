package fidocadj.primitives;

import java.io.*;
import java.util.*;

import fidocadj.dialogs.controls.ParameterDescription;
import fidocadj.export.ExportInterface;
import fidocadj.geom.MapCoordinates;
import fidocadj.globals.Globals;
import fidocadj.graphic.GraphicsInterface;
import fidocadj.circuit.controllers.SelectionActions;
import fidocadj.circuit.controllers.EditorActions;
import fidocadj.circuit.controllers.ParserActions;
import fidocadj.circuit.model.DrawingModel;
import fidocadj.circuit.views.Drawing;
import fidocadj.circuit.views.Export;
import fidocadj.layers.LayerDesc;


/** Class to handle the macro primitive. Code is somewhat articulated since
    I use recursion (a macro is another drawing seen as an unbreakable symbol).

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
public final class PrimitiveMacro extends GraphicPrimitive
{
    static final int N_POINTS=3;
    private final Map<String, MacroDesc> library;
    private final List<LayerDesc> layers;
    private int o;              // Macro orientation
    private boolean m;          // Macro mirroring
    private boolean drawOnlyPads;
    private int drawOnlyLayer;
    private boolean alreadyExported;
    private DrawingModel macro;
    private final MapCoordinates macroCoord;
    private boolean selected;
    private String macroName;
    private String macroDesc;
    private boolean exportInvisible;

    private Drawing drawingAgent;

    // Stored data for caching
    private float x1;             // NOPMD
    private float y1;             // NOPMD

    /** Some layers may be shown or hidden by the user. Therefore, in some
        cases they may be exported or not. Set if invisible layers should be
        exported.
        @param s the value export invisible. True means that invisible layers
            will be exported.
    */
    public void setExportInvisible(boolean s)
    {
        exportInvisible = s;
    }

    /** Gets the number of control points used.
        @return the number of points used by the primitive
    */
    public int getControlPointNumber()
    {
        return N_POINTS;
    }

    /** Constructor.
        @param lib the library to be inherited.
        @param l the list of layers.
        @param f the name of the font for attached text.
        @param size the size of the font for attached text.
    */
    public PrimitiveMacro(Map<String, MacroDesc>lib, List<LayerDesc> l,
            String f, float size)
    {
        super();
        library=lib;
        layers=l;
        drawOnlyPads=false;
        drawOnlyLayer=-1;
        macro=new DrawingModel();
        macroCoord=new MapCoordinates();
        changed=true;

        initPrimitive(-1, f, size);

        macroStore(layers);
    }

    /** Constructor.
        @param lib the library to be inherited.
        @param l the list of layers.
        @param x the x coordinate of the control point of the macro.
        @param y the y coordinate of the control point of the macro.
        @param keyT the key to be used to uniquely identify the macro (it will
            be converted to lowercase).
        @param na the name to be shown.
        @param xa the x coordinate of the name of the macro.
        @param ya the y coordinate of the name of the macro.
        @param va the value to be shown.
        @param xv the x coordinate of the value of the macro.
        @param yv the y coordinate of the value of the macro.
        @param macroF the font to be used for the name and the value of the
            macro.
        @param macroS the size of the font.
        @param oo the macro orientation.
        @param mm the macro mirroring.
        @throws IOException if an unrecognized macro is found.
    */
    public PrimitiveMacro(Map<String, MacroDesc> lib, List<LayerDesc> l,
         float x, float y, String keyT,
         String na, float xa, float ya, String va, 
         float xv, float yv, String macroF,
         float macroS, int oo, boolean mm)
        throws IOException
    {
        super();
        initPrimitive(-1, macroF, macroS);
        library=lib;
        layers=l;
        String key=keyT.toLowerCase(Locale.forLanguageTag("en"));
        macro=new DrawingModel();
        macroCoord=new MapCoordinates();
        changed=true;
        setMacroFontSize(macroS);
        o=oo;
        m=mm;

        // Store the points of the macro and the text describing it.
        virtualPoint[0].x=x;
        virtualPoint[0].y=y;
        virtualPoint[1].x=xa;
        virtualPoint[1].y=ya;
        virtualPoint[2].x=xv;
        virtualPoint[2].y=yv;

        name=na;
        value=va;

        MacroDesc macro=library.get(key);

        // Check if the macro description is contained in the database
        // containing all the libraries.
        if (macro==null){
            throw new IOException("Unrecognized macro "
                                          + key);
        }
        macroDesc = macro.description;
        macroName = key;
        macroFont = macroF;

        macroStore(layers);
    }


    /** Returns true if the macro contains the specified layer. This
        is a calculation done at the DrawingModel level.
        @param l the layer to be checked.
        @return true if the layer is contained in the drawing and therefore
            should be drawn.
    */
    public boolean containsLayer(int l)
    {
        return macro.containsLayer(l);
    }

    /** Draw the macro contents.
        @param g the graphic context.
        @param coordSys the coordinate system.
        @param layerV the vector containing all layers.
    */
    private void drawMacroContents(GraphicsInterface g, MapCoordinates coordSys)
    {
        /* in the macro primitive, the the virtual point represents
           the position of the reference point of the macro to be drawn. */
        if(changed) {
            changed = false;
            x1=virtualPoint[0].x;
            y1=virtualPoint[0].y;

            macroCoord.setXMagnitude(coordSys.getXMagnitude());
            macroCoord.setYMagnitude(coordSys.getYMagnitude());

            macroCoord.setXCenter(coordSys.mapXr(x1,y1));
            macroCoord.setYCenter(coordSys.mapYr(x1,y1));
            macroCoord.setOrientation((o+coordSys.getOrientation())%4);
            macroCoord.mirror=m ^ coordSys.mirror;
            macroCoord.isMacro=true;
            macroCoord.resetMinMax();

            macro.setChanged(true);
        }

        if(isSelected()) {
            new SelectionActions(macro).setSelectionAll(true);
            selected = true;
        } else if (selected) {
            new SelectionActions(macro).setSelectionAll(false);
            selected = false;
        }

        macro.setDrawOnlyLayer(drawOnlyLayer);
        macro.setDrawOnlyPads(drawOnlyPads);

        drawingAgent = new Drawing(macro);
        drawingAgent.draw(g,macroCoord);

        if (macroCoord.getXMax()>macroCoord.getXMin() &&
            macroCoord.getYMax()>macroCoord.getYMin())
        {
            coordSys.trackPoint(macroCoord.getXMax(),macroCoord.getYMax());
            coordSys.trackPoint(macroCoord.getXMin(),macroCoord.getYMin());
        }
    }

    /** Specifies that the current primitive has been modified or not.
        If it is true, during the redraw all parameters should be calulated
        from scratch.
        @param c the value of the parameter.
    */
    public void setChanged(boolean c)
    {
        super.setChanged(c);
        macro.setChanged(c);
    }

    /** Parse and store the tokenized version of the macro.
        @layerV the array containing the layer description to be inherited.

    */
    private void macroStore(List<LayerDesc> layerV)
    {
        macro.setLibrary(library);          // Inherit the library
        macro.setLayers(layerV);    // Inherit the layers
        changed=true;

        if (macroDesc!=null) {
            ParserActions pa = new ParserActions(macro);
            pa.parseString(new StringBuffer(macroDesc));
            // Recursive call
        }
    }

    /** Set the layer vector.
        @param layerV the layer vector.
    */
    public void setLayers(List<LayerDesc> layerV)
    {
        macro.setLayers(layerV);
    }

    /** Draw the graphic primitive on the given graphic context.
        @param g the graphic context in which the primitive should be drawn.
        @param coordSys the graphic coordinates system to be applied.
        @param layerV the layer description.
    */
    public void draw(GraphicsInterface g, MapCoordinates coordSys,
                              List layerV)
    {
        // Macros are *always* on layer 0 (they can contain elements to be
        // drawn, of course, on other layers).
        setLayer(0);
        if(selectLayer(g,layerV)) {
            drawText(g, coordSys, layerV, drawOnlyLayer);
        }

        drawMacroContents(g, coordSys);
    }

    /** Set the Draw Only Pads mode.

        @param pd the wanted value
    */
    public void setDrawOnlyPads(boolean pd)
    {
        drawOnlyPads=pd;
    }

    /** Set the Draw Only Layer mode.
        @param la the layer that should be drawn.
    */

    public void setDrawOnlyLayer(int la)
    {
        drawOnlyLayer=la;
    }

    /** Get the maximum index of the layers contained in the macro.
        @return the maximum index of layers contained in the macro.
    */
    public int getMaxLayer()
    {
        return macro.getMaxLayer();
    }

    /** Parse a token array and store the graphic data for a given primitive
        Obviously, that routine should be called *after* having recognized
        that the called primitive is correct.
        That routine also sets the current layer.

        @param tokens the tokens to be processed. tokens[0] should be the
        command of the actual primitive.
        @param nn the number of tokens present in the array
        @throws IOException if the arguments are incorrect or the primitive
            is invalid.
    */
    public void parseTokens(String[] tokens, int nn)
        throws IOException
    {
        // assert it is the correct primitive
        changed=true;
        if ("MC".equals(tokens[0])) {   // Line
            if (nn<6) {
                throw new IOException("Bad arguments on MC");
            }
            // Load the points in the virtual points associated to the
            // current primitive.

            virtualPoint[0].x=Float.parseFloat(tokens[1]);
            virtualPoint[0].y=Float.parseFloat(tokens[2]);
            virtualPoint[1].x=virtualPoint[0].x+10;
            virtualPoint[1].y=virtualPoint[0].y+10;
            virtualPoint[2].x=virtualPoint[0].x+10;
            virtualPoint[2].y=virtualPoint[0].y+5;
            o=Integer.parseInt(tokens[3]);  // orientation
            m=Integer.parseInt(tokens[4])==1;  // mirror
            macroName=tokens[5];

            // This is useful when a filename contains spaces. However, it does
            // not work when there are two or more consecutive spaces.

            for (int i=6; i<nn; ++i) {
                macroName+=" "+tokens[i];
            }

            // The macro key recognition is made case insensitive by converting
            // internally all keys to lower case.

            macroName=macroName.toLowerCase(Locale.forLanguageTag("en"));

            // Let's see if the macro is recognized and store it.
            MacroDesc macro=library.get(macroName);

            if (macro==null){
                throw new IOException("Unrecognized macro '"
                                              + macroName+"'");
            }
            macroDesc = macro.description;
            macroStore(layers);

        } else {
            throw new IOException("MC: Invalid primitive:"+tokens[0]+
                                          " programming error?");
        }

    }

    /** Check if the macro contains elements which need to draw holes.
        @return true if the macro contains elements requiring holes, false
            otherwise.

    */
    public boolean needsHoles()
    {
        return drawingAgent.getNeedHoles();
    }

    /** Gets the distance (in primitive's coordinates space) between a
        given point and the primitive.
        When it is reasonable, the behaviour can be binary (polygons,
        ovals...). In other cases (lines, points), it can be proportional.
        @param px the x coordinate of the given point.
        @param py the y coordinate of the given point.
        @return the distance in logical units.
    */
    public int getDistanceToPoint(float px, float py)
    {
        /* in the macro primitive, the the first virtual point represents
           the position of the reference point of the macro to be drawn. */

        float x1=virtualPoint[0].x;
        float y1=virtualPoint[0].y;
        float dt=Float.MAX_VALUE;

        // Here we check if the given point lies inside the text areas

        if(checkText(Math.round(px), Math.round(py))) {
            return 0;
        }

        // If not, we need to see more throughly about the inners of the macro

        float vx=px-x1+100;
        float vy= py-y1+100;

        // This is a sort of inelegant code: we need to translate the position
        // given in the macro's coordinate system.

        if(m) {
            switch(o){
                case 1:
                    vx=py-y1+100;
                    vy=px-x1+100;
                    break;

                case 2:
                    vx=px-x1+100;
                    vy=-(py-y1)+100;
                    break;

                case 3:
                    vx=-(py-y1)+100;
                    vy=-(px-x1)+100;
                    break;

                case 0:
                    vx=-(px-x1)+100;
                    vy=py-y1+100;
                    break;

                default:
                    vx=0;
                    vy=0;
                    break;
            }
        } else {
            switch(o){
                case 1:
                    vx=py-y1+100;
                    vy=-(px-x1)+100;
                    break;

                case 2:
                    vx=-(px-x1)+100;
                    vy=-(py-y1)+100;
                    break;

                case 3:
                    vx=-(py-y1)+100;
                    vy=px-x1+100;
                    break;

                case 0:
                    vx=px-x1+100;
                    vy=py-y1+100;
                    break;

                default:
                    vx= 0;
                    vy= 0;
                    break;
            }
        }

        if (macroDesc==null) {
            System.out.println("1-Unrecognized macro "+
                    "WARNING this can be a programming problem...");
        } else {
            SelectionActions sa = new SelectionActions(macro);
            EditorActions edt=new EditorActions(macro, sa, null);
            return Math.min(edt.distancePrimitive(
                Math.round(vx), Math.round(vy)), Math.round(dt));
        }
        return Integer.MAX_VALUE;
    }

    /** Select the primitive if one of its virtual point is in the specified
        rectangular region (given in logical coordinates).
        @param px the x coordinate of the top left point.
        @param py the y coordinate of the top left point.
        @param w the width of the region
        @param h the height of the region
        @return true if at least a primitive has been selected
    */
    public boolean selectRect(int px, int py, int w, int h)
    {
        // Here is a trick: if there is at least one active layer,
        // distancePrimitive will return a value less than the maximum.
        SelectionActions sa = new SelectionActions(macro);
        EditorActions edt=new EditorActions(macro, sa, null);
        if (edt.distancePrimitive(0, 0)<Integer.MAX_VALUE) {
            return super.selectRect(px, py, w, h);
        } else {
            return false;
        }
    }

    /** Get the macro orientation
        @return the orientation.
    */
    public int getOrientation()
    {
        return o;
    }

    /** Determine wether the macro is mirrored or not
        @return true if the macro is mirrored

    */
    public boolean isMirrored()
    {
        return m;
    }

    /** Rotate the primitive. For a macro, it is different than for the other
        primitive, since we need to rotate its coordinate system.
        @param bCounterClockWise specify if the rotation should be done
                counterclockwise.
        @param ix the x coordinate of the center of rotation
        @param iy the y coordinate of the center of rotation
    */
    @Override public void rotatePrimitive(boolean bCounterClockWise,
        float ix, float iy)
    {
        super.rotatePrimitive(bCounterClockWise, ix, iy);

        if (bCounterClockWise) {
            o=(o+3)%4;
        } else {
            o=++o%4;
        }

        changed=true;
    }


    /** Mirror the primitive. For a macro, it is different than for the other
        primitive, since we just need to toggle the mirror flag.
        @param xpos the x value of the pivot axis.
    */
    @Override public void mirrorPrimitive(float xpos)
    {
        super.mirrorPrimitive(xpos);
        m ^= true;
        changed=true;
    }

    /** Obtain a string command descripion of the primitive.
        @param extensions true if FidoCadJ extensions to the old FidoCAD format
            should be active.
        @return the FIDOCAD command line.
    */
    public String toString(boolean extensions)
    {
        String mirror="0";
        if(m) {
            mirror="1";
        }

        String s="MC "+roundIntelligently(virtualPoint[0].x)
            +" "+roundIntelligently(virtualPoint[0].y)+" "+o+" "
                +mirror+" "+macroName+"\n";

        s+=saveText(extensions);

        return s;
    }

    /** Get the control parameters of the given primitive.
        @return a vector of ParameterDescription containing each control
                parameter.
                The first parameters should always be the virtual points.

    */
    public List<ParameterDescription> getControls()
    {
        List<ParameterDescription> v=new Vector<ParameterDescription>(10);
        ParameterDescription pd = new ParameterDescription();

        pd.parameter=name;
        pd.description=Globals.messages.getString("ctrl_name");
        pd.isExtension = true;
        v.add(pd);

        pd = new ParameterDescription();

        pd.parameter=value;
        pd.description=Globals.messages.getString("ctrl_value");
        pd.isExtension = true;

        v.add(pd);
        return v;
    }

    /** Set the control parameters of the given primitive.
        This method is specular to getControls().
        @param v a vector of ParameterDescription containing each control
                parameter.
                The first parameters should always be the virtual points.
        @return the next index in v to be scanned (if needed) after the
            execution of this function.
    */
    public int setControls(List<ParameterDescription> v)
    {
        int i=0;
        ParameterDescription pd;

        changed=true;

        pd=v.get(i);
        ++i;
        // Check, just for sure...
        if (pd.parameter instanceof String) {
            name=((String)pd.parameter);
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }

        pd=v.get(i);
        ++i;
        // Check, just for sure...
        if (pd.parameter instanceof String) {
            value=((String)pd.parameter);
        } else {
            System.out.println("Warning: unexpected parameter!"+pd);
        }

        return i;
    }

    /** Ensure that the next time the macro is exported, it will be done.
        Macro that are not expanded during exportation does not need to be
        replicated thru the layers. For this reason, there is an inibition
        system which is activated. Calling this method resets the inibition
        flag.
    */
    public void resetExport()
    {
        alreadyExported=false;
    }

    /** Each graphic primitive should call the appropriate exporting method
        of the export interface specified.
        @param exp the export interface that should be used.
        @param cs the actual coordinate mapping.
        @throws IOException if a problem occurs, such as it is impossible to
            write on the output file.
    */
    public void export(ExportInterface exp, MapCoordinates cs)
        throws IOException
    {
        if(alreadyExported) {
            return;
        }

        // Call the macro interface, to see if the macro should be expanded
        if (exp.exportMacro(cs.mapX(virtualPoint[0].x, virtualPoint[0].y),
            cs.mapY(virtualPoint[0].x, virtualPoint[0].y),
            m, o*90, macroName, macroDesc, name,
            cs.mapX(virtualPoint[1].x, virtualPoint[1].y),
            cs.mapY(virtualPoint[1].x, virtualPoint[1].y),
            value,
            cs.mapX(virtualPoint[2].x, virtualPoint[2].y),
            cs.mapY(virtualPoint[2].x, virtualPoint[2].y),
            macroFont,
            (int)Math.round(cs.mapYr(getMacroFontSize(),getMacroFontSize())-
                cs.mapYr(0,0)),
            library))
        {
            alreadyExported = true;
            return;
        }
        /* in the macro primitive, the virtual point represents
           the position of the reference point of the macro to be drawn. */

        float x1=virtualPoint[0].x;
        float y1=virtualPoint[0].y;

        MapCoordinates macroCoord=new MapCoordinates();

        macroCoord.setXMagnitude(cs.getXMagnitude());
        macroCoord.setYMagnitude(cs.getYMagnitude());

        macroCoord.setXCenter(cs.mapXr(x1,y1));
        macroCoord.setYCenter(cs.mapYr(x1,y1));

        macroCoord.setOrientation((o+cs.getOrientation())%4);
        macroCoord.mirror=m ^ cs.mirror;
        macroCoord.isMacro=true;

        macro.setDrawOnlyLayer(drawOnlyLayer);

        if(isSelected()) {
            new SelectionActions(macro).setSelectionAll(true);
        }

        macro.setDrawOnlyPads(drawOnlyPads);
        new Export(macro).exportDrawing(exp, exportInvisible, macroCoord);
        exportText(exp, cs, drawOnlyLayer);

    }

    /** Get the number of the virtual point associated to the Name property
        @return the number of the virtual point associated to the Name property
    */
    public int getNameVirtualPointNumber()
    {
        return 1;
    }

    /** Get the number of the virtual point associated to the Value property
        @return the number of the virtual point associated to the Value property
    */
    public  int getValueVirtualPointNumber()
    {
        return 2;
    }

    /** Get the current macro description string.
        @return the macro description string.
    */
    public String getMacroDesc()
    {
        return macroDesc;
    }

    /** Set the current macro description string.
        @param macroDesc the macro description string.
    */
    public void setMacroDesc(String macroDesc)
    {
        this.macroDesc = macroDesc;
    }
}