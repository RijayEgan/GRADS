package org.grads;
import javax.swing.SwingUtilities;

public class PDFSplitterApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow main = new MainWindow();
            main.setVisible(true);
        });
    }
}
