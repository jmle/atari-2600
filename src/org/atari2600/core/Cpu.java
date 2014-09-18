package org.atari2600.core;

import org.atari2600.util.M;

/**
 * Class that represents the 6507 chip.
 * 
 * @author Juan Manuel Leflet Estrada
 */
public class Cpu {
	private Memory mem;
	private ProcessorStatus p;
	private int pc, sp, ac, x, y;
	private int instruction;
	private int cycles;

	private boolean halted;

	// Page Boundary Crossed
	private boolean pbCrossed;

	// For debugging purposes
	private int operand;
	private String lastInstructionName;
	private String nextInstructionName;

	public Cpu() {
		p = new ProcessorStatus();

		pc = 0;
		sp = 0;
		ac = 0;
		x = 0;
		y = 0;
		instruction = 0;
		cycles = 0;

		halted = false;
		pbCrossed = false;
	}

	/**
	 * Looks for the start vector (the 2 first of the last 4 bytes of the
	 * binary) and sets the pc to that value.
	 */
	public void boot() {
		int h, l;

		l = mem.read(0x17FC);
		h = mem.read(0x17FD);
		// l = mem.read(0x1FFC);
		// h = mem.read(0x1FFD);

		pc = (h << 8) | l;
	}

	// CPU control ------------------------------------

	/**
	 * Halts the CPU.
	 */
	public void halt() {
		if (!halted) {
			halted = true;
		}
	}

	/**
	 * Resumes execution of the CPU.
	 */
	public void resume() {
		if (halted) {
			halted = false;
		}
	}

	/**
	 * Resets the CPU (but not the memory).
	 */
	public void reset() {
		p = new ProcessorStatus();

		pc = 0;
		sp = 0;
		ac = 0;
		x = 0;
		y = 0;
		instruction = 0;
		cycles = 0;

		halted = false;
	}

	/**
	 * Executes the next instruction, checking if the cpu is halted, and updates
	 * the PIA timer.
	 * 
	 * @return The number of cycles executed if the processor wasn't halting, 1
	 *         otherwise. Actually, no cycles are executed, but the clock is
	 *         still running. Since the Atari2600 class depends on the number of
	 *         cycles executed here to run the TIA, we return 1 if the cpu is
	 *         halted.
	 */
	public int executeNext() {
		int cycles;

		if (!halted) {
			cycles = execute();
		} else {
			cycles = 1;
		}

		return cycles;
	}

	/**
	 * Executes the next instruction.
	 * 
	 * @return The number of cycles executed.
	 */
	private int execute() {
		int resCycles = 0;

		if ((pc & 0xFFF) == 0x014) {
		//	System.out.println("STOP");
		}

		// Read and increment afterwards.
		instruction = mem.read(pc++);

		// Get the instruction name (without the operand yet).
		lastInstructionName = Integer.toHexString(pc - 1) + " - "
				+ M.index[instruction];

		// System.out.println(lastInstructionName);

		// On each case, we execute the instruction passing the direction where
		// the data is stored (if necessary), and set the cycles consumed.
		switch (instruction) {
		// ADC's
		case 0x69:
			adc(imm());
			resCycles = 2;
			break;

		case 0x65:
			adc(zp());
			resCycles = 3;
			break;

		case 0x75:
			adc(zpx());
			resCycles = 4;
			break;

		case 0x6D:
			adc(abs());
			resCycles = 4;
			break;

		case 0x7D:
			adc(abx());
			resCycles = pbCrossed ? 5 : 4;
			break;

		case 0x79:
			adc(aby());
			resCycles = pbCrossed ? 5 : 4;
			break;

		case 0x61:
			adc(indx());
			resCycles = 6;
			break;

		case 0x71:
			adc(indy());
			resCycles = pbCrossed ? 6 : 5;
			break;

		// AND's
		case 0x29:
			and(imm());
			resCycles = 2;
			break;

		case 0x25:
			and(zp());
			resCycles = 2;
			break;

		case 0x35:
			and(zpx());
			resCycles = 3;
			break;

		case 0x2D:
			and(abs());
			resCycles = 4;
			break;

		case 0x3D:
			and(abx());
			resCycles = pbCrossed ? 5 : 4;
			break;

		case 0x39:
			and(aby());
			resCycles = pbCrossed ? 5 : 4;
			break;

		case 0x21:
			and(indx());
			resCycles = 6;
			break;

		case 0x31:
			and(indy());
			resCycles = pbCrossed ? 6 : 5;
			break;

		// ASL's
		case 0x0A:
			asla();
			resCycles = 2;
			break;

		case 0x06:
			asl(zp());
			resCycles = 5;
			break;

		case 0x16:
			asl(zpx());
			resCycles = 6;
			break;

		case 0x0E:
			asl(abs());
			resCycles = 6;
			break;

		case 0x1E:
			asl(abx());
			resCycles = 7;
			break;

		// BCC
		case 0x90:
			resCycles = (bcc(rel())) ? ((pbCrossed) ? 4 : 3) : 2;
			break;

		// BCS
		case 0xB0:
			resCycles = (bcs(rel())) ? ((pbCrossed) ? 4 : 3) : 2;
			break;

		// BEQ
		case 0xF0:
			resCycles = (beq(rel())) ? ((pbCrossed) ? 4 : 3) : 2;
			break;

		// BIT
		case 0x24:
			bit(zp());
			resCycles = 3;
			break;

		case 0x2C:
			bit(abs());
			resCycles = 4;
			break;

		// BMI
		case 0x30:
			resCycles = (bmi(rel())) ? ((pbCrossed) ? 4 : 3) : 2;
			break;

		// BNE
		case 0xD0:
			resCycles = (bne(rel())) ? ((pbCrossed) ? 4 : 3) : 2;
			break;

		// BPL
		case 0x10:
			resCycles = (bpl(rel())) ? ((pbCrossed) ? 4 : 3) : 2;
			break;

		// BRK
		case 0x00:
			brk();
			resCycles = 7;
			break;

		// BVC
		case 0x50:
			resCycles = (bvc(rel())) ? ((pbCrossed) ? 4 : 3) : 2;
			break;

		// BVS
		case 0x70:
			resCycles = (bvs(rel())) ? ((pbCrossed) ? 4 : 3) : 2;
			break;

		case 0x18:
			clc();
			resCycles = 2;
			break;

		case 0xD8:
			cld();
			resCycles = 2;
			break;

		case 0x58:
			cli();
			resCycles = 2;
			break;

		case 0xB8:
			clv();
			break;

		// CMP
		case 0xC9:
			cmp(imm(), R.A);
			resCycles = 2;
			break;

		case 0xC5:
			cmp(zp(), R.A);
			resCycles = 3;
			break;

		case 0xD5:
			cmp(zpx(), R.A);
			resCycles = 4;
			break;

		case 0xCD:
			cmp(abs(), R.A);
			resCycles = 4;
			break;

		case 0xDD:
			cmp(abx(), R.A);
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		case 0xD9:
			cmp(aby(), R.A);
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		case 0xC1:
			cmp(indx(), R.A);
			resCycles = 6;
			break;

		case 0xD1:
			cmp(indy(), R.A);
			resCycles = (pbCrossed) ? 6 : 5;
			break;

		// CPX
		case 0xE0:
			cmp(imm(), R.X);
			resCycles = 2;
			break;

		case 0xE4:
			cmp(zp(), R.X);
			resCycles = 3;
			break;

		case 0xEC:
			cmp(abs(), R.X);
			resCycles = 4;
			break;

		// CPY
		case 0xC0:
			cmp(imm(), R.Y);
			resCycles = 2;
			break;

		case 0xC4:
			cmp(zp(), R.Y);
			resCycles = 3;
			break;

		case 0xCC:
			cmp(abs(), R.Y);
			resCycles = 4;
			break;

		// DEC
		case 0xC6:
			dec(zp());
			resCycles = 5;
			break;

		case 0xD6:
			dec(zpx());
			resCycles = 6;
			break;

		case 0xCE:
			dec(abs());
			resCycles = 6;
			break;

		case 0xDE:
			dec(abx());
			resCycles = 7;
			break;

		// DEX
		case 0xCA:
			decxy(R.X);
			resCycles = 2;
			break;

		// DEY
		case 0x88:
			decxy(R.Y);
			resCycles = 2;
			break;

		// EOR
		case 0x49:
			eor(imm());
			resCycles = 2;
			break;

		case 0x45:
			eor(zp());
			resCycles = 3;
			break;

		case 0x55:
			eor(zpx());
			resCycles = 4;
			break;

		case 0x4D:
			eor(abs());
			resCycles = 4;
			break;

		case 0x5D:
			eor(abx());
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		case 0x59:
			eor(aby());
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		case 0x41:
			eor(indx());
			resCycles = 6;
			break;

		case 0x51:
			eor(indy());
			resCycles = (pbCrossed) ? 6 : 5;
			break;

		// INC
		case 0xE6:
			inc(zp());
			resCycles = 5;
			break;

		case 0xF6:
			inc(zpx());
			resCycles = 6;
			break;

		case 0xEE:
			inc(abs());
			resCycles = 6;
			break;

		case 0xFE:
			inc(abx());
			resCycles = 7;
			break;

		// INX
		case 0xE8:
			incxy(R.X);
			resCycles = 2;
			break;

		// INY
		case 0xC8:
			incxy(R.Y);
			resCycles = 2;
			break;

		// JMP
		case 0x4C:
			jmp(abs());
			resCycles = 3;
			break;

		case 0x6C:
			jmp(ind());
			resCycles = 5;
			break;

		// JSR
		case 0x20:
			jsr(abs());
			resCycles = 6;
			break;

		// LDA
		case 0xA9:
			ldr(imm(), R.A);
			resCycles = 2;
			break;

		case 0xA5:
			ldr(zp(), R.A);
			resCycles = 3;
			break;

		case 0xB5:
			ldr(zpx(), R.A);
			resCycles = 4;
			break;

		case 0xAD:
			ldr(abs(), R.A);
			resCycles = 4;
			break;

		case 0xBD:
			ldr(abx(), R.A);
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		case 0xB9:
			ldr(aby(), R.A);
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		case 0xA1:
			ldr(indx(), R.A);
			resCycles = 6;
			break;

		case 0xB1:
			ldr(indy(), R.A);
			resCycles = (pbCrossed) ? 6 : 5;
			break;

		// LDX
		case 0xA2:
			ldr(imm(), R.X);
			resCycles = 2;
			break;

		case 0xA6:
			ldr(zp(), R.X);
			resCycles = 3;
			break;

		case 0xB6:
			ldr(zpy(), R.X);
			resCycles = 4;
			break;

		case 0xAE:
			ldr(abs(), R.X);
			resCycles = 4;
			break;

		case 0xBE:
			ldr(aby(), R.X);
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		// LDY
		case 0xA0:
			ldr(imm(), R.Y);
			resCycles = 2;
			break;

		case 0xA4:
			ldr(zp(), R.Y);
			resCycles = 3;
			break;

		case 0xB4:
			ldr(zpx(), R.Y);
			resCycles = 4;
			break;

		case 0xAC:
			ldr(abs(), R.Y);
			resCycles = 4;
			break;

		case 0xBC:
			ldr(abx(), R.Y);
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		// LSR
		case 0x4A:
			lsra();
			resCycles = 2;
			break;

		case 0x46:
			lsrm(zp());
			resCycles = 5;
			break;

		case 0x56:
			lsrm(zpx());
			resCycles = 6;
			break;

		case 0x4E:
			lsrm(abs());
			resCycles = 6;
			break;

		case 0x5E:
			lsrm(abx());
			resCycles = 7;
			break;

		// NOP
		case 0xEA:
			nop();
			resCycles = 2;
			break;

		// ORA
		case 0x09:
			ora(imm());
			resCycles = 2;
			break;

		case 0x05:
			ora(imm());
			resCycles = 2;
			break;

		case 0x15:
			ora(imm());
			resCycles = 3;
			break;

		case 0x0D:
			ora(imm());
			resCycles = 4;
			break;

		case 0x1D:
			ora(imm());
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		case 0x19:
			ora(imm());
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		case 0x01:
			ora(imm());
			resCycles = 6;
			break;

		case 0x11:
			ora(imm());
			resCycles = (pbCrossed) ? 6 : 5;
			break;

		// PHA
		case 0x48:
			pha();
			resCycles = 3;
			break;

		// PHP
		case 0x08:
			php();
			resCycles = 3;
			break;

		// PLA
		case 0x68:
			pla();
			resCycles = 4;
			break;

		// PLP
		case 0x28:
			plp();
			resCycles = 4;
			break;

		// ROL
		case 0x2A:
			rola();
			resCycles = 2;
			break;

		case 0x26:
			rolm(zp());
			resCycles = 5;
			break;

		case 0x36:
			rolm(zpx());
			resCycles = 6;
			break;

		case 0x2E:
			rolm(abs());
			resCycles = 6;
			break;

		case 0x3E:
			rolm(abx());
			resCycles = 7;
			break;

		// ROR
		case 0x6A:
			rora();
			resCycles = 2;
			break;

		case 0x66:
			rorm(zp());
			resCycles = 5;
			break;

		case 0x76:
			rorm(zpx());
			resCycles = 6;
			break;

		case 0x6E:
			rorm(abs());
			resCycles = 6;
			break;

		case 0x7E:
			rorm(abx());
			resCycles = 7;
			break;

		// RTI
		case 0x40:
			rti();
			resCycles = 6;
			break;

		// RTS
		case 0x60:
			rts();
			resCycles = 6;
			break;

		// SBC
		case 0xE9:
			sbc(imm());
			resCycles = 2;
			break;

		case 0xE5:
			sbc(zp());
			resCycles = 3;
			break;

		case 0xF5:
			sbc(zpx());
			resCycles = 4;
			break;

		case 0xED:
			sbc(abs());
			resCycles = 4;
			break;

		case 0xFD:
			sbc(abx());
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		case 0xF9:
			sbc(aby());
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		case 0xE1:
			sbc(indx());
			resCycles = 6;
			break;

		case 0xF1:
			sbc(indy());
			resCycles = (pbCrossed) ? 6 : 5;
			break;

		// SEC
		case 0x38:
			sec();
			resCycles = 2;
			break;

		// SED
		case 0xF8:
			sed();
			resCycles = 2;
			break;

		// SEI
		case 0x78:
			sei();
			resCycles = 2;
			break;

		// STA
		case 0x85:
			st(zp(), R.A);
			resCycles = 3;
			break;

		case 0x95:
			st(zpx(), R.A);
			resCycles = 4;
			break;

		case 0x8D:
			st(abs(), R.A);
			resCycles = 4;
			break;

		case 0x9D:
			st(abx(), R.A);
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		case 0x99:
			st(aby(), R.A);
			resCycles = (pbCrossed) ? 5 : 4;
			break;

		case 0x81:
			st(indx(), R.A);
			resCycles = 6;
			break;

		case 0x91:
			st(indy(), R.A);
			resCycles = (pbCrossed) ? 6 : 5;
			break;

		// STX
		case 0x86:
			st(zp(), R.X);
			resCycles = 3;
			break;

		case 0x96:
			st(zpy(), R.X);
			resCycles = 4;
			break;

		case 0x8E:
			st(abs(), R.X);
			resCycles = 4;
			break;

		// STY
		case 0x84:
			st(zp(), R.Y);
			resCycles = 3;
			break;

		case 0x94:
			st(zpx(), R.Y);
			resCycles = 4;
			break;

		case 0x8C:
			st(abs(), R.Y);
			resCycles = 4;
			break;

		// TAX
		case 0xAA:
			taxy(R.X);
			resCycles = 2;
			break;

		// TAY
		case 0xA8:
			taxy(R.Y);
			resCycles = 2;
			break;

		// TSX
		case 0xBA:
			tsx();
			resCycles = 2;
			break;

		// TXA
		case 0x8A:
			txya(R.X);
			resCycles = 2;
			break;

		// TXS
		case 0x9A:
			txs();
			resCycles = 2;
			break;

		// TYA
		case 0x98:
			txya(R.Y);
			resCycles = 2;
			break;

		default:
			break;
		}

		cycles += resCycles;

		return resCycles;
	}

	/*
	 * Executing methods. Information was mostly taken from
	 * http://homepage.ntlworld.com/cyborgsystems/CS_Main/6502/6502.htm
	 */
	private void adc(int addr) {
		int data = mem.read(addr);

		// Calculate auxiliary value
		int t = ac + data + p.c;

		// Set flags: overflow, sign, zero, and carry.
		p.v = ((ac & M.BIT_7) != (t & M.BIT_7)) ? 1 : 0;
		p.setN(t);
		p.setZ(t);

		if (p.d == 1) {
			t = bcd(ac) + bcd(data) + p.c;
			p.c = (t > 99) ? 1 : 0;
		} else {
			p.c = (t > 255) ? 1 : 0;
		}

		// Mask 0xFF to take the possible "carry" out.
		ac = t & 0xFF;
	}

	private void and(int addr) {
		int data = mem.read(addr);

		ac &= data;

		// Set flags: sign, zero.
		p.setN(ac);
		p.setZ(ac);
	}

	private void asla() {
		p.c = ((ac & M.BIT_7) == M.BIT_7) ? 1 : 0;
		ac <<= 1;

		p.setN(ac);
		p.setZ(ac);
	}

	private void asl(int addr) {
		int data = mem.read(addr);

		p.c = ((data & M.BIT_7) == M.BIT_7) ? 1 : 0;
		data = (data << 1) & 0xFE;

		p.setN(data);
		p.setZ(data);

		mem.write(addr, data);
	}

	private boolean bcc(int addr) {
		if (p.c == 0) {
			pc = addr;
			return true;
		}

		return false;
	}

	private boolean bcs(int addr) {
		if (p.c == 1) {
			pc = addr;
			return true;
		}

		return false;
	}

	private boolean beq(int addr) {
		if (p.z == 1) {
			pc = addr;
			return true;
		}

		return false;
	}

	private void bit(int addr) {
		int data = mem.read(addr) & ac;

		p.setN(data);
		p.v = ((data & M.BIT_6) != 0) ? 1 : 0;
		p.setZ(data);
	}

	private boolean bmi(int addr) {
		if (p.n == 1) {
			pc = addr;
			return true;
		}

		return false;
	}

	private boolean bne(int addr) {
		if (p.z == 0) {
			pc = addr;
			return true;
		}

		return false;
	}

	private boolean bpl(int addr) {
		if (p.n == 0) {
			pc = addr;
			return true;
		}

		return false;
	}

	private void brk() {
		int l, h;

		// Even though the brk instruction is just one byte long, the pc is
		// incremented, meaning that the instruction after brk is ignored.
		pc++;

		mem.write(sp, pc & 0xF0);
		mem.commit();
		sp--;
		mem.write(sp, pc & 0xF);
		mem.commit();
		sp--;
		mem.write(sp, p.b);
		sp--;

		l = mem.read(0xFFFE);
		h = mem.read(0xFFFF) << 8;

		pc = h | l;
	}

	private boolean bvc(int addr) {
		if (p.v == 0) {
			pc = addr;
			return true;
		}

		return false;
	}

	private boolean bvs(int addr) {
		if (p.v == 1) {
			pc = addr;
			return true;
		}

		return false;
	}

	private void clc() {
		p.c = 0;
	}

	private void cld() {
		p.d = 0;
	}

	private void cli() {
		p.i = 0;
	}

	private void clv() {
		p.v = 0;
	}

	private void cmp(int addr, R r) {
		int data = mem.read(addr);

		// Calculate auxiliary value
		int t = 0;
		switch (r) {
		case A:
			t = ac - data;
			p.c = (ac >= data) ? 1 : 0;
			break;

		case X:
			t = x - data;
			p.c = (x >= data) ? 1 : 0;
			break;

		case Y:
			t = y - data;
			p.c = (y >= data) ? 1 : 0;
			break;

		default:
			break;
		}

		// Set flags
		p.setN(t);
		p.setZ(t);
	}

	private void dec(int addr) {
		int data = mem.read(addr);

		// Decrement & AND 0xFF
		data = --data & 0xFF;
		mem.write(addr, data);

		p.setN(data);
		p.setZ(data);
	}

	private void decxy(R r) {
		switch (r) {
		case X:
			x = (x - 1) & 0xFF;
			p.setN(x);
			p.setZ(x);
			break;

		case Y:
			y = (y - 1) & 0xFF;
			p.setN(y);
			p.setZ(y);
			break;

		default:
			break;
		}
	}

	private void eor(int addr) {
		int data = mem.read(addr);

		ac ^= data;
		p.setN(ac);
		p.setZ(ac);
	}

	private void inc(int addr) {
		int data = mem.read(addr);

		data = ++data & 0xFF;
		mem.write(addr, data);

		p.setN(data);
		p.setZ(data);
	}

	private void incxy(R r) {
		switch (r) {
		case X:
			x = (x + 1) & 0xFF;
			p.setN(x);
			p.setZ(x);
			break;

		case Y:
			y = (y + 1) & 0xFF;
			p.setN(y);
			p.setZ(y);
			break;

		default:
			break;
		}
	}

	private void jmp(int addr) {
		pc = addr;
	}

	private void jsr(int addr) {
		int t = pc - 1;

		// Push PC onto the stack
		mem.write(sp, (t & 0xFF00) >> 8);
		mem.commit();
		sp--;
		mem.write(sp, t & 0xFF);
		sp--;

		// Jump
		pc = addr;
	}

	private void ldr(int addr, R r) {
		int data = mem.read(addr);

		// One function for three different opcodes. Have to switch the register
		switch (r) {
		case A:
			ac = data;
			p.setN(ac);
			p.setZ(ac);
			break;

		case X:
			x = data;
			p.setN(x);
			p.setZ(x);
			break;

		case Y:
			y = data;
			p.setN(y);
			p.setZ(y);
			break;

		default:
			break;
		}
	}

	private void lsra() {
		p.n = 0;
		p.c = ((ac & M.BIT_0) == 0) ? 0 : 1;
		ac = (ac >> 1) & 0x7F;
		p.setZ(ac);
	}

	private void lsrm(int addr) {
		int data = mem.read(addr);

		p.n = 0;
		p.c = ((data & M.BIT_0) == 0) ? 0 : 1;
		data = (data >> 1) & 0x7F;
		p.setZ(data);

		mem.write(addr, data);
	}

	private void nop() {

	}

	private void ora(int addr) {
		int data = mem.read(addr);

		ac |= data;
		p.setN(data);
		p.setZ(data);
	}

	private void pha() {
		mem.write(sp, ac);
		sp--;
	}

	private void php() {
		mem.write(sp, p.getProcessorStatus());
		sp--;
	}

	private void pla() {
		sp++;
		ac = mem.read(sp);

		p.setN(ac);
		p.setZ(ac);
	}

	private void plp() {
		sp++;
		p.setProcessorStatus(mem.read(sp));
	}

	private void rola() {
		// This opcode uses the carry to fill the LSB, and then sets the carry
		// according to the MSB of the rolled byte

		// Take from the byte what will be the future carry
		int t = ((ac & M.BIT_7) != 0) ? 1 : 0;

		// Rotate left and &
		ac = (ac << 1) & 0xFE;
		// Set LSB with the carry value from before the operation
		ac |= p.c;
		// Set the next carry
		p.c = t;
		// Set flags
		p.setZ(ac);
		p.setN(ac);
	}

	private void rolm(int addr) {
		int data = mem.read(addr);
		int t = ((data & M.BIT_7) != 0) ? 1 : 0;

		// Rotate left and &
		data = (data << 1) & 0xFE;
		// Set LSB with the carry value from before the operation
		data |= p.c;
		// Set the next carry
		p.c = t;
		// Set flags
		p.setZ(data);
		p.setN(data);

		// Write to memory
		mem.write(addr, data);
	}

	private void rora() {
		// This opcode uses the carry to fill the MSB, and then sets the carry
		// according to the LSB of the rolled byte

		// Take from the byte what will be the future carry
		int t = ((ac & M.BIT_0) != 0) ? 1 : 0;

		// Rotate right and &
		ac = (ac >> 1) & 0x7F;
		// Set MSB with the carry value from before the operation
		ac |= (((p.c == 1) ? 0x80 : 0x00));
		// Set the next carry
		p.c = t;
		// Set flags
		p.setZ(ac);
		p.setN(ac);
	}

	private void rorm(int addr) {
		int data = mem.read(addr);
		int t = ((data & M.BIT_0) != 0) ? 1 : 0;

		// Rotate right and &
		data = (data >> 1) & 0x7F;
		// Set LSB with the carry value from before the operation
		data |= (((p.c == 1) ? 0x80 : 0x00));
		// Set the next carry
		p.c = t;
		// Set flags
		p.setZ(data);
		p.setN(data);

		// Write to memory
		mem.write(addr, data);
	}

	private void rti() {
		int l, h;

		sp--;
		p.setProcessorStatus(mem.read(sp));
		sp--;
		l = mem.read(sp);
		sp--;
		h = mem.read(sp);

		pc = (h << 8) | l;
	}

	private void rts() {
		int l, h;

		sp++;
		l = mem.read(sp);
		sp++;
		h = mem.read(sp);

		pc = ((h << 8) | l) + 1;
	}

	private void sbc(int addr) {
		int data = mem.read(addr);
		int t;

		// If decimal mode is on...
		if (p.d == 1) {
			// When using SBC, the code should have used SEC to set the carry
			// before. This is to make sure that, if we need to borrow, there is
			// something to borrow.
			t = bcd(ac) - bcd(data) - (((p.c & M.BIT_0) != 0) ? 0 : 1);
			p.v = (t > 99 || t < 0) ? 1 : 0;
		} else {
			t = ac - data - (((p.c & M.BIT_0) != 0) ? 0 : 1);
			p.v = (t > 127 || t < -128) ? 1 : 0;
		}

		// Set the flags
		p.c = (t >= 0) ? 1 : 0;
		p.setN(t);
		p.setZ(t);

		// Write the result (ANDed, just in case it overflowed)
		ac = t & 0xFF;
	}

	private void sec() {
		p.c = 1;
	}

	private void sed() {
		p.d = 1;
	}

	private void sei() {
		p.i = 1;
	}

	private void st(int addr, R r) {
		switch (r) {
		case A:
			mem.write(addr, ac);
			break;

		case X:
			mem.write(addr, x);
			break;

		case Y:
			mem.write(addr, y);
			break;

		default:
			break;
		}
	}

	private void taxy(R r) {
		switch (r) {
		case X:
			x = ac;
			p.setN(x);
			p.setZ(x);
			break;

		case Y:
			y = ac;
			p.setN(y);
			p.setZ(y);
			break;

		default:
			break;
		}
	}

	private void tsx() {
		x = sp;
		p.setN(x);
		p.setZ(x);
	}

	private void txya(R r) {
		switch (r) {
		case X:
			ac = x;
			break;

		case Y:
			ac = y;
			break;

		default:
			break;
		}

		p.setN(ac);
		p.setZ(ac);
	}

	private void txs() {
		sp = x;
	}

	// -----------------------------------
	// Addressing modes
	// - Page crossing is checked
	// - The operand is retrieved and stored for debugging purposes
	// -----------------------------------

	/**
	 * Immediate: The operand is used directly to perform the computation.
	 */
	private int imm() {
		operand = mem.read(pc + 1);
		return pc++;
	}

	/**
	 * Zero page: A single byte specifies an address in the first page of mem
	 * ($00xx), also known as the zero page, and the byte at that address is
	 * used to perform the computation.
	 */
	private int zp() {
		operand = mem.read(pc++) & 0xFF;
		return operand;
	}

	/**
	 * Zero page,X: The value in X is added to the specified zero page address
	 * for a sum address. The value at the sum address is used to perform the
	 * computation.
	 */
	private int zpx() {
		operand = mem.read(pc++);
		return (operand + x) & 0xFF;
	}

	/**
	 * Zero page,Y: The value in Y is added to the specified zero page address
	 * for a sum address. The value at the sum address is used to perform the
	 * computation.
	 */
	private int zpy() {
		operand = mem.read(pc++);
		return (operand + y) & 0xFF;
	}

	/**
	 * The offset specified is added to the current address stored in the
	 * Program Counter (PC). Offsets can range from -128 to +127.
	 */
	private int rel() {
		operand = mem.read(pc++);
		int offset = (int) ((byte) operand);
		int addr = pc + offset;

		pageBoundaryCrossed(pc, addr);

		return addr;
	}

	/**
	 * Absolute: A full 16-bit address is specified and the byte at that address
	 * is used to perform the computation.
	 */
	private int abs() {
		operand = mem.read(pc++) | (mem.read(pc++) << 8);
		return operand;
	}

	/**
	 * Absolute indexed with X: The value in X is added to the specified address
	 * for a sum address. The value at the sum address is used to perform the
	 * computation.
	 */
	private int abx() {
		operand = (mem.read(pc++) | (mem.read(pc++) << 8));
		int before = operand;
		int after = (before + x);

		pageBoundaryCrossed(before, after);

		return after & 0xFFFF;
	}

	/**
	 * Absolute indexed with Y: The value in Y is added to the specified address
	 * for a sum address. The value at the sum address is used to perform the
	 * computation.
	 */
	private int aby() {
		operand = (mem.read(pc++) | (mem.read(pc++) << 8));
		int before = operand;
		int after = (before + y);

		pageBoundaryCrossed(before, after);

		return after & 0xFFFF;
	}

	/**
	 * Indirect addressing. With this instruction, the 8-bit address (location)
	 * supplied by the programmer is considered to be a Zero-Page address, that
	 * is, an address in the first 256 (0..255) bytes of memory. The content of
	 * this Zero-Page address must contain the low 8-bits of a memory address.
	 * The following byte (the contents of address+1) must contain the upper
	 * 8-bits of a memory address
	 */
	private int ind() {
		operand = mem.read(pc++) & 0xFF;

		return mem.read(operand) | (mem.read(operand + 1) << 8);
	}

	/**
	 * Zero Page Indexed Indirect: Much like Indirect Addressing, but the
	 * content of the index register is added to the Zero-Page address
	 * (location)
	 */
	private int indx() {
		operand = mem.read(pc++) & 0xFF;

		return (mem.read(operand + x) | (mem.read(operand + 1 + x) << 8));
	}

	/**
	 * Indirect Indexed Addressing: Much like Indexed Addressing, but the
	 * contents of the index register is added to the Base_Location after it is
	 * read from Zero-Page memory.
	 */
	private int indy() {
		operand = mem.read(pc++) & 0xFF;

		int before = mem.read(mem.read(operand) | (mem.read(operand + 1) << 8));
		int after = before + y;

		pageBoundaryCrossed(before, after);

		return after;
	}

	/**
	 * Checks if a page boundary was crossed between two addresses.
	 * 
	 * "For example, in the instruction LDA 1234,X, where the value in the X
	 * register is added to address 1234 to get the effective address to load
	 * the accumulator from, the operand's low byte is fetched before the high
	 * byte, so the processor can start adding the X register's value before it
	 * has the high byte. If there is no carry operation, the entire indexed
	 * operation takes only four clocks, which is one microsecond at 4MHz. If
	 * there is a carry requiring the high byte to be incremented, it takes one
	 * additional clock." (Taken from the AtariAge forums)
	 * 
	 * @param addr1
	 *            : The first address
	 * @param addr2
	 *            : The second address
	 */
	private void pageBoundaryCrossed(int addr1, int addr2) {
		pbCrossed = ((addr1 ^ addr2) & M.BIT_8) != 0;
	}

	// Getters & setters ----------------------------------------

	public boolean getHalted() {
		return halted;
	}

	public int getCycles() {
		return cycles;
	}

	public int getCyclesNextInstr() {
		// TODO: getCyclesNextInstr
		return 0;
	}

	public Memory getMemory() {
		return mem;
	}

	public void setMemory(Memory mem) {
		this.mem = mem;
	}

	public ProcessorStatus getP() {
		return p;
	}

	public int getPc() {
		return pc;
	}

	public int getSp() {
		return sp;
	}

	public int getAc() {
		return ac;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public String getNextInstructionName() {
		return nextInstructionName;
	}

	public String getLastInstructionName() {
		return lastInstructionName;
	}

	/**
	 * Private class representing the status register.
	 * 
	 * @author Juan Manuel Leflet Estrada
	 */
	public class ProcessorStatus {
		public int c, z, i, d, b, n, v;

		public ProcessorStatus() {
			c = 0;
			z = 0;
			i = 0;
			d = 0;
			b = 0;
			n = 0;
			v = 0;
		}

		// These flags are always set checking the same things
		private void setN(int data) {
			p.n = ((data & M.BIT_7) == M.BIT_7) ? 1 : 0;
		}

		private void setZ(int data) {
			p.z = (data == 0) ? 1 : 0;
		}

		public int getProcessorStatus() {
			return c | z << 1 | i << 2 | d << 3 | b << 4 | v << 6 | n << 7;
		}

		public void setProcessorStatus(int p) {
			c = ((p & M.BIT_0) == 0) ? 0 : 1;
			z = ((p & M.BIT_1) == 0) ? 0 : 1;
			i = ((p & M.BIT_2) == 0) ? 0 : 1;
			d = ((p & M.BIT_3) == 0) ? 0 : 1;
			b = ((p & M.BIT_4) == 0) ? 0 : 1;
			n = ((p & M.BIT_6) == 0) ? 0 : 1;
			v = ((p & M.BIT_7) == 0) ? 0 : 1;
		}
	}

	/**
	 * Registers
	 * 
	 * @author Juan Manuel Leflet Estrada
	 */
	private enum R {
		A, X, Y
	}

	/*
	 * Auxiliary functions
	 */
	private int bcd(int n) {
		return (n & 0xF) + ((n & 0xF0) * 10);
	}
}
