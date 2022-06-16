package emu;

import chip.Chip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TopMenu {

    private JMenuBar topMenu;
    private String currentRom = "ROMS/IBM Logo.ch8";

    private JMenu file, options, memoryView;
    private JMenuItem openRom, saveState, loadState, changeControls, changeColors, changeClockSpeed, resetRom, viewRam;

    private DisplayFrame displayFrame;
    private Chip chip;

    List<Character> controls;


    TopMenu(DisplayFrame displayFrame, Chip chip) {
        this.displayFrame = displayFrame;
        this.chip = chip;
        controls = new ArrayList<>(displayFrame.getKeyIdToKey().keySet());
    }

    public void addTopMenuBar() {
        topMenu = new JMenuBar();
        file = new JMenu("File");
        openRom = new JMenuItem("Open ROM");
        resetRom = new JMenuItem("Reset ROM");
        saveState = new JMenuItem("Save State");
        loadState = new JMenuItem("Load State");

        file.add(openRom);
        file.add(saveState);
        file.add(loadState);
        file.add(resetRom);

        openRom.addActionListener(displayFrame);
        saveState.addActionListener(displayFrame);
        loadState.addActionListener(displayFrame);
        resetRom.addActionListener(displayFrame);

        options = new JMenu("Options");
        changeControls = new JMenuItem("Change Controls");
        changeColors = new JMenuItem("Change Colors");
        changeClockSpeed = new JMenuItem("Change Clock Speed");

        options.add(changeControls);
        options.add(changeColors);
        options.add(changeClockSpeed);

        changeControls.addActionListener(displayFrame);
        changeClockSpeed.addActionListener(displayFrame);
        changeColors.addActionListener(displayFrame);

        memoryView = new JMenu("Memory View");
        viewRam = new JMenuItem("RAM usage");

        memoryView.add(viewRam);
        viewRam.addActionListener(displayFrame);

        topMenu.add(file);
        topMenu.add(options);
        topMenu.add(memoryView);
        displayFrame.setJMenuBar(topMenu);
    }

    public void openControlsWindow() {
        JFrame controlsWindow = new JFrame("Change controls");
        controlsWindow.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                chip.isPaused = false;
            }
            @Override
            public void windowOpened(WindowEvent e) {
                chip.isPaused = true;
            }
        });

        JPanel panel = new JPanel();
        for (Character key : controls) {
            JButton button = new JButton(displayFrame.getKeyIdToKey().get(key) + " - " + String.valueOf(key));
            button.addActionListener(
                    actionEvent -> {
                        String input = JOptionPane.showInputDialog(controlsWindow, "Enter key");
                        if (input.length() > 1) {
                            JOptionPane.showMessageDialog(displayFrame, "Inputs of more than one character are not allowed", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        displayFrame.changeControl(button.getText().charAt(button.getText().indexOf('-') + 2), input.toUpperCase().charAt(0));
                        button.setText(displayFrame.getKeyIdToKey().get(input.toUpperCase().charAt(0)) + " - " + input.toUpperCase().charAt(0));
                        controls = controls.stream().map(x -> {
                            if (x == key) return input.toUpperCase().charAt(0);
                            else return x;
                        }).collect(Collectors.toList());
                    }
            );
            panel.add(button);
        }
        controlsWindow.add(panel);
        controlsWindow.setSize(new Dimension(640 / 2, 340 / 2));
        controlsWindow.setResizable(false);
        controlsWindow.setVisible(true);
    }

    public void onFileMenuItemsClicked(ActionEvent actionEvent) {
        List<Component> children = Arrays.asList(file.getMenuComponents());
        JFileChooser fileChooser = null;
        int option = 0;
        if (children.contains(actionEvent.getSource())) {
            if(actionEvent.getSource() != resetRom) {
                chip.isPaused = true;
                fileChooser = new JFileChooser();
                option = fileChooser.showOpenDialog(displayFrame);
            }

            if (option == JFileChooser.APPROVE_OPTION) {
                File f;
                String filepath = null;
                if(actionEvent.getSource() != resetRom) {
                    f = fileChooser.getSelectedFile();
                   filepath = f.getPath();
                }
                try {
                    if (actionEvent.getSource() == openRom) {
                        currentRom = filepath;
                        chip.loadProgram(filepath);
                        displayFrame.drawUpdates();
                    } else if (actionEvent.getSource() == saveState) {
                        chip.saveState(fileChooser.getSelectedFile().getPath());
                    } else if (actionEvent.getSource() == loadState) {
                        chip.loadState(fileChooser.getSelectedFile().getPath());
                    } else if (actionEvent.getSource() == resetRom) {
                        chip.loadProgram(currentRom);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
            chip.isPaused = false;
        }

        if (actionEvent.getSource() == changeControls) {
            openControlsWindow();
        } else if (actionEvent.getSource() == changeClockSpeed) {
            openClockDialog();
        } else if (actionEvent.getSource() == changeColors) {
            openColorPicker();
        } else if (actionEvent.getSource() == viewRam){
            viewRamUsage();
        }
    }

    private void openColorPicker() {
        JFrame cpick = new PickerFrame(displayFrame, chip);
        cpick.setVisible(true);
    }

    private void openClockDialog() {
        try {
            int x = 0;
            chip.isPaused = true;
            String input = JOptionPane.showInputDialog("Enter the new clock rate", JOptionPane.OK_OPTION);
            if(input != null) x = Integer.parseInt(input);
            if (x == 0)
                JOptionPane.showMessageDialog(displayFrame, "0 is not allowed!", "Error", JOptionPane.ERROR_MESSAGE);
            else MainLoop.rate = 1000 / x;

        } catch (NumberFormatException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(displayFrame, "Please enter an integer", "Error", JOptionPane.ERROR_MESSAGE);
        }
        chip.isPaused = false;
    }

    private void viewRamUsage(){
        JFrame ramMessage = new JFrame();
        Runtime runtime = Runtime.getRuntime();

        NumberFormat format = NumberFormat.getInstance();

        StringBuilder sb = new StringBuilder();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        sb.append("Free memory: ").append(format.format(freeMemory / 1024)).append("\n");
        sb.append("Allocated memory: ").append(format.format(allocatedMemory / 1024)).append("\n");
        sb.append("Max memory: ").append(format.format(maxMemory / 1024)).append("\n");
        sb.append("Total free memory: ").append(format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024)).append("\n");

        JOptionPane.showConfirmDialog(ramMessage, sb, "RAM Information", JOptionPane.DEFAULT_OPTION);
    }
}
