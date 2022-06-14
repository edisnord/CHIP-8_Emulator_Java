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
import java.util.Stack;

public class Chip {

    private Memory memory;

    public boolean isPaused;
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
    private Stack<Character> stack;

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

    private boolean drawFlag;
    private static boolean beeped;

    /**
     * Reset the Chip 8 memory and pointers
     */
    public void init() {

        memory = new Memory();
        stack = new Stack<>();

        isPaused = false;

        I = 0x0;
        pc = 0x200;

        beeped = false;

        delay_timer = 0;
        sound_timer = 0;

        keys = new byte[16];

        display = new byte[64 * 32];

        drawFlag = false;

    }

    /**
     * Executes a single Operation Code (Opcode)
     */
    public void run() {
        //fetch Opcode
        char opcode = (char) ((memory.RAM[pc] << 8) | memory.RAM[pc + 1]);
        System.out.print(Integer.toHexString(opcode).toUpperCase() + ": ");
        //decode opcode
        switch (opcode & 0xF000) {

            case 0x0000: //Multi-case
                switch (extractKK(opcode)) {
                    case 0x00E0: //00E0: Clear Screen
                        display = new byte[32 * 64];
                        nextInstruction();
                        break;

                    case 0x00EE: //00EE: Returns from subroutine
                        pc = (char) (stack.pop() + 2);
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

            case 0x1000: { //1NNN: Jumps to address NNN
                System.out.println("Jumping to address " + Integer.toHexString(extractNNN(opcode)));
                pc = (char) extractNNN(opcode);
                break;
            }
            case 0x2000: //2NNN: Calls subroutine at NNN
                stack.push(pc);
                pc = (char) (opcode & 0x0FFF);
                System.out.println("Calling " + Integer.toHexString(pc).toUpperCase() + " from " + Integer.toHexString(stack.peek()).toUpperCase());
                break;

            case 0x3000: { //3XNN: Skips the next instruction if VX equals NN
                int nn = (opcode & 0x00FF);
                if (memory.V[extractX(opcode)] == extractKK(opcode)) {
                    nextInstruction();
                    nextInstruction();
                    System.out.println("Skipping next instruction (V[" + extractX(opcode) + "] == " + extractKK(opcode) + ")");
                } else {
                    nextInstruction();
                    System.out.println("Not skipping next instruction (V[" + extractX(opcode) + "] != " + nn + ")");
                }
                break;
            }

            case 0x4000://4XNN: Skips the next instruction if VX does not equal NN
            {
                if (memory.V[extractX(opcode)] != extractKK(opcode)) {
                    pc += 4;
                    System.out.println("Skipping next instruction (V[" + extractX(opcode) + "] == " + extractKK(opcode) + ")");
                } else {
                    nextInstruction();
                    System.out.println("Not skipping next instruction (V[" + extractX(opcode) + "] != " + extractKK(opcode) + ")");
                }
                break;
            }
            case 0x5000: { //5XY0 Skips the next instruction if VX equals VY.
                if(memory.V[extractX(opcode)] == memory.V[extractY(opcode)]) {
                    System.out.println("Skipping next instruction V[" + extractX(opcode) + "] == V[" + extractY(opcode) + "]");
                    pc += 4;
                } else {
                    System.out.println("Skipping next instruction V[" + extractX(opcode) + "] =/= V[" + extractY(opcode) + "]");
                    nextInstruction();
                }
                break;
            }

            case 0x6000: { //6XNN: Set VX to NN
                memory.V[extractX(opcode)] = (char) (opcode & 0x00FF);
                nextInstruction();
                System.out.println("Setting V[" + extractX(opcode) + "] to " + (int) memory.V[extractX(opcode)]);
                break;
            }

            case 0x7000: { //7XNN: Adds NN to VX
                memory.V[extractX(opcode)] = (char) ((memory.V[extractX(opcode)] + extractKK(opcode)) & 0xFF);
                nextInstruction();
                System.out.println("Adding " + extractKK(opcode) + " to V[" 
                        + extractX(opcode) + "] = " + (int) memory.V[extractX(opcode)]);
                break;
            }

            case 0x8000: //Multi-case

                switch (extractN(opcode)) {
                    case 0x0000: //8XY0: Sets VX to the value of VY.
                    {
                        memory.V[extractX(opcode)] = memory.V[extractY(opcode)];
                        nextInstruction();
                        System.out.println("V[" + extractX(opcode) + "] has been set to V[" + extractY(opcode) + "]");
                        break;
                    }
                    case 0x0001: { //8XY1 Sets VX to VX or VY.
                        System.out.println("Setting V[" + extractX(opcode) + "] = V[" + extractX(opcode) + "] | V[" + extractY(opcode) + "]");
                        memory.V[extractX(opcode)] = (char)((memory.V[extractX(opcode)] | memory.V[extractY(opcode)]) & 0xFF);
                        nextInstruction();
                        break;
                    }
                    case 0x0002: //8XY2: Sets VX to VX and VY. (Bitwise AND operation)
                    {
                        int vx = memory.V[extractX(opcode)];
                        int vy = memory.V[extractY(opcode)];
                        memory.V[(opcode & 0x0F00) >> 8] = (char) (vx & vy);
                        System.out.println("Set V[" + extractX(opcode) + "] to V[" + extractX(opcode) + "] & V[" + extractY(opcode) + "]");
                        nextInstruction();
                        break;
                    }
                    case 0x0003: {//8XY3 Sets VX to VX xor VY.
                        System.out.println("XOR-ing V[" + extractX(opcode) + "] and V[" + extractY(opcode) + "] and storing result to V[" + extractX(opcode) + "]");
                        memory.V[extractX(opcode)] = (char) ((memory.V[extractX(opcode)] ^ memory.V[extractY(opcode)]) & 0xFF);
                        nextInstruction();
                        break;
                    }
                    case 0x0004: {
                        System.out.println("Adding V[" + extractX(opcode) + "] and V[" + extractY(opcode) + "], apply carry if needed");
                        if (memory.V[extractY(opcode)] > 255 - memory.V[extractX(opcode)]) memory.V[0xF] = 1;
                        else memory.V[0xF] = 0;
                        memory.V[extractX(opcode)] = (char) ((memory.V[extractX(opcode)] + memory.V[extractY(opcode)]) & 0xFF);
                        nextInstruction();
                        break;
                    }

                    case 0x0005: { //VY is subtracted from VX. VF is set to 0 when there is a borrow else 1
                        System.out.print("V[" + extractX(opcode) + "] = " + (int) memory.V[extractX(opcode)] + " V["
                                + extractY(opcode) + "] = " + (int) memory.V[extractY(opcode)] + ", ");
                        if (memory.V[extractX(opcode)] > memory.V[extractY(opcode)]) {
                            memory.V[0xF] = 1;
                            System.out.println("No Borrow");
                        } else {
                            memory.V[0xF] = 0;
                            System.out.println("Borrow");
                        }
                        memory.V[extractX(opcode)] = (char) ((memory.V[extractX(opcode)] - memory.V[extractY(opcode)]) & 0xFF);
                        nextInstruction();
                        break;
                    }
                    case 0x0006: {//8XY6 Stores the least significant bit of VX in VF and then shifts VX to the right by 1.
                        //I don't know why they had Y in this opcode, but WikiPedia claims that the functionality
                        //of this OpCode was unintentional, so it might have had a different implementation originally
                        int lsb = memory.V[extractX(opcode)] & 0x1;
                        memory.V[0xF] = (char) lsb;
                        memory.V[extractX(opcode)] = (char) (memory.V[extractX(opcode)] >> 1);
                        System.out.println("Stored least significant bit of V[" + extractX(opcode) +
                                "] to V[0xF] and shifted V[" + extractX(opcode) + "] to the right");
                        nextInstruction();
                        break;
                    }
                    case 0x000E: {//8XYE Stores the most significant bit of VX in VF and then shifts VX to the left by 1.
                        int msb = memory.V[extractX(opcode)] & 0x8000;
                        memory.V[0xF] = (char) msb;
                        memory.V[extractX(opcode)] = (char) (memory.V[extractX(opcode)] << 1);
                        System.out.println("Stored most significant bit of V[" + extractX(opcode) +
                                "] to V[0xF] and shifted V[" + extractX(opcode) + "] to the left");
                        nextInstruction();
                        break;
                    }
                    default:
                        System.err.println("Unsupported Opcode!(0x8)");
                        System.exit(0);
                        break;
                }

                break;

            case 0x9000: { //9XY0 Skips the next instruction if VX doesn't equal VY.
                if(memory.V[extractX(opcode)] != memory.V[extractY(opcode)]) {
                    System.out.println("Skipping next instruction V[" + extractX(opcode) + "] != V[" + extractY(opcode) + "]");
                    pc += 4;
                } else {
                    System.out.println("Skipping next instruction V[" + extractX(opcode) + "] !/= V[" + extractY(opcode) + "]");
                    nextInstruction();
                }
                break;
            }

            case 0xA000: //ANNN: Set I to NNN
                I = extractNNN(opcode);
                nextInstruction();
                System.out.println("Set I to " + Integer.toHexString(I).toUpperCase());
                break;

            case 0xB000: { //BNNN: Jumps to the address NNN plus V0.
                pc = (char)(extractNNN(opcode) + extractKK(memory.V[0]));
                break;
            }

            case 0xC000: //Set VX to random number anded with NN (CXNN)
            {
                extractX(opcode);
                int randomNumber = new Random().nextInt(256) & extractKK(opcode);
                System.out.println("V[" + extractX(opcode) + "] has been set to (randomised) " + randomNumber);
                memory.V[extractX(opcode)] = (char) randomNumber;
                nextInstruction();
                break;
            }
            case 0xD000: { //DXYN: Draw a sprite (X, Y) size (8, N). Sprite is located at I
                int x = memory.V[extractX(opcode)] % 64;
                int y = memory.V[extractY(opcode)] % 32;
                int height = opcode & 0x000F;

                memory.V[0xF] = 0;

                for (int i = 0; i < height; i++) {
                    int line = memory.RAM[I + i];
                    for (int j = 0; j < 8; j++) {
                        int pixel = line & (0x80 >> j);
                        if (pixel != 0) {
                            int totalX = x + j;
                            int totalY = y + i;
                            if (totalY > 31) {
                                totalY = totalY - 31;
                            }
                            if (totalX > 63) {
                                totalX = totalX - 63;
                            }
                            int index = (totalY * 64) + totalX;

                            if (display[index] == 1)
                                memory.V[0xF] = 1;

                            display[index] ^= 1;
                        }
                    }
                }
                nextInstruction();
                drawFlag = true;
                System.out.println("Drawing at V[" + extractX(opcode) + "] = " + x + ", V[" + extractY(opcode) + "] = " + y);
                break;
            }

            case 0xE000: {
                switch (extractKK(opcode)) {
                    case 0x009E: { //EX9E Skip the next instruction if the Key VX is pressed
                        int key = memory.V[extractX(opcode)];
                        if (keys[key] == 1) {
                            nextInstruction();
                            nextInstruction();
                            System.out.println("Skipped instruction, V[" + extractX(opcode) + "] was pressed");
                        } else {
                            nextInstruction();
                        }
                        break;
                    }

                    case 0x00A1: { //EXA1 Skip the next instruction if the Key VX is NOT pressed
                        int key = memory.V[extractX(opcode)];
                        if (keys[key] == 0) {
                            nextInstruction();
                            nextInstruction();
                        } else {
                            nextInstruction();
                        }
                        break;
                    }

                    default:
                        System.err.println("Unexisting opcode 0xE");
                        System.exit(0);
                        return;
                }
                break;
            }

            case 0xF000:
                switch (extractKK(opcode)) {
                    case 0xA: { //FX0A waits for user input and places input in VX
                        int key = -1;
                        byte[] state = keys.clone();
                        while (!DisplayFrame.keyPressed);
                        setKeyBuffer(DisplayFrame.getKeyBuffer());
                        for (int i = 0; i < keys.length; i++) {
                            if(state[i] != keys[i]){
                                key = i;
                                break;
                            }
                        }
                        if(key != -1)
                        memory.V[extractX(opcode)] = (char) key;
                        nextInstruction();
                        keys = new byte[16];
                        break;
                    }
                    case 0x18: //FX18 Sets the sound timer to VX.
                    {
                        sound_timer = memory.V[extractX(opcode)];
                        nextInstruction();
                        System.out.println("Sound timer has been set to " + sound_timer);
                        break;
                    }
                    case 0x7: { //FX07: Set VX to the value of delay_timer
                        memory.V[extractX(opcode)] = (char) delay_timer;
                        nextInstruction();
                        System.out.println("V[" + extractX(opcode) + "] has been set to " + delay_timer);
                        break;
                    }

                    case 0x15: { //FX15: Set delay timer to V[x]
                        delay_timer = memory.V[extractX(opcode)];
                        nextInstruction();
                        System.out.println("Set delay_timer to V[" + extractX(opcode) + "] = " + (int) memory.V[extractX(opcode)]);
                        break;
                    }

                    case 0x29: { //Sets I to the location of the sprite for the character VX (Fontset)
                        int character = memory.V[extractX(opcode)];
                        I = (char) (0x050 + (character * 5));
                        System.out.println("Setting I to Character V[" + extractX(opcode) + "] = " + (int) memory.V[extractX(opcode)] +
                                " Offset to 0x" + Integer.toHexString(I).toUpperCase());
                        nextInstruction();
                        break;
                    }

                    case 0x33: { //FX33 Store a binary-coded decimal value VX in I, I + 1 and I + 2
                        int value = memory.V[extractX(opcode)];
                        int hundreds = (value - (value % 100)) / 100;
                        value -= hundreds * 100;
                        int tens = (value - (value % 10)) / 10;
                        value -= tens * 10;
                        memory.RAM[I] = (char) hundreds;
                        memory.RAM[I + 1] = (char) tens;
                        memory.RAM[I + 2] = (char) value;
                        System.out.println("Storing Binary-Coded Decimal V[" + extractX(opcode) + "] = " + (int) (memory.V[(opcode & 0x0F00) >> 8]) + " as { " + hundreds + ", " + tens + ", " + value + "}");
                        nextInstruction();
                        break;
                    }
                    case 0x55: { //FX35 Stores from V0 to VX (including VX) in memory, starting at address I.
                        for (int i = 0; i <= extractX(opcode); i++) {
                            memory.RAM[I + i] = memory.V[i];
                        }
                        System.out.println("Setting the values of memory from " +
                                Integer.toHexString(I) + " to " + Integer.toHexString(I + extractX(opcode)) + " from the registers " +
                                "V[0] to V[" + Integer.toHexString(extractX(opcode)) + "]");
                        nextInstruction();
                        break;
                    }
                    case 0x65: { //FX65 Fills V0 to VX with values from I
                        for (int i = 0; i <= extractX(opcode); i++) {
                            memory.V[i] = memory.RAM[I + i];
                        }
                        System.out.println("Setting V[0] to V[" + extractX(opcode) + "] to the values of memory[0x" + Integer.toHexString(I & 0xFFFF).toUpperCase() + "]");
                        nextInstruction();
                        break;
                    }

                    case 0x1E: //Add VX to I (FX1E)
                    {
                        I += memory.V[extractX(opcode)];
                        System.out.println("Added V[" + extractX(opcode) + "] to I");
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
        return drawFlag;
    }

    /**
     * Notify the chip that is has been redrawn
     */
    public void removeDrawFlag() {
        drawFlag = false;
    }

    /**
     * Loads the program into the memory
     *
     * @param file The location of the program
     */
    public void loadProgram(String file) {
        init();
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(new File(file)));

            int offset = 0;
            while (input.available() > 0) {
                memory.RAM[0x200 + offset] = (char) (input.readByte() & 0xFF);
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
        this.drawFlag = temp.needsRedraw();
        this.pc = temp.getPc();
        this.stack = temp.getStack();
        } catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    //nnn are the 12 lowest bits (oNNN)
    private char extractNNN(char instruction){
        return (char)(instruction & 0xFFF);
    }

    //kk are the 8 lowest bits (ookk)
    private char extractKK(char instruction){
        return (char)(instruction & 0xFF);
    }

    //x are the oXoo
    private char extractX(char instruction){
        return (char) ( (instruction & 0x0F00) >>> 8);
    }

    //y are the ooYo
    private char extractY(char instruction){
        return (char) ( (instruction & 0x00F0) >>> 4);
    }

    //n are the oooN
    private char extractN(char instruction){
        return (char) (instruction & 0x00F);
    }
    
    private void nextInstruction(){
        pc += 0x2;
    }

    public Memory getMemory() {
        return memory;
    }
    public char getI() {
        return I;
    }
    public char getPc() {
        return pc;
    }
    public Stack<Character> getStack() {
        return stack;
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