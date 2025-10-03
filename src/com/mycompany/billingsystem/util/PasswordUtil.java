package com.mycompany.billingsystem.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * A utility class for handling password hashing and verification.
 * It uses a strong, salt-based hashing algorithm (PBKDF2) to securely store passwords.
 */
public class PasswordUtil {

    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * Generates a random salt.
     * @return A byte array containing the salt.
     */
    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Hashes a plain-text password using a given salt.
     * @param password The plain-text password.
     * @param salt The salt to use for hashing.
     * @return The hashed password as a byte array.
     */
    private static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssertionError("Error while hashing password: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Hashes a plain-text password and combines it with a new salt.
     * The result is a single string that can be stored in the database.
     *
     * @param plainPassword The password to hash.
     * @return A salted and hashed password string in the format "salt:hash".
     */
    public static String hashPassword(String plainPassword) {
        byte[] salt = generateSalt();
        byte[] hashedPassword = hash(plainPassword.toCharArray(), salt);
        // Store salt and hash together, encoded in Base64
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hashedPassword);
    }

    /**
     * Verifies a plain-text password against a stored salted hash.
     *
     * @param plainPassword The password to verify.
     * @param storedPassword The salted and hashed password string from the database.
     * @return True if the password is correct, false otherwise.
     */
    public static boolean verifyPassword(String plainPassword, String storedPassword) {
        try {
            // Split the stored password into salt and hash
            String[] parts = storedPassword.split(":");
            if (parts.length != 2) {
                // Invalid stored password format
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] storedHash = Base64.getDecoder().decode(parts[1]);

            // Hash the new password attempt with the original salt
            byte[] newHash = hash(plainPassword.toCharArray(), salt);

            // Compare the two hashes
            if (newHash.length != storedHash.length) {
                return false;
            }
            for (int i = 0; i < newHash.length; i++) {
                if (newHash[i] != storedHash[i]) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            // If any error occurs during decoding or comparison, fail safely
            System.err.println("Error during password verification: " + e.getMessage());
            return false;
        }
    }
}


