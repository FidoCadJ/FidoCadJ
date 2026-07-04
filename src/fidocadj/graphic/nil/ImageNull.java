package fidocadj.graphic.nil;

import fidocadj.graphic.ImageInterface;

/**         SWING VERSION

    Null image class. Does nothing :-)
    Classes like this one are useful when calculating the size of the
    drawings.

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
public class ImageNull implements ImageInterface
{
    /** Does nothing.
        @return 0.
    */
    public int getWidth()
    {
        return 0;
    }

    /** Does nothing.
        @return 0.
    */
    public int getHeight()
    {
        return 0;
    }

    /** Does nothing.
        @return a new ImageNull instance.
    */
    public ImageInterface toGrayscale()
    {
        return new ImageNull();
    }
}
