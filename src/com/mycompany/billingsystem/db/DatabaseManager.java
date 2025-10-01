package com.mycompany.billingsystem.db;

import com.mycompany.billingsystem.model.Product;
import java.sql.*;
import java.util.List;

/**
 * Manages all database operations for the billing system.
 * Uses SQLite as the database engine.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:inventory.db";

    /**
     * Initializes the database. Creates the products table if it doesn't exist
     * and inserts sample data.
     */
    public static void initializeDatabase() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS products ("
                + " barcode TEXT PRIMARY KEY,"
                + " name TEXT NOT NULL,"
                + " price REAL NOT NULL,"
                + " stock_quantity INTEGER NOT NULL"
                + ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            // Create table
            stmt.execute(createTableSQL);

            // Insert some sample data for demonstration if the table is empty
            if (!stmt.executeQuery("SELECT 1 FROM products LIMIT 1").next()) {
                stmt.execute("INSERT INTO products (barcode, name, price, stock_quantity) VALUES ('123456789012', 'Sample Soda Can', 1.50, 100);");
                stmt.execute("INSERT INTO products (barcode, name, price, stock_quantity) VALUES ('987654321098', 'Sample Chips Bag', 2.75, 50);");
                stmt.execute("INSERT INTO products (barcode, name, price, stock_quantity) VALUES ('555554444433', 'Sample Chocolate Bar', 1.25, 200);");
            }

        } catch (SQLException e) {
            System.out.println("Database Initialization Error: " + e.getMessage());
        }
    }

    /**
     * Finds a product in the database by its barcode.
     * @param barcode The barcode of the product to find.
     * @return A Product object if found, otherwise null.
     */
    public static Product findProductByBarcode(String barcode) {
        String sql = "SELECT * FROM products WHERE barcode = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Product(
                        rs.getString("barcode"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity")
                );
            }
        } catch (SQLException e) {
            System.out.println("Find Product Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates the stock for a list of products in a single transaction.
     * @param products The list of products whose stock needs to be updated.
     * @return true if the update was successful, false otherwise.
     */
    public static boolean updateStock(List<Product> products) {
        String sql = "UPDATE products SET stock_quantity = ? WHERE barcode = ?";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false); // Start transaction

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Product product : products) {
                    pstmt.setInt(1, product.getStockQuantity());
                    pstmt.setString(2, product.getBarcode());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            conn.commit(); // Commit transaction
            return true;

        } catch (SQLException e) {
            System.out.println("Update Stock Error: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException ex) {
                    System.out.println("Rollback Error: " + ex.getMessage());
                }
            }
            return false;
        } finally {
             if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                     System.out.println("Connection Close Error: " + ex.getMessage());
                }
            }
        }
    }
}
