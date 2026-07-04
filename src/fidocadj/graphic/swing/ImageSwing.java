package fidocadj.graphic.swing;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;

import fidocadj.graphic.ImageInterface;

/** This class maps the general image interface to java.awt.image.BufferedImage.

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

    @author Manuel Finessi

    Copyright 2026 by the FidoCadJ team
</pre>
*/
public class ImageSwing implements ImageInterface
{
    private final BufferedImage img;

    /** Standard constructor.
        @param img the Swing image to be employed.
    */
    public ImageSwing(BufferedImage img)
    {
        this.img = img;
    }

    /** Get the underlying Swing image.
        @return the Swing (AWT) image.
    */
    public BufferedImage getImageSwing()
    {
        return img;
    }

    /** {@inheritDoc} */
    public int getWidth()
    {
        return img == null ? 0 : img.getWidth();
    }

    /** {@inheritDoc} */
    public int getHeight()
    {
        return img == null ? 0 : img.getHeight();
    }

    /** {@inheritDoc} */
    public ImageInterface toGrayscale()
    {
        if (img == null) {
            return this;
        }
        ColorConvertOp op = new ColorConvertOp(
            ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        return new ImageSwing(op.filter(img, null));
    }
}
