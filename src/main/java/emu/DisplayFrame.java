package emu;

import chip.Chip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

public class DisplayFrame extends JFrame implements KeyListener, ActionListener {
    public static volatile boolean keyPressed;
    private static final long serialVersionUID = 1L;
    private DisplayPanel panel;
    private static int[] keyBuffer = new int[16];
    private int[] keyIdToKey;

    private Chip chip;

    private JMenuBar topMenu;

    private JMenu file, options;
    private JMenuItem openRom, saveState, loadState, changeControls, changeColors, changeClockSpeed;
    public DisplayFrame(Chip chip) {
        this.chip = chip;
        setPreferredSize(new Dimension(640, 320));
        pack();
        setPreferredSize(new Dimension(640 + getInsets().left + getInsets().right, 320 + getInsets().top + getInsets().bottom));
        panel = new DisplayPanel(chip);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("Chip 8 Emulator");
        pack();
        addTopMenuBar();
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

    private void addTopMenuBar() {
        topMenu = new JMenuBar();

        file=new JMenu("File");
        openRom = new JMenuItem("Open ROM");
        saveState = new JMenuItem("Save State");
        loadState = new JMenuItem("Load State");

        file.add(openRom);
        file.add(saveState);
        file.add(loadState);

        file.addActionListener(this); //lesht, s'punon
        openRom.addActionListener(this);
        saveState.addActionListener(this);
        loadState.addActionListener(this);

        options = new JMenu("Options");
        changeControls = new JMenuItem("Change Controls");
        changeColors = new JMenuItem("Change Colors");
        changeClockSpeed = new JMenuItem("Change Clock Speed");

        options.add(changeControls);
        options.add(changeColors);
        options.add(changeClockSpeed);

        topMenu.add(file);
        topMenu.add(options);
        setJMenuBar(topMenu);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        //SPAGHET
        if(actionEvent.getSource() == openRom){
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(this);

            if(option == JFileChooser.APPROVE_OPTION){
                File f = fileChooser.getSelectedFile();
                String filepath=f.getPath();
                try{
                    chip.loadProgram(filepath);
                    drawUpdates();
                }catch (Exception ex) {ex.printStackTrace();  }

            }
        } else if (actionEvent.getSource() == saveState){
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(this);

            if (option == JFileChooser.APPROVE_OPTION){
                try{
                    chip.saveState(fileChooser.getSelectedFile().getPath());
                } catch (Exception ex){
                    ex.printStackTrace();
                }

            }
        } else if (actionEvent.getSource() == loadState){
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(this);

            if (option == JFileChooser.APPROVE_OPTION){
                try{
                    chip.loadState(fileChooser.getSelectedFile().getPath());
                } catch (Exception ex){
                    ex.printStackTrace();
                }

            }
        }



    }
}
