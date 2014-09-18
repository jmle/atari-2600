package org.atari2600.test;

import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.atari2600.core.Atari2600;
import org.atari2600.core.Cartridge;
import org.atari2600.debug.Debugger;
import org.atari2600.tv.TV;

public class Main {
	public static void main(String args[]) {
		TV tv = new TV();
		// Cartridge cart = new Cartridge("roms/kernel_15.bin");
		// Cartridge cart = new Cartridge("roms/3_Bars_Background.bin");
		// Cartridge cart = new Cartridge("roms/sp.bin");
		// Cartridge cart = new Cartridge("roms/Right_Scrolling.bin");
		// Cartridge cart = new Cartridge("roms/Surround.bin");
		// Cartridge cart = new Cartridge("roms/fullscrn.bin");
		// Cartridge cart = new Cartridge("roms/kernel_13.bin");
		// Cartridge cart = new Cartridge("roms/Background_Scrolling.bin");
		// Cartridge cart = new Cartridge("roms/asymmetrical2.bin");
		// Cartridge cart = new Cartridge("roms/asymmetrical.bin");
		Cartridge cart = new Cartridge("roms/heart_color.bin");
		Atari2600 atari = new Atari2600(cart, tv);

		JFrame frame = new JFrame();
		frame.setTitle("Atari 2600");
		frame.setSize(228 * 3, 262 * 2);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		Container contentPane = frame.getContentPane();

		contentPane.add(tv);
		frame.setVisible(true);

		if (args.length >= 1) {
			if (args[0].equals("-d")) {
				new Debugger(cart, tv).on();
			} else {
				System.out.println("Ein?");
			}
		} else {
			atari.on();
		}
	}
}
