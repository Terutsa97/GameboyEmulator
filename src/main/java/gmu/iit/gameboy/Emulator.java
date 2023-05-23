package gmu.iit.gameboy;


import java.io.*;
import java.util.Date;

import javax.swing.*;
import javax.swing.table.*;

import java.awt.*;

/**
 * This is the main program code. (Edited by Angel)
 *
 * If you want to compile and run this, open the src folder in the terminal.
 *
 * You compile this with this command:
 * javac -d compiled Program.java
 *
 * run with this command:
 * java -cp compiled Program
 */
public class Emulator
{
    final static int FRAME_RATE = 60;
    final static double DELTA_TIME = (1e9 / FRAME_RATE);

    public static Float _fps = 0.0f;

    private static CPU cpu;
    private static Timers timer;
    private static MemoryMap memoryMap;
    private static Interrupts interrupts;
    private static RegisterSet registerSet;
    private static Joystick joy;
    private static LCD lcd;

    //#region ---- ---- ---- ---- ---- Emulator Functions

    /**
     * Reads a rom file and returns a series of bytes
     * @param fileName
     * @return
     */
    public static byte[] loadCartridge(String fileName) throws FileNotFoundException, IOException {
        byte[] cartridgeMemory = new byte[0x8000];

        FileInputStream inputStream = new FileInputStream(fileName);

        int total = 0, nRead = 0;

        while((nRead = inputStream.read(cartridgeMemory)) != -1) { total += nRead; }
        inputStream.close();

        System.out.println("Read " + total + " bytes");

        return cartridgeMemory;
    }

    /**
     * Override Method for loadCartridge that by default runs the Tetris Rom
     * @return
     */
    public static byte[] loadCartridge() throws FileNotFoundException, IOException {
        return loadCartridge("./test_roms/Tetris (Japan) (En).gb");
    }

    /**
     * Renders the screen. Presumably this is where the JFrame stuff should be
     * here to display the screen graphics somehow.
     */
    public static void renderScreen() {
        System.out.println("The screen is rendering!");
    }

    /**
     * Causes the program to pause for the specified amount of time in milliseconds.
     * This is so there's no need to try/catch everytime the function is needed.
     * @param debugTime
     */
    public static void delay(int debugTime) {
        // try { time.sleep(debugTime); } catch (Exception e) { }

        float timeDiff = 0;
        long startTime = new Date().getTime();
        while(timeDiff < debugTime) {
            timeDiff = new Date().getTime() - startTime;
        }

        // System.out.println("FPS: " + (1000.0f/timeDiff));
    }


    public static int cycles_count = 0;

    /**
     * Update step of the emulator. Gets called every frame for the emulator
     * Code from http://www.codeslinger.co.uk/pages/projects/gameboy/opcodes.html
     * https://github.com/retrio/gb-test-roms/tree/master/instr_timing
     */
    public static void emulatorUpdate() {
        final int MAXCYCLES = 69905;  // The amount of cycles that get executed every 60 Hz. (4194304/60)
        cycles_count = 0;

        while (cycles_count < MAXCYCLES) {
            // System.out.println("SB Contents: " + memoryMap.readMemory(0xFF01)+"\n");

            String regBefore = "";
            if (isDebug) { regBefore = "Before Op:" + cpu.regSet.toString() + cpu.regSet.getFlagsShort(); }

            int cycles = cpu.executeNextOpcode();
            cycles_count += cycles;

            timer.updateTimers(cycles);
            lcd.updateGraphics(cycles);
            interrupts.doInterrupts();

            if (memoryMap.readMemory(MemoryMap.SC) == 0x81) {
                char c = memoryMap.readMemory(MemoryMap.SB);
                System.out.println("SC: " + c);
                memoryMap.writeMemory(MemoryMap.SC, (char)0);
            }

            // Debugging:
            // startDebugAt((short)0x215E, 150);
            // breakpointAtAddress((short)0x215E);

            if (isDebug) {
            //     updateRAMTable(vRamTable, memoryMap.getVRAM());
            //     updateRAMTable(hRamTable, memoryMap.getHRAM());
            //     updateRAMTable(ioTable, memoryMap.getIO());
            //     updateRegSetTable(regTable);
                updateMemRegSetTable(memRegTable);
            }

            // startDebugAt((short)0x0183, 0, regBefore);
            // breakpointAtAddress((short)0x0183);

            // startDebugAtRegisterValue(Reg_16.BC, 0x0001);
            // startDebugAtMemoryValue(MemoryMap.LY, (char)LCD.SCREEN_RESOLUTION_Y);

            // breakpointAtOpcode((short)0x40);
        }

        lcd.renderGraphics();
    }

    //#endregion

    //#region ---- ---- ---- ---- ---- Debug Functions

    // Global variable used by startDebugAt that controls when opcode / reg data is printed
    // to the command line.
    public static boolean isDebug;

    /**
     * Starts printing the current opcode and the flags to the cmdln when the program is at an addrress.
     * @param address
     * @param delaystep
     */
    public static void startDebugAt(short address, int delaystep, String regBefore) {
        if (cpu.regSet.getPC() == address) { isDebug = true; }
        if (isDebug) {
            System.out.println("OP Code: " + cpu.current_opcode);
            System.out.println(regBefore);
            System.out.println("After Op:" + cpu.regSet + cpu.regSet.getFlagsShort());

            delay(delaystep);
        }
    }

    /**
     * Stops the execution of the program when the program counter at an address.
     * @param address
     */
    public static void breakpointAtAddress(short address) {
        if (cpu.regSet.getPC() != address) {
            return;
        }

        System.out.println("\nBREAKPOINT at address: " + String.format("0x%04x", address));
        System.out.println("OP Code: " + cpu.current_opcode);
        System.out.println(cpu.regSet + cpu.regSet.getFlagsShort());

        try {
            System.out.print("Press Enter to step through: ");
            System.in.read();
            System.out.println("Pressed enter!\n");
        } catch (Exception e) { }
    }

    public static void breakpointAtOpcode(short opcode, String regBefore) {
        if (opcode != (short)cpu.memMap.readMemory(cpu.regSet.getPC())) {
            return;
        }

        System.out.println("OP Code: " + cpu.current_opcode);
        System.out.println("Old: " + regBefore);
        System.out.println("New: " + cpu.regSet + cpu.regSet.getFlagsShort());

        try {
            System.out.print("Press Enter to step through: ");
            System.in.read();
            System.out.println("Pressed enter!\n");
            isDebug = true;

        } catch (Exception e) { }
    }

    public static void startDebugAtRegisterValue(Reg_16 src, int value) {
        int regValue = registerSet.getWord(src);

        if (isDebug) {
            System.out.println("-- OP Code: " + cpu.current_opcode);
            System.out.println("-- " + src.name() + " == " + regValue);
            System.out.println(cpu.regSet + cpu.regSet.getFlagsShort());
        }

        if (regValue != value) {
            return;
        }

        isDebug = true;

        try {
            System.out.print("Press Enter to step through: ");
            System.in.read();
            System.out.println("Pressed enter!\n");
            isDebug = true;

        } catch (Exception e) { }
    }

    public static void startDebugAtMemoryValue(int address, char value) {
        char memoryValue = memoryMap.readMemory(address);

        if (isDebug) {
            System.out.println("-- OP Code: " + cpu.current_opcode);
            System.out.println("-- " + address + " == " + (int)memoryValue);
        }

        if (memoryValue != value) {
            return;
        }

        isDebug = true;

        try {
            System.out.print("Press Enter to step through: ");
            System.in.read();
            System.out.println("Pressed enter!\n");
            isDebug = true;

        } catch (Exception e) { }
    }

    //#endregion

    //#region ---- ---- ---- ---- ---- UI Menues

    /**
     *
     * @return
     */
    public static void initializeMenuBar(JFrame jframe) {
        JMenuBar menubar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem file = new JMenuItem("Open rom");
        JMenu editMenu = new JMenu("Edit");

        file.addActionListener((l) -> {
            // JOptionPane.showMessageDialog(null,"You selected: Load.");
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("."));
            int result = fileChooser.showOpenDialog(jframe);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            }
        });

        fileMenu.add(file);
        menubar.add(fileMenu);
        menubar.add(editMenu);
        jframe.setJMenuBar(menubar);
    }

    public static JTable debugMemoryPanel(String name, char[] memorySet, int offset) {
        JFrame jframe = new JFrame(name);

        final JTable table = new JTable();

        final DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[] {
            name + " Address",
            "Value"
        });

        for (int i = 0; i < memorySet.length; i++) {
            model.addRow(new String[] {
                "0x" + String.format("%04x", offset + i),
                "B",
            });
        }

        table.setModel(model);

        table.setPreferredScrollableViewportSize(new Dimension(200, 500));
        table.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(table);

        jframe.add(scrollPane);
        jframe.pack();
        jframe.setLocationRelativeTo(null);
        jframe.setVisible(true);

        return table;
    }

    public static JTable debugMemoryRegSet() {
        JFrame jframe = new JFrame("Register Set");

        final JTable table = new JTable();

        final DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[] {
            "SB",
            "SC",
            "DIV",
            "TIMA",
            "TMA",
            "TAC",
            "IF",
            "IE",
            "LCDC",
            "STAT",
            "SCY",
            "SCX",
            "LY",
            "BGP",
            "KEY1",
            "VBK"
        });

        model.addRow(new String[] {
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.SB)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.SC)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.DIV)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.TIMA)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.TMA)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.TAC)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.IF)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.IE)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.LCDC)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.STAT)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.SCY)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.SCX)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.LY)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.BGP)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.KEY1)),
            "0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.VBK)),
        });

        table.setModel(model);

        table.setPreferredScrollableViewportSize(new Dimension(500, table.getMinimumSize().height));
        table.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(table);

        jframe.add(scrollPane);
        jframe.pack();
        jframe.setLocationRelativeTo(null);
        jframe.setVisible(true);

        return table;
    }

    public static JTable debugRegisterSet() {
        JFrame jframe = new JFrame("Register Set");

        final JTable table = new JTable();

        final DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[] {
            "AF",
            "BC",
            "DE",
            "HL",
            "SP",
            "PC"
        });

        model.addRow(new String[] {
            "0x" + String.format("%04x", registerSet.getWord(Reg_16.AF)),
            "0x" + String.format("%04x", registerSet.getWord(Reg_16.BC)),
            "0x" + String.format("%04x", registerSet.getWord(Reg_16.DE)),
            "0x" + String.format("%04x", registerSet.getWord(Reg_16.HL)),
            "0x" + String.format("%04x", registerSet.getWord(Reg_16.SP)),
            "0x" + String.format("%04x", registerSet.getWord(Reg_16.PC))
        });

        table.setModel(model);

        table.setPreferredScrollableViewportSize(new Dimension(500, table.getMinimumSize().height));
        table.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(table);

        jframe.add(scrollPane);
        jframe.pack();
        jframe.setLocationRelativeTo(null);
        jframe.setVisible(true);

        return table;
    }

    private static void updateRAMTable(JTable table, char[] ramSet) {
        for (int i = 0; i < ramSet.length; i++) {
            table.setValueAt("0x" + String.format("%02x", (int)ramSet[i]), i, 1);
        }
    }

    private static void updateMemRegSetTable(JTable table) {
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.SB)), 0, 0);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.SC)), 0, 1);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.DIV)), 0, 2);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.TIMA)), 0, 3);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.TMA)), 0, 4);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.TAC)), 0, 5);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.IF)), 0, 6);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.IE)), 0, 7);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.LCDC)), 0, 8);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.STAT)), 0, 9);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.SCY)), 0, 10);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.SCX)), 0, 11);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.LY)), 0, 12);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.BGP)), 0, 13);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.KEY1)), 0, 14);
        table.setValueAt("0x" + String.format("%04x", (int)memoryMap.readMemory(MemoryMap.VBK)), 0, 15);
    }

    private static void updateRegSetTable(JTable table) {
        table.setValueAt("0x" + String.format("%04x", registerSet.getWord(Reg_16.AF)), 0, 0);
        table.setValueAt("0x" + String.format("%04x", registerSet.getWord(Reg_16.BC)), 0, 1);
        table.setValueAt("0x" + String.format("%04x", registerSet.getWord(Reg_16.DE)), 0, 2);
        table.setValueAt("0x" + String.format("%04x", registerSet.getWord(Reg_16.HL)), 0, 3);
        table.setValueAt("0x" + String.format("%04x", registerSet.getWord(Reg_16.SP)), 0, 4);
        table.setValueAt("0x" + String.format("%04x", registerSet.getWord(Reg_16.PC)), 0, 5);
    }

    //#endregion

    static JTable vRamTable;
    static JTable ioTable;
    static JTable hRamTable;
    static JTable romTable;
    static JTable memRegTable;
    static JTable regTable;

    /**
     * Main function.
     * Makes the java program actually run. (edited by Angel)
     * @param args : the arguments in the command line.
     * We might make that the file name of the ROM it runs for all I know... (edited by Angel)
     */
    public static void main(String[] args) {
        // String rom = "test_roms/Tetris (Japan) (En).gb";
        // String rom = "test_roms/mts_binaries/acceptance/bits/reg_f.gb";
        // String rom = "test_roms/interrupt_time.gb";
        String rom = "test_roms/mts_binaries/manual-only/sprite_priority.gb";

        try {
            registerSet = new RegisterSet();
            memoryMap = new MemoryMap(registerSet, rom);
            interrupts = new Interrupts(memoryMap, registerSet);

            cpu = new CPU(registerSet, memoryMap, interrupts);
            timer = new Timers(memoryMap, interrupts);
            joy = new Joystick(memoryMap, interrupts);

            memoryMap.initializeDependencies(joy, timer);

            JFrame jframe = new JFrame("GMU IIT Gameboy Emulator");
            jframe.addKeyListener(joy);

            initializeMenuBar(jframe);

            lcd = new LCD(jframe, memoryMap, interrupts);
            jframe.pack();
            jframe.setLocationRelativeTo(null);
            jframe.isVisible();

            romTable = debugMemoryPanel("ROM", memoryMap.getRom(), 0x0000);
            memRegTable = debugMemoryRegSet();
            regTable = debugRegisterSet();

            updateRAMTable(romTable, memoryMap.getRom());
            updateMemRegSetTable(memRegTable);

            long startTime = System.nanoTime();

            // Main loop for the emulator
            while(true) {
                emulatorUpdate();

                double timeDiff = 0;
                while(timeDiff < DELTA_TIME) { timeDiff = System.nanoTime() - startTime; }
                _fps = (float)(1e9/timeDiff);
                startTime = System.nanoTime();

                jframe.setTitle("FPS: " + _fps.toString());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
