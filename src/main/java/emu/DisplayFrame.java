package emu;

import chip.Chip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class DisplayFrame extends JFrame implements KeyListener {
    public static volatile boolean keyPressed;
    private static final long serialVersionUID = 1L;
    private DisplayPanel panel;
    private static int[] keyBuffer = new int[16];
    private int[] keyIdToKey;

    public DisplayFrame(Chip c) {
        setPreferredSize(new Dimension(640, 320));
        pack();
        setPreferredSize(new Dimension(640 + getInsets().left + getInsets().right, 320 + getInsets().top + getInsets().bottom));
        panel = new DisplayPanel(c);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("Chip 8 Emulator");
        pack();
        setVisible(true);
        addKeyListener(this);

        keyIdToKey = new int[256];
        fillKeyIds();
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

    public void drawUpdates(){
        panel.repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (keyIdToKey[e.getKeyCode()] != -1) {
            DisplayFrame.keyBuffer[keyIdToKey[e.getKeyCode()]] = 1;
            DisplayFrame.keyPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (keyIdToKey[e.getKeyCode()] != -1) {
            DisplayFrame.keyPressed = false;
            DisplayFrame.keyBuffer[keyIdToKey[e.getKeyCode()]] = 0;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static int[] getKeyBuffer() {
        return keyBuffer;
    }

}
