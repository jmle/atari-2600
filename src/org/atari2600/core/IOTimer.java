package org.atari2600.core;

import org.atari2600.util.M;

/**
 * Class representing the IO and Timer parts of the PIA.
 * 
 * @author Juan Manuel Leflet Estrada
 */
public class IOTimer {
	private int[] regs;
	private int swchaWrite, swchbWrite;
	private int tim, interval;
	private boolean timEnable, countdown;

	public IOTimer() {
		regs = new int[6];
		timEnable = false;
		countdown = false;

		// Set switches' default state
		regs[M.SWCHB] |= M.BIT_0; // No reset
		regs[M.SWCHB] |= M.BIT_1; // No select
		regs[M.SWCHB] |= M.BIT_3; // Color
		// Bits 2, 4 and 5 are not connected, so they are always read as '1'
		regs[M.SWCHB] |= (M.BIT_2 | M.BIT_4 | M.BIT_5);
		
		regs[M.SWCHA] = 0xFF;
	}

	public void write(int addr, int data) {
		// Careful: this mapping is more complex than the rest.
		if ((addr & M.BIT_2) == 0) {
			// This writes to SWCHA, SWACNT, SWCHB, SWBCNT
			switch (addr & 0x3) {
			case M.SWCHA:
				swchaWrite = data;
				break;
				
			case M.SWCHB:
				swchbWrite = data;
				break;

			default:
				regs[addr & 0x3] = data;
				break;
			}
		} else {
			if ((addr & M.BIT_4) == 0) {
				// INTIM and INTSTAT: these are mapped to 0x4, 0x5, 0x6, 0x7,
				// 0xC, 0xD, 0xE, 0xF. But they are repeated in pairs: 0x4 and
				// 0x5 are mirrored into 0x6 and 0x7, 0xC and 0xD, 0xE and 0xF
				// respectively. So basically we remove bits A1 and A3 and
				// convert everything to 0x4 and 0x5.
				// --READ ONLY--
				// regs[addr & 0xA] = data;
			} else {
				// TIMERS: mapped to 0x14, 0x15, 0x16 and 0x17 and mirrored from
				// 0x1C onwards.
				setTimer(addr & 0x3, data);
			}
		}

	}

	public int read(int addr) {
		int t = regs[addr & 0x7];

		// INSTAT: undocumented behavior
		switch (addr) {
		case M.INSTAT:
			regs[M.INSTAT] &= ~M.BIT_6;
			break;

		case M.INTIM:
			regs[M.INSTAT] &= ~M.BIT_7;
			break;

		default:
			break;
		}
		return t;
	}

	/**
	 * Sets the proper timer and starts it.
	 * 
	 * @param tim
	 *            : The timer to use.
	 * @param data
	 *            : The value to set.
	 */
	private void setTimer(int tim, int data) {
		switch (tim) {
		case 0:
			// TIM1T
			interval = 1;
			break;

		case 1:
			// TIM8T
			interval = 8;
			break;

		case 2:
			// TIM64T
			interval = 64;
			break;

		case 3:
			// TIM1024T
			interval = 1024;
			break;

		default:
			break;
		}

		// The timer is decremented once immediately after writing (ie. value
		// 00h does immediately underflow). It is then decremented once every N
		// clock cycle interval (depending on the port address used).
		regs[M.INTIM] = (data - 1) & 0xFF;
		timEnable = true;
	}

	/**
	 * Updates the timer. Must be called after each executed instruction.
	 */
	public void updateTimer(int cycles) {
		for (int i = 0; i < cycles; i++) {
			updateTimer();
		}
	}

	/**
	 * Updates the timer.
	 */
	private void updateTimer() {
		// After we checked if the timer is running, we increment tim. This
		// variable increments until the interval is reached, and then is set to
		// 0 to begin counting up to the interval again. The process is repeated
		// until the value at INTIM is 0.
		if (timEnable) {
			tim++; // Increment timer counter

			if (tim == interval) {
				// The PIA decrements the value or count loaded into it once
				// each interval until it reaches 0. It holds that 0 counts
				// for one interval, then the counter flips to FF(HEX) and
				// decrements once each clock cycle, rather than once per
				// interval. The purpose of this feature is to allow the
				// programmer to determine how long ago the timer zeroed out
				// in the event the timer was read after it passed zero.
				tim = 0;

				if (regs[M.INTIM] < 0) {
					timEnable = false;
					countdown = true;
					regs[M.INTIM] = 0xFF;
					regs[M.INSTAT] |= M.BIT_7;
				} else {
					regs[M.INTIM]--;
				}
			}
		} else if (countdown) {
			if (regs[M.INTIM] != 0) {
				regs[M.INTIM]--;
			} else {
				countdown = false;
			}
		}

	}

}
