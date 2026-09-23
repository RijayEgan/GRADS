package org.grads;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class MainWindow extends JFrame {
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 700;
    private static final Color PRIMARY_COLOR = new Color(45, 64, 89);
    private static final Color SECONDARY_COLOR = new Color(67, 97, 133);
    private static final Color ACCENT_COLOR = new Color(93, 173, 226);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);

    private JTable studentTable;
    private DefaultTableModel studentTableModel;
    private String currentPdfPath;
    private JLabel pdfStatusLabel;
    private JButton configureEmailButton;
    private JButton editEmailTemplateButton;

    public static void main(String[] args) {
        System.setProperty("org.apache.pdfbox.fontcache", "false");
        System.setProperty("org.apache.pdfbox.rendering.usepurejava", "true");

        SwingUtilities.invokeLater(() -> {
            MainWindow frame = new MainWindow();
            frame.setVisible(true);
        });
    }

    public MainWindow() {
        setupMainWindow();
        initializeComponents();
        setupLayout();
        loadInitialCredentials();
    }

    private void setupMainWindow() {
        setTitle("GRADS - Grading System");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        studentTableModel = new DefaultTableModel(new Object[]{"Name", "ID", "Email"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        studentTable = new JTable(studentTableModel);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.setFont(MAIN_FONT);
        studentTable.setRowHeight(30);
        studentTable.setShowGrid(false);
        studentTable.setIntercellSpacing(new Dimension(0, 0));

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem moveUpItem = new JMenuItem("Move Up");
        JMenuItem moveDownItem = new JMenuItem("Move Down");
        JMenuItem deleteItem = new JMenuItem("Delete");

        moveUpItem.addActionListener(e -> moveStudent(-1));
        moveDownItem.addActionListener(e -> moveStudent(1));
        deleteItem.addActionListener(e -> removeSelectedStudent());

        popupMenu.add(moveUpItem);
        popupMenu.add(moveDownItem);
        popupMenu.addSeparator();
        popupMenu.add(deleteItem);

        studentTable.setComponentPopupMenu(popupMenu);

        pdfStatusLabel = new JLabel("No PDF loaded");
        pdfStatusLabel.setFont(MAIN_FONT);
        pdfStatusLabel.setForeground(TEXT_COLOR);

        configureEmailButton = new JButton("CONFIGURE EMAIL");
        configureEmailButton.addActionListener(e -> showEmailConfigDialog());

        editEmailTemplateButton = new JButton("EDIT EMAIL TEMPLATE");
        editEmailTemplateButton.addActionListener(e -> showEmailTemplateDialog());
    }

    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(PRIMARY_COLOR);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);

        JLabel titleLabel = new JLabel("GRADS", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT_COLOR);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBackground(PRIMARY_COLOR);
        statusPanel.add(pdfStatusLabel);
        headerPanel.add(statusPanel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(studentTable);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEmptyBorder(), "Students",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            MAIN_FONT, TEXT_COLOR));
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        tablePanel.setBackground(PRIMARY_COLOR);
        mainPanel.add(tablePanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        buttonPanel.setBackground(PRIMARY_COLOR);

        buttonPanel.add(createStyledButton("ADD STUDENT", e -> showAddStudentWindow(), ACCENT_COLOR));
        buttonPanel.add(createStyledButton("REMOVE STUDENT", e -> removeSelectedStudent(), ACCENT_COLOR));
        buttonPanel.add(createStyledButton("LOAD PDF", e -> loadPdfFile(), SECONDARY_COLOR));
        buttonPanel.add(createStyledButton("LOAD CSV", e -> loadCsvFile(), SECONDARY_COLOR));
        buttonPanel.add(createStyledButton("SPLIT PDF", e -> splitPdf(), SECONDARY_COLOR));
        buttonPanel.add(createStyledButton("SEND EMAILS", e -> sendEmails(), ACCENT_COLOR));
        buttonPanel.add(configureEmailButton);
        buttonPanel.add(editEmailTemplateButton);

        styleButton(configureEmailButton, SECONDARY_COLOR);
        styleButton(editEmailTemplateButton, SECONDARY_COLOR);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setFont(BUTTON_FONT);
        button.setBackground(bgColor);
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
    }

    private void loadInitialCredentials() {
        String[] credentials = ConfigManager.loadCredentials();
        if (credentials != null) {
            configureEmailButton.setText("EDIT EMAIL CONFIG");
        }
    }

    private void showEmailConfigDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextField emailField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        String[] credentials = ConfigManager.loadCredentials();
        if (credentials != null) {
            emailField.setText(credentials[0]);
            passwordField.setText(credentials[1]);
        }

        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel(""));
        panel.add(new JLabel(""));

        int result = JOptionPane.showConfirmDialog(
            this, panel, "Configure Email Settings",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (!email.isEmpty() && !password.isEmpty()) {
                ConfigManager.saveCredentials(email, password);
                configureEmailButton.setText("EDIT EMAIL CONFIG");
                JOptionPane.showMessageDialog(this, "Email configuration saved successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Both email and password are required!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEmailTemplateDialog() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel instructionLabel = new JLabel("<html><b>Customize your email message:</b><br>" +
            "Use {NAME} for student name, {ID} for student ID</html>");
        instructionLabel.setFont(MAIN_FONT);

        JTextArea templateArea = new JTextArea(10, 40);
        templateArea.setFont(MAIN_FONT);
        templateArea.setLineWrap(true);
        templateArea.setWrapStyleWord(true);

        String currentTemplate = ConfigManager.loadEmailTemplate();
        templateArea.setText(currentTemplate);

        JScrollPane scrollPane = new JScrollPane(templateArea);

        panel.add(instructionLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
            this, panel, "Edit Email Template",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String template = templateArea.getText().trim();
            if (!template.isEmpty()) {
                ConfigManager.saveEmailTemplate(template);
                JOptionPane.showMessageDialog(this, "Email template saved successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Template cannot be empty!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JButton createStyledButton(String text, ActionListener action, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(bgColor);
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.addActionListener(action);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        return button;
    }

    private void moveStudent(int direction) {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) return;

        int newRow = selectedRow + direction;
        if (newRow < 0 || newRow >= studentTableModel.getRowCount()) return;

        Object[] rowData = new Object[]{
            studentTableModel.getValueAt(selectedRow, 0),
            studentTableModel.getValueAt(selectedRow, 1),
            studentTableModel.getValueAt(selectedRow, 2)
        };

        studentTableModel.removeRow(selectedRow);
        studentTableModel.insertRow(newRow, rowData);
        studentTable.setRowSelectionInterval(newRow, newRow);
    }

    private void showAddStudentWindow() {
        AddStudentWindow addWindow = new AddStudentWindow(studentTableModel);
        addWindow.setVisible(true);
    }

    private void removeSelectedStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow != -1) {
            studentTableModel.removeRow(selectedRow);
        }
    }

    private void loadPdfFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentPdfPath = chooser.getSelectedFile().getAbsolutePath();
            pdfStatusLabel.setText("Loaded: " + new File(currentPdfPath).getName());
            JOptionPane.showMessageDialog(this, "PDF loaded successfully!");
        }
    }

    private void loadCsvFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Path filePath = chooser.getSelectedFile().toPath();
                List<String> lines = Files.readAllLines(filePath);
                studentTableModel.setRowCount(0);
                
                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        studentTableModel.addRow(new Object[]{
                            parts[0].trim(),
                            parts[1].trim(),
                            parts[2].trim()
                        });
                    }
                }
                
                JOptionPane.showMessageDialog(this, "CSV loaded successfully!");
                    
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Error loading CSV: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            } catch (SecurityException e) {
                JOptionPane.showMessageDialog(this,
                    "No permission to read file: " + e.getMessage(),
                    "Security Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Unexpected error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void splitPdf() {
        if (currentPdfPath == null) {
            JOptionPane.showMessageDialog(this, "Please load a PDF file first!");
            return;
        }
        if (studentTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Please load student data first!");
            return;
        }
        PDFSplitter.processPDF(currentPdfPath, studentTableModel, this, "Normal");
    }

    public static String getOutputFolderPath(String pdfPath) {
        File inputFile = new File(pdfPath);
        String folderName = inputFile.getName().replace(".pdf", "") + "_split";
        return new File(System.getProperty("user.dir"), folderName).getAbsolutePath();
    }

    private void sendEmails() {
        if (studentTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No student data loaded!");
            return;
        }

        if (currentPdfPath == null) {
            JOptionPane.showMessageDialog(this, "No PDF loaded. Please load and split a PDF first.");
            return;
        }

        String outputFolderPath = getOutputFolderPath(currentPdfPath);
        File outputFolder = new File(outputFolderPath);

        if (!outputFolder.exists()) {
            JOptionPane.showMessageDialog(this,
                "Output folder not found:\n" + outputFolderPath + "\n\nPlease split the PDF first.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File[] pdfFiles = outputFolder.listFiles((dir, name) -> name.endsWith(".pdf"));
        if (pdfFiles == null || pdfFiles.length == 0) {
            JOptionPane.showMessageDialog(this,
                "No PDFs found in output folder:\n" + outputFolderPath + "\n\nPlease split the PDF first.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        EmailSender.EmailReport report = EmailSender.sendEmails(
            outputFolderPath, studentTableModel, this);
        showEmailReport(report);
    }

    private void showEmailReport(EmailSender.EmailReport report) {
        JFrame reportFrame = new JFrame("Email Sending Report");
        reportFrame.setSize(600, 400);
        reportFrame.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(MAIN_FONT);

        StringBuilder reportText = new StringBuilder();
        reportText.append("=== EMAIL SENDING REPORT ===\n\n");
        reportText.append("SUCCESSFUL EMAILS (").append(report.successMessages.size()).append("):\n");
        for (String msg : report.successMessages) {
            reportText.append("✓ ").append(msg).append("\n");
        }
        reportText.append("\nFAILED EMAILS (").append(report.errorMessages.size()).append("):\n");
        for (String msg : report.errorMessages) {
            reportText.append("✗ ").append(msg).append("\n");
        }
        reportText.append("\nSKIPPED EMAILS (").append(report.skippedMessages.size()).append("):\n");
        for (String msg : report.skippedMessages) {
            reportText.append("○ ").append(msg).append("\n");
        }

        reportArea.setText(reportText.toString());
        mainPanel.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> reportFrame.dispose());
        closeButton.setFont(BUTTON_FONT);
        closeButton.setBackground(TEXT_COLOR);
        closeButton.setForeground(ACCENT_COLOR);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(closeButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        reportFrame.setContentPane(mainPanel);
        reportFrame.setVisible(true);
    }
}

class AddStudentWindow extends JFrame {
    private final DefaultTableModel tableModel;
    private JTextField nameField, idField, emailField;

    public AddStudentWindow(DefaultTableModel tableModel) {
        this.tableModel = tableModel;
        setupWindow();
        initializeComponents();
        setupLayout();
    }

    private void setupWindow() {
        setTitle("Add New Student");
        setSize(350, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void initializeComponents() {
        nameField = new JTextField(20);
        idField = new JTextField(20);
        emailField = new JTextField(20);
    }

    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 15));
        formPanel.add(new JLabel("Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("ID:"));
        formPanel.add(idField);
        formPanel.add(new JLabel("Email:"));
        formPanel.add(emailField);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        JButton addButton = new JButton("ADD STUDENT");
        addButton.addActionListener(e -> addStudent());
        mainPanel.add(addButton, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void addStudent() {
        if (nameField.getText().isEmpty() || idField.getText().isEmpty() || emailField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        tableModel.addRow(new Object[]{
            nameField.getText().trim(),
            idField.getText().trim(),
            emailField.getText().trim()
        });
        dispose();
    }
}