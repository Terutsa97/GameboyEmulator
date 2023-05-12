package gmu.iit.gameboy;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import gmu.iit.gameboy.CPU.Alu_t;
import gmu.iit.gameboy.CPU.Rot_t;

public class CPUTest {

    CPU cpu;

    void alu_tests(int ALU_opcode) {
        System.out.println("\nRegisters before instruction:");
        System.out.println(cpu.regSet);

        // Loops through every 8-bit register that can be an argument
        for (int j = 0; j < 7; j++) {
            // Encodes the instruction to the proper format: alu[y], r[z]
            int instruction = 0b10000000 + (cpu.Y_MASK & (ALU_opcode << 3)) + (cpu.Z_MASK & j);

            // Runs the opcode and returns how many M-cycles it took
            int clockCycles = cpu.executeOpcode((short) instruction);

            System.out.print("Current instruction: " + cpu.current_opcode);
            System.out.printf("\tResults in A: 0x%02x", cpu.regSet.getByte(Reg_8.A));
            System.out.print("   " + cpu.regSet.getFlagsShort());
            System.out.println("   Cycles: " + (clockCycles / 4) + " M");
        }

        System.out.println("\nRegisters after instructions:");
        System.out.println(cpu.regSet);
        System.out.println("--------------------------------------------------------------------");
    }

    void x0_tests(int z_num, int y, int is_Q) {

        System.out.println("--------------------------------------------------------------------");

        if (y == -1) {
            System.out.println("\nRegisters before instruction:");
            System.out.println(cpu.regSet);
        }

        // Loops though every 16-bit register that can be an argument.
        // If the instruction uses 8-bit registers, is_Q is used to get
        // either even or odd indexed registers
        for (int j = 0; j < 4; j++) {
            if (y != -1) {
                j = y;
                System.out.println("\nRegisters before instruction:");
                System.out.print(cpu.regSet);
            }

            // Encodes the instruction to the proper format: alu[y] a, r[z]
            int instruction = (cpu.P_MASK & (j << 4)) + (cpu.Q_MASK * is_Q) + (cpu.Z_MASK & z_num);

            // Runs the opcode and returns how many M-cycles it took
            int clockCycles = cpu.executeOpcode((short) instruction);

            System.out.print("Current instruction: " + cpu.current_opcode);

            // This is so the proper destination register is printed out for the results
            if (z_num == 1) {
                System.out.printf("\tResults in HL: 0x%04x", cpu.regSet.getWord(Reg_16.HL));
            } else if (z_num == 3) {
                System.out.printf("\tResults in %s: 0x%04x", cpu.rp_args[j], cpu.regSet.getWord(cpu.rp_args[j]));
            } else if (z_num == 7) {
                System.out.printf("\tResults in A: 0x%02x", cpu.regSet.getA());
            } else if (((j << 1) + is_Q) != 6) {
                System.out.printf("\tResults in %s: 0x%04x", cpu.r_args[(j << 1) + is_Q],
                        cpu.regSet.getByte(cpu.r_args[(j << 1) + is_Q]));
            }

            System.out.print("   " + cpu.regSet.getFlagsShort());
            System.out.println("   Cycles " + (clockCycles / 4) + " M");
            if (y != -1) {
                break;
            }
        }

        System.out.println("\nRegisters after instructions:");
        System.out.println(cpu.regSet);
    }

    void rot_tests(int ROT_opcode, boolean setCarry) {

        System.out.println("\nRegisters before instructions:");
        System.out.println(cpu.regSet);

        // Loops through every 8-bit register that can be an argument
        for (int j = 0; j < 8; j++) {
            if (setCarry) {
                cpu.regSet.setCarryFlag();
            } else {
                cpu.regSet.clearCarryFlag();
            }

            System.out.println(cpu.regSet.getFlagsShort());

            // Encodes the instruction to the proper format: rot[y], r[z]
            int instruction = (cpu.Y_MASK & (ROT_opcode << 3)) + (cpu.Z_MASK & j);

            // Runs the opcode and returns how many M-cycles it took
            int clockCycles = cpu.executeExtendedOpcode((short) instruction);

            System.out.print("Current instruction (extended): " + cpu.current_opcode);
            if (j != 6) {
                System.out.printf("\tResults in %s: 0x%02x", cpu.r_args[j], cpu.regSet.getByte(cpu.r_args[j]));
            } else {
                System.out.printf("\tResults in (HL): 0x%02x", 0);
            }
            System.out.print("   " + cpu.regSet.getFlagsShort());
            System.out.print("   Cycles: " + (clockCycles / 4) + " M\n");
        }

        System.out.println("\nRegisters after instructions:");
        System.out.println(cpu.regSet);
        System.out.println(cpu.regSet.getFlags());
        System.out.println("--------------------------------------------------------------------");
    }

    void bit_tests(int BIT_opcode) {

        System.out.println("\nRegisters before instructions:");
        System.out.println(cpu.regSet);

        // Loops through every 8-bit register that can be an argument
        for (int j = 0; j < 8; j++) {
            for (int i = 0; i < 8; i++) {
                System.out.println(cpu.regSet.getFlagsShort());

                // Encodes the instruction to the proper format: BIT/RES/SET y, r[z]
                int instruction = (BIT_opcode << 6) + (cpu.Y_MASK & (i << 3)) + (cpu.Z_MASK & j);

                // Runs the opcode and returns how many M-cycles it took
                int clockCycles = cpu.executeExtendedOpcode((short) instruction);

                System.out.print("Current instruction (extended): " + cpu.current_opcode);
                if (j != 6) {
                    System.out.printf("\tResults in %s: 0x%02x", cpu.r_args[j], cpu.regSet.getByte(cpu.r_args[j]));
                } else {
                    System.out.printf("\tResults in (HL): 0x%02x", 0);
                }
                System.out.print("   " + cpu.regSet.getFlagsShort());
                System.out.print("   Cycles: " + (clockCycles / 4) + " M\n");
            }
        }

        System.out.println("\nRegisters after instructions:");
        System.out.println(cpu.regSet);
        System.out.println(cpu.regSet.getFlags());
        System.out.println("--------------------------------------------------------------------");
    }

    @Before
    public void init() throws FileNotFoundException, IOException {
        var registerSet = new RegisterSet();
        var memoryMap = new MemoryMap(registerSet, "test_roms/instr_timing.gb");
        var interrupts = new Interrupts(memoryMap, registerSet);

        cpu = new CPU(registerSet, memoryMap, interrupts);
        System.out.println("CPU Tests....");
    }

    @Test
    public void eightBitALUTests() {
        alu_tests(Alu_t.ADD.index);
        alu_tests(Alu_t.ADC.index);
        alu_tests(Alu_t.SUB.index);
        alu_tests(Alu_t.SBC.index);
        alu_tests(Alu_t.AND.index);
        alu_tests(Alu_t.XOR.index);
        alu_tests(Alu_t.OR.index);
        alu_tests(Alu_t.CP.index);
    }

    @Test
    public void xIs0Tests() {
        // Resets the register set
        cpu.regSet = new RegisterSet();

        // 16-bit ADD HL
        x0_tests(1, -1, 1);

        // 16-bit INC
        x0_tests(3, -1, 0);

        // 16-bit DEC
        x0_tests(3, -1, 1);

        // 8-bit INC
        x0_tests(4, -1, 0);
        x0_tests(4, -1, 1);

        // 8-bit DEC
        x0_tests(5, -1, 0);
        x0_tests(5, -1, 1);
    }

    @Test
    public void rotationAndXisZeroTests() {
        // RLCA
        cpu.regSet = new RegisterSet();
        cpu.regSet.setA(0xF0);
        cpu.regSet.clearCarryFlag();
        x0_tests(7, 0, 0);
        System.out.println("Without carry Expected: 0xe1");

        cpu.regSet.setA(0x70);
        cpu.regSet.setCarryFlag();
        x0_tests(7, 0, 0);
        System.out.println("With Carry Expected: 0xe0");

        // RLA
        cpu.regSet.setA(0xF0);
        cpu.regSet.clearCarryFlag();
        x0_tests(7, 1, 0);
        System.out.println("w/ carry Expected: 0xe0");
        cpu.regSet.setA(0x70);
        cpu.regSet.setCarryFlag();
        x0_tests(7, 1, 0);
        System.out.println("w/out Expected: 0xe1");

        // RRCA
        cpu.regSet.setA(0x0F);
        cpu.regSet.clearCarryFlag();
        x0_tests(7, 0, 1);
        System.out.println("w/ Expected: 0x87");
        cpu.regSet.setA(0x0E);
        cpu.regSet.setCarryFlag();
        x0_tests(7, 0, 1);
        System.out.println("w/out Expected: 0x07");

        // RRA
        cpu.regSet.setA(0x0F);
        cpu.regSet.clearCarryFlag();
        x0_tests(7, 1, 1);
        System.out.println("w/ Expected: 0x07");

        cpu.regSet.setA(0x0E);
        cpu.regSet.setCarryFlag();
        x0_tests(7, 1, 1);
        System.out.println("w/out Expected: 0x87");

        cpu.regSet = new RegisterSet();

        x0_tests(7, 2, 0); // DAA
        x0_tests(7, 2, 1); // CPL
        x0_tests(7, 3, 1); // SCF
        x0_tests(7, 3, 1); // CCF
    }

    @Test
    public void jumpTests() {
        int jumpAddress = 0x156f;

        cpu.regSet.setWord(Reg_16.HL, jumpAddress);
        cpu.executeOpcode((short) 0xE9);

        assertEquals(jumpAddress, cpu.regSet.getPC());
    }

    @Test
    public void extendedOpCodeTests() {
        cpu.regSet = new RegisterSet();

        // Rotational/Shift operations
        rot_tests(Rot_t.SWAP.index, false);
        rot_tests(Rot_t.RLC.index, false);
        rot_tests(Rot_t.RLC.index, true);
        rot_tests(Rot_t.RRC.index, false);
        rot_tests(Rot_t.RRC.index, true);
        rot_tests(Rot_t.RL.index, false);
        rot_tests(Rot_t.RL.index, true);
        rot_tests(Rot_t.RR.index, false);
        rot_tests(Rot_t.RR.index, true);
        rot_tests(Rot_t.SLA.index, false);
        rot_tests(Rot_t.SLA.index, true);
        rot_tests(Rot_t.SRA.index, false);
        rot_tests(Rot_t.SRA.index, true);
        rot_tests(Rot_t.SRL.index, false);
        rot_tests(Rot_t.SRL.index, true);

        // Bit tests
        bit_tests(1);

        // Bit resets
        cpu.regSet = new RegisterSet(0xFF00, 0xFFFF, 0xFFFF, 0xFFFF, 0xFFFE, 0x100);
        bit_tests(2);

        // Bit sets
        bit_tests(3);
    }
}
