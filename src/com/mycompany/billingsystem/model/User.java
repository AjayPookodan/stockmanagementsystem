package com.mycompany.billingsystem.model;

/**
 * Represents a user of the application.
 * This is a simple Plain Old Java Object (POJO) that holds user information.
 */
public class User {
    private String username;
    private String role;

    public User(String username, String role) {
        this.username = username;
        this.role = role;
    }

    // --- Getters ---
    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}


