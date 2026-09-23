package org.grads;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class PDFSplitter {
    
    private static final double HEADER_DETECTION_AREA = 0.40;
    private static final int OCR_DPI = 400;
    private static final boolean SAVE_DEBUG_IMAGES = true;
    
    private static final String[] TESSERACT_PATHS = {
        System.getProperty("user.dir") + "/tessdata",
        "/opt/homebrew/share/tessdata",
        "/opt/homebrew/Cellar/tesseract/5.5.0_1/share/tessdata",
        "/usr/local/share/tessdata",
        "/usr/share/tesseract-ocr/4.00/tessdata",
        System.getProperty("user.home") + "/tessdata"
    };

    public static void processPDF(String inputPdfPath, DefaultTableModel studentTableModel, 
                                JFrame parentFrame, String orientationMode) {
        PDDocument originalDocument = null;
        List<PDDocument> studentDocs = new ArrayList<>();
        
        try {
            File inputFile = new File(inputPdfPath);
            String outputFolderName = inputFile.getName().replace(".pdf", "") + "_split";
            Path outputFolderPath = Paths.get(outputFolderName);
            
            if (Files.exists(outputFolderPath)) {
                deleteDirectory(outputFolderPath.toFile());
            }
            Files.createDirectories(outputFolderPath);
            
            Path debugFolderPath = null;
            if (SAVE_DEBUG_IMAGES) {
                debugFolderPath = Paths.get(outputFolderName + "_debug");
                if (Files.exists(debugFolderPath)) {
                    deleteDirectory(debugFolderPath.toFile());
                }
                Files.createDirectories(debugFolderPath);
            }

            originalDocument = Loader.loadPDF(inputFile);
            PDFRenderer renderer = new PDFRenderer(originalDocument);
            Tesseract tesseract = initializeTesseract(parentFrame);

            List<List<Integer>> assignmentPageIndices = new ArrayList<>();
            List<Integer> currentAssignmentPages = new ArrayList<>();
            List<Integer> firstPageIndices = new ArrayList<>();
            boolean firstPage = true;

            System.out.println("\n╔════════════════════════════════════════════════════════╗");
            System.out.println("║          QUIZ SCAN PROCESSING (FIXED)                 ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");
            System.out.println("Input: " + inputPdfPath);
            System.out.println("Total pages: " + originalDocument.getNumberOfPages());
            System.out.println("Expected students: " + studentTableModel.getRowCount());
            System.out.println();
            
            for (int i = 0; i < originalDocument.getNumberOfPages(); i++) {
                BufferedImage image = null;
                try {
                    image = renderer.renderImageWithDPI(i, OCR_DPI);
                } catch (IOException e) {
                    System.err.println("Warning: Could not render page " + (i+1));
                    currentAssignmentPages.add(i);
                    continue;
                }
                
                image = rotateImageByOrientation(image, orientationMode);
                BufferedImage topSection = getTopSection(image);
                BufferedImage processedImage = simplePreprocess(topSection);
                
                if (SAVE_DEBUG_IMAGES && debugFolderPath != null) {
                    try {
                        ImageIO.write(topSection, "png", 
                            new File(debugFolderPath.toFile(), "page_" + (i+1) + "_original.png"));
                        ImageIO.write(processedImage, "png", 
                            new File(debugFolderPath.toFile(), "page_" + (i+1) + "_processed.png"));
                    } catch (IOException e) {
                        // Ignore
                    }
                }
                
                String ocrText = "";
                try {
                    ocrText = tesseract.doOCR(processedImage);
                    if (ocrText != null) {
                        ocrText = ocrText.toLowerCase();
                    } else {
                        ocrText = "";
                    }
                } catch (TesseractException e) {
                    System.err.println("Warning: OCR failed on page " + (i+1));
                    ocrText = "";
                }
                
                boolean isNewAssignment = containsStudentIdHeader(ocrText);
                
                System.out.print("Page " + (i + 1) + ": ");
                if (ocrText.length() > 0) {
                    String preview = ocrText.substring(0, Math.min(100, ocrText.length()))
                        .replace("\n", " ").trim();
                    System.out.println(preview + "...");
                } else {
                    System.out.println("(empty OCR)");
                }
                System.out.println("  " + (isNewAssignment ? "✓ NEW ASSIGNMENT" : "  continuation"));
                
                if (isNewAssignment && !firstPage) {
                    if (currentAssignmentPages.size() > 0) {
                        assignmentPageIndices.add(new ArrayList<>(currentAssignmentPages));
                        System.out.println("  → Assignment " + assignmentPageIndices.size() + 
                            " (" + currentAssignmentPages.size() + " pages)\n");
                        currentAssignmentPages = new ArrayList<>();
                    }
                }
                
                if (isNewAssignment) {
                    firstPageIndices.add(i);
                }
                
                currentAssignmentPages.add(i);
                firstPage = false;
            }

            if (currentAssignmentPages.size() > 0) {
                assignmentPageIndices.add(currentAssignmentPages);
                System.out.println("  → Final assignment (" + currentAssignmentPages.size() + " pages)\n");
            }

            System.out.println("=== SUMMARY ===");
            System.out.println("Assignments found: " + assignmentPageIndices.size());
            System.out.println("Students expected: " + studentTableModel.getRowCount());
            System.out.println();

            if (assignmentPageIndices.size() == 0) {
                originalDocument.close();
                showError(parentFrame, "No Assignments Found", 
                    "Could not detect any 'Student Id:' text.\n\nCheck debug folder: " + debugFolderPath);
                return;
            }

            for (List<Integer> pageIndices : assignmentPageIndices) {
                PDDocument assignmentDoc = new PDDocument();
                
                for (Integer pageIndex : pageIndices) {
                    PDPage originalPage = originalDocument.getPage(pageIndex);
                    PDPage importedPage = assignmentDoc.importPage(originalPage);
                }
                
                studentDocs.add(assignmentDoc);
            }
            
            System.out.println("✓ Created " + studentDocs.size() + " separate documents");

            AssignmentMapping[] mappings = showPreviewAndConfirmDialog(
                inputPdfPath, studentDocs, firstPageIndices, studentTableModel, parentFrame);
            
            if (mappings == null) {
                originalDocument.close();
                for (PDDocument doc : studentDocs) {
                    doc.close();
                }
                JOptionPane.showMessageDialog(parentFrame, "PDF splitting cancelled.", 
                    "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            saveDocumentsWithMappings(studentDocs, mappings, outputFolderPath, parentFrame);
            
            originalDocument.close();
            for (PDDocument doc : studentDocs) {
                doc.close();
            }

            String debugMsg = SAVE_DEBUG_IMAGES ? 
                "\n\nDebug images: " + debugFolderPath : "";
            
            JOptionPane.showMessageDialog(parentFrame, 
                String.format("✓ Successfully split and saved %d assignments!%s", 
                    studentDocs.size(), debugMsg),
                "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (TesseractException e) {
            showError(parentFrame, "OCR Error", 
                "Failed to initialize OCR: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            showError(parentFrame, "File Error", 
                "Failed to process PDF: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError(parentFrame, "Unexpected Error", 
                "An error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (originalDocument != null) {
                    originalDocument.close();
                }
                for (PDDocument doc : studentDocs) {
                    if (doc != null) {
                        doc.close();
                    }
                }
            } catch (IOException e) {
                // Ignore cleanup errors
            }
        }
    }

    private static boolean containsStudentIdHeader(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        String normalized = text.toLowerCase().replaceAll("\\s+", "");
        
        return normalized.contains("studentid") || 
               normalized.contains("student1d") ||
               normalized.contains("studentld") ||
               normalized.contains("studenti") ||
               text.toLowerCase().contains("student id") ||
               text.toLowerCase().contains("student i d");
    }

    private static AssignmentMapping[] showPreviewAndConfirmDialog(
            String pdfPath, 
            List<PDDocument> documents,
            List<Integer> firstPageIndices,
            DefaultTableModel studentTableModel,
            JFrame parentFrame) throws IOException {
        
        AssignmentPreviewDialog dialog = new AssignmentPreviewDialog(
            parentFrame, pdfPath, documents, firstPageIndices, studentTableModel);
        
        dialog.setVisible(true);
        
        return dialog.getConfirmedMappings();
    }

    private static void saveDocumentsWithMappings(
            List<PDDocument> documents,
            AssignmentMapping[] mappings,
            Path outputPath,
            JFrame parentFrame) throws IOException {
        
        System.out.println("\n=== SAVING FILES ===");
        for (int i = 0; i < mappings.length; i++) {
            AssignmentMapping mapping = mappings[i];
            String studentId = mapping.getStudentId();
            String studentName = mapping.getStudentName();
            
            // Clean the name to make it filesystem-safe
            String cleanName = studentName.replaceAll("[^a-zA-Z0-9\\s]", "").trim().replaceAll("\\s+", "_");
            String cleanId = studentId.replaceAll("[^a-zA-Z0-9]", "");
            
            // Format: Name_ID.pdf (e.g., "John_Smith_12345.pdf")
            String filename = cleanName + "_" + cleanId + ".pdf";
            String pdfPath = outputPath + "/" + filename;
            
            PDDocument doc = documents.get(i);
            System.out.println("Saving: " + filename + " (" + doc.getNumberOfPages() + " pages)");
            doc.save(pdfPath);
            System.out.println("✓ Saved: " + filename);
        }
    }

    private static BufferedImage rotateImageByOrientation(BufferedImage image, String orientationMode) {
        if (orientationMode == null || orientationMode.equals("Normal")) {
            return image;
        }
        
        int rotation = 0;
        switch (orientationMode) {
            case "Counter-clockwise (←↑)": rotation = 90; break;
            case "Clockwise (→↓)": rotation = 270; break;
            case "Upside down (↓←)": rotation = 180; break;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        
        BufferedImage rotated = new BufferedImage(
            rotation % 180 == 0 ? width : height,
            rotation % 180 == 0 ? height : width,
            image.getType());
        
        Graphics2D g = rotated.createGraphics();
        g.rotate(Math.toRadians(rotation), width/2.0, height/2.0);
        g.drawImage(image, 0, 0, null);
        g.dispose();
        
        return rotated;
    }

    private static BufferedImage getTopSection(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int cropHeight = (int)(height * HEADER_DETECTION_AREA);
        return image.getSubimage(0, 0, width, cropHeight);
    }

    private static BufferedImage simplePreprocess(BufferedImage input) {
        BufferedImage gray = new BufferedImage(
            input.getWidth(), input.getHeight(), 
            java.awt.image.BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();
        return gray;
    }

    private static Tesseract initializeTesseract(JFrame parentFrame) throws TesseractException {
        Tesseract tesseract = new Tesseract();
        
        boolean foundTessdata = false;
        
        for (String path : TESSERACT_PATHS) {
            File tessdata = new File(path);
            if (tessdata.exists() && tessdata.isDirectory()) {
                File engData = new File(tessdata, "eng.traineddata");
                if (engData.exists()) {
                    tesseract.setDatapath(path);
                    foundTessdata = true;
                    System.out.println("✓ Tessdata: " + path);
                    break;
                }
            }
        }
        
        if (!foundTessdata) {
            throw new TesseractException("Tessdata not found");
        }
        
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(3);
        tesseract.setOcrEngineMode(1);
        
        return tesseract;
    }

    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    private static void showError(JFrame parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
        System.err.println(title + ": " + message);
    }
}