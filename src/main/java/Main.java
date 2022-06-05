import chip.Chip;
import emu.Display;

public class Main extends Thread{
    private Chip chip8;
    private Display frame;

    public Main()
    {
        chip8 = new Chip();
        chip8.init();
        frame = new Display();
        frame.draw(chip8.getDisplay());
        chip8.loadProgram("/home/edis/IdeaProjects/CHIP-8/src/main/java/Pong 2 (Pong hack) [David Winter, 1997].ch8");
    }

    public void run(){
        while (true){
            chip8.run();
            if(chip8.needsRedraw()){
                frame.draw(chip8.getDisplay());
                chip8.removeDrawFlag();
            }
            try {
                Thread.sleep(16);
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
