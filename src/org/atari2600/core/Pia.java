package org.atari2600.core;

import org.atari2600.util.M;

public class Pia {
	private Ram ram;
	private IOTimer io;

	public Pia() {

	}

	public void write(int addr, int data) {
		// We have to decode the address to know where the write is headed to.
		if (((addr & M.BIT_12) == 0) && ((addr & M.BIT_9) == 0)
				&& ((addr & M.BIT_7) != 0)) {
			// RAM chip is addressed by A12==0, A9==0 and A7==0. We AND so it
			// ends up being a number between 0 and 127 (the size of the RAM)
			ram.write(addr & 0x7F, data);
		} else {
			// IO is addressed by A12==0, A9==1 and A7==0.
			io.write(addr & 0x7F, data);
		}
	}

	public int read(int addr) {
		// We have to decode the address to know where the read is headed to.
		if (((addr & M.BIT_12) == 0) && ((addr & M.BIT_9) == 0)
				&& ((addr & M.BIT_7) != 0)) {
			// RAM chip is addressed by A12==0, A9==0 and A7==0
			return ram.read(addr & 0x7F);
		} else {
			// IO is addressed by A12==0, A9==1 and A7==0
			return io.read(addr & 0x1F);
		}
	}

	// Setters & getters --------------------------------

	public Ram getRam() {
		return ram;
	}

	public void setRam(Ram ram) {
		this.ram = ram;
	}

	public IOTimer getIo() {
		return io;
	}

	public void setIo(IOTimer io) {
		this.io = io;
	}

}
