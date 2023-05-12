package gmu.iit.gameboy;

import javax.swing.*;

import gmu.iit.gameboy.Interrupts.InterruptTypes;

public class LCD {
    static final int CYCLES_TO_DRAW = 456;
    static final int SCREEN_RESOLUTION_X = 160;
    static final int SCREEN_RESOLUTION_Y = 144;
    static final int WINDOW_LENGTH = 256;

    static final int VBLANK_LINE_END = 153;
    static final int OAM_CYCLES = 80;
    static final int VRAM_TRANSFER_CYCLES = 172;

    static final int VBLANK_PERIOD = CYCLES_TO_DRAW - OAM_CYCLES;
    static final int VRAM_TRANSFER_PERIOD = VBLANK_PERIOD - VRAM_TRANSFER_CYCLES;

    Screen _screen = new Screen();
    MemoryMap _memoryMap;
    Interrupts _interrupts;
    JFrame _jframe;

    enum LCDStatus {
        HBLANK, // 00
		VBLANK, // 01
		OAM,    // 10
		VRAM,   // 11
    }

    LCDStatus lcdMode = LCDStatus.HBLANK;

    int lcdStatus;
    int totalScreen;
    int viewableScreen;

    int scrollX;
    int scrollY;
    int windowX;
    int windowY;
    int controlReg;

    int _scanlineCounter = 0;

    //int currentBackground[] = ;
    int currentWindow[] = new int[1024];
    int currentTileMap[] = new int[1024];
    int currentBGWindow[] = new int[4096];

    int[][] tempScreen = new int[WINDOW_LENGTH][WINDOW_LENGTH];
    int[] currentTile = new int[64];

    boolean LCDEnable;
    boolean windowDisplay;
    boolean spriteSize;
    boolean spriteEnabled;
    boolean bgDisplay;

    int lastTicks = 0;
    int tick = 0;

    public int[][] screenData = new int[SCREEN_RESOLUTION_X][SCREEN_RESOLUTION_Y];

    boolean isLCDEnableFlag() { return ((int)_memoryMap.readMemory(MemoryMap.LCDC) & (0x1 << 7)) > 0; }
    boolean isWindowTileMapDisplaySelectFlag() { return ((int)_memoryMap.readMemory(MemoryMap.LCDC) & (0x1 << 6)) > 0; }
    boolean isWindowDisplayEnableFlag() { return ((int)_memoryMap.readMemory(MemoryMap.LCDC) & (0x1 << 5)) > 0; }
    boolean isBGAndWindowTileDataSelectFlag() { return ((int)_memoryMap.readMemory(MemoryMap.LCDC) & (0x1) << 4) > 0; }
    boolean isBGTileMapDisplaySelectFlag() { return ((int)_memoryMap.readMemory(MemoryMap.LCDC) & (0x1 << 3)) > 0; }
    boolean isOBJSpriteSizeFlag() { return ((int)_memoryMap.readMemory(MemoryMap.LCDC) & (0x1) << 2) > 0; }
    boolean isOBJSpriteDisplayEnable() { return ((int)_memoryMap.readMemory(MemoryMap.LCDC) & (0x1 << 1)) > 0; }
    boolean isBGDisplay() { return ((int)_memoryMap.readMemory(MemoryMap.LCDC) & (0x1)) > 0; }

    public LCD(JFrame jframe, MemoryMap memoryMap, Interrupts interrupts) {
        // super("GMU IIT Gameboy Emulator");

        _memoryMap = memoryMap;
        _interrupts = interrupts;
        _jframe = jframe;

        _jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        _jframe.getContentPane().add(_screen);
    }

    public void changePalette(int[][] palette) { _screen.changePalette(palette); }

    public void updateGraphics(int cycles) {
        setLCDStatus();

        if (!isLCDEnableFlag()) { return; }

        _scanlineCounter -= cycles;
        if (_scanlineCounter > 0) { return; }

        _memoryMap.getIO()[0x44] = (char)(_memoryMap.getIO()[0x44] + 1);
        int currentLine = _memoryMap.readMemory(MemoryMap.LY);

        _scanlineCounter = CYCLES_TO_DRAW;

        if (currentLine == SCREEN_RESOLUTION_Y) {
            _interrupts.requestInterrupt(Interrupts.InterruptTypes.VBANK);
        } else if (currentLine > VBLANK_LINE_END) {
            _memoryMap.writeMemory(MemoryMap.LY, (char)0);
        } else if (currentLine < SCREEN_RESOLUTION_Y) {
            drawScanLine();
        }
    }

    public void drawScanLine() {
        char controlReg = _memoryMap.readMemory(MemoryMap.LCDC);
        if ((controlReg & 0x1) > 0) {
            drawBackgrounds();
        } else {
            drawSprites();
        }
    }

    public void setLCDStatus() {
        int status = _memoryMap.readMemory(MemoryMap.STAT);
        if (!isLCDEnableFlag()) {
            _scanlineCounter = CYCLES_TO_DRAW;
            _memoryMap.getIO()[0x44] = (char)0;
            status &= 0xFC;
            status |= 0x1;

            _memoryMap.writeMemory(MemoryMap.STAT, (char)status);
            return;
        }

        boolean hasRequestedInterrupt = false;
        char currentLine = _memoryMap.readMemory(MemoryMap.LY);
        lcdMode = LCDStatus.HBLANK;

        if (currentLine >= SCREEN_RESOLUTION_Y) {
            lcdMode = LCDStatus.OAM;
            status |= 0x1;
            status &= ~(0x1 << 1);
            hasRequestedInterrupt = ((status & (0x1 << 4)) > 0);
        } else if (_scanlineCounter >= VBLANK_PERIOD) {
            lcdMode = LCDStatus.VBLANK;
            status |= 0x1 << 1;
            status &= ~(0x1);
            hasRequestedInterrupt = ((status & (0x1 << 5)) > 0);
        } else if (_scanlineCounter >= VRAM_TRANSFER_PERIOD) {
            lcdMode = LCDStatus.VRAM;
            status |= 0x1 << 1;
            status |= 0x1;
        } else {
            lcdMode = LCDStatus.HBLANK;
            status &= ~(0x1 << 1);
            status &= ~(0x1);
            hasRequestedInterrupt = ((status & (0x1 << 3)) > 0);
        }

        LCDStatus currentMode = LCDStatus.values()[(status & 0x3)];
        if (hasRequestedInterrupt && (lcdMode != currentMode)) {
            _interrupts.requestInterrupt(InterruptTypes.LCDC);
        }

        if (currentLine == _memoryMap.readMemory(0xff45)) {
            status |= 0x1 << 2;
            if ((status & (0x1 << 2)) > 0) {
                _interrupts.requestInterrupt(InterruptTypes.LCDC);
            }
        } else {
            status &= ~(0x1 << 2);
        }

        _memoryMap.writeMemory(MemoryMap.STAT, (char)status);
    }

    public void drawBackgrounds() {
        scrollX = _memoryMap.readMemory(MemoryMap.SCX);
        scrollY = _memoryMap.readMemory(MemoryMap.SCY);
        windowX = _memoryMap.readMemory(MemoryMap.WINDOW_X) - 7;
        windowY = _memoryMap.readMemory(MemoryMap.WINDOW_Y);

        boolean isUsingWindow = isWindowDisplayEnableFlag() && (windowY <= _memoryMap.readMemory(MemoryMap.LY));

        boolean isUnsigned = isBGAndWindowTileDataSelectFlag();
        int tileData = isBGAndWindowTileDataSelectFlag()
            ? 0x8000
            : 0x8800;

        int backgroundMemory = isUsingWindow
            ? isWindowTileMapDisplaySelectFlag()
                ? 0x9c00
                : 0x9800
            : isBGTileMapDisplaySelectFlag()
                ? 0x9c00
                : 0x9800;

        int yPos = (isUsingWindow
            ? _memoryMap.readMemory(MemoryMap.LY) - windowY
            : _memoryMap.readMemory(MemoryMap.LY) + scrollY);

        int tileRow = (yPos / 8) * 32;
        for (int i = 0; i < SCREEN_RESOLUTION_X; i++) {
            int xPos = (isUsingWindow && i >= windowX)
                ? i - windowX
                : i + scrollX;

            int tileCol = xPos / 8;
            int tileAddress = backgroundMemory + tileRow + tileCol;

            int tileNum = isUnsigned
                ? _memoryMap.readMemory(tileAddress) % 0xFF
                : _memoryMap.readMemory(tileAddress);

            int tileLocation = isUnsigned
                ? tileData + (tileNum * 16)
                : tileData + ((tileNum + 128) * 16);

            int line = (yPos % 8) * 2;
            int data1 = _memoryMap.readMemory(tileLocation + line);
            int data2 = _memoryMap.readMemory(tileLocation + line + 1);

            int colorBit = xPos % 8;
            int colorNum = ((data2 & (0x1 << colorBit)) > 0 ? 2 : 0) + ((data1 & (0x1 << colorBit)) > 0 ? 1 : 0);
            screenData[i][(int)_memoryMap.readMemory(MemoryMap.LY)] = colorNum;
        }
    }


    public void drawSprites() {
        boolean use8x16 = isOBJSpriteSizeFlag();
        int ySize = use8x16 ? 16 : 8;

        for (int i = 0; i < 40; i++) {
            int index = i * 4;
            int yPos = (_memoryMap.readMemory(MemoryMap.OAM_START + index) - 16);
            int xPos = (_memoryMap.readMemory(MemoryMap.OAM_START + index + 1) - 8);
            int tileLocation = _memoryMap.readMemory(MemoryMap.OAM_START + index + 2);
            int attributes = _memoryMap.readMemory(MemoryMap.OAM_START + index + 3);

            boolean yFlip = (attributes & (0x1 << 6)) > 0;
            boolean xFlip = (attributes & (0x1 << 5)) > 0;

            int currentLine = _memoryMap.readMemory(MemoryMap.LY);

            if ((currentLine >= yPos) && (currentLine < (yPos + ySize))) {
                int line = yFlip
                    ? -(currentLine - yPos - ySize) * 2
                    :  (currentLine - yPos) * 2;

                int dataAddress = (0x8000 + (tileLocation * 16)) + line;
                char data1 = _memoryMap.readMemory(dataAddress);
                char data2 = _memoryMap.readMemory(dataAddress + 1);

                for (int j = 7; j >= 0; j--) {
                    int pixel = 7 + xPos - j;
                    screenData[pixel][currentLine] = 3;
                }
            }
        }
    }

    public void renderGraphics() {
        _screen.clearScreen();
        for (int i = 0; i < screenData.length; i++) {
            for (int j = 0; j < screenData[i].length; j++) {
                _screen.setPixel(i, j, screenData[i][j]);
            }
        }

        _jframe.setVisible(true);
        _screen.repaint();
    }
}