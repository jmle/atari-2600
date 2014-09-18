package org.atari2600.util;

/**
 * Class with different static definitions and utilities.
 * 
 * @author Juan Manuel Leflet Estrada
 * 
 */
public class M {
	// Bits for masks
	public static final int BIT_12 = 0x1000;
	public static final int BIT_11 = 0x800;
	public static final int BIT_10 = 0x400;
	public static final int BIT_9 = 0x200;
	public static final int BIT_8 = 0x100;
	public static final int BIT_7 = 0x80;
	public static final int BIT_6 = 0x40;
	public static final int BIT_5 = 0x20;
	public static final int BIT_4 = 0x10;
	public static final int BIT_3 = 0x08;
	public static final int BIT_2 = 0x04;
	public static final int BIT_1 = 0x02;
	public static final int BIT_0 = 0x01;

	// TIA: Write address summary (write only):

	// When the electron beam has scanned 262 lines, the TV set must be
	// signaled to blank the beam and position it at the top of the screen to
	// start a new frame. This signal is called vertical sync, and the TIA must
	// transmit this signal for at least 3 scan lines. This is accomplished by
	// writing a "1" in D1 of VSYNC to turn it on, count at least 2 scan lines,
	// then write a "0" to D1 of VSYNC to turn it off.
	public static final int VSYNC = 0x00;

	// To physically turn the beam off during its repositioning time, the TV set
	// needs 37 scan lines of vertical blanks signal from the TIA. This is
	// accomplished by writing a "1" in D1 of VBLANK to turn it on, count 37
	// lines, then write a "0" to D1 of VBLANK to turn it off. The
	// microprocessor is of course free to execute other software during the
	// vertical timing commands, VSYNC and VBLANK.
	public static final int VBLANK = 0x01;

	// Writing to the WSYNC causes the microprocessor to halt until the electron
	// beam reaches the right edge of the screen, then the microprocessor
	// resumes operation at the beginning of the 68 color clocks for horizontal
	// blanking.
	public static final int WSYNC = 0x02;
	public static final int RSYNC = 0x03;

	// These addresses control the number and size of players and missiles.
	public static final int NUSIZ0 = 0x04;
	public static final int NUSIZ1 = 0x05;

	// These addresses write data into the player, playfield, and background
	// color-luminance registers
	public static final int COLUP0 = 0x06;
	public static final int COLUP1 = 0x07;
	public static final int COLUPF = 0x08;
	public static final int COLUBK = 0x09;
	public static final int CTRLPF = 0x0A;

	// These addesses write D3 into the 1 bit player reflect registers
	public static final int REFP0 = 0x0B;
	public static final int REFP1 = 0x0C;

	// These addresses are used to write into playfield registers
	public static final int PF0 = 0x0D;
	public static final int PF1 = 0x0E;
	public static final int PF2 = 0x0F;

	// These addresses are used to reset players, missiles and the ball. The
	// object will begin its serial graphics at the time of a horizontal line at
	// which the reset address occurs.
	public static final int RESP0 = 0x10;
	public static final int RESP1 = 0x11;
	public static final int RESM0 = 0x12;
	public static final int RESM1 = 0x13;
	public static final int RESBL = 0x14;
	
	public static final int AUDC0 = 0x15;
	public static final int AUDC1 = 0x16;
	public static final int AUDF0 = 0x17;
	public static final int AUDF1 = 0x18;
	public static final int AUDV0 = 0x19;
	public static final int AUDV1 = 0x1A;
	public static final int GRP0 = 0x1B;
	public static final int GRP1 = 0x1C;
	public static final int ENAM0 = 0x1D;
	public static final int ENAM1 = 0x1E;
	public static final int ENABL = 0x1F;

	// Specifies the motion direction and step (in pixels). Writing to these
	// registers does not directly affect the objects position, unless when
	// subsequently using the HMOVE command.
	// Bit Expl.
	// 0-3 Not used
	// 4-7 Signed Motion Value (-8..-1=Right, 0=No motion, +1..+7=Left)
	// Repeatedly writing to HMOVE will repeat the same movement (without having
	// to rewrite the motion registers).
	public static final int HMP0 = 0x20;
	public static final int HMP1 = 0x21;
	public static final int HMM0 = 0x22;
	public static final int HMM1 = 0x23;
	public static final int HMBL = 0x24;
	
	
	public static final int VDELP0 = 0x25;
	public static final int VDELP1 = 0x26;
	public static final int VDELBL = 0x27;
	public static final int RESMP0 = 0x28;
	public static final int RESMP1 = 0x29;
	public static final int HMOVE = 0x2A;
	public static final int HMCLR = 0x2B;
	public static final int CXCLR = 0x2C;

	// Important individual bits
	public static final int VERTICAL_SYNC = BIT_1;
	public static final int VERTICAL_BLANK = BIT_1;

	// CTRLPF bits
	public static final int PLAYFIELD_REFLECTION = BIT_0;
	public static final int PLAYFIELD_COLOR = BIT_1;
	public static final int PLAYFIELD_BALL_PRIORITY = BIT_2;
	public static final int COLOR_SWITCH = BIT_3;
	public static final int BALL_SIZE = BIT_4 | BIT_5;

	// TIA: Read address summary (read only):
	public static final int CXM0P = 0x30;
	public static final int CXM1P = 0x31;
	public static final int CXP0FB = 0x32;
	public static final int CXP1FB = 0x33;
	public static final int CXM0FB = 0x34;
	public static final int CXM1FB = 0x35;
	public static final int CXBLPF = 0x36;
	public static final int CXPPMM = 0x37;
	public static final int INPT0 = 0x38;
	public static final int INPT1 = 0x39;
	public static final int INPT2 = 0x3A;
	public static final int INPT3 = 0x3B;
	public static final int INPT4 = 0x3C;
	public static final int INPT5 = 0x3D;

	// PIA
	public static final int SWCHA = 0x0;
	public static final int SWACNT = 0x1;
	public static final int SWCHB = 0x2;
	public static final int SWBCNT = 0x3;
	public static final int INTIM = 0x4;
	public static final int INSTAT = 0x5;
	public static final int TIM1T = 0x14;
	public static final int TIM8T = 0x15;
	public static final int TIM64T = 0x16;
	public static final int TIM1024T = 0x17;

	// Opcode LUT: look-up table to get the name of each instruction.
	public static final String index[] = { "BRK", "ORA X,ind", "-", "-", "-",
			"ORA zp", "ASL zp", "-", "PHP", "ORA imm", "ASL A", "-", "-",
			"ORA abs", "ASL abs", "-", "BPL rel", "ORA ind,Y", "-", "-", "-",
			"ORA zp,X", "ASL zp,X", "-", "CLC", "ORA abs,Y", "-", "-", "-",
			"ORA abs,X", "ASL abs,X", "-", "JSR abs", "AND ind,Y", "-", "-",
			"BIT zp", "AND zp", "ROL zp", "-", "PLP", "AND imm", "ROL A", "-",
			"BIT abs", "AND abs", "ROL abs", "-", "BMI rel", "AND ind,Y", "-",
			"-", "-", "AND zp,X", "ROL zp,X", "-", "SEC", "AND abs,Y", "-",
			"-", "-", "AND abs,X", "ROL abs,X", "-", "RTI", "EOR X,ind", "-",
			"-", "-", "EOR zp", "LSR zp", "-", "PHA", "EOR imm", "LSR A", "-",
			"JMP abs", "EOR abs", "LSR abs", "-", "BVC rel", "EOR ind,Y", "-",
			"-", "-", "EOR zp,X", "LSR zp,X", "-", "CLI", "EOR abs,Y", "-",
			"-", "-", "EOR abs,X", "LSR abs,X", "-", "RTS", "ADC X,ind", "-",
			"-", "-", "ADC zp", "ROR zp", "-", "PLA", "ADC imm", "ROR A", "-",
			"JMP ind", "ADC abs", "ROR abs", "-", "BVS rel", "ADC ind,Y", "-",
			"-", "-", "ADC zp,X", "ROR zp,X", "-", "SEI", "ADC abs,Y", "-",
			"-", "-", "ADC abs,X", "ROR abs,X", "-", "-", "STA X,ind", "-",
			"-", "STY zp", "STA zp", "STX zp", "-", "DEY", "-", "TXA", "-",
			"STY abs", "STA abs", "STX abs", "-", "BCC rel", "STA ind,Y", "-",
			"-", "STY zp,X", "STA zp,X", "STX zp,Y", "-", "TYA", "STA abs,Y",
			"TXS", "-", "-", "STA abs,X", "-", "-", "LDY imm", "LDA X,ind",
			"LDX imm", "-", "LDY zp", "LDA zp", "LDX zp", "-", "TAY",
			"LDA imm", "TAX", "-", "LDY abs", "LDA abs", "LDX abs", "-",
			"BCS rel", "LDA ind,Y", "-", "-", "LDY zp,X", "LDA zp,X",
			"LDX zp,Y", "-", "CLV", "LDA abs,Y", "TSX", "-", "LDY abs,X",
			"LDA abs,X", "LDX abs,Y", "-", "CPY imm", "CMP X,ind", "-", "-",
			"CPY zp", "CMP zp", "DEC zp", "-", "INY", "CMP imm", "DEX", "-",
			"CPY abs", "CMP abs", "DEC abs", "-", "BNE rel", "CMP ind,Y", "-",
			"-", "-", "CMP zp,X", "DEC zp,X", "-", "CLD", "CMP abs,Y", "-",
			"-", "-", "CMP abs,X", "DEC abs,X", "-", "CPX imm", "SBC X,ind",
			"-", "-", "CPX zp", "SBC zp", "INC zp", "-", "INX", "SBC imm",
			"NOP", "-", "CPX abs", "SBC abs", "INC abs", "-", "BEQ rel",
			"SBC ind,Y", "-", "-", "-", "SBC zp,X", "INC zp,X", "-", "SED",
			"SBC abs,Y", "-", "-", "-", "SBC abs,X", "INC abs,X", "-" };
	

}
