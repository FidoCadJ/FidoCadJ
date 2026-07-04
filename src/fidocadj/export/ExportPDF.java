package fidocadj.export;

import java.util.*;
import java.io.*;
import javax.swing.*;

import fidocadj.globals.Globals;
import fidocadj.layers.LayerDesc;
import fidocadj.primitives.Arrow;
import fidocadj.graphic.DimensionG;
import fidocadj.graphic.GraphicsInterface;
import fidocadj.graphic.ColorInterface;
import fidocadj.graphic.DecoratedText;
import fidocadj.graphic.PointDouble;
import fidocadj.graphic.TextInterface;

/**
    Export towards the Adobe Portable Document File

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

    Copyright 2008-2023 by Davide Bucci
</pre>

    @author Davide Bucci
*/
public final class ExportPDF implements ExportInterface, TextInterface
{
    private final File temp;
    private final OutputStreamWriter fstream;
    private final OutputStreamWriter fstreamt;
    private BufferedWriter out;
    private BufferedWriter outt;
    private boolean fontWarning;
    private String userfont;
    private float dashPhase;
    private float currentPhase=-1;
    private float currentFontSize=0;
    private DecoratedText dt;
    private String currentFont;        // Some info about the font is stored
    private float textx;            // This is used in sub-sup scripts position
    private float texty;


    // A graphic interface object is used here to get information about the
    // size of the different glyphs in the font.
    private final GraphicsInterface gi;

    // Well, this is a complex stuff. In practice, in the PDF format we have to
    // map the UTF8 code to the Adobe name of glyphs in a font. This is
    // accomplished by reading a file presenting the correspondance between
    // the standard UTF8 code and the glyph name. This file is from Adobe and
    // it is called glyphlist.txt available here:
    // https://github.com/adobe-type-tools/agl-aglfn
    // The Map is completed in the exportStart method of this class.
    // During the export of text (exportAdvText), a list of unicode chars
    // whose encoding will be considered in the PDF is filled.
    // At the end of the export, an encoding mapping will be created.
    private Map<Integer, String> unicodeToGlyph;
    private Map<Integer, Integer> uncodeCharsNeeded;
    private int unicodeCharIndex;

    // The file header
    private String head;

    // An array which will contain String elements representing all the
    // objects present in the PDF file.
    private String obj_PDF[];

    // The maximum number of objects contained in the PDF file.
    private static final int numOfObjects = 20;

    private String closeObject;
    private long fileLength;

    private List layerV;
    private ColorInterface actualColor;
    private double actualWidth;
    private int currentDash;

    static final String encoding="UTF8";

    private String sDash[];

    /** Set the multiplication factor to be used for the dashing.
        @param u the factor.
    */
    public void setDashUnit(double u)
    {
        sDash = new String[Globals.dashNumber];

        // If the line width has been changed, we need to update the
        // stroke table

        // The first entry is non dashed
        sDash[0]="";

        // Resize the dash sizes depending on the current zoom size.
        String dashArrayStretched;
        // Then, the dashed stroke styles are created.
        for(int i=1; i<Globals.dashNumber; ++i) {
            // Prepare the resized dash array.
            dashArrayStretched = "";
            for(int j=0; j<Globals.dash[i].length;++j) {
                dashArrayStretched+=(Globals.dash[i][j]*(float)u/2.0f);
                if(j<Globals.dash[i].length-1) {
                    dashArrayStretched+=" ";
                }
            }
            sDash[i]="["+dashArrayStretched+"]";
        }
    }

    /** Set the "phase" in output units of the dashing style.
        For example, if a dash style is composed by a line followed by a space
        of equal size, a phase of 0 indicates that the dash starts with the
        line.
        @param p the phase, in output units.
    */
    public void setDashPhase(float p)
    {
        dashPhase=p;
    }

    /** Constructor
        @param f the File object in which the export should be done.
        @param gg the graphic object. Mainly required to calculate text sizes
            and calculating text positions.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public ExportPDF (File f, GraphicsInterface gg) throws IOException
    {
        gi=gg;
        dashPhase=0;

        fstream =  new OutputStreamWriter(new FileOutputStream(f), encoding);

        temp = File.createTempFile("real",".howto");
        temp.deleteOnExit();

        fstreamt =  new OutputStreamWriter(new FileOutputStream(temp),
            encoding);
        obj_PDF = new String[numOfObjects];
        dt=new DecoratedText(this);
    }

    /** Called at the beginning of the export phase. Ideally, in this routine
        there should be the code to write the header of the file on which
        the drawing should be exported.

        @param totalSize the size of the image. Useful to calculate for example
        the bounding box.
        @param la a vector describing the attributes of each layer.
        @param grid the grid size. This is useful when exporting to another
            drawing program having some kind of grid concept. You might use
            this value to synchronize FidoCadJ's grid with the one used by
            the target.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportStart(DimensionG totalSize, List<LayerDesc> la,
        int grid)
        throws IOException
    {
        initGlyphList();

        // We need to save layers informations, since we will use them later.

        layerV=la;
        out = new BufferedWriter(fstream);

        fontWarning=false;
        // To track the block sizes, we will first write all graphic elements
        // in a temporary file, whose stream is called outt instead of out.
        // At the end, the contents of the temporary file will be copied in
        // the definitive destination file.

        outt = new BufferedWriter(fstreamt);

        // A header of the EPS file

        // 200 dpi is the internal resolution of FidoCadJ
        // 72 dpi is the internal resolution of the Postscript coordinates

        double resMult=200.0/72.0;


        int border = 5;

        head = "%PDF-1.4\n";

        // Object 5 is a single page of the appropriate size, containing
        // as a child object 4.
        obj_PDF[5]= "5 0 obj\n"+
                "  <</Kids [4 0 R ]\n"+
                "    /Count 1\n"+
                "    /Type /Pages\n"+
                "    /MediaBox [ 0 0  "+
                (int)(totalSize.width/resMult+1+border)+" "+
                (int)(totalSize.height/resMult+1+border)+" ]\n"+
                "  >> endobj\n";

        // Since in a postscript drawing, the origin is at the bottom left,
        // we introduce a coordinate transformation to have it at the top
        // left of the drawing.

        actualColor = null;
        actualWidth = -1;
        outt.write("   1 0 0 1 0 "+(totalSize.height/resMult+border)+
            "  cm\n");

        outt.write("  "+(1/resMult)+" 0  0 "+(-1/resMult)+" 0 0  cm\n");

        outt.write("1 J\n");

    }

    /** Init the list of glyphs by reading the glyphlist.txt file if it is
        available.
    */
    private void initGlyphList() throws IOException
    {
        // The glyphlist.txt file has about 4300 lines, therefore starting
        // with a size of 5000 seems reasonable.
        unicodeToGlyph = new HashMap<Integer, String>(5000);

        // 128 chars for the moment will suffice.
        uncodeCharsNeeded = new HashMap<Integer, Integer>(128);

        // The mapping of Unicode chars will be done starting from code 128
        // up to 256. For the moment it will suffice.
        unicodeCharIndex=127;

        // Read the glyphlist.txt file and store its contents in the hash
        // map for easy retrieval during the calculation of encoding needs.
        BufferedReader br=null;
        InputStreamReader isr=null;
        try{
            isr = new InputStreamReader(
                      getClass().getResourceAsStream("glyphlist.txt"),
                      encoding);
            br = new BufferedReader(isr);
            if(isr==null) {

            }

            String line = br.readLine();
            String glyph;
            Integer code;
            String codeStr;
            int p;
            int q;
            while (line != null) {
                if(!line.startsWith("#")) {
                    p=line.indexOf(';');
                    q=line.indexOf(' ');
                    glyph=line.substring(0,p);
                    if(q<0) {
                        codeStr=line.substring(p+1);
                    } else {
                        codeStr=line.substring(p+1,q);
                    }
                    code=Integer.decode("0x"+codeStr);
                    unicodeToGlyph.put(code, glyph);
                }
                line = br.readLine();
            }
        } catch(IOException ee) {
            assignStandardGlyph();
        } catch(NullPointerException ee) {
            assignStandardGlyph();
        } finally {
            if (br!=null) {
                br.close();
            }
            if (isr!=null) {
                isr.close();
            }
        }
    }


    /** This is called when it is impossible to access to glyphlist.txt.
        In this case, UTF-8 is not mapped and all extended characters will be
        not rendered correctly.
    */
    private void assignStandardGlyph()
    {
        System.err.println("We could not access glyphlist.txt. A standard"+
            " matching of glyphs is attempted.");
        for(int i=32; i<128; ++i) {
            unicodeToGlyph.put(Integer.valueOf(i), ""+i);
        }
    }

    /** Called at the end of the export phase.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportEnd()
        throws IOException
    {
        //DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        //Date date = new Date();

        outt.close();
        fileLength=temp.length();
        writeFontDescription();

        obj_PDF[8]="8 0 obj\n" +
                "  <<\n" +
                "    /Length "+fileLength+"\n" +
                "  >>\n"+
                "  stream\n";

        out.write(head+obj_PDF[5]+obj_PDF[6]+obj_PDF[7]+obj_PDF[8]);

        // Object 4 is a font container. The objects corresponding to fonts
        // F1--F8 will be objects 6 to 14 (except object 8).
        obj_PDF[4] = "4 0 obj\n"+
                "<< \n"+
                "  /Type /Page\n"+
                "  /Parent 5 0 R\n"+
                "  /Resources <<\n"+
                "  /Font <<\n"+
                "  /F1 6 0 R\n"+
                "  /F2 7 0 R\n"+
                "  /F3 9 0 R\n"+
                "  /F4 10 0 R\n"+
                "  /F5 11 0 R\n"+
                "  /F6 12 0 R\n"+
                "  /F7 13 0 R\n"+
                "  /F8 14 0 R\n"+
                "  /F9 15 0 R\n"+
                ">>\n"+
                "/ProcSet 2 0 R\n"+
                ">>\n"+
                "  /Contents 8 0 R\n"+
                ">>\n"+
                "endobj\n";

        obj_PDF[2] =    "2 0 obj\n"+
                "[ /PDF /Text  ]\n"+
                "endobj\n";

        // Object 1 is just a header
        obj_PDF[1]="1 0 obj\n"+
                "<<\n"+
                "  /Creator (FidoCadJ"+Globals.version+
                ", PDF export filter by Davide Bucci)\n"+
        //      "  /CreationDate ("+dateFormat.format(date)+")\n"+
                "  /Author ("+System.getProperty("user.name")+")\n"+
                "  /Producer (FidoCadJ)\n"+
                ">>\n"+
                "endobj\n";

        obj_PDF[3] = "3 0 obj\n"+
                "<<\n"+
                "  /Pages 5 0 R\n"+
                "  /Type /Catalog\n"+
                ">>\n"+
                "endobj\n";

        BufferedReader br= new BufferedReader(new InputStreamReader(
                      new FileInputStream(temp), encoding));
        try{
            String line = br.readLine();
            while (line != null) {
                out.write(line+"\n");
                line = br.readLine();
            }
        } finally {
            br.close();
        }
        closeObject="endstream\n"+"endobj\n";

        out.write(closeObject+obj_PDF[4]+obj_PDF[2]+obj_PDF[1]+
            obj_PDF[3]+obj_PDF[9]+obj_PDF[10]+obj_PDF[11]+obj_PDF[12]+
            obj_PDF[13]+obj_PDF[14]+obj_PDF[15]+obj_PDF[16]);

        writeCrossReferenceTable();

        out.close();

        if (fontWarning) {
            if (java.awt.GraphicsEnvironment.isHeadless()) {
                System.err.println("Some fonts were not available!");
            } else {
                JOptionPane.showMessageDialog(null,
                    Globals.messages.getString("PDF_Font_error"));
            }
        }
    }

    /** This routine creates the eight definition of the fonts currently
        available:
        F1 - Courier
        F2 - Courier Bold
        F3 - Times Roman
        F4 - Times Bold
        F5 - Helvetica
        F6 - Helvetica Bold
        F7 - Symbol
        F8 - Symbol
        F9 - Undefinite
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    private void writeFontDescription() throws IOException
    {
        obj_PDF[6]= "6 0 obj\n" +
                "  <<   /Type /Font\n" +
                "    /Subtype /Type1\n" +
                "    /BaseFont /Courier\n" +
                calcWidthsIndex("Courier")+
                "    /Encoding 16 0 R\n" +
                "  >> endobj\n";
        obj_PDF[7]="7 0 obj\n" +
                "  <<   /Type /Font\n" +
                "    /Subtype /Type1\n" +
        //      "    /Name /F2\n" +
                "    /BaseFont /Courier-Bold\n" +
                calcWidthsIndex("Courier-Bold")+
                "    /Encoding  16 0 R\n" +
                "  >> endobj\n";

        obj_PDF[9]="9 0 obj\n" +
                "  <<   /Type /Font\n" +
                "    /Subtype /Type1\n" +
        //      "    /Name /F3\n" +
                "    /BaseFont /Times-Roman\n" +
                calcWidthsIndex("Times-Roman")+
                "    /Encoding  16 0 R\n" +
                "  >> endobj\n";
        obj_PDF[10]="10 0 obj\n" +
                "  <<   /Type /Font\n" +
                "    /Subtype /Type1\n" +
        //      "    /Name /F4\n" +
                "    /BaseFont /Times-Bold\n" +
                calcWidthsIndex("Times-Bold")+
                "    /Encoding  16 0 R\n" +
                "  >> endobj\n";
        obj_PDF[11]="11 0 obj\n" +
                "  <<   /Type /Font\n" +
                "    /Subtype /Type1\n" +
        //      "    /Name /F5\n" +
                "    /BaseFont /Helvetica\n" +
                calcWidthsIndex("Helvetica")+
                "    /Encoding  16 0 R\n" +
                "  >> endobj\n";
        obj_PDF[12]="12 0 obj\n" +
                "  <<   /Type /Font\n" +
                "    /Subtype /Type1\n" +
        //      "    /Name /F6\n" +
                "    /BaseFont /Helvetica-Bold\n" +
                calcWidthsIndex("Helvetica-Bold")+
                "    /Encoding  16 0 R\n" +
                "  >> endobj\n";
        obj_PDF[13]="13 0 obj\n" +
                "  <<   /Type /Font\n" +
                "    /Subtype /Type1\n" +
        //      "    /Name /F7\n" +
                "    /BaseFont /Symbol\n" +
                calcWidthsIndex("Symbol")+
                "    /Encoding  16 0 R\n" +
                "  >> endobj\n";
        obj_PDF[14]="14 0 obj\n" +
                "  <<   /Type /Font\n" +
                "    /Subtype /Type1\n" +
                calcWidthsIndex("Symbol")+
        //      "    /Name /F8\n" +
                "    /BaseFont /Symbol\n" +
                "    /Encoding  16 0 R\n" +
                "  >> endobj\n";

        obj_PDF[15]="15 0 obj\n" +
                "  <<   /Type /Font\n" +
                "    /Subtype /Type1\n" +
                calcWidthsIndex(userfont)+
                "    /BaseFont /"+userfont+"\n" +
                "    /Encoding  16 0 R\n" +
                "  >> endobj\n";

        obj_PDF[16]="16 0 obj\n"+
                "   <<  /Type /Encoding\n"+
                "    /BaseEncoding /WinAnsiEncoding\n"+
                "    /Differences [";

        for (Integer code : uncodeCharsNeeded.keySet()) {
            obj_PDF[16]+=""+code+"/"+unicodeToGlyph.get(
                uncodeCharsNeeded.get(code))+" ";
        }
        obj_PDF[16]+="]\n  >> endobj\n";
    }

    private String calcWidthsIndex(String font)
    {
        StringBuilder charWidths=new StringBuilder();

        gi.setFont(font, 24);
        int basewidth=gi.getStringWidth("M");

        charWidths.append("    /FirstChar 32\n");
        charWidths.append("    /LastChar ");
        charWidths.append(unicodeCharIndex);
        charWidths.append("\n");
        charWidths.append("    /Widths [");

        int calcwidth;
        int mwidth=900;

        for (int i=32; i<128;++i) {
            calcwidth=mwidth*gi.getStringWidth(""+(char)i)/basewidth;
            charWidths.append(calcwidth);
            charWidths.append(" ");
        }

        for (Integer code : uncodeCharsNeeded.keySet()) {
            calcwidth=mwidth*gi.getStringWidth(""+
                (char)uncodeCharsNeeded.get(code).intValue())/basewidth;
            charWidths.append(calcwidth);
            charWidths.append(" ");
        }
        charWidths.append("]\n");

        return charWidths.toString();
    }


    /** Here we create the cross reference table for the PDF file, as well as
        the trailer.
        Order of the objects:
        header, 5, 6, 7, 8, file, closeObject, 4, 2, 1, 3

        This is probably among the most boring code in FidoCadJ.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    private void writeCrossReferenceTable()  throws IOException
    {
        out.write("xref \n"+
            "0 15\n"+
            "0000000000 65535 f \n"+          // header
            addLeadZeros(head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length()+
            obj_PDF[8].length()+
            fileLength+
            closeObject.length()+
            obj_PDF[4].length()+
            obj_PDF[2].length())+
            " 00000 n \n"+        // 1
            addLeadZeros(head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length()+
            obj_PDF[8].length()+
            fileLength +
            closeObject.length()+
            obj_PDF[4].length())+
            " 00000 n \n"+        // 2
            addLeadZeros(head.length() +
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length()+
            obj_PDF[8].length()+
            fileLength +
            closeObject.length()+
            obj_PDF[4].length()+
            obj_PDF[2].length()+
            obj_PDF[1].length())+
            " 00000 n \n"+        // 3

            addLeadZeros(head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length()+
            obj_PDF[8].length()+
            fileLength +
            closeObject.length())+
            " 00000 n \n"+        // 4

            addLeadZeros(head.length())+
            " 00000 n \n"+        // 5

            addLeadZeros(head.length()+
            obj_PDF[5].length())+
            " 00000 n \n"+        // 6

            addLeadZeros(head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length())+
            " 00000 n \n"+        // 7

            addLeadZeros(head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length())+
            " 00000 n \n"+        // 8

            addLeadZeros(head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length()+
            obj_PDF[8].length()+
            fileLength +
            closeObject.length()+
            obj_PDF[4].length()+
            obj_PDF[2].length()+
            obj_PDF[1].length()+
            obj_PDF[3].length())+
            " 00000 n \n"+        // 9

            addLeadZeros(head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length()+
            obj_PDF[8].length()+
            fileLength +
            closeObject.length()+
            obj_PDF[4].length()+
            obj_PDF[2].length()+
            obj_PDF[1].length()+
            obj_PDF[3].length()+
            obj_PDF[9].length())+
            " 00000 n \n"+        // 10

            addLeadZeros(head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length()+
            obj_PDF[8].length()+
            fileLength +
            closeObject.length()+
            obj_PDF[4].length()+
            obj_PDF[2].length()+
            obj_PDF[1].length()+
            obj_PDF[3].length()+
            obj_PDF[9].length()+
            obj_PDF[10].length())+
            " 00000 n \n"+        // 11


            addLeadZeros(head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length()+
            obj_PDF[8].length()+
            fileLength +
            closeObject.length()+
            obj_PDF[4].length()+
            obj_PDF[2].length()+
            obj_PDF[1].length()+
            obj_PDF[3].length()+
            obj_PDF[9].length()+
            obj_PDF[10].length()+
            obj_PDF[11].length())+
            " 00000 n \n"+        // 12


            addLeadZeros(head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length()+
            obj_PDF[8].length()+
            fileLength +
            closeObject.length()+
            obj_PDF[4].length()+
            obj_PDF[2].length()+
            obj_PDF[1].length()+
            obj_PDF[3].length()+
            obj_PDF[9].length()+
            obj_PDF[10].length()+
            obj_PDF[11].length()+
            obj_PDF[12].length())+
            " 00000 n \n"+        // 13

            addLeadZeros(head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length()+
            obj_PDF[8].length()+
            fileLength +
            closeObject.length()+
            obj_PDF[4].length()+
            obj_PDF[2].length()+
            obj_PDF[1].length()+
            obj_PDF[3].length()+
            obj_PDF[9].length()+
            obj_PDF[10].length()+
            obj_PDF[11].length()+
            obj_PDF[12].length()+
            obj_PDF[13].length())+
            " 00000 n \n"+        // 14

            addLeadZeros(head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length()+
            obj_PDF[8].length()+
            fileLength +
            closeObject.length()+
            obj_PDF[4].length()+
            obj_PDF[2].length()+
            obj_PDF[1].length()+
            obj_PDF[3].length()+
            obj_PDF[9].length()+
            obj_PDF[10].length()+
            obj_PDF[11].length()+
            obj_PDF[12].length()+
            obj_PDF[13].length()+
            obj_PDF[14].length())+
            " 00000 n \n"+        // 15

            addLeadZeros(head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length()+
            obj_PDF[8].length()+
            fileLength +
            closeObject.length()+
            obj_PDF[4].length()+
            obj_PDF[2].length()+
            obj_PDF[1].length()+
            obj_PDF[3].length()+
            obj_PDF[9].length()+
            obj_PDF[10].length()+
            obj_PDF[11].length()+
            obj_PDF[12].length()+
            obj_PDF[13].length()+
            obj_PDF[14].length()+
            obj_PDF[15].length())+
            " 00000 n \n");       // 16

        out.write("trailer\n"+
            "<<\n"+
                "  /Size 16\n"+
                "  /Root 3 0 R\n"+
                "  /Info 1 0 R\n"+
                ">>\n"+
                "startxref\n"+
                (head.length()+
            obj_PDF[5].length()+
            obj_PDF[6].length()+
            obj_PDF[7].length()+
            obj_PDF[8].length()+
            fileLength +
            closeObject.length()+
            obj_PDF[4].length()+
            obj_PDF[2].length()+
            obj_PDF[1].length()+
            obj_PDF[3].length()+
            obj_PDF[9].length()+
            obj_PDF[10].length()+
            obj_PDF[11].length()+
            obj_PDF[12].length()+
            obj_PDF[13].length()+
            obj_PDF[14].length()+
            obj_PDF[15].length()+
            obj_PDF[16].length())+
            "\n%%EOF");

    }



    /** Called when exporting an Advanced Text primitive.

        @param x the x position of the beginning of the string to be written.
        @param y the y position of the beginning of the string to be written.
        @param sizex the x size of the font to be used.
        @param sizey the y size of the font to be used.
        @param fontname the font to be used.
        @param isBold true if the text should be written with a boldface font.
        @param isMirrored true if the text should be mirrored.
        @param isItalic true if the text should be written with an italic font.
        @param orientation angle of orientation (degrees).
        @param layer the layer that should be used.
        @param textT the text that should be written.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportAdvText (int x, int y, double sizex, double sizey,
        String fontname, boolean isBold, boolean isMirrored, boolean isItalic,
        int orientation, int layer, String textT)
        throws IOException
    {
        String text=textT;
        if ("".equals(text)) {
            return;
        }

        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();

        checkColorAndWidth(c, .33);

        outt.write("BT\n");
        int ys = (int)(sizex*12.0/7.0+.5);


        if("Courier".equals(fontname) || "Courier New".equals(fontname)) {
            if(isBold) {
                currentFont="/F2";
            } else {
                currentFont="/F1";
            }
        } else if("Times".equals(fontname) ||
            "Times New Roman".equals(fontname) ||
            "Times Roman".equals(fontname))
        {
            if(isBold) {
                currentFont="/F4";
            } else {
                currentFont="/F3";
            }
        } else if("Helvetica".equals(fontname) ||
            "Arial".equals(fontname))
        {
            if(isBold) {
                currentFont="/F6";
            } else {
                currentFont="/F5";
            }
        } else if("Symbol".equals(fontname)) {
            if(isBold) {
                currentFont="/F8";
            } else {
                currentFont="/F7";
            }
        } else {
            fontWarning = true;
            userfont=fontname;
            currentFont="/F9";
        }
        outt.write(currentFont+" "+ys+" Tf\n");
        currentFontSize=(float)ys;
        outt.write("q\n");
        outt.write("  1 0 0 1 "+ Globals.roundTo(x)+" "+ Globals.roundTo(y)+
            " cm\n");
        textx=x;
        texty=y;
        if(orientation !=0) {
            double alpha=(-orientation)/180.0*Math.PI;
            outt.write("  "+Globals.roundTo(Math.cos(alpha))+" "
                + Globals.roundTo(Math.sin(alpha))+ " "
                + Globals.roundTo(-Math.sin(alpha))+
                " "+Globals.roundTo(Math.cos(alpha))+" 0 0 cm\n");
        }
        if(isMirrored) {
            outt.write("  -1 0 0 -1 0 0 cm\n");
        } else {
            outt.write("  1 0 0 -1 0 0 cm\n");
        }
        double ratio;

        if((int)(sizey/sizex) == 10/7){
            ratio = 1.0;
        } else {
            ratio=(double)sizey/(double)sizex*22.0/40.0;
        }
        outt.write("  1 0 0 "+Globals.roundTo(ratio)+ " 0 "+
                (-ys*ratio*0.8)+" cm\n");
        dt.drawString(text,x,y);
        outt.write("Q\nET\n");
    }

    /** Called when exporting a Bézier primitive.

        @param x1 the x position of the first point of the trace.
        @param y1 the y position of the first point of the trace.
        @param x2 the x position of the second point of the trace.
        @param y2 the y position of the second point of the trace.
        @param x3 the x position of the third point of the trace.
        @param y3 the y position of the third point of the trace.
        @param x4 the x position of the fourth point of the trace.
        @param y4 the y position of the fourth point of the trace.
        @param layer the layer that should be used.

                // from 0.22.1

        @param arrowStart specify if an arrow is present at the first point.
        @param arrowEnd specify if an arrow is present at the second point.
        @param arrowStyle the style of the arrow.
        @param arrowLength total lenght of arrows (if present).
        @param arrowHalfWidth half width of arrows (if present).
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportBezier (int x1, int y1,
        int x2, int y2,
        int x3, int y3,
        int x4, int y4,
        int layer,
        boolean arrowStart,
        boolean arrowEnd,
        int arrowStyle,
        int arrowLength,
        int arrowHalfWidth,
        int dashStyle,
        double strokeWidth)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();
        checkColorAndWidth(c, strokeWidth);
        registerDash(dashStyle);

        if (arrowStart) {
            PointPr p=exportArrow(x1, y1, x2, y2, arrowLength,
                arrowHalfWidth, arrowStyle);
            // This fixes issue #172
            // If the arrow length is negative, the arrow extends
            // outside the line, so the limits must not be changed.
            if(arrowLength>0) {
                x1=(int)Math.round(p.x);
                y1=(int)Math.round(p.y);
            }
        }
        if (arrowEnd) {
            PointPr p=exportArrow(x4, y4, x3, y3, arrowLength,
                arrowHalfWidth, arrowStyle);
            // Fix #172
            if(arrowLength>0) {
                x4=(int)Math.round(p.x);
                y4=(int)Math.round(p.y);
            }
        }

        outt.write(""+x1+" "+y1+" m \n");
        outt.write(""+x2+" "+y2+" "+x3+" "+y3+" "+x4+" "+y4+" c S\n");

    }

    /** Called when exporting a Connection primitive.
        @param x the x position of the position of the connection.
        @param y the y position of the position of the connection.
        @param layer the layer that should be used.
        @param nodeSize the size of the connection, in logical units.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportConnection (int x, int y, int layer, double nodeSize)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();

        checkColorAndWidth(c, .33);

        ellipse(x-nodeSize/2.0, y-nodeSize/2.0,
                x+nodeSize/2.0, y+nodeSize/2.0, true);
    }

    /** Called when exporting a Line primitive.

        @param x1 the x position of the first point of the segment.
        @param y1 the y position of the first point of the segment.
        @param x2 the x position of the second point of the segment.
        @param y2 the y position of the second point of the segment.
        @param layer the layer that should be used.

        // from 0.22.1

        @param arrowStart specify if an arrow is present at the first point.
        @param arrowEnd specify if an arrow is present at the second point.
        @param arrowStyle the style of the arrow.
        @param arrowLength total lenght of arrows (if present).
        @param arrowHalfWidth half width of arrows (if present).
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportLine (double x1, double y1,
        double x2, double y2,
        int layer,
        boolean arrowStart,
        boolean arrowEnd,
        int arrowStyle,
        int arrowLength,
        int arrowHalfWidth,
        int dashStyle,
        double strokeWidth)
        throws IOException
    {
        double xstart=x1;
        double ystart=y1;
        double xend=x2;
        double yend=y2;

        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();

        checkColorAndWidth(c, strokeWidth);
        registerDash(dashStyle);

        if (arrowStart) {
            PointPr p=exportArrow(x1, y1, x2, y2, arrowLength,
                arrowHalfWidth, arrowStyle);
            // This fixes issue #172
            // If the arrow length is negative, the arrow extends
            // outside the line, so the limits must not be changed.
            if(arrowLength>0) {
                xstart=p.x;
                ystart=p.y;
            }
        }
        if (arrowEnd) {
            PointPr p=exportArrow(x2, y2, x1, y1, arrowLength,
                arrowHalfWidth, arrowStyle);
            // Fix #172
            if(arrowLength>0) {
                xend=p.x;
                yend=p.y;
            }
        }

        outt.write("  "+xstart+" "+ystart+" m "+ xend+" "+yend+" l S\n");
    }

    /** Called when exporting a Macro call.
        This function can just return false, to indicate that the macro should
        be rendered by means of calling the other primitives. Please note that
        a macro does not have a reference layer, since it is defined by its
        components.

        @param x the x position of the position of the macro.
        @param y the y position of the position of the macro.
        @param isMirrored true if the macro is mirrored.
        @param orientation the macro orientation in degrees.
        @param macroName the macro name.
        @param macroDesc the macro description, in the FidoCad format.
        @param name the shown name.
        @param xn coordinate of the shown name.
        @param yn coordinate of the shown name.
        @param value the shown value.
        @param xv coordinate of the shown value.
        @param yv coordinate of the shown value.
        @param font the used font.
        @param fontSize the size of the font to be used.
        @param m the library.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
        @return false if the macro should be split into primitives or true
            if the export is handled entirely by this function.
    */
    public boolean exportMacro(int x, int y, boolean isMirrored,
        int orientation, String macroName, String macroDesc,
        String name, int xn, int yn, String value, int xv, int yv, String font,
        int fontSize, Map m)
        throws IOException
    {
        // The macro will be expanded into primitives.
        return false;
    }

    /** Called when exporting an Oval primitive. Specify the bounding box.

        @param x1 the x position of the first corner
        @param y1 the y position of the first corner
        @param x2 the x position of the second corner
        @param y2 the y position of the second corner.
        @param isFilled it is true if the oval should be filled.
        @param layer the layer that should be used.
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportOval(int x1, int y1, int x2, int y2,
        boolean isFilled, int layer, int dashStyle, double strokeWidth)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();
        checkColorAndWidth(c, strokeWidth);
        registerDash(dashStyle);
        ellipse(x1,y1, x2, y2, isFilled);
    }

    /** Called when exporting a PCBLine primitive.

        @param x1 the x position of the first point of the segment.
        @param y1 the y position of the first point of the segment.
        @param x2 the x position of the second point of the segment.
        @param y2 the y position of the second point of the segment.
        @param width the width ot the line.
        @param layer the layer that should be used.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportPCBLine(int x1, int y1, int x2, int y2, int width,
        int layer)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();
        checkColorAndWidth(c, width);
        registerDash(0);

        outt.write("  "+x1+" "+y1+" m "+
            x2+" "+y2+" l S\n");
    }

    /** Called when exporting a PCBPad primitive.

        @param x the x position of the pad.
        @param y the y position of the pad.
        @param style the style of the pad (0: oval, 1: square, 2: rounded
            square).
        @param six the x size of the pad.
        @param siy the y size of the pad.
        @param indiam the hole internal diameter.
        @param layer the layer that should be used.
        @param onlyHole true if only the hole has to be exported.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportPCBPad(int x, int y, int style, int six, int siy,
        int indiam, int layer, boolean onlyHole)
        throws IOException
    {
        double xdd;
        double ydd;

        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();

        checkColorAndWidth(c, 0.33);

        // At first, draw the pad...
        if(!onlyHole) {
            switch (style) {
                case 2: // Rounded pad
                    roundRect(x-six/2.0, y-siy/2.0,
                            six, siy, 4, true);
                    break;
                case 1: // Square pad
                    xdd=(double)x-six/2.0;
                    ydd=(double)y-siy/2.0;
                    outt.write(""+xdd+" "+ydd+" m\n");
                    outt.write(""+(xdd+six)+" "+ydd+" l\n");
                    outt.write(""+(xdd+six)+" "+(ydd+siy)+" l\n");
                    outt.write(""+xdd+" "+(ydd+siy)+" l\n");
                    outt.write("B\n");

                    break;
                case 0: // Oval pad
                default:
                    ellipse(x-six/2.0, y-siy/2.0,
                            x+six/2.0, y+siy/2.0, true);

                    outt.write("f\n");
                    break;
            }
        }
        // ... then, drill the hole!
        checkColorAndWidth(c.white(), .33);

        ellipse(x-indiam/2.0, y-indiam/2.0,
                x+indiam/2.0, y+indiam/2.0, true);
        outt.write("f\n");
    }

    /** Called when exporting a Polygon primitive.

        @param vertices array containing the position of each vertex.
        @param nVertices number of vertices.
        @param isFilled true if the polygon is filled.
        @param layer the layer that should be used.
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportPolygon(PointDouble[] vertices, int nVertices,
        boolean isFilled, int layer, int dashStyle, double strokeWidth)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();

        if (nVertices<1) {
            return;
        }

        checkColorAndWidth(c, strokeWidth);
        registerDash(dashStyle);

        outt.write("  "+vertices[0].x+" "+vertices[0].y+" m\n");

        for (int i=1; i<nVertices; ++i) {
            outt.write("  "+vertices[i].x+" "+vertices[i].y+" l\n");
        }
        if(isFilled) {
            outt.write("  f*\n");
        } else {
            outt.write("  s\n");
        }
    }
    /** Called when exporting a Curve primitive.

        @param vertices array containing the position of each vertex.
        @param nVertices number of vertices.
        @param isFilled true if the polygon is filled.
        @param isClosed true if the curve is closed.
        @param layer the layer that should be used.
        @param arrowStart specify if an arrow is present at the first point.
        @param arrowEnd specify if an arrow is present at the second point.
        @param arrowStyle the style of the arrow.
        @param arrowLength total lenght of arrows (if present).
        @param arrowHalfWidth half width of arrows (if present).
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.

        @return false if the curve should be rendered using a polygon, true
            if it is handled by the function.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public boolean exportCurve(PointDouble[] vertices, int nVertices,
        boolean isFilled, boolean isClosed, int layer,
        boolean arrowStart,
        boolean arrowEnd,
        int arrowStyle,
        int arrowLength,
        int arrowHalfWidth,
        int dashStyle,
        double strokeWidth)
        throws IOException
    {
        return false;
    }

    /** Called when exporting a Rectangle primitive.

        @param x1 the x position of the first corner.
        @param y1 the y position of the first corner.
        @param x2 the x position of the second corner.
        @param y2 the y position of the second corner.
        @param isFilled it is true if the rectangle should be filled;

        @param layer the layer that should be used.
        @param dashStyle dashing style.
        @param strokeWidth the width of the pen to be used when drawing.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportRectangle(int x1, int y1, int x2, int y2,
        boolean isFilled, int layer, int dashStyle, double strokeWidth)
        throws IOException
    {

        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();

        checkColorAndWidth(c, strokeWidth);
        registerDash(dashStyle);

        outt.write("  "+x1+" "+y1+" m\n");
        outt.write("  "+x2+" "+y1+" l\n");
        outt.write("  "+x2+" "+y2+" l\n");
        outt.write("  "+x1+" "+y2+" l\n");
        if(isFilled) {
            outt.write("f\n");
        } else {
            outt.write("s\n");
        }
    }

    /** Called when exporting an embedded raster Image primitive. This
        hand-written exporter builds its cross-reference table with a
        fixed count of objects and manually-tracked byte offsets (see
        numOfObjects): adding a new indirect object (an embedded Image
        XObject) would risk corrupting that structure in a way this
        exporter has no way to self-validate. A dashed placeholder
        rectangle with a "[immagine]" label is drawn instead, using only
        content-stream operators, which do not touch the object/xref
        bookkeeping at all.

        @param x1 the x position of the first corner.
        @param y1 the y position of the first corner.
        @param x2 the x position of the second corner.
        @param y2 the y position of the second corner.
        @param layer the layer that should be used.
        @param opacity the opacity, between 0.0 (transparent) and 1.0
            (fully opaque).
        @param blackAndWhite true if the image should be shown in black
            and white.
        @param mimeType the mime subtype of the original file (e.g. "png").
        @param base64Data the base64-encoded bytes of the original file.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public void exportImage(int x1, int y1, int x2, int y2, int layer,
        double opacity, boolean blackAndWhite, String mimeType,
        String base64Data)
        throws IOException
    {
        LayerDesc l=(LayerDesc)layerV.get(layer);
        ColorInterface c=l.getColor();

        checkColorAndWidth(c, 0.33);
        registerDash(1);

        outt.write("  "+x1+" "+y1+" m\n");
        outt.write("  "+x2+" "+y1+" l\n");
        outt.write("  "+x2+" "+y2+" l\n");
        outt.write("  "+x1+" "+y2+" l\n");
        outt.write("s\n");
        registerDash(0);

        int cx=(x1+x2)/2;
        int cy=(y1+y2)/2;
        int ys=20;

        outt.write("BT\n");
        currentFont="/F5";
        outt.write(currentFont+" "+ys+" Tf\n");
        currentFontSize=(float)ys;
        outt.write("q\n");
        outt.write("  1 0 0 1 "+ Globals.roundTo(cx)+" "+ Globals.roundTo(cy)+
            " cm\n");
        textx=cx;
        texty=cy;
        outt.write("  1 0 0 -1 0 0 cm\n");
        dt.drawString("[immagine]", cx, cy);
        outt.write("Q\nET\n");
    }

    private void roundRect (double x1, double y1, double w, double h,
        double r, boolean filled)
        throws IOException
    {
        outt.write(""+ (x1+r) + " " +y1+" m\n");
        outt.write(""+ (x1+w-r) + " " +y1+" l\n");
        outt.write(""+ (x1+w) + " " +y1+" "+ (x1+w) + " " +(y1+r)+" y\n");

        outt.write(""+ (x1+w) + " " +(y1+h-r)+" l\n");
        outt.write(""+ (x1+w) + " " +(y1+h)+" "+ (x1+w-r) + " " +(y1+h)+" y\n");

        outt.write(""+ (x1+r) + " " +(y1+h)+" l\n");
        outt.write(""+ x1 + " " +(y1+h)+" "+ x1 + " " +(y1+h-r)+" y\n");

        outt.write(""+ x1 + " " +(y1+r)+" l\n");
        outt.write(""+ x1 + " " +y1+" "+ (x1+r) + " " +y1+" y \n");

        outt.write("  "+(filled?"f\n":"s\n"));
    }

    /** TODO: I am sure that a better solution for drawing ellipses may be
        found. This code is pretty inefficient.
    */
    private void ellipse(double x1, double y1, double x2, double y2,
        boolean filled)
        throws IOException
    {
        double cx = (x1+x2)/2.0;
        double cy = (y1+y2)/2.0;

        double rx = Math.abs(x2-x1)/2.0;
        double ry = Math.abs(y2-y1)/2.0;

        final int nMAX=32;

        double xC;
        double yC;

        double xD;
        double yD;

        double alpha;

        final double tt = 1.01;

        outt.write("  "+ Globals.roundTo(cx+rx)+" "+ Globals.roundTo(cy)+
            " m\n");

        for(int i=0; i<nMAX; ++i) {
            alpha = 2.0*Math.PI*(double)i/(double)nMAX;
            alpha += 2.0*Math.PI/(double)nMAX/3.0;
            alpha += 2.0*Math.PI/(double)nMAX/3.0;

            xC = cx + tt*rx * Math.cos(alpha);
            yC = cy + tt*ry * Math.sin(alpha);

            alpha += 2.0*Math.PI/(double)nMAX/3.0;

            xD = cx + rx * Math.cos(alpha);
            yD = cy + ry * Math.sin(alpha);

            outt.write(Globals.roundTo(xC)+" "+
                Globals.roundTo(yC)+" "+ Globals.roundTo(xD)+" "+
                Globals.roundTo(yD)+" y\n");
        }
        outt.write("  "+(filled?"f\n":"s\n"));
    }

    private String addLeadZeros(long n)
    {
        String s=""+n;

        // simple and inefficient.
        while (s.length()<10) {
            s="0"+s;
        }

        return s;
    }

    private void checkColorAndWidth(ColorInterface c, double wl)
        throws IOException
    {
        if(!c.equals(actualColor)) {
            outt.write("  "+Globals.roundTo(c.getRed()/255.0)+" "+
                Globals.roundTo(c.getGreen()/255.0)+ " "
                +Globals.roundTo(c.getBlue()/255.0)+    " rg\n");
            outt.write("  "+Globals.roundTo(c.getRed()/255.0)+" "+
                Globals.roundTo(c.getGreen()/255.0)+ " "
                +Globals.roundTo(c.getBlue()/255.0)+    " RG\n");
            actualColor=c;
        }
        if(wl != actualWidth) {
            outt.write("  " +wl+" w\n");
            actualWidth = wl;
        }
    }

    private void registerDash(int dashStyle)
        throws IOException
    {
        if(currentDash!=dashStyle ||currentPhase!=dashPhase) {
            currentDash=dashStyle;
            currentPhase=dashPhase;
            if(dashStyle==0) {
                outt.write("[] 0 d\n");
            } else {
                outt.write(""+sDash[dashStyle]+" "+dashPhase+" d\n");
            }
        }
    }

    /** Called when exporting an arrow.
        @param x position of the tip of the arrow.
        @param y position of the tip of the arrow.
        @param xc direction of the tip of the arrow.
        @param yc direction of the tip of the arrow.
        @param l length of the arrow.
        @param h width of the arrow.
        @param style style of the arrow.
        @return the coordinates of the base of the arrow.
        @throws IOException if a disaster happens, i.e. a file can not be
            accessed.
    */
    public PointPr exportArrow(double x, double y, double xc, double yc,
        double l, double h,
        int style)
        throws IOException
    {
        double alpha;
        double x0;
        double y0;
        double x1;
        double y1;
        double x2;
        double y2;

        // At first we need the angle giving the direction of the arrow
        // a little bit of trigonometry :-)

        if (x==xc) {
            alpha = Math.PI/2.0+(y-yc<0.0?0.0:Math.PI);
        } else {
            alpha = Math.atan((double)(y-yc)/(double)(x-xc));
        }
        alpha += x-xc>0.0?0.0:Math.PI;

        // Then, we calculate the points for the polygon
        x0 = x - l*Math.cos(alpha);
        y0 = y - l*Math.sin(alpha);

        x1 = x0 - h*Math.sin(alpha);
        y1 = y0 + h*Math.cos(alpha);

        x2 = x0 + h*Math.sin(alpha);
        y2 = y0 - h*Math.cos(alpha);

        outt.write(""+Globals.roundTo(x)+" " +Globals.roundTo(y)+ " m\n");
        outt.write(""+Globals.roundTo(x1)+" "+Globals.roundTo(y1)+" l\n");
        outt.write(""+Globals.roundTo(x2)+" "+Globals.roundTo(y2)+" l\n");

        if ((style & Arrow.flagEmpty) == 0) {
            outt.write("  f*\n");
        } else {
            outt.write("  s\n");
        }

        if ((style & Arrow.flagLimiter) != 0) {
            double x3;
            double y3;
            double x4;
            double y4;
            x3 = x - h*Math.sin(alpha);
            y3 = y + h*Math.cos(alpha);

            x4 = x + h*Math.sin(alpha);
            y4 = y - h*Math.cos(alpha);
            outt.write(""+Globals.roundTo(x3)+" "+Globals.roundTo(y3)+" m\n"+
                Globals.roundTo(x4)+" "+Globals.roundTo(y4)+" l s\n");
        }
        return new PointPr(x0,y0);
    }


    // Functions required for the TextInterface.

    /** Get the font size.
        @return the font size.
    */
    public double getFontSize()
    {
        return currentFontSize;
    }

    /** Set the font size.
        @param size the font size.
    */
    public void setFontSize(double size)
    {
        currentFontSize=(float)size;
        try {
            outt.write(currentFont+" "+currentFontSize+" Tf\n");
        } catch(IOException ee) {
            System.err.println("Can not write to file in PDF export.");
        }
    }

    /** Get the width of the given string with the current font.
        @param s the string to be used.
        @return the width of the string, in pixels.
    */
    public int getStringWidth(String s)
    {
        return 0;
    }

    /** Draw a string on the current graphic context.
        @param str the string to be drawn.
        @param x the x coordinate of the starting point.
        @param y the y coordinate of the starting point.
    */
    public void drawString(String str,
                                int x,
                                int y)
    {
        try {
            outt.write("  1 0 0 1 "+ Globals.roundTo(textx-x)+
                " "+ Globals.roundTo(texty-y)+
                " cm\n");
            texty=y;

            outt.write(" <");
            int ch;
            for(int i=0; i<str.length();++i) {
                ch=(int)str.charAt(i);
                // Proceed to encode UTF-8 characters as much as possible.
                if(ch>127) {
                    if(uncodeCharsNeeded.containsKey(ch)) {
                        ch=uncodeCharsNeeded.get(ch);
                    } else {
                        ++unicodeCharIndex;
                        if(unicodeCharIndex<256) {
                            uncodeCharsNeeded.put(unicodeCharIndex,ch);
                            ch=unicodeCharIndex;
                        } else {
                            System.err.println("Too many Unicode chars! "+
                                "The present version of the PDF export filter "+
                                "handles up to 128 different Unicode chars in "+
                                "one file.");
                        }
                    }
                }
                outt.write(Integer.toHexString(ch));
                outt.write(" ");
            }
            outt.write("> Tj\n");
        } catch(IOException ee) {
            System.err.println("Can not write to file in EPS export.");
        }
    }
}
