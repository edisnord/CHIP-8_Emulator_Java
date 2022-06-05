package chip;

import java.io.*;
import java.util.Stack;

public class Chip {
    private char[] memory; //RAM
    private char[] v; //Registers
    private char I; //Memory address pointer
    private char pc; //Program counter(intitial program point)

    private Stack<Character> stack;

    private byte delay_timer; //Delay timer
    private byte sound_timer; //Sound timer

    private byte[] keys; //Keyboard keys

    private boolean drawFlag;

    private byte[][] display; //Pixels

    public byte[][] getDisplay() {
        return display;
    }

    public boolean needsRedraw(){
        return drawFlag;
    }

    public void removeDrawFlag(){
        drawFlag = false;
    }

    public void init() {
        memory = new char[4096]; //RAM initialization for 4kb(4096 bytes)

        char[] fontset = {
                0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
                0x20, 0x60, 0x20, 0x20, 0x70, // 1
                0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
                0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
                0x90, 0x90, 0xF0, 0x10, 0x10, // 4
                0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
                0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
                0xF0, 0x10, 0x20, 0x40, 0x40, // 7
                0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
                0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
                0xF0, 0x90, 0xF0, 0x90, 0x90, // A
                0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
                0xF0, 0x80, 0x80, 0x80, 0xF0, // C
                0xE0, 0x90, 0x90, 0x90, 0xE0, // D
                0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
                0xF0, 0x80, 0xF0, 0x80, 0x80  // F
        };

        //Convention: Load fonts from address 050 to 09F
        for (int i = 0x050; i < 0x09F ; i++) {
            memory[i] = (char)(fontset[i - 0x050] & 0xFF);
        }

        v = new char[16];
        I = 0x0;
        pc = 0x200; //All CHIP-8 programs start at slot 512 in the memory or at 200 hex

        drawFlag = false;

        stack = new Stack<>();

        delay_timer = 0;
        sound_timer = 0;

        keys = new byte[16];

        display = new byte[32][64];
    }

    public void run() {
        //Fetch Opcode(operation code)
        char opcode = (char) ((memory[pc] << 8) | memory[pc + 1]); //Combine two bytes from memory into one opcode using
        //bitwise or and left shift
        System.out.print(Integer.toHexString(opcode) + ": ");
        //Decode Opcode and Execute
        switch (opcode & 0xF000) { //Only 4 last bits(first nibble)
            case 0x0000:
                break;
            case 0x1000:
                pc = (char) (opcode & 0x0FFF);
                break;
            case 0x2000: //Call subroutine in the address from the last 3 nibbles of the opcode
                stack.push(pc);
                pc = (char)(opcode & 0x0FFF);
                break;
            case 0x3000:
                if((opcode & 0x00FF) == v[(opcode & 0x0F00) >> 8]) pc += 0x4;
                else pc += 0x2;
                break;
            case 0x4000:
                break;
            case 0x5000:
                break;
            case 0x6000://Set the value of the register X in V(2bnd nibble) to the value NN(3rd and 4th nibble)
                v[(char)(opcode & 0x0F00) >> 8] = (char)(opcode & 0x00FF);
                pc += 0x2;
                break;
            case 0x7000: //Add NN to VX (7XNN)
                v[(char)(opcode & 0x0F00) >> 8] = (char)(((opcode & 0x00FF) +
                                                   v[(char)(opcode & 0x0F00) >> 8])
                                                   & 0xFF); //Masked qe tmos beje overflow
                pc += 0x2;
                break;
            case 0x8000: //Contains variable data in last nibble
                switch (opcode & 0x000F){
                    case 0x0000: //Set value of register VX to value of VY
                    default:
                        System.err.println("Unsupported OpCode!");
                        System.exit(1);
                        break;
                }
            case 0x9000:
                break;
            case 0xA000: //Set I to NNN (ANNN)
                I = (char)(opcode & 0x0FFF);
                pc += 0x2;
                break;
            case 0xB000:
                break;
            case 0xC000:
                break;
            case 0xD000: //Draw Sprite (X, Y) with size (8, N), with location at I (DXYN)
                int x = v[(opcode & 0x0F00) >> 8] % 64;
                int y = v[(opcode & 0x00F0) >> 4] % 32;
                int height = opcode & 0x00F;

                for (int i = 0; i < height; i++) {
                    int line = memory[I + i];
                    for (int j = 0; j < 8; j++) {
                        int pixel = line & (0x80 >> j);
                        if(pixel != 0){
                            display[y + i][x + j] ^= 1;
                        }
                    }
                }
                pc += 0x2;
                drawFlag = true;
                break;
            case 0xF000:
                break;
            default:
                System.err.println("Unsupported opcode!");
                System.exit(1);
        }

    }

    public void loadProgram(String File) {
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(File));
            int offset = 0;
            while (input.available() > 0){
                memory[0x200 + offset++] = (char)(input.readByte() & 0xff); //Bitmask to only take first 8 bits
            }
        } catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

}
