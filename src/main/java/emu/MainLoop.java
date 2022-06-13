package emu;

import chip.Chip;

public class MainLoop extends Thread{
    private Chip chip8;
    private DisplayPanel frame;
    private DisplayFrame DisplayFrame;
    static int rate = 16;

    public MainLoop()
    {
        chip8 = new Chip();
        chip8.init();
        DisplayFrame = new DisplayFrame(chip8);
        chip8.loadProgram("ROMS/IBM Logo.ch8");

    }

    public void run(){
        while (true){
            chip8.run();
            chip8.setKeyBuffer(DisplayFrame.getKeyBuffer());
            if(chip8.needsRedraw()){
                DisplayFrame.drawUpdates();
                chip8.removeDrawFlag();
            }
            try {
                Thread.sleep(MainLoop.rate);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        var main = new MainLoop();
        main.start();
    }

}
