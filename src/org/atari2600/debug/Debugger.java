package org.atari2600.debug;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.atari2600.core.Cartridge;
import org.atari2600.core.Cpu;
import org.atari2600.core.IOTimer;
import org.atari2600.core.Memory;
import org.atari2600.core.Pia;
import org.atari2600.core.Ram;
import org.atari2600.core.Tia;
import org.atari2600.tv.TV;
import org.atari2600.util.M;

public class Debugger {
	private Reader reader;

	private Cpu cpu;
	private Cartridge cart;
	private TV tv;

	private ArrayList<Integer> breakpoints;

	public Debugger(Cartridge cart, TV tv) {
		this.cart = cart;
		this.tv = tv;
		reader = new Reader();
		breakpoints = new ArrayList<>();

		initialize();
	}

	/**
	 * Boots the system.
	 */
	public void on() {
		cpu.boot();

		System.out.println("Debugger ON. Select a command:");

		while (true) {
			select();
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
		// pia.setTimer(timer);

		this.cpu = cpu;
	}

	private void waitFor(int cycles) {
		for (int i = 0; i < cycles; i++) {
			System.nanoTime();

			// while(System.nanoTime() - start < 25);
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

	/**
	 * Select the next action to take.
	 */
	public void select() {
		String s;
		String[] ss;
		boolean wrong = true;

		// We keep going until the user inputs something right.
		while (wrong) {
			s = reader.read();
			ss = s.split(" ");

			switch (ss[0]) {
			case "start":
				resume();
				break;

			case "resume":
				resume();
				break;

			case "exe":
				try {
					exe(Integer.parseInt(ss[2]));
				} catch (IndexOutOfBoundsException e) {
					exe(1);
				}
				break;

			case "show":
				show(ss);
				break;

			case "add":
				addBreakpoints(ss);
				break;

			case "help":
				showHelp();
				break;

			default:
				System.out.println("I can't do " + s + "\n");
				break;
			}

			System.out.println("");
		}
	}

	/**
	 * Adds one or more breakpoints to the list.
	 * 
	 * @param ss
	 *            The String input.
	 */
	private void addBreakpoints(String[] ss) {
		for (int i = 1; i < ss.length; i++) {
			int n = Integer.decode(ss[i]);

			breakpoints.add(n & 0x1FFF);
		}
	}

	/**
	 * Executes until a breakpoint is reached. Doesn't execute the instruction
	 * in the breakpoint's address.
	 */
	public void resume() {
		boolean breakpointReached;
		int cycles;

		breakpointReached = false;

		// Execute until breakpoint
		while (!breakpointReached) {
			if (breakpoints.contains(cpu.getPc() & 0x0FFF)) {
				breakpointReached = true;
			} else {
				cycles = cpu.executeNext();

				if (!cpu.getHalted())
					System.out.println(cpu.getLastInstructionName());
				else
					System.out.println("Halted...");

				for (int i = 0; i < cycles * 3; i++) {
					cpu.getMemory().getTia().executeNext();
				}

				waitFor(cycles);
			}
		}
	}

	/**
	 * Executes a number of instructions.
	 * 
	 * @param n
	 *            : The number of instructions to perform.
	 */
	private void exe(int n) {
		boolean breakpointReached = false;
		int cycles = 0;

		for (int i = 0; i < n && !breakpointReached; i++) {
			cycles = cpu.executeNext();

			if (!cpu.getHalted())
				System.out.println(cpu.getLastInstructionName());
			else
				System.out.println("Halted...");

			for (int j = 0; j < cycles * 3; j++) {
				cpu.getMemory().getTia().executeNext();
			}

			waitFor(cycles);

			if (breakpoints.contains(cpu.getPc() & 0x0FFF)) {
				breakpointReached = true;
			}
		}
	}

	/**
	 * Prints the specified register or position in memory
	 * 
	 * @param ss
	 *            The String input.
	 */
	private void show(String[] ss) {
		int print = 0;

		if (ss.length > 1) {
			if (ss[1].equals("mem")) {
				int n = Integer.decode(ss[2]);

				if (n > 0x1FFF || n < 0) {
					System.out.println("Address out of range");
				} else {
					print = cpu.getMemory().read(n);
				}
			} else {
				switch (ss[1]) {
				case "ac":
					print = cpu.getAc();
					break;

				case "x":
					print = cpu.getX();
					break;

				case "y":
					print = cpu.getY();
					break;

				case "sp":
					print = cpu.getSp();
					break;

				case "pc":
					print = cpu.getPc();
					break;

				case "p":
					print = cpu.getP().getProcessorStatus();
					break;

				default:
					Field[] fields = M.class.getFields();
					boolean found = false;

					for (int i = 0; i < fields.length && !found; i++) {
						Field f = fields[i];

						if (f.getName().equals(ss[1])) {
							try {
								print = cpu.getMemory().read((int) f.get(null));
							} catch (Exception e) {

							}
						}
					}

					break;
				}
			}

			System.out.println(Integer.toHexString(print));
		} else {
			System.out.println("What to show??");
		}
	}

	private void showHelp() {
		System.out.println("Help, I need somebody");
		System.out.println("Help, not just anybody");
		System.out.println("Help, you know I need someone");
		System.out.println("Heeelp!");
	}

}
