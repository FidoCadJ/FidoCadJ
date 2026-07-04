package fidocadj.circuit.controllers;

import java.io.*;
import java.util.*;
import java.net.*;

import fidocadj.circuit.model.DrawingModel;
import fidocadj.export.ExportGraphic;
import fidocadj.globals.Globals;
import fidocadj.layers.LayerDesc;
import fidocadj.layers.StandardLayers;

import fidocadj.primitives.GraphicPrimitive;
import fidocadj.primitives.PrimitiveAdvText;
import fidocadj.primitives.PrimitiveBezier;
import fidocadj.primitives.PrimitiveComplexCurve;
import fidocadj.primitives.PrimitiveConnection;
import fidocadj.primitives.PrimitiveLine;
import fidocadj.primitives.PrimitiveMacro;
import fidocadj.primitives.PrimitivePCBLine;
import fidocadj.primitives.PrimitivePCBPad;
import fidocadj.primitives.PrimitiveRectangle;
import fidocadj.primitives.PrimitiveOval;
import fidocadj.primitives.PrimitiveImage;
import fidocadj.primitives.MacroDesc;
import fidocadj.primitives.PrimitivePolygon;

/** ParserActions: perform parsing of FidoCadJ code.
    In general, those routines are constructed such as they are relatively
    fault-tolerant. If an error is detected in the file, the parsing is
    continued anyway and just an error message is sent to the console. Most
    of the times, the user will not see the error and this is probably OK
    since he/she will not be interested in it.

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
*/

public class ParserActions
{
    private final DrawingModel model;

    // This is the maximum number of tokens which will be considered in a line
    static final int MAX_TOKENS=10000;

    // True if FidoCadJ should use Windows style line feeds (appending \r
    // to the text generated).
    static final boolean useWindowsLineFeed=false;

    // Name of the last file opened
    public String openFileName = null;

    /** Standard constructor: provide the database class.
        @param pp the drawing model (database of the circuit).
    */
    public ParserActions (DrawingModel pp)
    {
        model=pp;
    }

    /** Parse the circuit contained in the StringBuffer specified.
        This function resets the primitive database and then parses the circuit.

        @param s the string containing the circuit
    */
    public void parseString(StringBuffer s)
    {
        model.getPrimitiveVector().clear();
        addString(s, false);
        model.setChanged(true);
    }

    /** Renders a split version of the macros contained in the given string.
        @param s a string containing macros to be splitted.
        @param splitStandardMacros if it is true, even the standard macros
            will be split.
        @return the split macros.
    */
    public StringBuffer splitMacros(StringBuffer s,
        boolean splitStandardMacros)
    {
        StringBuffer txt= new StringBuffer("");

        DrawingModel qQ=new DrawingModel();
        qQ.setLibrary(model.getLibrary());  // Inherit the library
        qQ.setLayers(model.getLayers());    // Inherit the layers

        // from the obtained string, obtain the new qQ object which will
        // be exported and then loaded into the clipboard.

        try {
            ParserActions pas=new ParserActions(qQ);
            pas.parseString(s);

            File temp= File.createTempFile("copy", ".fcd");
            temp.deleteOnExit();
            String frm="";
            if(splitStandardMacros) {
                frm = "fcda";
            } else {
                frm = "fcd";
            }

            ExportGraphic.export(temp,  qQ, frm, 1,true,false, true,false,
                false);

            FileInputStream input = null;
            BufferedReader bufRead = null;

            try {
                input = new FileInputStream(temp);
                bufRead = new BufferedReader(
                    new InputStreamReader(input, Globals.encoding));

                String line=bufRead.readLine();
                if (line==null) {
                    bufRead.close();
                    return new StringBuffer("");
                }

                txt = new StringBuffer();

                do {
                    txt.append(line);
                    txt.append("\n");
                    line =bufRead.readLine();
                } while (line != null);
            } finally {
                if(input!=null) { input.close(); }
                if(bufRead!=null) { bufRead.close(); }
            }

        } catch(IOException e) {
            System.out.println("Error: "+e);
        }

        return txt;
    }

    /** Get the FidoCadJ text file.

        @param extensions specify if FCJ extensions should be used
        @return the sketch in the text FidoCadJ format
    */
    public StringBuffer getText(boolean extensions)
    {
        StringBuffer s=registerConfiguration(extensions);

        // Embedded images are always serialized last, regardless of their
        // drawing/z-order, so that a quick look at the FidoCadJ file shows
        // the "regular" primitives first and the (potentially huge)
        // base64-encoded image lines at the very end.
        StringBuffer images=new StringBuffer();

        for (GraphicPrimitive g:model.getPrimitiveVector()){
            StringBuffer dest = g instanceof PrimitiveImage ? images : s;
            dest.append(g.toString(extensions));
            if(useWindowsLineFeed) {
                dest.append("\r");
            }
        }
        s.append(images);
        return s;
    }
    /** If it is needed, provides all the configurations settings at
        the beginning of the FidoCadJ file.
        @param extensions it is true when FidoCadJ should export using
            its extensions.
        @return a StringBuffer containing the configuration settings in the
            FidoCadJ file format.
    */
    public StringBuffer registerConfiguration(boolean extensions)
    {
        StringBuffer s = new StringBuffer();

        // This is something which is not contemplated by the original
        // FidoCAD for Windows. If extensions are not activated, just exit.
        if(!extensions) {
            return s;
        }

        // Here is the beginning of the output. We can eventually provide
        // some hints about the configuration of the software (if needed).

        // We start by checking if the diameter of the electrical connection
        // should be written.

        // We consider that a difference of 1e-5 is small enough

        if(Math.abs(Globals.diameterConnectionDefault-
            Globals.diameterConnection)>1e-5)
        {
            s.append("FJC C "+Globals.diameterConnection+"\n");
        }

        s.append(checkAndRegisterLayers());

        // Check if the line widths should be indicated
        if(Math.abs(Globals.lineWidth -
            Globals.lineWidthDefault)>1e-5)
        {
            s.append("FJC A "+Globals.lineWidth+"\n");
        }
        if(Math.abs(Globals.lineWidthCircles -
            Globals.lineWidthCirclesDefault)>1e-5)
        {
            s.append("FJC B "+Globals.lineWidthCircles+"\n");
        }

        return s;
    }

    /** Check if the layers should be indicated.
    */
    private StringBuffer checkAndRegisterLayers()
    {
        StringBuffer s=new StringBuffer();
        List<LayerDesc> layerV=model.getLayers();
        List<LayerDesc> standardLayers =
            StandardLayers.createStandardLayers();

        for(int i=0; i<layerV.size();++i) {
            LayerDesc l = (LayerDesc)layerV.get(i);

            if (l.isModified()) {
                int rgb=l.getColor().getRGB();
                float alpha=l.getAlpha();
                s.append("FJC L "+i+" "+rgb+" "+alpha+"\n");
                // We compare the layers to the standard configuration.
                // If the name has been modified, the name configuration
                // is also saved.
                String defaultName=
                    ((LayerDesc)standardLayers.get(i)).getDescription();
                if (!l.getDescription().equals(defaultName)) {
                    s.append("FJC N "+i+" "+l.getDescription()+"\n");
                }
                if (l.isLocked()) {
                    s.append("FJC K "+i+" "+l.isLocked()+"\n");
                }
            }
        }
        return s;
    }

    /** Parse the circuit contained in the StringBuffer specified.
        this funcion add the circuit to the current primitive database.

        @param s the string containing the circuit
        @param selectNew specify that the added primitives should be selected.
    */
    public void addString(StringBuffer s, boolean selectNew)
        //throws IOException
    {
        int i; // Character pointer within the string
        int j; // Token counter within the string
        boolean hasFCJ=false; // The last primitive had FCJ extensions
        StringBuffer token=new StringBuffer();
        String macroFont = model.getTextFont();
        float macroFontSize = model.getTextFontSize();

        // Flag indicating that the line is already too long and should not be
        // processed anymore:
        boolean lineTooLong=false;

        GraphicPrimitive g = new PrimitiveLine(macroFont, macroFontSize);

        // The tokenized command string.
        String[] tokens=new String[MAX_TOKENS];

        // Name and value fields for a primitive. Those arrays will contain
        // the tokenized TJ commands which follow an appropriate FCJ modifier.
        String[] name=null;
        String[] value=null;

        int vn=0;
        int vv=0;

        // Since the modifier FCJ follow the command, we need to save the
        // tokens of the line previously read, as well as the number of
        // tokens found in it.
        String[] oldTokens=new String[MAX_TOKENS];
        int oldJ=0;

        int macroCounter=0;
        int l;

        token.ensureCapacity(256);

        /*  This code is not very easy to read. If more extensions of the
            original FidoCAD format (performed with the FCJ tag) are to be
            implemented, it can be interesting to rewrite the parser as a
            state machine.
        */
        synchronized(this) {
            List<LayerDesc> layerV=model.getLayers();

            char c='\n';
            int len;

            // Actual line number. This is useful to indicate where errors are.
            int lineNum=1;

            j=0;
            token.setLength(0);
            len=s.length();

            // The purpose of this code is to tokenize the lines. Things are
            // made more complicated by the FCJ mechanism which acts as a
            // modifier for the previous command.

            for(i=0; i<len;++i){
                c=s.charAt(i);
                if(c=='\n' || c=='\r'|| i==len-1) { //The string is finished
                    lineTooLong=false;
                    if(i==len-1 && c!='\n' && c!=' '){
                        token.append(c);
                    }
                    ++lineNum;
                    tokens[j]=token.toString();
                    if (token.length()==0) { // Avoids trailing spaces
                        j--;
                    }

                    try{
                        // When we enter here, we have tokenized the current
                        // line and we kept in memory also the previous one.

                        // The first possibility is that the current line does
                        // not contain a FCJ modifier. In this case, process
                        // the previous line since we have all the information
                        // needed
                        // for doing that.

                        if(hasFCJ && !"FCJ".equals(tokens[0])) {
                            hasFCJ = registerPrimitivesWithFCJ(hasFCJ,
                                tokens, g, oldTokens, oldJ, selectNew);
                        }

                        if("FCJ".equals(tokens[0])) {
                            // FidoCadJ extension!
                            // Here the FCJ modifier changes something on the
                            // previous command. So ve check case by case what
                            // has to be modified.

                            if(hasFCJ && "MC".equals(oldTokens[0])) {
                                macroCounter=2;
                                g=new PrimitiveMacro(model.getLibrary(),layerV,
                                    macroFont, macroFontSize);
                                g.parseTokens(oldTokens, oldJ+1);
                            } else if (hasFCJ && "LI".equals(oldTokens[0])) {
                                g=new PrimitiveLine(macroFont, macroFontSize);

                                // We concatenate the two lines in a single
                                // array
                                // of tokens (the same code will be repeated
                                // several
                                // times for other commands also).

                                for(l=0; l<j+1; ++l) {
                                    oldTokens[l+oldJ+1]=tokens[l];
                                }

                                // Update the number of tokens
                                oldJ+=j+1;

                                // The actual parsing of the tokens is
                                // relegated
                                // to the primitive.
                                g.parseTokens(oldTokens, oldJ+1);
                                g.setSelected(selectNew);

                                if(oldJ>5 && "1".equals(oldTokens[oldJ])) {
                                    macroCounter = 2;
                                } else {
                                    model.addPrimitive(g,false,null);
                                }

                            } else if (hasFCJ && "BE".equals(oldTokens[0])) {
                                g=new PrimitiveBezier(macroFont, macroFontSize);

                                for(l=0; l<j+1; ++l) {
                                    oldTokens[l+oldJ+1]=tokens[l];
                                }

                                oldJ+=j+1;
                                g.parseTokens(oldTokens, oldJ+1);
                                g.setSelected(selectNew);
                                if(oldJ>5 && "1".equals(oldTokens[oldJ])) {
                                    macroCounter = 2;
                                } else {
                                    model.addPrimitive(g,false,null);
                                }
                            } else if (hasFCJ && ("RV".equals(oldTokens[0])||
                                "RP".equals(oldTokens[0])))
                            {
                                g=new PrimitiveRectangle(macroFont,
                                    macroFontSize);

                                for(l=0; l<j+1; ++l) {
                                    oldTokens[l+oldJ+1]=tokens[l];
                                }

                                oldJ+=j+1;
                                g.parseTokens(oldTokens, oldJ+1);
                                g.setSelected(selectNew);
                                if(oldJ>2 && "1".equals(oldTokens[oldJ])) {
                                    macroCounter = 2;
                                } else {
                                    model.addPrimitive(g,false,null);
                                }
                            } else if (hasFCJ && ("EV".equals(oldTokens[0])||
                                "EP".equals(oldTokens[0])))
                            {
                                g=new PrimitiveOval(macroFont, macroFontSize);

                                for(l=0; l<j+1; ++l) {
                                    oldTokens[l+oldJ+1]=tokens[l];
                                }

                                oldJ+=j+1;
                                g.parseTokens(oldTokens, oldJ+1);
                                g.setSelected(selectNew);
                                if(oldJ>2 && "1".equals(oldTokens[oldJ])) {
                                    macroCounter = 2;
                                } else {
                                    model.addPrimitive(g,false,null);
                                }
                            } else if (hasFCJ && ("PV".equals(oldTokens[0])||
                                "PP".equals(oldTokens[0])))
                            {
                                g=new PrimitivePolygon(macroFont,
                                    macroFontSize);

                                for(l=0; l<j+1; ++l) {
                                    oldTokens[l+oldJ+1]=tokens[l];
                                }

                                oldJ+=j+1;
                                g.parseTokens(oldTokens, oldJ+1);
                                g.setSelected(selectNew);
                                if(oldJ>2 && "1".equals(oldTokens[oldJ])) {
                                    macroCounter = 2;
                                } else {
                                    model.addPrimitive(g,false,null);
                                }
                            } else if (hasFCJ && ("CV".equals(oldTokens[0])||
                                "CP".equals(oldTokens[0])))
                            {
                                g=new PrimitiveComplexCurve(macroFont,
                                    macroFontSize);

                                for(l=0; l<j+1; ++l) {
                                    oldTokens[l+oldJ+1]=tokens[l];
                                }

                                oldJ+=j+1;
                                g.parseTokens(oldTokens, oldJ+1);
                                g.setSelected(selectNew);
                                // If we have a name/value following, we
                                // put macroCounter (successively used by
                                // TY to determine that we are in a case in
                                // which
                                // TY commands must not be considered as
                                // separate).
                                if(oldJ>2 && "1".equals(oldTokens[oldJ])) {
                                    macroCounter = 2;
                                } else {
                                    model.addPrimitive(g,false,null);
                                }
                            } else if (hasFCJ && "PL".equals(oldTokens[0])) {
                                macroCounter = 2;
                            } else if (hasFCJ && "PA".equals(oldTokens[0])) {
                                macroCounter = 2;
                            } else if (hasFCJ && "SA".equals(oldTokens[0])) {
                                macroCounter = 2;
                            } else if (hasFCJ && "IM".equals(oldTokens[0])) {
                                macroCounter = 2;
                            }
                            hasFCJ=false;

                        } else if("FJC".equals(tokens[0])) {
                            fidoConfig(tokens, j, layerV);
                        } else if("LI".equals(tokens[0])) {
                            // Save the tokenized line.
                            // We cannot create the macro until we parse the
                            // following line (which can be FCJ)
                            macroCounter=0;

                            for(l=0; l<j+1; ++l) {
                                oldTokens[l]=tokens[l];
                            }
                            oldJ=j;
                            hasFCJ=true;

                        } else if("BE".equals(tokens[0])) {
                            macroCounter=0;

                            for(l=0; l<j+1; ++l) {
                                oldTokens[l]=tokens[l];
                            }
                            oldJ=j;
                            hasFCJ=true;
                        } else if("MC".equals(tokens[0])) {
                            // Save the tokenized line.
                            macroCounter=0;
                            for(l=0; l<j+1; ++l) {
                                oldTokens[l]=tokens[l];
                            }
                            oldJ=j;
                            hasFCJ=true;
                        } else if("TE".equals(tokens[0])) {
                            hasFCJ=false;
                            macroCounter=0;
                            g=new PrimitiveAdvText();
                            g.parseTokens(tokens, j+1);
                            g.setSelected(selectNew);
                            model.addPrimitive(g,false,null);
                        } else if("TY".equals(tokens[0])) {
                            // The TY command is somewhat special, because
                            // it can be used to specify the name and the value
                            // of a primitive or a macro. Therefore, we try
                            // to understand in which case we are
                            hasFCJ=false;

                            if(macroCounter==2) {
                                macroCounter--;
                                name=new String[j+1];
                                for(l=0; l<j+1;++l) {
                                    name[l]=tokens[l];
                                }
                                vn=j;
                            } else if(macroCounter==1) {
                                value=new String[j+1];
                                for(l=0; l<j+1;++l) {
                                    value[l]=tokens[l];
                                }
                                vv=j;
                                if (name!=null) { g.setName(name,vn+1); }
                                g.setValue(value,vv+1);

                                g.setSelected(selectNew);
                                model.addPrimitive(g, false,null);
                                macroCounter=0;
                            } else {
                                // If we are in the classical case of a simple
                                // isolated TY command, we process it.
                                g=new PrimitiveAdvText();
                                g.parseTokens(tokens, j+1);
                                g.setSelected(selectNew);
                                model.addPrimitive(g,false,null);
                            }
                        } else if("PL".equals(tokens[0])) {
                            hasFCJ=true;
                            for(l=0; l<j+1; ++l) {
                                oldTokens[l]=tokens[l];
                            }

                            macroCounter=0;
                            oldJ=j;
                            g=new PrimitivePCBLine(macroFont, macroFontSize);
                            g.parseTokens(tokens, j+1);
                            g.setSelected(selectNew);
                        } else if("PA".equals(tokens[0])) {
                            hasFCJ=true;
                            for(l=0; l<j+1; ++l) {
                                oldTokens[l]=tokens[l];
                            }
                            macroCounter=0;
                            g=new PrimitivePCBPad(macroFont, macroFontSize);
                            oldJ=j;
                            g.parseTokens(tokens, j+1);
                            g.setSelected(selectNew);
                        } else if("SA".equals(tokens[0])) {
                            hasFCJ=true;
                            for(l=0; l<j+1; ++l) {
                                oldTokens[l]=tokens[l];
                            }
                            oldJ=j;
                            macroCounter=0;
                            g=new PrimitiveConnection(macroFont, macroFontSize);
                            g.parseTokens(tokens, j+1);
                            g.setSelected(selectNew);
                            //addPrimitive(g,false,false);
                        } else if("IM".equals(tokens[0])) {
                            hasFCJ=true;
                            for(l=0; l<j+1; ++l) {
                                oldTokens[l]=tokens[l];
                            }
                            oldJ=j;
                            macroCounter=0;
                            g=new PrimitiveImage(macroFont, macroFontSize);
                            g.parseTokens(tokens, j+1);
                            g.setSelected(selectNew);
                        }  else if("EV".equals(tokens[0])
                            ||"EP".equals(tokens[0]))
                        {
                            macroCounter=0;
                            for(l=0; l<j+1; ++l) {
                                oldTokens[l]=tokens[l];
                            }
                            oldJ=j;
                            hasFCJ=true;
                        } else if("RV".equals(tokens[0])
                            ||"RP".equals(tokens[0]))
                        {
                            macroCounter=0;

                            for(l=0; l<j+1; ++l) {
                                oldTokens[l]=tokens[l];
                            }
                            oldJ=j;
                            hasFCJ=true;
                        } else if("PV".equals(tokens[0])
                            ||"PP".equals(tokens[0]))
                        {
                            macroCounter=0;

                            for(l=0; l<j+1; ++l) {
                                oldTokens[l]=tokens[l];
                            }
                            oldJ=j;
                            hasFCJ=true;
                        } else if("CV".equals(tokens[0])
                            ||"CP".equals(tokens[0]))
                        {
                            macroCounter=0;
                            for(l=0; l<j+1; ++l) {
                                oldTokens[l]=tokens[l];
                            }
                            oldJ=j;
                            hasFCJ=true;
                        }
                    } catch(IOException eE) {
                        System.out.println("Error encountered: "+eE.toString());
                        System.out.println("string parsing line: "+lineNum);
                        hasFCJ = true;
                        macroCounter = 0;

                        for(l=0; l<j+1; ++l) {
                            oldTokens[l]=tokens[l];
                        }
                        oldJ=j;
                    } catch(NumberFormatException fF) {
                        System.out.println(
                            "I could not read a number at line: "
                            +lineNum);
                        hasFCJ = true;
                        macroCounter = 0;
                        for(l=0; l<j+1; ++l) {
                            oldTokens[l]=tokens[l];
                        }
                        oldJ=j;
                    }
                    j=0;
                    token.setLength(0);
                } else if (c==' ' && !lineTooLong){ // Ready for next token
                    tokens[j]=token.toString();
                    token.setLength(0);
                    ++j;
                    if (j>=MAX_TOKENS) {
                        System.out.println("Too much tokens!");
                        System.out.println("string parsing line: "+lineNum);
                        j=MAX_TOKENS-1;
                        lineTooLong=true;
                        continue;
                    }
                } else {
                    if (!lineTooLong) {
                        token.append(c);
                    }
                }
            }

            // We need to process the very last line, which is contained in
            // the tokens currently read.
            try{
                registerPrimitivesWithFCJ(hasFCJ, tokens, g, oldTokens, oldJ,
                    selectNew);
            } catch(IOException eE) {
                System.out.println("Error encountered: "+eE.toString());
                System.out.println("string parsing line: "+lineNum);
            } catch(NumberFormatException fF) {
                System.out.println("I could not read a number at line: "
                                         +lineNum);
            }
            model.sortPrimitiveLayers();
        }
    }

    /** Handle the FCJ command for the program configuration.

    */
    private void fidoConfig(String[] tokens, int ntokens,
        List<LayerDesc> layerV)
    {
        double newConnectionSize = -1.0;
        double newLineWidth = -1.0;
        double newLineWidthCircles = -1.0;

        // FidoCadJ Configuration

        if("C".equals(tokens[1])) {
            // Connection size
            newConnectionSize = Double.parseDouble(tokens[2]);
        } else if("L".equals(tokens[1])) {
            // Layer configuration
            int layerNum = Integer.parseInt(tokens[2]);
            if (layerNum>=0&&layerNum<layerV.size()) {
                int rgb=Integer.parseInt(tokens[3]);
                float alpha=Float.parseFloat(tokens[4]);
                LayerDesc ll=(LayerDesc)layerV.get(layerNum);
                ll.getColor().setRGB(rgb);
                ll.setAlpha(alpha);
                ll.setModified(true);
            }
        } else if("N".equals(tokens[1])) {
            // Layer name

            int layerNum = Integer.parseInt(tokens[2]);
            if (layerNum>=0&&layerNum<layerV.size()){
                String lName="";

                StringBuffer temp=new StringBuffer(25);
                for(int t=3; t<ntokens+1; ++t) {
                    temp.append(tokens[t]);
                    temp.append(" ");
                }

                lName=temp.toString();
                LayerDesc ll=(LayerDesc)layerV.get(layerNum);
                ll.setDescription(lName);
                ll.setModified(true);
            }

        } else if("K".equals(tokens[1])) {
            // Layer locked state
            int layerNum = Integer.parseInt(tokens[2]);
            if (layerNum>=0&&layerNum<layerV.size()) {
                LayerDesc ll=(LayerDesc)layerV.get(layerNum);
                ll.setLocked(Boolean.parseBoolean(tokens[3]));
                ll.setModified(true);
            }
        } else if("A".equals(tokens[1])) {
            // Connection size
            newLineWidth = Double.parseDouble(tokens[2]);
        } else if("B".equals(tokens[1])) {
            // Connection size
            newLineWidthCircles = Double.parseDouble(tokens[2]);
        }

        // If the schematics has some configuration information, we need
        // to set them up.
        if (newConnectionSize>0) {
            Globals.diameterConnection=newConnectionSize;
        }
        if (newLineWidth>0) {
            Globals.lineWidth = newLineWidth;
        }
        if (newLineWidthCircles>0) {
            Globals.lineWidthCircles = newLineWidthCircles;
        }
    }

    /** This method checks if a primitive may have FCJ  modifiers following.
        If no further FCJ tokens are present, the primitive is created
        immediately. If a FCJ token follows, we proceed to further parsing
        what follows.
    */
    private boolean registerPrimitivesWithFCJ(boolean hasFCJt,
        String[] tokens,
        GraphicPrimitive gg, String[] oldTokens, int oldJ,
        boolean selectNew)
        throws IOException
    {
        String macroFont = model.getTextFont();
        float macroFontSize = model.getTextFontSize();
        List<LayerDesc> layerV=model.getLayers();

        GraphicPrimitive g=gg;
        boolean hasFCJ=hasFCJt;
        boolean addPrimitive = false;
        if(hasFCJ && !"FCJ".equals(tokens[0])) {
            if ("MC".equals(oldTokens[0])) {
                g=new PrimitiveMacro(model.getLibrary(),
                    layerV, macroFont, macroFontSize);
                addPrimitive = true;
            } else if ("LI".equals(oldTokens[0])) {
                g=new PrimitiveLine(macroFont, macroFontSize);
                addPrimitive = true;
            } else if ("BE".equals(oldTokens[0])) {
                g=new PrimitiveBezier(macroFont, macroFontSize);
                addPrimitive = true;
            } else if ("RP".equals(oldTokens[0])||
                "RV".equals(oldTokens[0]))
            {
                g=new PrimitiveRectangle(macroFont, macroFontSize);
                addPrimitive = true;
            } else if ("EP".equals(oldTokens[0])||
                "EV".equals(oldTokens[0]))
            {
                g=new PrimitiveOval(macroFont, macroFontSize);
                addPrimitive = true;
            } else if ("PP".equals(oldTokens[0])
                ||"PV".equals(oldTokens[0]))
            {
                g=new PrimitivePolygon(macroFont, macroFontSize);
                addPrimitive = true;
            } else if("PL".equals(oldTokens[0])) {
                g=new PrimitivePCBLine(macroFont, macroFontSize);
                addPrimitive = true;
            } else if ("CP".equals(oldTokens[0])
                ||"CV".equals(oldTokens[0]))
            {
                g=new PrimitiveComplexCurve(macroFont, macroFontSize);
                addPrimitive = true;
            }  else if("PA".equals(oldTokens[0])) {
                g=new PrimitivePCBPad(macroFont, macroFontSize);
                addPrimitive = true;
            } else if("SA".equals(oldTokens[0])) {
                g=new PrimitiveConnection(macroFont, macroFontSize);
                addPrimitive = true;
            } else if("IM".equals(oldTokens[0])) {
                g=new PrimitiveImage(macroFont, macroFontSize);
                addPrimitive = true;
            }
        }

        if(addPrimitive) {
            g.parseTokens(oldTokens, oldJ+1);
            g.setSelected(selectNew);
            model.addPrimitive(g,false,null);
            hasFCJ = false;
        }
        return hasFCJ;
    }

    /** Read all librairies contained in the given URL at the given prefix.
        This is particularly useful to read librairies shipped in a jar
        file.
        @param s the URL containing the libraries.
        @param prefixS the prefix to be adopted for the keys of all elements
            in the library. Most of the times it is the filename, except for
            standard or internal libraries.
    */
    public void loadLibraryInJar(URL s, String prefixS)
    {
        String prefix=prefixS;
        if(s==null) {
            if (prefix==null) {
                prefix="";
            }
            System.out.println("Resource not found! "+prefix);
            return;
        }
        try {
            readLibraryBufferedReader(new BufferedReader(new
                InputStreamReader(s.openStream(), Globals.encoding)), prefix);
        } catch (IOException eE) {
            System.out.println("Problems reading library: "+s.toString());
        }
    }

    /** Read the library contained in a file
        @param openFileName the name of the file to be loaded
        @throws IOException when something goes horribly wrong. Most of the
            times the filename is not found.
    */
    public void readLibraryFile(String openFileName)
        throws IOException
    {
        InputStreamReader input = null;
        BufferedReader bufRead = null;
        String prefix="";

        try {
            input = new InputStreamReader(new
                FileInputStream(openFileName), Globals.encoding);

            bufRead = new BufferedReader(input);

            prefix = Globals.getFileNameOnly(openFileName);
            if ("FCDstdlib".equals(prefix)) {
                prefix="";
            }

            readLibraryBufferedReader(bufRead, prefix);
        } finally {
            if(bufRead!=null) { bufRead.close(); }
            if(input!=null) { input.close(); }
        }
    }

    /** Read a library provided by a buffered reader. Adds all the macro keys
        in memory, with the given prefix.
        @param bufRead The buffered reader prepared with the stream containing
            the library we want to read.
        @param prefix The prefix which should be added to the macro key when
            using a non standard macro.
        @throws IOException when something goes horribly wrong.
    */
    public void readLibraryBufferedReader(BufferedReader bufRead,
        String prefix)
        throws IOException
    {
        String macroName="";
        String longName="";
        String categoryName="";
        String libraryName="";
        int i;
        String line="";

        MacroDesc macroDesc;

        while(true) {
            // Read and process line by line.
            line = bufRead.readLine();

            if(line==null) {
                break;
            }

            // Avoid trailing spaces
            line=line.trim();

            // Avoid processing shorter lines
            if (line.length()<=1) {
                continue;
            }

            // A category
            if(line.charAt(0)=='{') {
                categoryName="";
                StringBuffer temp=new StringBuffer(25);
                for(i=1; i<line.length()&&line.charAt(i)!='}'; ++i){
                    temp.append(line.charAt(i));
                }
                categoryName=temp.toString().trim();
                if(i==line.length()) {
                    IOException e=new IOException(
                        "Category non terminated with }.");
                    throw e;
                }
                continue;
            }

            // A macro
            if(line.charAt(0)=='[') {
                macroName="";

                longName="";
                StringBuffer temp=new StringBuffer(25);
                for(i=1; line.charAt(i)!=' ' &&
                         line.charAt(i)!=']' &&
                         i<line.length(); ++i)
                {
                    temp.append(line.charAt(i));
                }
                macroName=temp.toString().trim();
                int j;
                temp=new StringBuffer(25);
                for(j=i; j<line.length()&&line.charAt(j)!=']'; ++j){
                    temp.append(line.charAt(j));
                }
                longName=temp.toString();
                if(j==line.length()) {
                    IOException e=new IOException(
                        "Macro name non terminated with ].");
                    throw e;
                }

                if ("FIDOLIB".equals(macroName)) {
                    libraryName = longName.trim();
                    continue;
                } else {
                    if(!"".equals(prefix)) {
                        macroName=prefix+"."+macroName;
                    }

                    macroName=macroName.toLowerCase(
                                        Locale.forLanguageTag("en"));
                    model.getLibrary().put(macroName, new
                        MacroDesc(macroName,"","","","", prefix));
                    /*System.out.printf("-- macroName:%s | longName:%s |
                        categoryName:%s | libraryName:%s | prefix:%s\n",
                        macroName,longName,categoryName,libraryName,prefix);*/
                    continue;
                }
            }

            // TODO rewrite this block
            if(!"".equals(macroName)){
                // Add the macro name.
                // NOTE: in FidoCAD, the macro prefix is somewhat case
                // insensitive, since it indicates a file name and in
                // Windows all file names are case insensitive. Under
                // other operating systems, we need to be waaay much
                // careful, hence we convert the macro name to lower case.
                macroName = macroName.toLowerCase(Locale.forLanguageTag("en"));
                macroDesc = model.getLibrary().get(macroName);
                if(macroDesc==null) {
                    return;
                }
                macroDesc.name = longName;
                macroDesc.key = macroName;
                macroDesc.category = categoryName;
                macroDesc.library = libraryName;
                macroDesc.filename = prefix;

                macroDesc.description = macroDesc.description + "\n" +
                        line;
            }
        }
    }
    /** Try to load all libraries ("*.fcl") files in the given directory.
        FCDstdlib.fcl if exists will be considered as standard library.

        @param s the directory in which the libraries should be present.
    */
    public void loadLibraryDirectory(String s)
    {
        String[] files;  // The names of the files in the directory.
        File dir = new File(s);

        // Obtain the list of files in the specified directory.
        files = dir.list(new FilenameFilter()
        {
            @Override public boolean accept(File dir, String name)
            {
                // This filter allows to obtain all files with the fcd
                // file extension
                return name.toLowerCase(
                        Locale.forLanguageTag("en")).endsWith(".fcl");
            }
        });

        // We first check if the directory is existing or is not empty.
        if(!dir.exists() || files==null) {
            if (!"".equals(s)){
                System.out.println("Warning! Library directory is incorrect:");
                System.out.println(s);
            }
            System.out.println(
                "Activated FidoCadJ internal libraries and symbols.");
            return;
        }
        // We read all the directory content, file by file
        for (String fs: files) {
            File f = new File(dir, fs);
            try {
                // Here we have a hopefully valid file in f, so we may read its
                // contents
                readLibraryFile(f.getPath());
            } catch (IOException eE) {
                System.out.println("Problems reading library "+
                    f.getName()+" "+eE);
            }
        }
    }
}
