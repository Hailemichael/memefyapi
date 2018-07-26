package com.haile.apps.memefy.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.io.IOUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;

public class MemeImage {
	private static final Logger logger = LogManager.getLogger(MemeImage.class.getName());
	private static final int MAX_FONT_SIZE = 472;
    private static final int BOTTOM_MARGIN = 20;
    private static final int TOP_MARGIN = 20;
    private static final int SIDE_MARGIN = 20;

	public byte[] convertToMeme(BufferedImage originalImage, String suffix, String memeText, boolean top) throws Exception {
		
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				
				BufferedImage image = null;
				// resize image if larger than 600 x 600
				if ((originalImage.getWidth() > 600) || (originalImage.getHeight() > 600)) {
					if (originalImage.getWidth() > originalImage.getHeight()) {
						int newWidth = 600;
						int newHeight = (600 * originalImage.getHeight()) / originalImage.getWidth();
						image = resizeImage(originalImage, newWidth, newHeight,
								RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
						logger.debug("Image resize WxH: ", newWidth, newHeight);
					} else {
						int newWidth = (600 * originalImage.getWidth()) / originalImage.getHeight();
						int newHeight = 600;
						image = resizeImage(originalImage, newWidth, newHeight,
								RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
					}
				} else {
					image = originalImage;
				}

				// Add new line at the 3rd space occurrence
				if (memeText != null) {
					Graphics g = image.getGraphics();
					drawStringCentered(g, memeText, image, top);
					g.dispose();
				}
				
				boolean imageGenerated = ImageIO.write(image, suffix, bos);
				bos.flush();
				System.out.println("imageGenerated:  " + imageGenerated);
				return bos.toByteArray();

			} catch (Exception e) {
				throw new Exception("Exception occured while generating meme", e);
			} finally {
				bos.close();
			}
	}
	
	private static BufferedImage resizeImage(BufferedImage img, int targetWidth, int targetHeight, Object hint,
			boolean higherQuality) {
		int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
				: BufferedImage.TYPE_INT_ARGB;
		BufferedImage ret = (BufferedImage) img;
		int w, h;
		if (higherQuality) {
			// Use multi-step technique: start with original size, then
			// scale down in multiple passes with drawImage()
			// until the target size is reached
			w = img.getWidth();
			h = img.getHeight();
		} else {
			// Use one-step technique: scale directly from original
			// size to target size with a single drawImage() call
			w = targetWidth;
			h = targetHeight;
		}

		do {
			if (higherQuality && w > targetWidth) {
				w /= 2;
				if (w < targetWidth) {
					w = targetWidth;
				}
			}

			if (higherQuality && h > targetHeight) {
				h /= 2;
				if (h < targetHeight) {
					h = targetHeight;
				}
			}

			BufferedImage tmp = new BufferedImage(w, h, type);
			Graphics2D g2 = tmp.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
			g2.drawImage(ret, 0, 0, w, h, null);
			g2.dispose();

			ret = tmp;
		} while (w != targetWidth || h != targetHeight);

		return ret;
	}
	
	private static void drawStringCentered(Graphics g, String text, BufferedImage image, boolean top) throws InterruptedException {
        if (text == null)
            text = "";

        int height = 0;
        int fontSize = MAX_FONT_SIZE;
        int maxCaptionHeight = image.getHeight() / 5;
        int maxLineWidth = image.getWidth() - SIDE_MARGIN * 2;
        String formattedString = "";

        do {
            g.setFont(new Font("Arial", Font.BOLD, fontSize));

            // first inject newlines into the text to wrap properly
            StringBuilder sb = new StringBuilder();
            int left = 0;
            int right = text.length() - 1;
            while ( left < right ) {

                String substring = text.substring(left, right + 1);
                Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(substring, g);
                while ( stringBounds.getWidth() > maxLineWidth ) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }

                    // look for a space to break the line
                    boolean spaceFound = false;
                    for ( int i = right; i > left; i-- ) {
                        if ( text.charAt(i) == ' ' ) {
                            right = i - 1;
                            spaceFound = true;
                            break;
                        }
                    }
                    substring = text.substring(left, right + 1);
                    stringBounds = g.getFontMetrics().getStringBounds(substring, g);

                    // If we're down to a single word and we are still too wide,
                    // the font is just too big.
                    if ( !spaceFound && stringBounds.getWidth() > maxLineWidth ) {
                        break;
                    }
                }
                sb.append(substring).append("\n");
                left = right + 2;
                right = text.length() - 1;
            }

            formattedString = sb.toString();

            // now determine if this font size is too big for the allowed height
            height = 0;
            for ( String line : formattedString.split("\n") ) {
                Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(line, g);
                height += stringBounds.getHeight();
            }
            fontSize--;
        } while ( height > maxCaptionHeight );

        // draw the string one line at a time
        int y = 0;
        if ( top ) {
            y = TOP_MARGIN + g.getFontMetrics().getHeight();
        } else {
            y = image.getHeight() - height - BOTTOM_MARGIN + g.getFontMetrics().getHeight();
        }
        for ( String line : formattedString.split("\n") ) {        	
            // Draw each string twice for a shadow effect
            Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(line, g);
            int xPos = (image.getWidth() - (int) stringBounds.getWidth()) / 2;
            g.setColor(Color.BLACK);
            g.drawString(line, xPos + 1, y + 1);
            g.setColor(Color.WHITE);
            g.drawString(line, xPos, y);
            y += g.getFontMetrics().getHeight();
        }
        g.dispose();
    }


}
