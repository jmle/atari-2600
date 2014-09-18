package org.atari2600.tv;

import org.atari2600.core.COL;
import org.atari2600.core.PFO;

/**
 * Class representing a single pixel. It has a color and an object type to check
 * collisions.
 * 
 * @author Juan Manuel Leflet Estrada
 * 
 */
public class Pixel {
	private int color;
	private PFO obj;

	/**
	 * Default constructor. Creates a black BG pixel.
	 */
	public Pixel() {
		color = 0;

		// Is necessary to instantiate the pixel object by default, in case we
		// are modifying from a previous color clock because of HMOVE
		obj = PFO.BG;
	}

	public Pixel(int color) {
		this.color = color;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public PFO getObj() {
		return obj;
	}

	public void setObj(PFO obj) {
		this.obj = obj;
	}

	/**
	 * Checks if there is a collision with the given object and returns it.
	 * 
	 * @param obj
	 *            : The object to check collisions with.
	 * @return The collision (may be NONE)
	 */
	public COL checkCollision(PFO obj) {
		COL res = COL.NONE;

		// The background never makes collisions
		if (this.obj != PFO.BG) {
			// Check all the different possibilities:
			if (this.obj == PFO.PF) {
				if (obj == PFO.P0) {
					res = COL.P0_PF;
				} else if (obj == PFO.M0) {
					res = COL.M0_PF;
				} else if (obj == PFO.P1) {
					res = COL.P1_PF;
				} else if (obj == PFO.M1) {
					res = COL.M1_PF;
				} else if (obj == PFO.B) {
					res = COL.BL_PF;
				}
			} else if (this.obj == PFO.B) {
				if (obj == PFO.P0) {
					res = COL.P0_BL;
				} else if (obj == PFO.M0) {
					res = COL.M0_BL;
				} else if (obj == PFO.P1) {
					res = COL.P1_BL;
				} else if (obj == PFO.M1) {
					res = COL.M1_BL;
				} else if (obj == PFO.PF) {
					res = COL.BL_PF;
				}
			} else if (this.obj == PFO.P0) {
				if (obj == PFO.M0) {
					res = COL.M0_P0;
				} else if (obj == PFO.P1) {
					res = COL.P0_P1;
				} else if (obj == PFO.M1) {
					res = COL.M1_P0;
				} else if (obj == PFO.PF) {
					res = COL.P0_PF;
				} else if (obj == PFO.B) {
					res = COL.P0_BL;
				}
			} else if (this.obj == PFO.M0) {
				if (obj == PFO.PF) {
					res = COL.M0_PF;
				} else if (obj == PFO.P0) {
					res = COL.M0_P0;
				} else if (obj == PFO.P1) {
					res = COL.M0_P1;
				} else if (obj == PFO.M1) {
					res = COL.M0_M1;
				} else if (obj == PFO.B) {
					res = COL.M0_BL;
				}
			} else if (this.obj == PFO.P1) {
				if (obj == PFO.PF) {
					res = COL.P1_PF;
				} else if (obj == PFO.P0) {
					res = COL.P0_P1;
				} else if (obj == PFO.M0) {
					res = COL.M0_P1;
				} else if (obj == PFO.M1) {
					res = COL.M0_P1;
				} else if (obj == PFO.B) {
					res = COL.P1_BL;
				}
			} else if (this.obj == PFO.M1) {
				if (obj == PFO.PF) {
					res = COL.M1_PF;
				} else if (obj == PFO.P0) {
					res = COL.M1_P1;
				} else if (obj == PFO.P1) {
					res = COL.M1_P1;
				} else if (obj == PFO.M0) {
					res = COL.M0_M1;
				} else if (obj == PFO.B) {
					res = COL.M1_BL;
				}
			}
		}

		return res;
	}

	public boolean hasHigherPriorityThan(PFO pfo) {
		// Declaration order inside the ENUM is used to compare them. Since they
		// are declared from less priority on, this comes really handy.
		return obj.compareTo(pfo) > 0;
	}

}
