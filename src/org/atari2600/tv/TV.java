package org.atari2600.tv;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

public class TV extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6538890434231780545L;

	private TVFrame frame;

	public TV() {
		frame = new TVFrame();
	}

	@Override
	public void paint(Graphics g) {
		Pixel pixels[][] = frame.getPixels();
		int li = pixels.length/* - 68*/; // Remove HBlank
		int lj = pixels[0].length/* - 70*/; // Remove Vsync, Vblank and Overscan
		super.paint(g);

		for (int i = 0; i < li; i++) {
			for (int j = 0; j < lj; j++) {
				g.setColor(new Color(pixels[i/* + 68*/][j/* + 40*/].getColor()));
				g.fillRect(i * 3, j * 2, 3, 2);
			}
		}

	}

	/**
	 * Sets the new frame to paint and calls the repaint method. Must be called
	 * each time a new frame has to be painted.
	 * 
	 * @param frame
	 *            : The frame to paint
	 */
	public void repaint(TVFrame frame) {
		this.frame = frame;

		// Call JPanel's repaint() to trigger the paint method.
		repaint();
	}

	public TVFrame getFrame() {
		return frame;
	}

	public void setFrame(TVFrame frame) {
		this.frame = frame;
	}

}
