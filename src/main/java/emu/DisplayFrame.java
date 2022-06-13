package emu;

import chip.Chip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DisplayFrame extends JFrame implements KeyListener, ActionListener {
    public static volatile boolean keyPressed;
    private static final long serialVersionUID = 1L;
    private DisplayPanel panel;
    private static int[] keyBuffer = new int[16];
    //private int[] keyIdToKey;
    LinkedHashMap<Character, Integer> keyIdToKey;

    private Chip chip;

    private TopMenu topMenu;


    public DisplayFrame(Chip chip) {
        this.chip = chip;
        keyIdToKey = new LinkedHashMap<>();
        fillKeyIds();
        topMenu = new TopMenu(this, this.chip);
        setPreferredSize(new Dimension(640, 320));
        pack();
        setResizable(false);
        setPreferredSize(new Dimension(640 + getInsets().left + getInsets().right, 320 + getInsets().top + getInsets().bottom));
        panel = new DisplayPanel(chip);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("Chip 8 Emulator");
        pack();
        topMenu.addTopMenuBar();
        setVisible(true);
        addKeyListener(this);
    }

    private void fillKeyIds() {
        keyIdToKey.put('1', 1);
        keyIdToKey.put('2', 2);
        keyIdToKey.put('3', 3);
        keyIdToKey.put('Q', 4);
        keyIdToKey.put('W', 5);
        keyIdToKey.put('E', 6);
        keyIdToKey.put('S', 8);
        keyIdToKey.put('A', 7);
        keyIdToKey.put('D', 9);
        keyIdToKey.put('Z', 0xA);
        keyIdToKey.put('X', 0);
        keyIdToKey.put('C', 0xB);
        keyIdToKey.put('4', 0xC);
        keyIdToKey.put('R', 0xD);
        keyIdToKey.put('F', 0xE);
        keyIdToKey.put('V', 0xF);
    }

    public void drawUpdates(){
        panel.repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        try{
            keyIdToKey.get((char)e.getKeyCode());
            DisplayFrame.keyBuffer[keyIdToKey.get((char)e.getKeyCode())] = 1;
            DisplayFrame.keyPressed = true;
        }catch (Exception ex){

        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        try{
            keyIdToKey.get((char)e.getKeyCode());
            DisplayFrame.keyBuffer[keyIdToKey.get((char)e.getKeyCode())] = 0;
            DisplayFrame.keyPressed = false;
        }catch (Exception ex){

        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static int[] getKeyBuffer() {
        return keyBuffer;
    }


    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        topMenu.onFileMenuItemsClicked(actionEvent);
    }

    public void changeControl(Character prevVal, Character newVal){
        int value = keyIdToKey.get(prevVal);
        keyIdToKey.remove(prevVal);
        keyIdToKey.put(newVal, value);

    }

    public HashMap<Character, Integer> getKeyIdToKey() {
        return keyIdToKey;
    }
}
