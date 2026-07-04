package fidocadj.graphic;

/** ImageInterface is an interface used to specify a generic decoded raster
    image, abstracting away the concrete platform-specific image type
    (mirroring how {@link ColorInterface}/{@link ShapeInterface} abstract
    colors and shapes).

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
public interface ImageInterface
{
    /** Get the natural width of the image, in pixels.
        @return the width in pixels.
    */
    int getWidth();

    /** Get the natural height of the image, in pixels.
        @return the height in pixels.
    */
    int getHeight();

    /** Obtain a grayscale (black and white) version of this image.
        @return a new image, converted to grayscale.
    */
    ImageInterface toGrayscale();
}
