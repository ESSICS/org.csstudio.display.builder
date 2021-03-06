/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.javafx.swt;

import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import javafx.embed.swing.SwingFXUtils;

/** Helper for converting image types
 *  @author Kay Kasemir
 */
public class ImageConverter
{
    /** @param bufferedImage AWT image
     *  @return SWT image
     */
    public static ImageData convertToSWT(final BufferedImage bufferedImage)
    {
        // From http://git.eclipse.org/c/platform/eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet156.java
        if (bufferedImage.getColorModel() instanceof DirectColorModel)
        {
            DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
            PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());
            ImageData data = new ImageData(bufferedImage.getWidth(),
                                           bufferedImage.getHeight(), colorModel.getPixelSize(),
                                           palette);
            for (int y = 0; y < data.height; y++)
                for (int x = 0; x < data.width; x++)
                {
                    int rgb = bufferedImage.getRGB(x, y);
                    int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
                    data.setPixel(x, y, pixel);
                    if (colorModel.hasAlpha())
                        data.setAlpha(x, y, (rgb >> 24) & 0xFF);
                }
            return data;
        }
        else if (bufferedImage.getColorModel() instanceof IndexColorModel)
        {
            IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
            int size = colorModel.getMapSize();
            byte[] reds = new byte[size];
            byte[] greens = new byte[size];
            byte[] blues = new byte[size];
            colorModel.getReds(reds);
            colorModel.getGreens(greens);
            colorModel.getBlues(blues);
            RGB[] rgbs = new RGB[size];
            for (int i = 0; i < rgbs.length; i++)
                rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
            PaletteData palette = new PaletteData(rgbs);
            ImageData data = new ImageData(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), colorModel.getPixelSize(),
                    palette);
            data.transparentPixel = colorModel.getTransparentPixel();
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[1];
            for (int y = 0; y < data.height; y++)
                for (int x = 0; x < data.width; x++)
                {
                    raster.getPixel(x, y, pixelArray);
                    data.setPixel(x, y, pixelArray[0]);
                }
            return data;
        }
        return null;
    }

    /** @param jfx_image JFX Image
     *  @return SWT Image
     */
    public static ImageData convertToSWT(final javafx.scene.image.Image jfx_image)
    {
            // SWTFXUtils.fromFXImage() converts JFX Image into SWT image,
            // but adds direct dependency to the jfxswt.jar which is hard
            // to get onto the IDE classpath.
            // To convert JFX to AWT..
            final BufferedImage awt_image = SwingFXUtils.fromFXImage(jfx_image, null);
            // .. and then to SWT
            return convertToSWT(awt_image);
    }
}
