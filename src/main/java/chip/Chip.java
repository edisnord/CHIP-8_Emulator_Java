package chip;

import emu.DisplayFrame;

import com.google.gson.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class Chip {

    /**
     * 4kB of 8-bit memory<br/>
     * At position 0x50: The "bios" fontset
     * At position 0x200: The start of every program
     */
    private char[] memory;

    /**
     * 16 8-bit registers.<br/>
     * They will be used to store data which is used in several operation<br/>
     * Register 0xF is used for Carry, Borrow and collision detection
     */
    private char[] V;
    /**
     * 16-bit (only 12 are used) to point to a specific point in the memory
     */
    private char I;
    /**
     * The 16-bit (only 12 are used) to point to the current operation
     */
    private char pc;

    /**
     * Subroutine callstack<br/>
     * Allows up to 16 levels of nesting
     */
    private char stack[];
    /**
     * Points to the next free slot int the stack
     */
    private int stackPointer;

    /**
     * This timer is used to delay events in programs/games
     */
    private int delay_timer;
    /**
     * This timer is used to make a beeping sound
     */
    private int sound_timer;

    /**
     * This array will be our keyboard state
     */
    private byte[] keys;
    /**
     * The 64x32 pixel monochrome (black/white) display
     */
    private byte[] display;

    private boolean needRedraw;
    private static boolean beeped;

    /**
     * Reset the Chip 8 memory and pointers
     */
    public void init() {
        memory = new char[4096];
        V = new char[16];
        I = 0x0;
        pc = 0x200;

        stack = new char[16];
        stackPointer = 0;

        beeped = false;

        delay_timer = 0;
        sound_timer = 0;

        keys = new byte[16];

        display = new byte[64 * 32];

        needRedraw = false;
        loadFontset();
    }

    /**
     * Executes a single Operation Code (Opcode)
     */
    public void run() {
        //fetch Opcode
        char opcode = (char) ((memory[pc] << 8) | memory[pc + 1]);
        System.out.print(Integer.toHexString(opcode).toUpperCase() + ": ");
        //decode opcode
        switch (opcode & 0xF000) {

            case 0x0000: //Multi-case
                switch (opcode & 0x00FF) {
                    case 0x00E0: //00E0: Clear Screen
                        display = new byte[32 * 64];
                        pc += 0x2;
                        break;

                    case 0x00EE: //00EE: Returns from subroutine
                        stackPointer--;
                        pc = (char) (stack[stackPointer] + 2);
                        System.out.println("Returning to " + Integer.toHexString(pc).toUpperCase());
                        break;

                    default: //0NNN: Calls RCA 1802 Program at address NNN
                        //Very few programs use this, and realistically
                        //I cannot run this code on any modern CPUs, so
                        //it will remain unsupported
                        System.err.println("Unsupported Opcode!(0x0)");
                        System.exit(0);
                        break;
                }
                break;

            case 0x1000: //1NNN: Jumps to address NNN
                int nnn = opcode & 0x0FFF;
                System.out.println("Jumping to address " + Integer.toHexString(nnn));
                pc = (char) nnn;
                break;

            case 0x2000: //2NNN: Calls subroutine at NNN
                stack[stackPointer] = pc;
                stackPointer++;
                pc = (char) (opcode & 0x0FFF);
                System.out.println("Calling " + Integer.toHexString(pc).toUpperCase() + " from " + Integer.toHexString(stack[stackPointer - 1]).toUpperCase());
                break;

            case 0x3000: { //3XNN: Skips the next instruction if VX equals NN
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                if (V[x] == nn) {
                    pc += 4;
                    System.out.println("Skipping next instruction (V[" + x + "] == " + nn + ")");
                } else {
                    pc += 2;
                    System.out.println("Not skipping next instruction (V[" + x + "] != " + nn + ")");
                }
                break;
            }

            case 0x4000://4XNN: Skips the next instruction if VX does not equal NN
            {
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                if (V[x] != nn) {
                    pc += 4;
                    System.out.println("Skipping next instruction (V[" + x + "] == " + nn + ")");
                } else {
                    pc += 2;
                    System.out.println("Not skipping next instruction (V[" + x + "] != " + nn + ")");
                }
                break;
            }

            case 0x6000: { //6XNN: Set VX to NN
                int x = (opcode & 0x0F00) >> 8;
                V[x] = (char) (opcode & 0x00FF);
                pc += 2;
                System.out.println("Setting V[" + x + "] to " + (int) V[x]);
                break;
            }

            case 0x7000: { //7XNN: Adds NN to VX
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                V[x] = (char) ((V[x] + nn) & 0xFF);
                pc += 2;
                System.out.println("Adding " + nn + " to V[" + x + "] = " + (int) V[x]);
                break;
            }

            case 0x8000: //Contains more data in last nibble

                switch (opcode & 0x000F) {
                    case 0x0000: //8XY0: Sets VX to the value of VY.
                    {
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        V[x] = V[y];
                        pc += 0x2;
                        System.out.println("V[" + x + "] has been set to V[" + y + "]");
                        break;
                    }
                    case 0x0002: //8XY2: Sets VX to VX and VY. (Bitwise AND operation)
                    {
                        int vx = V[(opcode & 0x0F00) >> 8];
                        int vy = V[(opcode & 0x00F0) >> 4];
                        V[(opcode & 0x0F00) >> 8] = (char) (vx & vy);
                        System.out.println("Set V[" + ((opcode & 0x0F00) >> 8) + "] to V[" + ((opcode & 0x0F00) >> 8) + "] & V[" + ((opcode & 0x00F0) >> 4) + "]");
                        pc += 2;
                        break;
                    }
                    case 0x0003: {//8XY3 Sets VX to VX xor VY.
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        System.out.println("XOR-ing V[" + x + "] and V[" + y + "] and storing result to V[" + x + "]");
                        V[x] = (char) ((V[x] ^ V[y]) & 0xFF);
                        pc += 2;
                        break;
                    }
                    case 0x0004: {
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        System.out.println("Adding V[" + x + "] and V[" + y + "], apply carry if needed");
                        if (V[y] > 255 - V[x]) V[0xF] = 1;
                        else V[0xF] = 0;
                        V[x] = (char) ((V[x] + V[y]) & 0xFF);
                        pc += 0x2;
                        break;
                    }

                    case 0x0005: { //VY is subtracted from VX. VF is set to 0 when there is a borrow else 1
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        System.out.print("V[" + x + "] = " + (int) V[x] + " V[" + y + "] = " + (int) V[y] + ", ");
                        if (V[x] > V[y]) {
                            V[0xF] = 1;
                            System.out.println("No Borrow");
                        } else {
                            V[0xF] = 0;
                            System.out.println("Borrow");
                        }
                        V[x] = (char) ((V[x] - V[y]) & 0xFF);
                        pc += 2;
                        break;
                    }
                    case 0x0006: {//8XY6 Stores the least significant bit of VX in VF and then shifts VX to the right by 1.
                        //I don't know why they had Y in this opcode, but WikiPedia claims that the functionality
                        //of this OpCode was unintentional, so it might have had a different implementation originally
                        int x = (opcode & 0x0F00) >> 8;
                        int lsb = V[x] & 0x1;
                        V[0xF] = (char) lsb;
                        V[x] = (char) (V[x] >> 1);
                        System.out.println("Stored least significant bit of V[" + x +
                                "] to V[0xF] and shifted V[" + x + "] to the right");
                        pc += 2;
                        break;
                    }
                    default:
                        System.err.println("Unsupported Opcode!(0x8)");
                        System.exit(0);
                        break;
                }

                break;

            case 0xA000: //ANNN: Set I to NNN
                I = (char) (opcode & 0x0FFF);
                pc += 2;
                System.out.println("Set I to " + Integer.toHexString(I).toUpperCase());
                break;

            case 0xC000: //Set VX to random number anded with NN (CXNN)
            {
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                int randomNumber = new Random().nextInt(256) & nn;
                System.out.println("V[" + x + "] has been set to (randomised) " + randomNumber);
                V[x] = (char) randomNumber;
                pc += 2;
                break;
            }
            case 0xD000: { //DXYN: Draw a sprite (X, Y) size (8, N). Sprite is located at I
                int x = V[(opcode & 0x0F00) >> 8] % 64;
                int y = V[(opcode & 0x00F0) >> 4] % 32;
                int height = opcode & 0x000F;

                V[0xF] = 0;

                for (int _y = 0; _y < height; _y++) {
                    int line = memory[I + _y];
                    for (int _x = 0; _x < 8; _x++) {
                        int pixel = line & (0x80 >> _x);
                        if (pixel != 0) {
                            int totalX = x + _x;
                            int totalY = y + _y;
                            if (totalY > 31) {
                                totalY = totalY - 31;
                            }
                            if (totalX > 63) {
                                totalX = totalX - 63;
                            }
                            int index = (totalY * 64) + totalX;

                            if (display[index] == 1)
                                V[0xF] = 1;

                            display[index] ^= 1;
                        }
                    }
                }
                pc += 2;
                needRedraw = true;
                System.out.println("Drawing at V[" + ((opcode & 0x0F00) >> 8) + "] = " + x + ", V[" + ((opcode & 0x00F0) >> 4) + "] = " + y);
                break;
            }

            case 0xE000: {
                switch (opcode & 0x00FF) {
                    case 0x009E: { //EX9E Skip the next instruction if the Key VX is pressed
                        int key = V[(opcode & 0x0F00) >> 8];
                        if (keys[key] == 1) {
                            pc += 4;
                        } else {
                            pc += 2;
                        }
                        break;
                    }

                    case 0x00A1: { //EXA1 Skip the next instruction if the Key VX is NOT pressed
                        int key = V[(opcode & 0x0F00) >> 8];
                        if (keys[key] == 0) {
                            pc += 4;
                        } else {
                            pc += 2;
                        }
                        break;
                    }

                    default:
                        System.err.println("Unexisting opcode");
                        System.exit(0);
                        return;
                }
                break;
            }

            case 0xF000:
                switch (opcode & 0x00FF) {
                    case 0x000A: { //FX0A waits for user input and places input in VX
                        int x = (opcode & 0x0F00) >> 8;
                        int key = -1;
                        byte[] state = keys.clone();
                        while (!DisplayFrame.keyPressed){

                        }
                        setKeyBuffer(DisplayFrame.getKeyBuffer());
                        for (int i = 0; i < keys.length; i++) {
                            if(state[i] != keys[i]){
                                key = keys[i];
                                break;
                            }
                        }
                        if(key != -1)
                        V[x] = (char) key;
                        pc += 2;
                        keys = new byte[16];
                        break;
                    }
                    case 0x0018: //FX18 Sets the sound timer to VX.
                    {
                        int x = (opcode & 0x0F00) >> 8;
                        sound_timer = V[x];
                        pc += 2;
                        System.out.println("Sound timer has been set to " + sound_timer);
                        break;
                    }
                    case 0x0007: { //FX07: Set VX to the value of delay_timer
                        int x = (opcode & 0x0F00) >> 8;
                        V[x] = (char) delay_timer;
                        pc += 2;
                        System.out.println("V[" + x + "] has been set to " + delay_timer);
                        break;
                    }

                    case 0x0015: { //FX15: Set delay timer to V[x]
                        int x = (opcode & 0x0F00) >> 8;
                        delay_timer = V[x];
                        pc += 2;
                        System.out.println("Set delay_timer to V[" + x + "] = " + (int) V[x]);
                        break;
                    }

                    case 0x0029: { //Sets I to the location of the sprite for the character VX (Fontset)
                        int x = (opcode & 0x0F00) >> 8;
                        int character = V[x];
                        I = (char) (0x050 + (character * 5));
                        System.out.println("Setting I to Character V[" + x + "] = " + (int) V[x] + " Offset to 0x" + Integer.toHexString(I).toUpperCase());
                        pc += 2;
                        break;
                    }

                    case 0x0033: { //FX33 Store a binary-coded decimal value VX in I, I + 1 and I + 2
                        int x = (opcode & 0x0F00) >> 8;
                        int value = V[x];
                        int hundreds = (value - (value % 100)) / 100;
                        value -= hundreds * 100;
                        int tens = (value - (value % 10)) / 10;
                        value -= tens * 10;
                        memory[I] = (char) hundreds;
                        memory[I + 1] = (char) tens;
                        memory[I + 2] = (char) value;
                        System.out.println("Storing Binary-Coded Decimal V[" + x + "] = " + (int) (V[(opcode & 0x0F00) >> 8]) + " as { " + hundreds + ", " + tens + ", " + value + "}");
                        pc += 2;
                        break;
                    }

                    case 0x0065: { //FX65 Fills V0 to VX with values from I
                        int x = (opcode & 0x0F00) >> 8;
                        for (int i = 0; i <= x; i++) {
                            V[i] = memory[I + i];
                        }
                        System.out.println("Setting V[0] to V[" + x + "] to the values of merory[0x" + Integer.toHexString(I & 0xFFFF).toUpperCase() + "]");
                        pc += 2;
                        break;
                    }

                    case 0x001E: //Add VX to I (FX1E)
                        //V[x] = (char)((V[x] + V[y]) & 0xFF)
                    {
                        int x = (opcode & 0x0F00) >> 8;
                        I += V[x];
                        System.out.println("Added V[" + x + "] to I");
                        pc += 0x2;
                        break;
                    }
                    default:
                        System.err.println("Unsupported Opcode!(0xF)");
                        System.exit(0);
                }
                break;

            default:
                System.err.println("Unsupported Opcode!");
                System.exit(0);
        }
        if (sound_timer > 0) {
            sound_timer--;
            try {
                Chip.tone(1200, 100);
                System.out.println("Beep!");
            } catch (LineUnavailableException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            beeped = false;
        }
        if (delay_timer > 0)
            delay_timer--;
    }

    /**
     * Returns the display data
     *
     * @return Current state of the 64x32 display
     */
    public byte[] getDisplay() {
        return display;
    }

    /**
     * Checks if there is a redraw needed
     *
     * @return If a redraw is needed
     */
    public boolean needsRedraw() {
        return needRedraw;
    }

    /**
     * Notify the chip that is has been redrawn
     */
    public void removeDrawFlag() {
        needRedraw = false;
    }

    /**
     * Loads the program into the memory
     *
     * @param file The location of the program
     */
    public void loadProgram(String file) {
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(new File(file)));

            int offset = 0;
            while (input.available() > 0) {
                memory[0x200 + offset] = (char) (input.readByte() & 0xFF);
                offset++;
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    /**
     * Loads the fontset into the memory
     */
    public void loadFontset() {
        for (int i = 0; i < ChipData.fontset.length; i++) {
            memory[0x50 + i] = (char) (ChipData.fontset[i] & 0xFF);
        }
    }

    public void setKeyBuffer(int[] keyBuffer) {
        for (int i = 0; i < keys.length; i++) {
            keys[i] = (byte) keyBuffer[i];
        }
    }

    //Sound
    public static float SAMPLE_RATE = 8000f;

    public static void tone(int hz, int msecs)
            throws LineUnavailableException {
        if (!beeped)
            tone(hz, msecs, 1.0);
    }

    public static void tone(int hz, int msecs, double vol)
            throws LineUnavailableException {
        byte[] buf = new byte[1];
        AudioFormat af = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
        SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af);
        sdl.start();
        for (int i = 0; i < msecs * 8; i++) {
            double angle = i / (SAMPLE_RATE / hz) * 2.0 * Math.PI;
            buf[0] = (byte) (Math.sin(angle) * 127.0 * vol);
            sdl.write(buf, 0, 1);
        }
        sdl.drain();
        sdl.stop();
        sdl.close();
        beeped = true;
    }

    public void saveState(String filepath){
        String state = new Gson().toJson(this);
        try {
            Files.writeString(Paths.get(filepath), state);
        } catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void loadState(String filepath){
        try {
            Gson gson = new Gson();
        String state = Files.readString(Paths.get(filepath));
        Chip temp = gson.fromJson(state, Chip.class);
        this.init();
        this.memory = temp.getMemory();
        this.keys = temp.getKeys();
        this.display = temp.getDisplay();
        this.sound_timer = temp.getSound_timer();
        this.delay_timer = temp.getDelay_timer();
        this.I = temp.getI();
        this.needRedraw = temp.needsRedraw();
        this.pc = temp.getPc();
        this.stack = temp.getStack();
        this.stackPointer = temp.getStackPointer();
        this.V = temp.getV();
        } catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }
    public char[] getMemory() {
        return memory;
    }
    public char[] getV() {
        return V;
    }
    public char getI() {
        return I;
    }
    public char getPc() {
        return pc;
    }
    public char[] getStack() {
        return stack;
    }

    public int getStackPointer() {
        return stackPointer;
    }
    public int getDelay_timer() {
        return delay_timer;
    }
    public int getSound_timer() {
        return sound_timer;
    }
    public byte[] getKeys() {
        return keys;
    }

}