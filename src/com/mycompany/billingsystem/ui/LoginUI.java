package com.mycompany.billingsystem.ui;

import com.mycompany.billingsystem.db.DatabaseManager;

import javax.swing.*;
import java.awt.*;

/**
 * Creates the login window for the application.
 * This is the first screen the user interacts with.
 */
public class LoginUI extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginUI() {
        setTitle("User Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window
        setResizable(false);

        // --- Main Panel ---
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Components ---
        JLabel userLabel = new JLabel("Username:");
        usernameField = new JTextField(20);

        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField(20);

        JButton loginButton = new JButton("Login");

        // --- Layout ---
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(userLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(passwordLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2; // Span both columns
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(loginButton, gbc);
        
        // Add panel to frame
        add(panel);

        // --- Action Listeners ---
        loginButton.addActionListener(e -> performLogin());
        // Also allow login by pressing Enter in the password field
        passwordField.addActionListener(e -> performLogin());

        setVisible(true);
    }

    /**
     * Handles the login logic when the "Login" button is clicked.
     */
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Login Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Verify credentials against the database
        String role = DatabaseManager.verifyUser(username, password);

        if (role != null) {
            // Login successful
            JOptionPane.showMessageDialog(this, "Login successful. Welcome!", "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // Open the main application window with the user's role
            new BillingAppUI(role);
            
            // Close the login window
            this.dispose(); 
        } else {
            // Login failed
            JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}


