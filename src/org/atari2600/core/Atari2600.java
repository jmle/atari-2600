package org.atari2600.core;

import org.atari2600.tv.TV;

public class Atari2600 {
	private Cpu cpu;
	private Cartridge cart;
	private TV tv;

	public Atari2600(Cartridge cart, TV tv) {
		this.cart = cart;
		this.tv = tv;

		initialize();
	}

	/**
	 * Boots the system and starts execution loop.
	 */
	public void on() {
		int cycles;
		// Boot routine
		cpu.boot();

		// Execute
		while (true) {
			cycles = cpu.executeNext();
			
			for (int i = 0; i < cycles * 3; i++) {
				cpu.getMemory().getTia().executeNext();
			}
			
			cpu.getMemory().getPia().getIo().updateTimer(cycles);
			cpu.getMemory().commit();

			//waitFor(cycles);
		}
	}

	private void initialize() {
		Cpu cpu = new Cpu();

		Memory memory = new Memory();
		Tia tia = new Tia();
		Pia pia = new Pia();
		Ram ram = new Ram();
		IOTimer io = new IOTimer();

		cpu.setMemory(memory);

		memory.setTia(tia);
		memory.setPia(pia);
		memory.setCartridge(cart);

		tia.setCpu(cpu);
		tia.setPia(pia);
		tia.setTv(tv);

		pia.setIo(io);
		pia.setRam(ram);

		this.cpu = cpu;
	}
	
	private void waitFor(int cycles) {
		long start;
		
		for (int i = 0; i < cycles; i++) {
			start = System.nanoTime();
			
			//while(System.nanoTime() - start < 25);
		}
	}

	public Cpu getCpu() {
		return cpu;
	}

	public void setCpu(Cpu cpu) {
		this.cpu = cpu;
	}

	public Cartridge getCart() {
		return cart;
	}

	public void setCart(Cartridge cart) {
		this.cart = cart;
	}

}
