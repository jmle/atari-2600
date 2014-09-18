package org.atari2600.core;

public class Ram {
	private int[] mem;
	
	public Ram() {
		mem = new int[128];
	}
	
	public int read(int addr) {
		return mem[addr];
	}
	
	public void write(int addr, int data) {
		mem[addr] = data;
	}
}
