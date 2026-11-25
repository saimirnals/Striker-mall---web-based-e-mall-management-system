package com.emall;

import com.emall.ui.*;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LookAndFeelHelper.install();
            new LoginFrame().setVisible(true);
        });
    }
}
