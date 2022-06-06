package emu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Display implements KeyListener {
    private JLabel[] jLabel = new JLabel[32 * 64];
    private int[] keyBuffer;
    private int[] keyIdToKey;

    public Display() {
        JFrame jFrame = new JFrame("CHIP-8");
        jFrame.setLayout(new GridLayout(32, 64));
        jFrame.setSize(640, 320);
        jFrame.setDefaultCloseOperation(jFrame.EXIT_ON_CLOSE);
        jFrame.setVisible(false);
        for (int i = 0; i < 32 * 64; ++i) {

            jLabel[i] = new JLabel();
            jLabel[i].setHorizontalAlignment(SwingConstants.CENTER);

            jLabel[i].setSize(10, 10);
            jLabel[i].setOpaque(true);

            jLabel[i].setBackground(new Color(0, 0, 0));

            jFrame.add(jLabel[i]);

        }
        jFrame.setVisible(true);
    }

    public void draw(byte[] graphics) {
        for (int i = 0; i < 32 * 64; ++i) {
            switch (graphics[i]) {
                case 1:
                    jLabel[i].setBackground(Color.WHITE);
                    break;
                case 0:
                    jLabel[i].setBackground(Color.BLACK);
                    break;

            }
        }
    }

    private void fillKeyIds() {
        for (int i = 0; i < keyIdToKey.length; i++) {
            keyIdToKey[i] = -1;
        }
        keyIdToKey['1'] = 1;
        keyIdToKey['2'] = 2;
        keyIdToKey['3'] = 3;
        keyIdToKey['Q'] = 4;
        keyIdToKey['W'] = 5;
        keyIdToKey['E'] = 6;
        keyIdToKey['A'] = 7;
        keyIdToKey['S'] = 8;
        keyIdToKey['D'] = 9;
        keyIdToKey['Z'] = 0xA;
        keyIdToKey['X'] = 0;
        keyIdToKey['C'] = 0xB;
        keyIdToKey['4'] = 0xC;
        keyIdToKey['R'] = 0xD;
        keyIdToKey['F'] = 0xE;
        keyIdToKey['V'] = 0xF;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (keyIdToKey[e.getKeyCode()] != -1) {
            keyBuffer[keyIdToKey[e.getKeyCode()]] = 1;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (keyIdToKey[e.getKeyCode()] != -1) {
            keyBuffer[keyIdToKey[e.getKeyCode()]] = 0;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public int[] getKeyBuffer() {
        return keyBuffer;
    }

}
