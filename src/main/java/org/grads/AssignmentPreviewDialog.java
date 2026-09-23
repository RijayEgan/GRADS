package org.grads;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Preview dialog with manual Student ID input for training data collection
 */
public class AssignmentPreviewDialog extends JDialog {
    private static final Color PRIMARY_COLOR = new Color(45, 64, 89);
    private static final Color ACCENT_COLOR = new Color(93, 173, 226);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 14);
    
    private final String pdfPath;
    private final List<PDDocument> documents;
    private final List<Integer> firstPageIndices;
    private final DefaultTableModel studentTableModel;
    private final String debugFolderPath;
    
    private int currentAssignmentIndex = 0;
    private AssignmentMapping[] mappings;
    private String[] detectedStudentIds; // Store manually entered IDs
    private boolean confirmed = false;
    
    private JLabel assignmentNumberLabel;
    private JLabel studentInfoLabel;
    private JLabel pageCountLabel;
    private JPanel previewPanel;
    private JTextField studentIdField;
    private JLabel idStatusLabel;
    private JButton prevButton;
    private JButton nextButton;
    private JButton changeStudentButton;
    private JButton confirmButton;
    private JButton cancelButton;
    
    public AssignmentPreviewDialog(JFrame parent, String pdfPath, 
                                  List<PDDocument> documents,
                                  List<Integer> firstPageIndices,
                                  DefaultTableModel studentTableModel) {
        super(parent, "Review Assignments & Enter Student IDs", true);
        
        this.pdfPath = pdfPath;
        this.documents = documents;
        this.firstPageIndices = firstPageIndices;
        this.studentTableModel = studentTableModel;
        
        // Determine debug folder path
        String basePath = new File(pdfPath).getName().replace(".pdf", "");
        this.debugFolderPath = basePath + "_split_debug";
        
        // Initialize mappings and ID storage
        initializeMappings();
        this.detectedStudentIds = new String[documents.size()];
        
        // Setup UI
        setupUI();
        
        // Load first assignment
        loadAssignment(0);
        
        // Center on screen
        setSize(1000, 750);
        setLocationRelativeTo(parent);
    }
    
    private void initializeMappings() {
        mappings = new AssignmentMapping[documents.size()];
        
        for (int i = 0; i < documents.size(); i++) {
            if (i < studentTableModel.getRowCount()) {
                String name = studentTableModel.getValueAt(i, 0).toString();
                String id = studentTableModel.getValueAt(i, 1).toString();
                String email = studentTableModel.getValueAt(i, 2).toString();
                mappings[i] = new AssignmentMapping(i, name, id, email);
            } else {
                mappings[i] = new AssignmentMapping(i, "Unassigned", "???", "");
            }
        }
    }
    
    private void setupUI() {
        setLayout(new BorderLayout(10, 10));
        
        // Header panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Center panel with preview and ID input
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBorder(new EmptyBorder(0, 15, 0, 15));
        
        // Preview panel
        previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        previewPanel.setBackground(Color.WHITE);
        centerPanel.add(previewPanel, BorderLayout.CENTER);
        
        // ID input panel
        JPanel idInputPanel = createIdInputPanel();
        centerPanel.add(idInputPanel, BorderLayout.SOUTH);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Button panel (bottom)
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 10, 15));
        panel.setBackground(PRIMARY_COLOR);
        
        // Title
        JLabel titleLabel = new JLabel("Review Assignments & Enter Student IDs");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Manual ID entry helps train the OCR system");
        subtitleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        subtitleLabel.setForeground(Color.LIGHT_GRAY);
        
        // Info panel
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.setOpaque(false);
        
        assignmentNumberLabel = new JLabel();
        assignmentNumberLabel.setFont(MAIN_FONT);
        assignmentNumberLabel.setForeground(Color.WHITE);
        
        studentInfoLabel = new JLabel();
        studentInfoLabel.setFont(MAIN_FONT);
        studentInfoLabel.setForeground(ACCENT_COLOR);
        
        pageCountLabel = new JLabel();
        pageCountLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        pageCountLabel.setForeground(Color.LIGHT_GRAY);
        
        infoPanel.add(assignmentNumberLabel);
        infoPanel.add(studentInfoLabel);
        infoPanel.add(pageCountLabel);
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createIdInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR, 2),
            new EmptyBorder(15, 15, 15, 15)
        ));
        panel.setBackground(new Color(240, 248, 255)); // Light blue background
        
        // Left side - instruction
        JPanel instructionPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        instructionPanel.setOpaque(false);
        
        JLabel instructionLabel = new JLabel("Enter the Student ID shown on the PDF:");
        instructionLabel.setFont(LABEL_FONT);
        
        JLabel helpLabel = new JLabel("This helps train the system to recognize handwriting");
        helpLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        helpLabel.setForeground(Color.GRAY);
        
        instructionPanel.add(instructionLabel);
        instructionPanel.add(helpLabel);
        
        // Center - input field
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        inputPanel.setOpaque(false);
        
        studentIdField = new JTextField(15);
        studentIdField.setFont(new Font("Segoe UI", Font.BOLD, 16));
        studentIdField.setPreferredSize(new Dimension(200, 35));
        
        // Add listener to validate and save
        studentIdField.addActionListener(e -> saveStudentId());
        studentIdField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateStatus(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateStatus(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateStatus(); }
        });
        
        JButton saveIdButton = new JButton("✓ Save ID");
        saveIdButton.setFont(MAIN_FONT);
        saveIdButton.setBackground(SUCCESS_COLOR);
        saveIdButton.setForeground(Color.WHITE);
        saveIdButton.addActionListener(e -> saveStudentId());
        
        inputPanel.add(studentIdField);
        inputPanel.add(saveIdButton);
        
        // Right side - status
        idStatusLabel = new JLabel("Not entered");
        idStatusLabel.setFont(MAIN_FONT);
        idStatusLabel.setForeground(Color.GRAY);
        
        panel.add(instructionPanel, BorderLayout.WEST);
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(idStatusLabel, BorderLayout.EAST);
        
        return panel;
    }
    
    private void updateStatus() {
        String text = studentIdField.getText().trim();
        if (text.isEmpty()) {
            idStatusLabel.setText("Not entered");
            idStatusLabel.setForeground(Color.GRAY);
        } else if (text.matches("\\d+")) {
            idStatusLabel.setText("Valid format");
            idStatusLabel.setForeground(SUCCESS_COLOR);
        } else {
            idStatusLabel.setText("Numbers only");
            idStatusLabel.setForeground(Color.RED);
        }
    }
    
    private void saveStudentId() {
        String enteredId = studentIdField.getText().trim();
        
        if (enteredId.isEmpty()) {
            return;
        }
        
        if (!enteredId.matches("\\d+")) {
            JOptionPane.showMessageDialog(this, 
                "Student ID should contain only numbers", 
                "Invalid ID", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Save the entered ID
        detectedStudentIds[currentAssignmentIndex] = enteredId;
        
        // Update status
        idStatusLabel.setText("✓ Saved: " + enteredId);
        idStatusLabel.setForeground(SUCCESS_COLOR);
        
        // Show brief confirmation
        Timer timer = new Timer(2000, e -> updateStatus());
        timer.setRepeats(false);
        timer.start();
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 15, 15, 15));
        
        // Navigation buttons (left)
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        
        prevButton = new JButton("◀ Previous");
        prevButton.setFont(MAIN_FONT);
        prevButton.addActionListener(e -> {
            saveCurrentIdIfEntered();
            previousAssignment();
        });
        
        nextButton = new JButton("Next ▶");
        nextButton.setFont(MAIN_FONT);
        nextButton.addActionListener(e -> {
            saveCurrentIdIfEntered();
            nextAssignment();
        });
        
        navPanel.add(prevButton);
        navPanel.add(nextButton);
        
        // Action buttons (center)
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        changeStudentButton = new JButton("🔄 Change Student");
        changeStudentButton.setFont(MAIN_FONT);
        changeStudentButton.setBackground(WARNING_COLOR);
        changeStudentButton.setForeground(Color.BLACK);
        changeStudentButton.addActionListener(e -> changeStudent());
        
        actionPanel.add(changeStudentButton);
        
        // Confirm/Cancel buttons (right)
        JPanel confirmPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        
        cancelButton = new JButton("✖ Cancel");
        cancelButton.setFont(MAIN_FONT);
        cancelButton.setBackground(new Color(220, 53, 69));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.addActionListener(e -> cancel());
        
        confirmButton = new JButton("✓ Confirm All");
        confirmButton.setFont(MAIN_FONT);
        confirmButton.setBackground(SUCCESS_COLOR);
        confirmButton.setForeground(Color.WHITE);
        confirmButton.addActionListener(e -> {
            saveCurrentIdIfEntered();
            confirmAll();
        });
        
        confirmPanel.add(cancelButton);
        confirmPanel.add(confirmButton);
        
        panel.add(navPanel, BorderLayout.WEST);
        panel.add(actionPanel, BorderLayout.CENTER);
        panel.add(confirmPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private void saveCurrentIdIfEntered() {
        String text = studentIdField.getText().trim();
        if (!text.isEmpty() && text.matches("\\d+")) {
            detectedStudentIds[currentAssignmentIndex] = text;
        }
    }
    
    private void loadAssignment(int index) {
        currentAssignmentIndex = index;
        
        // Update labels
        assignmentNumberLabel.setText(String.format("Assignment %d of %d", 
            index + 1, documents.size()));
        
        AssignmentMapping mapping = mappings[index];
        studentInfoLabel.setText(String.format("Assigned to: %s (ID: %s)", 
            mapping.getStudentName(), mapping.getStudentId()));
        
        int pageCount = documents.get(index).getNumberOfPages();
        pageCountLabel.setText(String.format("%d page%s", pageCount, pageCount == 1 ? "" : "s"));
        
        // Update ID field with previously entered value
        String previouslyEntered = detectedStudentIds[index];
        if (previouslyEntered != null) {
            studentIdField.setText(previouslyEntered);
            idStatusLabel.setText("✓ Saved: " + previouslyEntered);
            idStatusLabel.setForeground(SUCCESS_COLOR);
        } else {
            studentIdField.setText("");
            idStatusLabel.setText("Not entered");
            idStatusLabel.setForeground(Color.GRAY);
        }
        studentIdField.requestFocus();
        
        // Update navigation buttons
        prevButton.setEnabled(index > 0);
        nextButton.setEnabled(index < documents.size() - 1);
        
        // Load PDF preview
        loadPreview(index);
    }
    
    private void loadPreview(int index) {
        previewPanel.removeAll();
        
        try {
            PDDocument doc = documents.get(index);
            PDFRenderer renderer = new PDFRenderer(doc);
            
            // Render first page
            BufferedImage image = renderer.renderImageWithDPI(0, 150);
            
            // Scale to fit
            ImageIcon icon = new ImageIcon(image);
            JLabel imageLabel = new JLabel(icon) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                                       RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    
                    int labelWidth = getWidth();
                    int labelHeight = getHeight();
                    int imageWidth = icon.getIconWidth();
                    int imageHeight = icon.getIconHeight();
                    
                    double scale = Math.min(
                        (double) labelWidth / imageWidth,
                        (double) labelHeight / imageHeight);
                    
                    int scaledWidth = (int) (imageWidth * scale);
                    int scaledHeight = (int) (imageHeight * scale);
                    
                    int x = (labelWidth - scaledWidth) / 2;
                    int y = (labelHeight - scaledHeight) / 2;
                    
                    g2.drawImage(icon.getImage(), x, y, scaledWidth, scaledHeight, this);
                    g2.dispose();
                }
            };
            
            JScrollPane scrollPane = new JScrollPane(imageLabel);
            scrollPane.setBackground(Color.DARK_GRAY);
            previewPanel.add(scrollPane, BorderLayout.CENTER);
            
        } catch (IOException e) {
            JLabel errorLabel = new JLabel("Could not load preview");
            errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            previewPanel.add(errorLabel, BorderLayout.CENTER);
        }
        
        previewPanel.revalidate();
        previewPanel.repaint();
    }
    
    private void previousAssignment() {
        if (currentAssignmentIndex > 0) {
            loadAssignment(currentAssignmentIndex - 1);
        }
    }
    
    private void nextAssignment() {
        if (currentAssignmentIndex < documents.size() - 1) {
            loadAssignment(currentAssignmentIndex + 1);
        }
    }
    
    private void changeStudent() {
        JDialog dialog = new JDialog(this, "Select Student", true);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JLabel promptLabel = new JLabel("Select the correct student for this assignment:");
        promptLabel.setBorder(new EmptyBorder(10, 10, 5, 10));
        promptLabel.setFont(MAIN_FONT);
        
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (int i = 0; i < studentTableModel.getRowCount(); i++) {
            String name = studentTableModel.getValueAt(i, 0).toString();
            String id = studentTableModel.getValueAt(i, 1).toString();
            listModel.addElement(String.format("%s (ID: %s)", name, id));
        }
        
        JList<String> studentList = new JList<>(listModel);
        studentList.setFont(MAIN_FONT);
        studentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentList.setSelectedIndex(currentAssignmentIndex);
        
        JScrollPane scrollPane = new JScrollPane(studentList);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        scrollPane.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        JButton selectButton = new JButton("Select");
        selectButton.setFont(MAIN_FONT);
        selectButton.addActionListener(e -> {
            int selectedIndex = studentList.getSelectedIndex();
            if (selectedIndex >= 0) {
                String name = studentTableModel.getValueAt(selectedIndex, 0).toString();
                String id = studentTableModel.getValueAt(selectedIndex, 1).toString();
                String email = studentTableModel.getValueAt(selectedIndex, 2).toString();
                
                mappings[currentAssignmentIndex].setStudent(name, id, email);
                studentInfoLabel.setText(String.format("Assigned to: %s (ID: %s)", name, id));
                dialog.dispose();
            }
        });
        
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(MAIN_FONT);
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(selectButton);
        
        dialog.add(promptLabel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void confirmAll() {
        // Check how many IDs were entered
        int enteredCount = 0;
        for (String id : detectedStudentIds) {
            if (id != null && !id.isEmpty()) {
                enteredCount++;
            }
        }
        
        String message;
        if (enteredCount == detectedStudentIds.length) {
            message = "All student IDs have been entered!\n\n" +
                     "Save training data and split PDFs?";
        } else {
            message = String.format(
                "You've entered %d out of %d student IDs.\n\n" +
                "Continue without entering all IDs?\n" +
                "(More IDs = better training data)",
                enteredCount, detectedStudentIds.length);
        }
        
        int result = JOptionPane.showConfirmDialog(this,
            message,
            "Confirm All Assignments",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            // Save training data
            saveTrainingData();
            confirmed = true;
            dispose();
        }
    }
    
    private void saveTrainingData() {
        try {
            // Create training_data folder if it doesn't exist
            Path trainingDataPath = Paths.get("training_data");
            if (!Files.exists(trainingDataPath)) {
                Files.createDirectories(trainingDataPath);
            }
            
            // Create a CSV file with the training data
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            Path csvPath = trainingDataPath.resolve("training_" + timestamp + ".csv");
            
            try (FileWriter writer = new FileWriter(csvPath.toFile())) {
                writer.write("assignment_index,expected_id,entered_id,debug_image_path,match\n");
                
                for (int i = 0; i < detectedStudentIds.length; i++) {
                    String enteredId = detectedStudentIds[i];
                    if (enteredId != null && !enteredId.isEmpty()) {
                        String expectedId = mappings[i].getStudentId();
                        boolean match = enteredId.equals(expectedId);
                        
                        // Get first page index for this assignment
                        int pageIndex = (i < firstPageIndices.size()) ? firstPageIndices.get(i) : i;
                        String debugImagePath = debugFolderPath + "/page_" + (pageIndex + 1) + "_original.png";
                        
                        writer.write(String.format("%d,%s,%s,%s,%s\n",
                            i + 1,
                            expectedId,
                            enteredId,
                            debugImagePath,
                            match ? "TRUE" : "FALSE"));
                    }
                }
            }
            
            System.out.println("\n✓ Training data saved to: " + csvPath);
            System.out.println("  - Total entries: " + countEnteredIds());
            System.out.println("  - Debug images: " + debugFolderPath);
            
        } catch (IOException e) {
            System.err.println("Warning: Could not save training data: " + e.getMessage());
        }
    }
    
    private int countEnteredIds() {
        int count = 0;
        for (String id : detectedStudentIds) {
            if (id != null && !id.isEmpty()) {
                count++;
            }
        }
        return count;
    }
    
    private void cancel() {
        int result = JOptionPane.showConfirmDialog(this,
            "Cancel without saving?\nAll entered IDs and splitting will be discarded.",
            "Cancel",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            confirmed = false;
            dispose();
        }
    }
    
    public AssignmentMapping[] getConfirmedMappings() {
        return confirmed ? mappings : null;
    }
}