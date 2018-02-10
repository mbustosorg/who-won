/*

    Copyright (C) 2018 Mauricio Bustos (m@bustos.org)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package org.bustos.whowon

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object ImageResize {
  def resize(inputImagePath: String, outputImagePath: String, scaledWidth: Int, scaledHeight: Int): Unit = { // reads input image
    val inputImage = ImageIO.read(new File(inputImagePath))
    // creates output image
    val outputImage = new BufferedImage(scaledWidth, scaledHeight, inputImage.getType)
    // scales the input image to the output image
    val g2d = outputImage.createGraphics
    g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null)
    g2d.dispose()
    // extracts extension of output file
    val formatName = outputImagePath.substring(outputImagePath.lastIndexOf(".") + 1)
    // writes to output file
    ImageIO.write(outputImage, "jpg", new File(outputImagePath))
  }

  def resize(inputImagePath: String, outputImagePath: String, percent: Double): Unit = {
    val inputImage = ImageIO.read(new File(inputImagePath))
    val scaledWidth = (inputImage.getWidth * percent).toInt
    val scaledHeight = (inputImage.getHeight * percent).toInt
    resize(inputImagePath, outputImagePath, scaledWidth, scaledHeight)
  }
}

