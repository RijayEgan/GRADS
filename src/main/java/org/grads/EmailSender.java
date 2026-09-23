package org.grads;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

public class EmailSender {

    public static EmailReport sendEmails(String outputFolderPath, DefaultTableModel tableModel, JFrame parentFrame) {
        EmailReport report = new EmailReport();

        String[] creds = ConfigManager.loadCredentials();
        if (creds == null || creds.length < 2) {
            JOptionPane.showMessageDialog(parentFrame,
                "Please configure sender email and password via Configure Email.",
                "Missing Credentials", JOptionPane.ERROR_MESSAGE);
            return report;
        }

        final String senderEmail = creds[0];
        final String senderPassword = creds[1];

        String emailTemplate = ConfigManager.loadEmailTemplate();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        File outputFolder = new File(outputFolderPath);
        File[] allPdfs = outputFolder.listFiles((dir, name) -> name.endsWith(".pdf"));

        int nameColIndex = findColumnIndex(tableModel, "Name");
        int idColIndex = findColumnIndex(tableModel, "ID");
        int emailColIndex = findColumnIndex(tableModel, "Email");

        if (nameColIndex == -1 || idColIndex == -1 || emailColIndex == -1) {
            JOptionPane.showMessageDialog(parentFrame,
                "Could not find required columns (Name, ID, Email).\n" +
                "Make sure your table has these column headers.",
                "Column Error", JOptionPane.ERROR_MESSAGE);
            return report;
        }

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String studentName = ((String) tableModel.getValueAt(i, nameColIndex)).trim();
            String studentId = ((String) tableModel.getValueAt(i, idColIndex)).trim();
            String email = ((String) tableModel.getValueAt(i, emailColIndex)).trim();

            File matchedPdf = findPdfForStudent(allPdfs, studentName, studentId);

            if (matchedPdf != null) {
                try {
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(senderEmail));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                    message.setSubject("Your Graded Assignment");

                    String personalizedMessage = emailTemplate
                        .replace("{NAME}", studentName)
                        .replace("{ID}", studentId);

                    MimeBodyPart messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setText(personalizedMessage);

                    Multipart multipart = new MimeMultipart();
                    multipart.addBodyPart(messageBodyPart);

                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.attachFile(matchedPdf);
                    multipart.addBodyPart(attachmentPart);

                    message.setContent(multipart);

                    Transport.send(message);
                    report.successMessages.add("Email sent to: " + email +
                        " (" + matchedPdf.getName() + ")");

                } catch (Exception e) {
                    report.errorMessages.add("Failed to send email to " + email +
                        ": " + e.getMessage());
                }
            } else {
                report.skippedMessages.add(
                    "No PDF found for " + studentName + " (ID: " + studentId + ")");
                System.out.println("Looking in: " + outputFolderPath);
                System.out.println("Available files:");
                if (allPdfs != null) {
                    for (File f : allPdfs) {
                        System.out.println("  " + f.getName());
                    }
                }
            }
        }

        return report;
    }

    private static int findColumnIndex(DefaultTableModel model, String columnName) {
        for (int i = 0; i < model.getColumnCount(); i++) {
            if (model.getColumnName(i).equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds a PDF matching the pattern: {CleanedName}_{CleanedID}.pdf
     * Example: John_Smith_12345.pdf
     */
    private static File findPdfForStudent(File[] pdfs, String studentName, String studentId) {
        if (pdfs == null || studentName == null || studentId == null) return null;
        
        // Clean the name and ID the same way PDFSplitter does
        String cleanName = studentName.replaceAll("[^a-zA-Z0-9\\s]", "").trim().replaceAll("\\s+", "_");
        String cleanId = studentId.replaceAll("[^a-zA-Z0-9]", "");
        
        String expectedFilename = cleanName + "_" + cleanId + ".pdf";
        
        for (File pdf : pdfs) {
            if (pdf.getName().equals(expectedFilename)) {
                return pdf;
            }
        }
        
        return null;
    }

    public static class EmailReport {
        public final List<String> successMessages = new ArrayList<>();
        public final List<String> errorMessages = new ArrayList<>();
        public final List<String> skippedMessages = new ArrayList<>();
    }
}