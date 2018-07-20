package com.haile.apps.memefy.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;

public class MemeImage {
	private static final Logger logger = LogManager.getLogger(MemeImage.class.getName());
	private static final boolean DEBUG = false;

	public byte[] convertToMeme(String imageUrl, String memeText) throws Exception {
		String position = "bottom";
		String suffix = null;
		if (imageUrl != null && checkUrlValidity(imageUrl)) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			
			try {
				URL imgUrl = new URL(imageUrl);
				URLConnection conn = imgUrl.openConnection();
				
				String contentType = conn.getContentType();

				Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(contentType);
				while (suffix == null && readers.hasNext()) {
					ImageReaderSpi provider = readers.next().getOriginatingProvider();
					if (provider != null) {
						String[] suffixes = provider.getFileSuffixes();
						if (suffixes != null) {
							suffix = suffixes[0];
						}
					}
				}

				final BufferedImage originalImage = ImageIO.read(imgUrl);
				System.out.println("originalImage: " + originalImage.getHeight() + "," + originalImage.getWidth());
				BufferedImage image = null;
				// resize image if larger than 600 x 600
				if ((originalImage.getWidth() > 600) || (originalImage.getHeight() > 600)) {
					if (originalImage.getWidth() > originalImage.getHeight()) {
						int newWidth = 600;
						int newHeight = (600/originalImage.getWidth())*originalImage.getHeight();
						image = resizeImage(originalImage, newWidth, newHeight, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
					} else {
						int newWidth = (600/originalImage.getHeight())*originalImage.getWidth();
						int newHeight = 600;
						image = resizeImage(originalImage, newWidth, newHeight, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
					}					
				} else {
					image = originalImage;
				}

				// Add new line at the 3rd space occurrence
				if (memeText != null) {
					Font font = new Font("Utopia", Font.BOLD, 48);//Lucida Sans Typewriter
					/**
					 * Font style ================ 
					 * 1. Bitstream Charter 
					 * 2. Courier 10 Pitch 
					 * 3. Cursor 
					 * 4. DejaVu Sans 
					 * 5. DejaVu Sans Condensed 
					 * 6. DejaVu Sans Light 
					 * 7. DejaVu Serif 
					 * 8. DejaVu Serif Condensed 
					 * 9. Dialog
					 * 10. DialogInput 
					 * 11. Lucida Bright 
					 * 12. Lucida Sans 
					 * 13. Lucida Sans Typewriter 
					 * 14. Monospaced 
					 * 15. SansSerif 
					 * 16. Serif 
					 * 17. Utopia
					 */
					Graphics g = image.getGraphics();
					g.setFont(font);
					System.out.println(
							"Font used: \tName = " + g.getFont().getName() + "\tSize = " + g.getFont().getSize());
					int textWithInPixel = g.getFontMetrics().stringWidth(memeText);

					int tempLength = (int) (memeText.length() * ((500 / (double) textWithInPixel)));
					System.out.println("tempLength int:  " + tempLength);
					// setting yPosition according to length of text would be
					// good....
					int yPosition = 0;
					if (position.equalsIgnoreCase("top")) {// position next
															// version...
						yPosition = 100;
					} else {
						yPosition = 400;
					}
					int xPosition = 50;
					int border = 2;
					if (tempLength < memeText.length()) {// needs to be printed in
														// multiple lines
						int line = 1;
						while (tempLength < memeText.length()) {
							String tempString = memeText.substring(0, tempLength);
							String memeLine = tempString.substring(0, tempString.lastIndexOf(" "));
							System.out.println("memeLine " + line + " = " + memeLine.trim());
							drawBorder(g, memeLine.trim(), Color.WHITE, Color.BLACK, xPosition, yPosition, border);
							memeText = memeText.substring(tempString.lastIndexOf(" "));
							yPosition += 65;
							line++;
						}
						System.out.println("memeLine " + line + " = " + memeText.trim());
						drawBorder(g, memeText.trim(), Color.WHITE, Color.BLACK, xPosition, yPosition, border);
					} else {// print in one line
						// Add text to image
						System.out.println("Single line meme = " + memeText.trim());
						drawBorder(g, memeText.trim(), Color.WHITE, Color.BLACK, xPosition, yPosition, border);
					}
					g.dispose();
				}

				boolean imageGenerated = ImageIO.write(image, suffix, bos);
				bos.flush();
				System.out.println("imageGenerated:  " + imageGenerated);

			} catch (MalformedURLException e) {
				throw new Exception("The image url is malformed, Url = " + imageUrl, e);
			}	catch (FileNotFoundException e) {
				throw new Exception("The image file is not found, Url = " + imageUrl, e);
			} catch (IllegalArgumentException e) {
				throw new Exception("Illegal argumen occured", e);
			} catch (IIOException e) {
				throw new Exception("Can't get input stream from URL: " + imageUrl, e);
			} catch (IOException e) {
				throw new Exception("IOException occured, Can't get input stream from URL!", e);
			}
			return bos.toByteArray();
		} else {
			throw new Exception("Image url not found" + imageUrl);
		}
	}

	private static void drawBorder(Graphics g, String text, Color mainColor, Color borderColor, int x, int y,
			int times) {
		g.setColor(borderColor);
		for (int i = 0; i < times; i++) {
			g.drawString(text, ShiftWest(x, i), ShiftNorth(y, i));
			g.drawString(text, ShiftWest(x, i), ShiftSouth(y, i));
			g.drawString(text, ShiftEast(x, i), ShiftNorth(y, i));
			g.drawString(text, ShiftEast(x, i), ShiftSouth(y, i));
		}
		g.setColor(mainColor);
		g.drawString(text, x, y);
	}

	static int ShiftNorth(int p, int distance) {
		return (p - distance);
	}

	static int ShiftSouth(int p, int distance) {
		return (p + distance);
	}

	static int ShiftEast(int p, int distance) {
		return (p + distance);
	}

	static int ShiftWest(int p, int distance) {
		return (p - distance);
	}

	public static BufferedImage resizeImage(BufferedImage img, int targetWidth, int targetHeight, Object hint,
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
	
	private static boolean checkUrlValidity(String url) throws MalformedURLException, ProtocolException, IOException {
		boolean isValid = false;
		URL pageUrl = null;
		int responseCode = 0;
		pageUrl = new URL(url);
		HttpURLConnection huc = (HttpURLConnection) pageUrl.openConnection();
		if (DEBUG)
			logger.info("Making Http HEAD request to url: " + url);
		huc.setRequestMethod("HEAD");
		huc.connect();
		responseCode = huc.getResponseCode();
		if (DEBUG)
			logger.info("Response code for Head request: " + responseCode);
		if (responseCode == 200) {
			isValid = true;
		}
		return isValid;
	}

}
