package org.atari2600.core;

import java.io.FileInputStream;

public class Cartridge {
	public int[] mem;

	public Cartridge() {
		mem = new int[4096];
	}

	public Cartridge(String path) {
		this();
		
		dump(path);
	}

	public int read(int addr) {
		return mem[addr];
	}
	
	public void dump(String path) {
		FileInputStream in = null;
		int c, i;
		
		try {
			in = new FileInputStream(path);
			i = 0;
			
			while ((c = in.read()) != -1) {
				mem[i] = c;
				i++;
			}
			
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
