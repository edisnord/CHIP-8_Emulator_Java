package emu;

import chip.Chip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class TopMenu{

    private JMenuBar topMenu;

    private JMenu file, options;
    private JMenuItem openRom, saveState, loadState, changeControls, changeColors, changeClockSpeed;

    private DisplayFrame displayFrame;
    private Chip chip;

    char[] controls = {'1','2','3','Q','W','E','A','S','D','Z','X','C','4','R','F','V'};
    TopMenu(DisplayFrame displayFrame, Chip chip){
        this.displayFrame=displayFrame;
        this.chip=chip;
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

        file.addActionListener(displayFrame); //lesht, s'punon
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

        topMenu.add(file);
        topMenu.add(options);
        displayFrame.setJMenuBar(topMenu);
    }

    public void openControlsWindow(){
        JFrame controlsWindow = new JFrame("Change controls");
        JPanel panel = new JPanel();

        for (char key : controls){
            JButton button = new JButton(String.valueOf(key));
            button.addActionListener((actionEvent -> {}));


            panel.add(button);
        }




        controlsWindow.add(panel);
        controlsWindow.setSize(new Dimension(640, 320));
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
        }


    }
}
