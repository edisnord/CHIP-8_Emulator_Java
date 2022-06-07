import chip.Chip;
import emu.DisplayPanel;
import emu.DisplayFrame;

public class Main extends Thread{
    private Chip chip8;
    private DisplayPanel frame;
    private DisplayFrame DisplayFrame;

    public Main()
    {
        chip8 = new Chip();
        DisplayFrame = new DisplayFrame(chip8);
        chip8.init();
        chip8.loadProgram("C:\\Users\\Edis Hasaj\\IdeaProjects\\CHIP-8_Emulator_Java\\src\\main\\java\\Breakout (Brix hack) [David Winter, 1997].ch8");
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
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        var main = new Main();
        main.start();
    }

}
