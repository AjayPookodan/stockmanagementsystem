package com.mycompany.billingsystem;

import com.mycompany.billingsystem.db.DatabaseManager;
import com.mycompany.billingsystem.ui.BillingAppUI;

import javax.swing.*;

/**
 * The entry point of the Billing System application.
 */
public class App {
    public static void main(String[] args) {
        // Initialize the database: create tables and add sample data if needed.
        DatabaseManager.initializeDatabase();

        // Run the Swing GUI on the Event Dispatch Thread for thread safety.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new BillingAppUI().setVisible(true);
            }
        });
    }
}
