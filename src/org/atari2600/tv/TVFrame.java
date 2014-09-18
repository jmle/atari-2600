package org.atari2600.tv;


/**
 * Class representing the frame to be built. Consists on a matrix of pixels.
 * 
 * @author Juan Manuel Leflet Estrada
 * 
 */
public class TVFrame {
	private Pixel[][] pixels;

	public TVFrame() {
		pixels = new Pixel[228][262];
		
		// Fill pixels.
		for (int i = 0; i < pixels.length; i++) {
			for (int j = 0; j < pixels[0].length; j++) {
				pixels[i][j] = new Pixel();
			}
		}
	}

	public void setPixel(int x, int y, int c) {
		pixels[x][y] = new Pixel(c);
	}
	
	public void setPixel(int x, int y, Pixel p) {
		pixels[x][y] = p;
	}

	public Pixel getPixel(int x, int y) {
		return pixels[x][y];
	}

	public void setPixelColor(int x, int y, int c) {
		pixels[x][y].setColor(c);
	}

	public int getPixelColor(int x, int y) {
		return pixels[x][y].getColor();
	}

	public Pixel[][] getPixels() {
		return pixels;
	}

}
