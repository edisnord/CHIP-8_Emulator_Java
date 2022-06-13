package emu;

import chip.Chip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TopMenu{

    private JMenuBar topMenu;

    private JMenu file, options;
    private JMenuItem openRom, saveState, loadState, changeControls, changeColors, changeClockSpeed;

    private DisplayFrame displayFrame;
    private Chip chip;

    List<Character> controls;
    TopMenu(DisplayFrame displayFrame, Chip chip){
        this.displayFrame=displayFrame;
        this.chip=chip;
        controls = new ArrayList<>(displayFrame.getKeyIdToKey().keySet());
    }

    public void addTopMenuBar() {
        topMenu = new JMenuBar();
        file=new JMenu("File");
        openRom = new JMenuItem("Open ROM");
        saveState = new JMenuItem("Save State");
        loadState = new JMenuItem("Load State");

        file.add(openRom);
        file.add(saveState);
        file.add(loadState);

        openRom.addActionListener(displayFrame);
        saveState.addActionListener(displayFrame);
        loadState.addActionListener(displayFrame);

        options = new JMenu("Options");
        changeControls = new JMenuItem("Change Controls");
        changeColors = new JMenuItem("Change Colors");
        changeClockSpeed = new JMenuItem("Change Clock Speed");

        options.add(changeControls);
        options.add(changeColors);
        options.add(changeClockSpeed);

        changeControls.addActionListener(displayFrame);
        changeClockSpeed.addActionListener(displayFrame);

        topMenu.add(file);
        topMenu.add(options);
        displayFrame.setJMenuBar(topMenu);
    }

    public void openControlsWindow(){
        JFrame controlsWindow = new JFrame("Change controls");
        JPanel panel = new JPanel();
        for (Character key : controls){
            JButton button = new JButton(displayFrame.getKeyIdToKey().get(key) + " - " + String.valueOf(key));
            button.addActionListener(
                    actionEvent -> {String input = JOptionPane.showInputDialog(controlsWindow,"Enter key");
                        displayFrame.changeControl(button.getText().charAt(button.getText().indexOf('-') + 2), input.toUpperCase().charAt(0));
                        button.setText(displayFrame.getKeyIdToKey().get(input.toUpperCase().charAt(0)) + " - " + String.valueOf(input.toUpperCase().charAt(0)));
                        controls = controls.stream().map(x->{
                            if(x == key) return input.toUpperCase().charAt(0);
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

        if (children.contains(actionEvent.getSource())){
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(displayFrame);

            if(option == JFileChooser.APPROVE_OPTION){
                File f = fileChooser.getSelectedFile();
                String filepath=f.getPath();
                try{
                    if(actionEvent.getSource() == openRom) {
                        chip.loadProgram(filepath);
                        displayFrame.drawUpdates();
                    } else if (actionEvent.getSource() == saveState){
                        chip.saveState(fileChooser.getSelectedFile().getPath());
                    } else if (actionEvent.getSource() == loadState){
                        chip.loadState(fileChooser.getSelectedFile().getPath());
                    }
                }catch (Exception ex) {ex.printStackTrace();  }
            }
        }

        if(actionEvent.getSource()==changeControls){
            openControlsWindow();
        } else if(actionEvent.getSource()==changeClockSpeed){
            openClockDialog();
        }


    }

    private void openClockDialog(){
        try{
            String input = JOptionPane.showInputDialog("Enter the new clock rate", JOptionPane.OK_OPTION);
            int x = Integer.parseInt(input);
            if(x == 0) JOptionPane.showMessageDialog(displayFrame, "0 is not allowed!", "Error", JOptionPane.ERROR_MESSAGE);
            else MainLoop.rate = 1000 / x;

        } catch (NumberFormatException e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(displayFrame, "Please enter an integer", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}
