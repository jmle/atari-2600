package org.atari2600.core;

import org.atari2600.tv.Pixel;
import org.atari2600.tv.TV;
import org.atari2600.tv.TVFrame;
import org.atari2600.util.M;

/**
 * Class that represents the TIA chip. This chip is in charge of handling all
 * graphic and music-related stuff inside the Atari 2600.
 * 
 * @author Juan Manuel Leflet Estrada
 * 
 */
public class Tia {
	// The TIA is connected to both the TV and the CPU. We need the PIA too,
	// because changes in the PIA's registers affect the position of the
	// different sprites.
	private TV tv;
	private Cpu cpu;
	private Pia pia;

	// The next frame to be drawn in the TV.
	private TVFrame nextFrame;

	// Horizontal and vertical counters to keep track of the beam.
	private int h, v;

	// Counters for the PF object sizes and for their hmoves
	private int p0SizeCount, p1SizeCount, m0SizeCount, m1SizeCount,
			blSizeCount;
	private int p0hMoveCnt, p1hMoveCnt, m0hMoveCnt, m1hMoveCnt, blhMoveCnt;

	// Different strobe registers
	private boolean resetP0, resetP1, resetM0, resetM1, resetBL, horizMove;

	// Vertical delays
	private boolean delayP0, delayP1, delayB;
	private int delayedDataP0, delayedDataP1, delayedDataB;

	// Memory
	private int mem[];

	public Tia() {
		mem = new int[64];
		nextFrame = new TVFrame();
		h = 0;
		v = 0;
	}

	/**
	 * Writes in the TIA memory and executes strobe behavior if necessary.
	 * 
	 * @param addr
	 *            : The address to write to.
	 * @param data
	 *            : The data to write.
	 */
	public void write(int addr, int data) {
		// Check for special writes (strobe and others)
		switch (addr) {

		// VSYNC
		case M.VSYNC:
			mem[addr] = data;

			// Make VSYNC
			if ((data & M.VERTICAL_SYNC) == M.VERTICAL_SYNC) {
				h = 0; // Stella doesn't do this... Don't know why
				v = 0;
			}

			break;

		// =========================
		// Strobe registers
		// =========================

		// WSYNC halts the CPU.
		case M.WSYNC:
			haltCPU();
			break;

		// RSYNC resets the horizontal counter.
		case M.RSYNC:
			h = 0;
			break;

		// RES sets the associated object's horizontal position equal to the
		// current position of the beam.
		case M.RESP0:
			resetP0 = true;
			break;

		case M.RESP1:
			resetP1 = true;
			break;

		case M.RESM0:
			resetM0 = true;
			break;

		case M.RESM1:
			resetM1 = true;
			break;

		case M.RESBL:
			resetBL = true;

			// The counter is set to the ball size, which can be 1, 2, 4 or 8.
			blSizeCount = (int) Math.pow(2,
					((mem[M.CTRLPF] & M.BALL_SIZE) >> 4));
			break;

		// HMOVE applies horizontal motion. The motion registers (HMP0, HMP1,
		// etc) are added to the position of the moveable objects (P0, P1, etc):
		// NewPos = ( OldPos +/- Motion ) % 160
		case M.HMOVE:
			horizMove = true;
			break;

		// HMCLR resets all five motion registers (HMP0, HMP1, etc) to zero.
		case M.HMCLR:
			clearMotionRegisters();
			break;

		// CXCLR clears all collision latches.
		case M.CXCLR:
			clearCollisions();
			break;

		// =========================
		// Others
		// =========================

		// When VDELPx is set, writes to GRPx and ENABL are delayed until GRPy
		// is written to. This behavior is implemented here.
		case M.VDELP0:
			mem[M.VDELP0] = data;

			if ((data & M.BIT_0) != 0)
				delayP0 = true;

			break;

		case M.VDELP1:
			mem[M.VDELP1] = data;

			if ((data & M.BIT_0) != 0)
				delayP1 = true;

			break;

		case M.VDELBL:
			mem[M.VDELBL] = data;

			if ((data & M.BIT_0) != 0)
				delayB = true;

			break;

		case M.GRP0:
			if (delayP1) {
				mem[M.GRP1] = delayedDataP1;
				delayP1 = false;
			}

			if (!delayP0) {
				mem[addr] = data;
			}

			break;

		case M.GRP1:
			if (delayP0) {
				mem[M.GRP0] = delayedDataP0;
				delayP0 = false;
			}

			if (delayB) {
				mem[M.ENABL] = delayedDataB;
				delayB = false;
			}

			if (!delayP1) {
				mem[addr] = data;
			}

			break;

		case M.ENABL:
			if (!delayB) {
				mem[addr] = data;
			}

			break;

		default:
			mem[addr] = data;
			break;
		}
	}

	public int read(int addr) {
		return mem[addr];
	}

	/**
	 * Performs a single color cycle:
	 * <ul>
	 * <li>Draws the next pixel.</li>
	 * <li>Detects collisions.</li>
	 * </ul>
	 */
	public void executeNext() {
		Pixel pixel;

		if (vSyncing() || vBlanking()) {
			// If we are blanking or syncing, paint it black.
			pixel = new Pixel();
		} else { // Else, we draw the next pixel!
			// We always draw the background first, and then draw something else
			// on top if necessary. The pixel's PFO only changes if something is
			// drawn upon it. If we are hBlanking, draw the background black.
			if (hBlanking()) {
				pixel = new Pixel();
			} else {
				pixel = new Pixel(getColor(PFO.BG));
				pixel.setObj(PFO.BG);
			}

			// Check priority
			if ((mem[M.CTRLPF] & M.PLAYFIELD_BALL_PRIORITY) == 0) {
				// Draw with the normal priority: players/missiles on top.
				pixel = drawPlayfield(pixel);
				//pixel = drawPlayers(pixel);

			} else {
				// Draw with inverted priority: playfield/ball on top.
				pixel = drawPlayers(pixel);
				//pixel = drawPlayfield(pixel);
			}

		}

		//if (v >= 42 && h >= 68)
		//	tv.repaint(nextFrame);

		nextFrame.setPixel(h, v, pixel);
		updateBeam();
	}

	/**
	 * Draws playfield and balls upon a pixel. This algorithm paints on a pixel
	 * per pixel basis, depending on the state of the different registers.
	 * 
	 * Pixels surrounding the actual pixel (which is (h, v)) may be affected
	 * because of HMOVE. If HMOVE is supposed to happen, then we have to
	 * retrieve the value of the motion to perform and paint a pixel that:
	 * <ol>
	 * <li>May be BEFORE the present pixel</li>
	 * <li>May be the present pixel</li>
	 * <li>May be AFTER the present pixel</li>
	 * </ol>
	 * 
	 * In cases 1 and 3, the pixel we have to draw upon may already be painted
	 * with something else apart from background, so priority must be checked in
	 * order to determine if we should paint or just check the collision.
	 * 
	 * @param p
	 *            : the pixel to draw
	 * @return The drawn pixel (the present one, not the ones modified because
	 *         of HMOVE).
	 */
	private Pixel drawPlayfield(Pixel p) {
		int pfReg, pfBit;

		// Playfield only drawn when not hblanking
		if (!hBlanking()) {
			// ===========================================
			// Get present Playfield register and bit
			// ===========================================
			
			// First, check if we are in the first or the second half of the
			// screen.
			if (firstHalf()) {
				// Select register depending on position.
				if (h < 84) {
					pfReg = M.PF0;
				} else if (h >= 84 && h < 116) {
					pfReg = M.PF1;
				} else {
					pfReg = M.PF2;
				}

				pfBit = normalPfBitsLUT[h - 68];
			} else {
				// Second half of the screen. Do we need to mirror?
				if ((mem[M.CTRLPF] & M.PLAYFIELD_REFLECTION) == 0) {
					// Normal reflection
					if (h < 164) {
						pfReg = M.PF0;
					} else if (h >= 164 && h < 196) {
						pfReg = M.PF1;
					} else {
						pfReg = M.PF2;
					}

					pfBit = normalPfBitsLUT[h - 148];
				} else {
					// Mirror reflection
					if (h < 180) {
						pfReg = M.PF2;
					} else if (h >= 180 && h < 212) {
						pfReg = M.PF1;
					} else {
						pfReg = M.PF0;
					}

					pfBit = mirroredPfBitsLut[h - 148];
				}
			}

			// ===========================================
			// Draw playfield
			// ===========================================

			// Check for score mode. If score mode is on then
			// draw the different halves with the color of the players.
			if (((mem[M.CTRLPF] & M.PLAYFIELD_BALL_PRIORITY) == 0)
					&& ((mem[M.CTRLPF] & M.PLAYFIELD_COLOR) != 0)) {
				// Score mode on, check halves and set color and pixel
				// object type in case the PF register bit was not transparent.
				if ((mem[pfReg] & pfBit) != 0) {
					// If we have higher priority, then we paint and set object
					// type
					if (!p.hasHigherPriorityThan(PFO.PF)) {
						if (firstHalf()) {
							p.setColor(getColor(PFO.P0));
						} else {
							p.setColor(getColor(PFO.P1));
						}

						p.setObj(PFO.PF);
					}

					// If priority was not higher we don't paint, but there may
					// still be a collision
					setCollision(p.checkCollision(PFO.PF));
				}
			} else {
				// Score mode off, paint it with PF color
				if ((mem[pfReg] & pfBit) != 0) {
					if (!p.hasHigherPriorityThan(PFO.PF)) {
						// if (h > 68 && v >= 92)
						// System.out.println("eeets");
						p.setColor(getColor(PFO.PF));
						p.setObj(PFO.PF);
					}

					// Check collisions
					setCollision(p.checkCollision(PFO.PF));
				}
			}

		}

		// ===========================================
		// Draw ball
		// ===========================================

		// Check if the ball is supposed to appear (the ball appears if the
		// RESBL strobe register was written and ENABL.1 == 1)
		if (resetBL && ((mem[M.ENABL] & M.BIT_1) != 0)) {
			// If size counter was 0, then we wouldn't have to paint anymore
			// because we reached the end of the figure
			if (blSizeCount > 0) {
				// We have to check the blanking, because if we were blanking,
				// then the ball must be drawn at the left edge of the screen
				// Not blanking:
				if (!hBlanking()) {
					// Correct horizontal position?
					if (horizMove) {
						// If so, we get the correction from HMBL.
						int hm = getHM(M.HMBL);

						if (hm == 0) {
							// No movement, check priority
							if (!p.hasHigherPriorityThan(PFO.B)) {
								// p had lower priority: draw
								p.setColor(getColor(PFO.B));
								p.setObj(PFO.B);
							}

							setCollision(p.checkCollision(PFO.B));
						} else {
							// There is movement: Have to paint the outer pixel
							// given by the HMBL reg
							Pixel outer = nextFrame.getPixel(h + hm, v);

							if (!outer.hasHigherPriorityThan(PFO.B)) {
								outer.setColor(getColor(PFO.B));
								outer.setObj(PFO.B);
							}

							setCollision(outer.checkCollision(PFO.B));
						}

					} else {
						// Else, paint the ball here without moving.
						if (!p.hasHigherPriorityThan(PFO.B)) {
							// p had lower priority: draw
							p.setColor(getColor(PFO.B));
							p.setObj(PFO.B);
						}
					}
				} else {
					// We are hBlanking; then draw at the left side of the
					// screen plus 2 pixels
					Pixel outer = nextFrame.getPixel(68 - h + 2, v);
					outer.setColor(getColor(PFO.B));
					setCollision(outer.checkCollision(PFO.B));
					outer.setObj(PFO.B);
				}

				setCollision(p.checkCollision(PFO.B));
				// p.setObj(PFO.B);
				blSizeCount--; // Dec. ball counter (size)
			} else {
				resetBL = false;
			}
		}

		return p;
	}

	/**
	 * Draws players and missiles upon a pixel.
	 * 
	 * @param p
	 *            : the pixel to draw
	 * @return The drawn pixel.
	 */
	private Pixel drawPlayers(Pixel p) {
		// We will draw the objects in inverse priority order, since the one
		// with more priority must be upon the one with least priority.
		// Everything here is drawn in a similar fashion as the ball.

		// ===========================================
		// Draw missile 1
		// ===========================================

		if (resetM1 && ((mem[M.ENAM1] & M.BIT_1) != 0)) {
			if (m1SizeCount > 0) {
				if (!hBlanking()) {
					if (horizMove) {
						int hm = getHM(M.HMM1);

						if (hm == 0) { // No movement
							if (!p.hasHigherPriorityThan(PFO.M1)) {
								p.setColor(getColor(PFO.M1));
								p.setObj(PFO.M1);
							}

							setCollision(p.checkCollision(PFO.M1));
						} else { // Movement
							Pixel outer = nextFrame.getPixel(h + hm, v);

							if (!outer.hasHigherPriorityThan(PFO.M1)) {
								outer.setColor(getColor(PFO.M1));
								outer.setObj(PFO.M1);
							}

							setCollision(outer.checkCollision(PFO.M1));
						}

					} else {
						if (!p.hasHigherPriorityThan(PFO.M1)) {
							p.setColor(getColor(PFO.M1));
							p.setObj(PFO.M1);
						}
					}
				} else { // Hblanking and reset position is on:
					Pixel outer = nextFrame.getPixel(68 - h + 2, v);
					outer.setColor(getColor(PFO.M1));
					setCollision(outer.checkCollision(PFO.M1));
					outer.setObj(PFO.M1);
				}

				setCollision(p.checkCollision(PFO.M1));
				// p.setObj(PFO.B);
				m1SizeCount--; // Dec. ball counter (size)
			} else {
				resetM1 = false;
			}
		}

		// ===========================================
		// Draw player 1
		// ===========================================

		if (resetP1 && ((mem[M.GRP1] & M.BIT_1) != 0)) {
			if (p1SizeCount > 0) {
				if (!hBlanking()) {
					if (horizMove) {
						int hm = getHM(M.HMP1);

						if (hm == 0) { // No movement
							if (!p.hasHigherPriorityThan(PFO.P1)) {
								p.setColor(getColor(PFO.P1));
								p.setObj(PFO.P1);
							}

							setCollision(p.checkCollision(PFO.P1));
						} else { // Movement
							Pixel outer = nextFrame.getPixel(h + hm, v);

							if (!outer.hasHigherPriorityThan(PFO.P1)) {
								outer.setColor(getColor(PFO.P1));
								outer.setObj(PFO.P1);
							}

							setCollision(outer.checkCollision(PFO.P1));
						}

					} else {
						if (!p.hasHigherPriorityThan(PFO.P1)) {
							p.setColor(getColor(PFO.P1));
							p.setObj(PFO.P1);
						}
					}
				} else { // Hblanking and reset position is on:
					Pixel outer = nextFrame.getPixel(68 - h + 2, v);
					outer.setColor(getColor(PFO.P1));
					setCollision(outer.checkCollision(PFO.P1));
					outer.setObj(PFO.P1);
				}

				setCollision(p.checkCollision(PFO.P1));
				// p.setObj(PFO.B);
				p1SizeCount--; // Dec. ball counter (size)
			} else {
				resetP1 = false;
			}
		}

		// ===========================================
		// Draw missile 0
		// ===========================================

		if (resetM1 && ((mem[M.ENAM0] & M.BIT_1) != 0)) {
			if (m0SizeCount > 0) {
				if (!hBlanking()) {
					if (horizMove) {
						int hm = getHM(M.HMM0);

						if (hm == 0) { // No movement
							if (!p.hasHigherPriorityThan(PFO.M0)) {
								p.setColor(getColor(PFO.M0));
								p.setObj(PFO.M0);
							}

							setCollision(p.checkCollision(PFO.M0));
						} else { // Movement
							Pixel outer = nextFrame.getPixel(h + hm, v);

							if (!outer.hasHigherPriorityThan(PFO.M0)) {
								outer.setColor(getColor(PFO.M0));
								outer.setObj(PFO.M0);
							}

							setCollision(outer.checkCollision(PFO.M0));
						}

					} else {
						if (!p.hasHigherPriorityThan(PFO.M0)) {
							p.setColor(getColor(PFO.M0));
							p.setObj(PFO.M0);
						}
					}
				} else { // Hblanking and reset position is on:
					Pixel outer = nextFrame.getPixel(68 - h + 2, v);
					outer.setColor(getColor(PFO.M0));
					setCollision(outer.checkCollision(PFO.M0));
					outer.setObj(PFO.M0);
				}

				setCollision(p.checkCollision(PFO.M0));
				// p.setObj(PFO.B);
				m0SizeCount--; // Dec. ball counter (size)
			} else {
				resetM0 = false;
			}
		}

		// ===========================================
		// Draw player 0
		// ===========================================

		if (resetP0 && ((mem[M.GRP0] & M.BIT_1) != 0)) {
			if (p0SizeCount > 0) {
				if (!hBlanking()) {
					if (horizMove) {
						int hm = getHM(M.HMP0);

						if (hm == 0) { // No movement
							if (!p.hasHigherPriorityThan(PFO.P0)) {
								p.setColor(getColor(PFO.P0));
								p.setObj(PFO.P0);
							}

							setCollision(p.checkCollision(PFO.P0));
						} else { // Movement
							Pixel outer = nextFrame.getPixel(h + hm, v);

							if (!outer.hasHigherPriorityThan(PFO.P0)) {
								outer.setColor(getColor(PFO.P0));
								outer.setObj(PFO.P0);
							}

							setCollision(outer.checkCollision(PFO.P0));
						}

					} else {
						if (!p.hasHigherPriorityThan(PFO.P0)) {
							p.setColor(getColor(PFO.P0));
							p.setObj(PFO.P0);
						}
					}
				} else { // Hblanking and reset position is on:
					Pixel outer = nextFrame.getPixel(68 - h + 2, v);
					outer.setColor(getColor(PFO.P0));
					setCollision(outer.checkCollision(PFO.P0));
					outer.setObj(PFO.P0);
				}

				setCollision(p.checkCollision(PFO.P0));
				// p.setObj(PFO.B);
				p0SizeCount--; // Dec. ball counter (size)
			} else {
				resetP0 = false;
			}
		}

		return p;
	}

	// Beam control -----------------------------------------------

	/**
	 * Increments the position of the beam, checking if a border has been
	 * reached, and if we finished painting the last frame, sends it to the TV.
	 */
	private void updateBeam() {
		if (h == 227) {
			h = 0;
			resumeCPU(); // In case the CPU was waiting for WSYNC

			if (v == 261) {
				v = 0;
				tv.repaint(nextFrame);
				nextFrame = new TVFrame();
			} else {
				v++;
			}
		} else {
			h++;
		}
	}

	// CPU control ------------------------------------------------

	private void haltCPU() {
		cpu.halt();
	}

	private void resumeCPU() {
		cpu.resume();
	}

	// State of the beam ------------------------------------------
	// (All coordinates begin in 0)

	private boolean vSyncing() {
		return ((mem[M.VSYNC] & M.VERTICAL_SYNC) == 0) ? false : true;
	}

	private boolean vBlanking() {
		return ((mem[M.VBLANK] & M.VERTICAL_BLANK) == 0) ? false : true;
	}

	private boolean hBlanking() {
		return (h < 68);
	}

	private boolean firstHalf() {
		return h >= 68 && h < 148;
	}

	private boolean overscan() {
		return v >= 232;
	}

	// Register-related -----------------------------------------

	private void clearMotionRegisters() {
		for (int i = 0x20; i < 0x25; i++) {
			mem[i] = 0;
		}
	}

	private void clearCollisions() {
		for (int i = 0x30; i < 0x38; i++) {
			mem[i] = 0;
		}
	}

	/**
	 * Sets the bit of the corresponding collision register to 1.
	 * 
	 * @param col
	 *            : The collision to set.
	 */
	private void setCollision(COL col) {
		switch (col) {
		case M0_P1:
			mem[M.CXM0P] |= M.BIT_7;
			break;

		case M0_P0:
			mem[M.CXM0P] |= M.BIT_6;
			break;

		case M1_P0:
			mem[M.CXM1P] |= M.BIT_7;
			break;

		case M1_P1:
			mem[M.CXM1P] |= M.BIT_6;
			break;

		case P0_PF:
			mem[M.CXP0FB] |= M.BIT_7;
			break;

		case P0_BL:
			mem[M.CXP0FB] |= M.BIT_6;
			break;

		case P1_PF:
			mem[M.CXP1FB] |= M.BIT_7;
			break;

		case P1_BL:
			mem[M.CXP1FB] |= M.BIT_6;
			break;

		case M0_PF:
			mem[M.CXM0FB] |= M.BIT_7;
			break;

		case M0_BL:
			mem[M.CXM0FB] |= M.BIT_6;
			break;

		case M1_PF:
			mem[M.CXM1FB] |= M.BIT_7;
			break;

		case M1_BL:
			mem[M.CXM1FB] |= M.BIT_6;
			break;

		case BL_PF:
			mem[M.CXBLPF] |= M.BIT_7;
			break;

		case P0_P1:
			mem[M.CXPPMM] |= M.BIT_7;
			break;

		case M0_M1:
			mem[M.CXPPMM] |= M.BIT_6;
			break;

		default:
			break;
		}
	}

	/**
	 * Gets the horizontal motion value. Actually transforms a 4 bit number to
	 * 2's complement.
	 * 
	 * @param reg
	 *            : The HM register (ball, missile0/player0, etc.)
	 * @return The HM value.
	 */
	private int getHM(int reg) {
		int hm = (mem[reg] & 0xF0) >> 4;

		if ((hm & 0x8) != 0) {
			if ((hm & 0x7) == 0)
				hm = -8; // Ugly
			else
				hm = ~hm + 1;
		}

		return hm;
	}

	// Setters & Getters -----------------------------------------

	public TV getTv() {
		return tv;
	}

	public void setTv(TV tv) {
		this.tv = tv;
	}

	public Cpu getCpu() {
		return cpu;
	}

	public void setCpu(Cpu cpu) {
		this.cpu = cpu;
	}

	public TVFrame getNextFrame() {
		return nextFrame;
	}

	public void setNextFrame(TVFrame nextFrame) {
		this.nextFrame = nextFrame;
	}

	public Pia getPia() {
		return pia;
	}

	public void setPia(Pia pia) {
		this.pia = pia;
	}

	// ---

	/**
	 * Gets the RGB color for the playfield object passed as a parameter,
	 * checking the registers.
	 * 
	 * @param obj
	 *            The object whose color is wanted to be checked.
	 * @return The current RGB color of the object
	 */
	private int getColor(PFO obj) {
		int c = 0;

		switch (obj) {
		case P0:
		case M0:
			c = mem[M.COLUP0];
			break;

		case P1:
		case M1:
			c = mem[M.COLUP1];
			break;

		case B:
		case PF:
			c = mem[M.COLUPF];
			break;

		case BG:
			c = mem[M.COLUBK];
			if (c != 0)
				// System.out.println(c);
				break;

		default:
			break;
		}

		// What we do here is check if the B/W switch is on. In that case, we
		// have to draw the pixels in black and white. That means that we are
		// only going use the black and white colors; that is, from 0x00 to
		// 0x0E. So we get rid of the "color" part (the 4 MSB's) and use only
		// the luminosity part (the 4 LSB's)
		if ((pia.read(M.SWCHB) & M.COLOR_SWITCH) == 0) {
			return colorLUT[(c >> 1) & 0x7];
		} else {
			return colorLUT[c >> 1];
		}
	}

	// Look-up table with the RGB color values ordered (0x00, 0x02, etc.)
	private static final int colorLUT[] = { 0x000000, 0x404040, 0x6c6c6c,
			0x909090, 0xb0b0b0, 0xc8c8c8, 0xdcdcdc, 0xffffff, 0x444400,
			0x646410, 0x848424, 0xa0a034, 0xb8b840, 0xd0d050, 0xe8e85c,
			0xfcfc68, 0x702800, 0x844414, 0x985c28, 0xac783c, 0xbc8c4c,
			0xcca05c, 0xdcb468, 0xecc878, 0x841800, 0x983418, 0xac5030,
			0xc06848, 0xd0805c, 0xe09470, 0xeca880, 0xfcbc94, 0x880000,
			0x9c2020, 0xb03c3c, 0xc05858, 0xd07070, 0xe08888, 0xeca0a0,
			0xfcb4b4, 0x78005c, 0x8c2074, 0xa03c88, 0xb0589c, 0xc070b0,
			0xd084c0, 0xdc9cd0, 0xecb0e0, 0x480078, 0x602090, 0x783ca4,
			0x8c58b8, 0xa070cc, 0xb484dc, 0xc49cec, 0xd4b0fc, 0x140084,
			0x302098, 0x4c3cac, 0x6858c0, 0x7c70d0, 0x9488e0, 0xa8a0ec,
			0xbcb4fc, 0x000088, 0x1c209c, 0x3840b0, 0x505cc0, 0x6874d0,
			0x7c8ce0, 0x90a4ec, 0xa4b8fc, 0x00187c, 0x1c3890, 0x3854a8,
			0x5070bc, 0x6888cc, 0x7c9cdc, 0x90b4ec, 0xa4c8fc, 0x002c5c,
			0x1c4c78, 0x386890, 0x5084ac, 0x689cc0, 0x7cb4d4, 0x90cce8,
			0xa4e0fc, 0x003c2c, 0x1c5c48, 0x387c64, 0x509c80, 0x68b494,
			0x7cd0ac, 0x90e4c0, 0xa4fcd4, 0x003c00, 0x205c20, 0x407c40,
			0x5c9c5c, 0x74b474, 0x8cd08c, 0xa4e4a4, 0xb8fcb8, 0x143800,
			0x345c1c, 0x507c38, 0x6c9850, 0x84b468, 0x9ccc7c, 0xb4e490,
			0xc8fca4, 0x2c3000, 0x4c501c, 0x687034, 0x848c4c, 0x9ca864,
			0xb4c078, 0xccd488, 0xe0ec9c, 0x442800, 0x644818, 0x846830,
			0xa08444, 0xb89c58, 0xd0b46c, 0xe8cc7c, 0xfce08c };

	// Got lazy. LUTs for the playfield registers.
	private static final int normalPfBitsLUT[] = { M.BIT_4, M.BIT_4, M.BIT_4,
			M.BIT_4, M.BIT_5, M.BIT_5, M.BIT_5, M.BIT_5, M.BIT_6, M.BIT_6,
			M.BIT_6, M.BIT_6, M.BIT_7, M.BIT_7, M.BIT_7, M.BIT_7, M.BIT_7,
			M.BIT_7, M.BIT_7, M.BIT_7, M.BIT_6, M.BIT_6, M.BIT_6, M.BIT_6,
			M.BIT_5, M.BIT_5, M.BIT_5, M.BIT_5, M.BIT_4, M.BIT_4, M.BIT_4,
			M.BIT_4, M.BIT_3, M.BIT_3, M.BIT_3, M.BIT_3, M.BIT_2, M.BIT_2,
			M.BIT_2, M.BIT_2, M.BIT_1, M.BIT_1, M.BIT_1, M.BIT_1, M.BIT_0,
			M.BIT_0, M.BIT_0, M.BIT_0, M.BIT_0, M.BIT_0, M.BIT_0, M.BIT_0,
			M.BIT_1, M.BIT_1, M.BIT_1, M.BIT_1, M.BIT_2, M.BIT_2, M.BIT_2,
			M.BIT_2, M.BIT_3, M.BIT_3, M.BIT_3, M.BIT_3, M.BIT_4, M.BIT_4,
			M.BIT_4, M.BIT_4, M.BIT_5, M.BIT_5, M.BIT_5, M.BIT_5, M.BIT_6,
			M.BIT_6, M.BIT_6, M.BIT_6, M.BIT_7, M.BIT_7, M.BIT_7, M.BIT_7

	};

	private static final int mirroredPfBitsLut[] = { M.BIT_7, M.BIT_7, M.BIT_7,
			M.BIT_7, M.BIT_6, M.BIT_6, M.BIT_6, M.BIT_6, M.BIT_5, M.BIT_5,
			M.BIT_5, M.BIT_5, M.BIT_4, M.BIT_4, M.BIT_4, M.BIT_4, M.BIT_3,
			M.BIT_3, M.BIT_3, M.BIT_3, M.BIT_2, M.BIT_2, M.BIT_2, M.BIT_2,
			M.BIT_1, M.BIT_1, M.BIT_1, M.BIT_1, M.BIT_0, M.BIT_0, M.BIT_0,
			M.BIT_0, M.BIT_0, M.BIT_0, M.BIT_0, M.BIT_0, M.BIT_1, M.BIT_1,
			M.BIT_1, M.BIT_1, M.BIT_2, M.BIT_2, M.BIT_2, M.BIT_2, M.BIT_3,
			M.BIT_3, M.BIT_3, M.BIT_3, M.BIT_4, M.BIT_4, M.BIT_4, M.BIT_4,
			M.BIT_5, M.BIT_5, M.BIT_5, M.BIT_5, M.BIT_6, M.BIT_6, M.BIT_6,
			M.BIT_6, M.BIT_7, M.BIT_7, M.BIT_7, M.BIT_7, M.BIT_7, M.BIT_7,
			M.BIT_7, M.BIT_7, M.BIT_6, M.BIT_6, M.BIT_6, M.BIT_6, M.BIT_5,
			M.BIT_5, M.BIT_5, M.BIT_5, M.BIT_4, M.BIT_4, M.BIT_4, M.BIT_4

	};

}
