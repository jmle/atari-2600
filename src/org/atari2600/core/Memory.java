package org.atari2600.core;

import org.atari2600.util.M;

/**
 * Class representing the whole memory map. The TIA and the PIA are mapped here,
 * since their registers are accessed by reading/writing from/to them.
 * 
 * @author Juan Manuel Leflet Estrada
 */
public class Memory {
	private Tia tia;
	private Pia pia;
	private Cartridge cartridge;

	private boolean waitForCommit;
	private int addr, data;

	public Memory() {
		waitForCommit = false;
		addr = 0;
		data = 0;
	}

	/**
	 * Sets a write ready for commitment, but the method actually writing is
	 * commit.
	 * 
	 * @param addr
	 *            The address to write to.
	 * @param data
	 *            The data to write.
	 */
	public void write(int addr, int data) {
		waitForCommit = true;
		this.addr = addr;
		this.data = data;
	}

	/**
	 * Since the CPU and the TIA must be tightly synchronized, writes are only
	 * performed after the TIA has executed all its cycles. This function must
	 * be called every time after a CPU execution + TIA executions.
	 */
	public void commit() {
		// Only write if there was something to write.
		if (waitForCommit) {
			// We have to decode the address to know where the write is headed
			// to.
			if (((addr & M.BIT_7) == 0) && ((addr & M.BIT_12) == 0)) {
				// TIA chip is addressed by A12==0 and A7==0
				tia.write(addr & 0x3F, data);
			} else if (((addr & M.BIT_7) == M.BIT_7)
					&& ((addr & M.BIT_12) == 0)) {
				// PIA is addressed by A12==0 and A7==1
				pia.write(addr, data);
			} else {
				System.out.println("Cagada");
			}

			waitForCommit = false;
		}
	}

	public int read(int addr) {
		// We have to decode the address to know where the read is headed to.
		if ((addr & M.BIT_12) != 0) {
			// Cartridge is addressed by A12 == 1. Since A12 doesn't belong to
			// the effective address, we remove it.
			return cartridge.read(addr & 0xFFF);
		} else if (((addr & M.BIT_7) == 0) && ((addr & M.BIT_12) == 0)) {
			// TIA chip is addressed by A12==0 and A7==0
			return tia.read(addr & 0x3F);
		} else if (((addr & M.BIT_7) == M.BIT_7) && ((addr & M.BIT_12) == 0)) {
			// PIA is addressed by A12==0 and A7==1
			return pia.read(addr);
		}

		return 0;
	}

	public int getSize() {
		return 0;
	}

	public Tia getTia() {
		return tia;
	}

	public void setTia(Tia tia) {
		this.tia = tia;
	}

	public Pia getPia() {
		return pia;
	}

	public void setPia(Pia pia) {
		this.pia = pia;
	}

	public Cartridge getCartridge() {
		return cartridge;
	}

	public void setCartridge(Cartridge cartridge) {
		this.cartridge = cartridge;
	}

}
