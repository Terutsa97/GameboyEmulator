package gmu.iit.gameboy;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Memory Map:
	0000-3FFF 16KB ROM Bank 00 (in cartridge, fixed at bank 00)
	4000-7FFF 16KB ROM Bank 01..NN (in cartridge, switchable bank number)
	8000-9FFF 8KB Video RAM (VRAM) (switchable bank 0-1 in CGB Mode)
	A000-BFFF 8KB External RAM (in cartridge, switchable bank, if any)
	C000-CFFF 4KB Work RAM Bank 0 (WRAM)
	D000-DFFF 4KB Work RAM Bank 1 (WRAM) (switchable bank 1-7 in CGB Mode)
	E000-FDFF Same as C000-DDFF (ECHO) (typically not used)
	FE00-FE9F Sprite Attribute Table (OAM)
	FEA0-FEFF Not Usable
	FF00-FF7F I/O Ports
	FF80-FFFE High RAM (HRAM)
	FFFF Interrupt Enable Register
 */
public class MemoryMap {

	// Constants:
	public static final int VRAM_START = 0x8000;
	public static final int TILES_START = 0x8000;
	public static final int BGMAP0_START = 0x9800;
	public static final int BGMAP1_START = 0x9C00;
	public static final int WRAM_START = 0xC000;
	public static final int OAM_START = 0xFE00;
	public static final int HRAM_START = 0xFF80;

	// Registers in Memory
	public static final int P1 = 0xFF00;
	public static final int SB = 0xFF01;
	public static final int SC = 0xFF02;
	public static final int DIV = 0xFF04;
	public static final int TIMA = 0xFF05;
	public static final int TMA = 0xFF06;
	public static final int TAC = 0xFF07;
	public static final int IF = 0xFF0F;
	public static final int IE = 0xFFFF;
	public static final int LCDC = 0xFF40;
	public static final int STAT = 0xFF41;
	public static final int SCY = 0xFF42;
	public static final int SCX = 0xFF43;
	public static final int LY = 0xFF44;
	public static final int DMA_TRANSFER = 0xFF46;
	public static final int BGP = 0xFF47;
	public static final int WINDOW_X = 0xFF4B;
	public static final int WINDOW_Y = 0xFF4A;
	public static final int KEY1 = 0xFF4D;
	public static final int VBK = 0xFF4F;


	// public byte[] cartridgeMemory = new byte[0x200000]; useless until other cartridge types are implemented
	private char[] romData = new char[0x8000];
	private char[] vRAM = new char[0x2000];
	private char[] sRAM = new char[0x2000];
	private char[] wRAM = new char[0x2000];
	private char[] OAM = new char[0xA0];
	private char[] io = new char[0x4C];
	private char[] hRAM = new char[0x80];

	private RegisterSet _registerSet;

	public Joystick joystick;
	public Timers timers;

	public void initializeDependencies(Joystick joystick, Timers timers) {
		this.joystick = joystick;
		this.timers = timers;
	}

	/**
	 * Constructor for memory map. Uses static method #Program.loadCartridge().
	 */
	public MemoryMap(RegisterSet registerSet, String catridgeName) throws FileNotFoundException, IOException {
		_registerSet = registerSet;

		byte[] cartridge = Emulator.loadCartridge(catridgeName);
		if (cartridge == null) { return; }

		for (int i = 0; i < cartridge.length; i++) {
			this.romData[i] = (char)(cartridge[i] & 0xFF);
		}

		initializeMemory();
		System.out.println("This is a new Memory Map!");
	}

	/**
	 * Constructor for memory map. Uses static method #Program.loadCartridge().
	 */
	public MemoryMap(RegisterSet registerSet) throws FileNotFoundException, IOException {
		_registerSet = registerSet;

		byte[] cartridge = Emulator.loadCartridge("test_roms/Tetris (Japan) (En).gb");

		for (int i = 0; i < cartridge.length; i++) {
			this.romData[i] = (char)(cartridge[i] & 0xFF);
		}

		initializeMemory();
		System.out.println("This is a new Memory Map!");

		// vRamPresetInit();
	}

	public void initializeMemory() {
		writeMemory(0xFF05, (char) 0x00);
		writeMemory(0xFF06, (char) 0x00);
		writeMemory(0xFF07, (char) 0x00);
		writeMemory(0xFF10, (char) 0x80);
		writeMemory(0xFF11, (char) 0xBF);
		writeMemory(0xFF12, (char) 0xF3);
		writeMemory(0xFF14, (char) 0xBF);
		writeMemory(0xFF16, (char) 0x3F);
		writeMemory(0xFF17, (char) 0x00);
		writeMemory(0xFF19, (char) 0xBF);
		writeMemory(0xFF1A, (char) 0x7f);
		writeMemory(0xFF1B, (char) 0xFF);
		writeMemory(0xFF1C, (char) 0x9F);
		writeMemory(0xFF1E, (char) 0xBF);
		writeMemory(0xFF20, (char) 0xFF);
		writeMemory(0xFF21, (char) 0x00);
		writeMemory(0xFF22, (char) 0x00);
		writeMemory(0xFF23, (char) 0xBF);
		writeMemory(0xFF24, (char) 0x77);
		writeMemory(0xFF25, (char) 0xF3);
		writeMemory(0xFF26, (char) 0xF1);
		writeMemory(0xFF40, (char) 0x91);
		writeMemory(0xFF42, (char) 0x00);
		writeMemory(0xFF43, (char) 0x00);
		writeMemory(0xFF45, (char) 0x00);
		writeMemory(0xFF47, (char) 0xFC);
		writeMemory(0xFF48, (char) 0xFF);
		writeMemory(0xFF49, (char) 0xFF);
		writeMemory(0xFF4A, (char) 0x00);
		writeMemory(0xFF4B, (char) 0x00);
		writeMemory(0xFFFF, (char) 0x00);
	}

	/**
	 * Retrieves the byte from a space in memory
	 * @param address
	 * @return Unsigned byte from designated space in memory
	 */
	public char readMemory(int address) {
		try {
			if (address < 0x8000) return romData[address];
			if (address == 0xFF00) return (char)joystick.getJoypadState();

			if (0x8000 <= address && address < 0xA000)
			{
				if (address == LY || address == SC) { return 0xff; }
				return vRAM[address - 0x8000];
			}

			if (0xA000 <= address && address < 0xC000) return sRAM[address - 0xA000];
			if (0xC000 <= address && address < 0xE000) return wRAM[address - 0xC000];
			if (0xE000 <= address && address < 0xFE00) return wRAM[address - 0xE000];
			if (0xFE00 <= address && address < 0xFEA0) return OAM[address - 0xFE00];
			if (0xFF00 <= address && address < 0xFF4C) return io[address - 0xFF00];
			if (0xFF80 <= address && address <= 0xFFFF) return hRAM[address - 0xFF80];

			throw new IndexOutOfBoundsException();
		} catch (IndexOutOfBoundsException e) {
			// System.out.println("Invalid memory read at " + String.format("%04x", address));
			return 0;
		}
	}

	/**
	 * Writes a char (unsigned byte) to memory
	 * @param address
	 * @param data
	 */
	public void writeMemory(int address, char data) {
		try {
			if (address < 0x8000) throw new IndexOutOfBoundsException();
			else if (address == DMA_TRANSFER) doDMATransfer(data);
			else if (address == LY) io[address - 0xFF00] = 0;
			else if (0x8000 <= address && address < 0xA000)	vRAM[address - 0x8000] = data;
			else if (0xA000 <= address && address < 0xC000) sRAM[address - 0xA000] = data;
			else if (0xC000 <= address && address < 0xE000) wRAM[address - 0xC000] = data;
			else if (0xE000 <= address && address < 0xFE00) wRAM[address - 0xE000] = data;
			else if (0xFE00 <= address && address < 0xFEA0) OAM[address - 0xFE00] = data;
			else if (0xFF00 <= address && address < 0xFF4C) io[address - 0xFF00] = data;
			else if (0xFF80 <= address && address <= 0xFFFF) hRAM[address - 0xFF80] = data;
			else throw new IndexOutOfBoundsException();
		}
		catch (IndexOutOfBoundsException e) {
			// System.out.println("Invalid memory write at " + String.format("%04x", address));
		}
	}

	public void writeWord(int address, int value) {
		writeMemory(address + 1, (char)((value & 0xff00) >> 8));
		writeMemory(address, (char)(value & 0xff));
	}

	// short operand = (short)(((readMemory(_registerSet.getSP()) + 1) << 8) + (readMemory(_registerSet.getSP())));
	public short readWord(int address) {
		int data1 = (int)readMemory(address + 1) << 8;
		int data2 = (int)readMemory(address);

		return (short)(data1 + (data2));
	}

	/**
	 * Pushes a register to the stack.
	 */
	public void pushToStack(Reg_16 source) {
		writeWord(_registerSet.getSP(), _registerSet.getWord(source));
		_registerSet.setSP(_registerSet.getSP() - 2);
	}

	/**
	 * Pops a short value from the stack.
	 */
	public short popFromStack() {
		_registerSet.setSP(_registerSet.getSP() + 2);
		short operand = readWord(_registerSet.getSP());
		return operand;
	}

	public void doDMATransfer(char data) {
		short address = (short)(data << 8);
		for (int i = 0; i < 0xA0; i++) {
			writeMemory(OAM_START + i, readMemory(address + i));
		}
	}

	public char[] getRom() { return romData; }
	public char[] getVRAM() { return vRAM; }
	public char[] getSRAM() { return sRAM; }
	public char[] getWRAM() { return wRAM; }
	public char[] getOAM() { return OAM; }
	public char[] getIO() { return io; }
	public char[] getHRAM() { return hRAM; }

	public void vRamPresetInit() {
		// Initializes VRAM with predefined contents.
		for (int i = 0; i < vRAM.length/16; i++) {
			vRAM[i*16] 	  = 0xFF;
			vRAM[i*16+1]  = 0x00;
			vRAM[i*16+2]  = 0x7F;
			vRAM[i*16+3]  = 0xFF;
			vRAM[i*16+4]  = 0x85;
			vRAM[i*16+5]  = 0x81;
			vRAM[i*16+6]  = 0x89;
			vRAM[i*16+7]  = 0x83;
			vRAM[i*16+8]  = 0x93;
			vRAM[i*16+9]  = 0x85;
			vRAM[i*16+10] = 0xA5;
			vRAM[i*16+11] = 0x8B;
			vRAM[i*16+12] = 0xC9;
			vRAM[i*16+13] = 0x97;
			vRAM[i*16+14] = 0x7E;
			vRAM[i*16+15] = 0xFF;
		}
	}
}
