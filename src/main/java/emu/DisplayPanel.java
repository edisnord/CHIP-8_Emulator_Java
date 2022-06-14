package emu;

import chip.Chip;

import javax.swing.*;
import java.awt.*;

public class DisplayPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    public static Color primaryColor = Color.WHITE;
    public static Color secondaryColor = Color.BLACK;
    private Chip chip;

    public DisplayPanel(Chip chip) {
        this.chip = chip;
    }

    public void paint(Graphics g) {
        byte[] display = chip.getDisplay();
        for(int i = 0; i < display.length; i++) {
            if(display[i] == 1)
                g.setColor(primaryColor);
            else
                g.setColor(secondaryColor);

            int x = (i % 64);
            int y = (int)Math.floor(i / 64);

            g.fillRect(x * 10, y * 10, 10, 10);
        }
    }
}
