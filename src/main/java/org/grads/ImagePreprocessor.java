package org.grads;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class ImagePreprocessor {
    public static BufferedImage enhanceForOCR(BufferedImage original) {
        // Simple scaling for better readability
        BufferedImage scaled = new BufferedImage(
            original.getWidth() * 2, 
            original.getHeight() * 2, 
            BufferedImage.TYPE_BYTE_GRAY);
        
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                         RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
        g.dispose();
        
        return scaled;
    }
}