package com.mycompany.billingsystem;

import com.mycompany.billingsystem.db.DatabaseManager;
import com.mycompany.billingsystem.ui.LoginUI;
import javax.swing.SwingUtilities;

/**
 * The main entry point for the Stock and Billing System application.
 * This class is responsible for initializing the database and launching the login screen.
 */
public class App {

    /**
     * The main method that kicks off the entire application.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // Swing applications should be run on the Event Dispatch Thread (EDT) for thread safety.
        SwingUtilities.invokeLater(() -> {
            // Step 1: Initialize the database. This creates the .db file and tables if they don't exist.
            DatabaseManager.initializeDatabase();
            
            // Step 2: Create and show the login user interface.
            // The application flow starts from the login screen.
            new LoginUI();
        });
    }
}

