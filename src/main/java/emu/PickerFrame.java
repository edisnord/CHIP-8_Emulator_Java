package emu;

import chip.Chip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PickerFrame extends JFrame implements AdjustmentListener {
    DisplayFrame displayFrame;
    private JLabel redLabel;

    private JLabel greenLabel;

    private JLabel blueLabel;

    private JScrollBar red;

    private JScrollBar green;

    private JScrollBar blue;

    private JPanel colorPanel;

    private JButton primaryConfirm;
    private JButton secondaryConfirm;

    private JPanel buttonPanel;

    PickerFrame(DisplayFrame displayFrame, Chip chip) {
        setTitle("ColorSelect");
        setSize(400, 300);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                chip.isPaused = false;
            }

            @Override
            public void windowOpened(WindowEvent e) {
                chip.isPaused = true;
            }
        });

        Container contentPane = getContentPane();

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(3, 2, 3, 3));

        p.add(redLabel = new JLabel("Red 0"));
        p.add(red = new JScrollBar(Adjustable.HORIZONTAL, 0, 0, 0, 255));
        red.setBlockIncrement(16);
        red.addAdjustmentListener(this);

        p.add(greenLabel = new JLabel("Green 0"));
        p.add(green = new JScrollBar(Adjustable.HORIZONTAL, 0, 0, 0, 255));
        green.setBlockIncrement(16);
        green.addAdjustmentListener(this);

        p.add(blueLabel = new JLabel("Blue 0"));
        p.add(blue = new JScrollBar(Adjustable.HORIZONTAL, 0, 0, 0, 255));
        blue.setBlockIncrement(16);
        blue.addAdjustmentListener(this);


        contentPane.add(p, "South");

        colorPanel = new JPanel();
        colorPanel.setBackground(new Color(0, 0, 0));
        contentPane.add(colorPanel, "Center");

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 2, 0, 0));
        buttonPanel.add(primaryConfirm = new JButton("Set primary Color"));
        primaryConfirm.setMaximumSize(new Dimension(100, 20));
        primaryConfirm.addActionListener(action -> {
                    DisplayPanel.primaryColor = new Color(red.getValue(), green.getValue(), blue.getValue());
                    displayFrame.drawUpdates();
                }
        );

        buttonPanel.add(secondaryConfirm = new JButton("Set secondary Color"));
        secondaryConfirm.setMaximumSize(new Dimension(100, 20));
        secondaryConfirm.addActionListener(action -> {
                    DisplayPanel.secondaryColor = new Color(red.getValue(), green.getValue(), blue.getValue());
                    displayFrame.drawUpdates();
                }
        );

        contentPane.add(buttonPanel, "North");
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent evt) {
        redLabel.setText("Red " + red.getValue());
        greenLabel.setText("Green " + green.getValue());
        blueLabel.setText("Blue " + blue.getValue());
        colorPanel.setBackground(new Color(red.getValue(), green.getValue(),
                blue.getValue()));

        colorPanel.repaint();
    }

}
