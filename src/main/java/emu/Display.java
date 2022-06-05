package emu;

import javax.swing.*;
import java.awt.*;

public class Display {
    private JLabel[][] jLabel = new JLabel[32][64];

    public Display() {
        JFrame jFrame = new JFrame("CHIP-8");
        jFrame.setLayout(new GridLayout(32, 64));
        jFrame.setSize(640, 320);
        jFrame.setDefaultCloseOperation(jFrame.EXIT_ON_CLOSE);
        jFrame.setVisible(false);
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 64; ++j) {
                jLabel[i][j] = new JLabel();
                jLabel[i][j].setHorizontalAlignment(SwingConstants.CENTER);

                jLabel[i][j].setSize(10, 10);
                jLabel[i][j].setOpaque(true);

                jLabel[i][j].setBackground(new Color(0, 0, 0));

                jFrame.add(jLabel[i][j]);
            }
            jFrame.setVisible(true);
        }

    }

    public void draw(byte[][] graphics){
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 64; ++j) {
                switch (graphics[i][j]){
                    case 1:
                        jLabel[i][j].setBackground(Color.WHITE);
                        break;
                    case 0:
                        jLabel[i][j].setBackground(Color.BLACK);
                        break;
                }
            }
        }
    }

}
