package org.grads;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * Enhanced image preprocessing specifically optimized for handwritten text recognition
 */
public class HandwritingEnhancer {
    
    /**
     * Main method to enhance image for handwritten digit recognition
     * Applies multiple preprocessing steps to improve OCR accuracy
     */
    public static BufferedImage enhanceForHandwriting(BufferedImage input) {
        // Step 1: Scale up (larger images = better OCR)
        BufferedImage scaled = scaleImage(input, 2.5);
        
        // Step 2: Convert to grayscale
        BufferedImage gray = toGrayscale(scaled);
        
        // Step 3: Reduce noise with Gaussian blur
        BufferedImage denoised = gaussianBlur(gray, 1.0);
        
        // Step 4: Increase contrast
        BufferedImage contrasted = increaseContrast(denoised, 1.5);
        
        // Step 5: Adaptive thresholding (better than global threshold for handwriting)
        BufferedImage binary = adaptiveThreshold(contrasted, 15, 5);
        
        // Step 6: Dilate to thicken handwriting strokes
        BufferedImage dilated = dilate(binary, 1);
        
        return dilated;
    }
    
    /**
     * Scale image by a given factor using high-quality interpolation
     */
    private static BufferedImage scaleImage(BufferedImage input, double scale) {
        int newWidth = (int)(input.getWidth() * scale);
        int newHeight = (int)(input.getHeight() * scale);
        
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        
        // Use high-quality rendering hints
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.drawImage(input, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        return scaled;
    }
    
    /**
     * Convert color image to grayscale
     */
    private static BufferedImage toGrayscale(BufferedImage input) {
        BufferedImage gray = new BufferedImage(
            input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();
        return gray;
    }
    
    /**
     * Apply Gaussian blur to reduce noise
     */
    private static BufferedImage gaussianBlur(BufferedImage input, double radius) {
        int size = (int) Math.ceil(radius * 2) + 1;
        if (size % 2 == 0) size++; // Ensure odd size
        
        float[] matrix = new float[size * size];
        float sum = 0.0f;
        
        // Generate Gaussian kernel
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int dx = x - size / 2;
                int dy = y - size / 2;
                float value = (float) Math.exp(-(dx * dx + dy * dy) / (2 * radius * radius));
                matrix[y * size + x] = value;
                sum += value;
            }
        }
        
        // Normalize kernel
        for (int i = 0; i < matrix.length; i++) {
            matrix[i] /= sum;
        }
        
        Kernel kernel = new Kernel(size, size, matrix);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(input, null);
    }
    
    /**
     * Increase image contrast
     */
    private static BufferedImage increaseContrast(BufferedImage input, double factor) {
        BufferedImage output = new BufferedImage(
            input.getWidth(), input.getHeight(), input.getType());
        
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                Color c = new Color(input.getRGB(x, y));
                int gray = (int)(0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
                
                // Apply contrast adjustment
                int newGray = (int)(((gray / 255.0 - 0.5) * factor + 0.5) * 255);
                newGray = Math.max(0, Math.min(255, newGray));
                
                Color newColor = new Color(newGray, newGray, newGray);
                output.setRGB(x, y, newColor.getRGB());
            }
        }
        
        return output;
    }
    
    /**
     * Apply adaptive thresholding - works better than global thresholding for varying lighting
     */
    private static BufferedImage adaptiveThreshold(BufferedImage input, int blockSize, int c) {
        BufferedImage output = new BufferedImage(
            input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        
        int halfBlock = blockSize / 2;
        
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                // Calculate local mean in block around this pixel
                int sum = 0;
                int count = 0;
                
                for (int by = Math.max(0, y - halfBlock); by < Math.min(input.getHeight(), y + halfBlock + 1); by++) {
                    for (int bx = Math.max(0, x - halfBlock); bx < Math.min(input.getWidth(), x + halfBlock + 1); bx++) {
                        Color bc = new Color(input.getRGB(bx, by));
                        sum += bc.getRed(); // Grayscale, so R=G=B
                        count++;
                    }
                }
                
                int localMean = sum / count;
                Color pixel = new Color(input.getRGB(x, y));
                int value = pixel.getRed();
                
                // Threshold using local mean minus constant
                int newValue = (value > localMean - c) ? 255 : 0;
                output.setRGB(x, y, new Color(newValue, newValue, newValue).getRGB());
            }
        }
        
        return output;
    }
    
    /**
     * Dilate image to thicken lines (makes handwriting more solid)
     */
    private static BufferedImage dilate(BufferedImage input, int iterations) {
        BufferedImage output = input;
        
        for (int iter = 0; iter < iterations; iter++) {
            BufferedImage temp = new BufferedImage(
                output.getWidth(), output.getHeight(), output.getType());
            
            for (int y = 1; y < output.getHeight() - 1; y++) {
                for (int x = 1; x < output.getWidth() - 1; x++) {
                    // Check 3x3 neighborhood
                    boolean hasBlack = false;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            Color c = new Color(output.getRGB(x + dx, y + dy));
                            if (c.getRed() == 0) { // Black pixel
                                hasBlack = true;
                                break;
                            }
                        }
                        if (hasBlack) break;
                    }
                    
                    int color = hasBlack ? Color.BLACK.getRGB() : Color.WHITE.getRGB();
                    temp.setRGB(x, y, color);
                }
            }
            
            output = temp;
        }
        
        return output;
    }
}