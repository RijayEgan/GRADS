package org.grads;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "grads_config.properties";
    private static final String DEFAULT_EMAIL_TEMPLATE = 
        "Hello {NAME},\n\n" +
        "Please find attached your graded assignment.\n\n" +
        "Best,\nYour Teacher";

    public static void saveCredentials(String email, String password) {
        Properties props = new Properties();
        props.setProperty("email", email);
        props.setProperty("password", password);
        
        // Preserve existing template if it exists
        String existingTemplate = loadEmailTemplate();
        if (existingTemplate != null) {
            props.setProperty("emailTemplate", existingTemplate);
        }
        
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "GRADS Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String[] loadCredentials() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            String email = props.getProperty("email");
            String password = props.getProperty("password");
            if (email != null && password != null) {
                return new String[]{email, password};
            }
        } catch (IOException e) {
            // File doesn't exist or can't be read
        }
        return null;
    }

    public static void saveEmailTemplate(String template) {
        Properties props = new Properties();
        
        // Load existing credentials if they exist
        String[] creds = loadCredentials();
        if (creds != null) {
            props.setProperty("email", creds[0]);
            props.setProperty("password", creds[1]);
        }
        
        props.setProperty("emailTemplate", template);
        
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "GRADS Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadEmailTemplate() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            String template = props.getProperty("emailTemplate");
            if (template != null) {
                return template;
            }
        } catch (IOException e) {
            // File doesn't exist or can't be read
        }
        return DEFAULT_EMAIL_TEMPLATE;
    }
}