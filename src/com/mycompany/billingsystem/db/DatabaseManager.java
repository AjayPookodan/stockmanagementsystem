package com.mycompany.billingsystem.db;

import com.mycompany.billingsystem.model.Product;
import com.mycompany.billingsystem.model.User;
import com.mycompany.billingsystem.util.PasswordUtil;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages all database operations for the billing system.
 * This version includes the new doesProductNameExist method.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:inventory.db";

    /**
     * Initializes the database, creating all necessary tables if they don't exist.
     * Also creates a default administrator account on first run.
     */
    public static void initializeDatabase() {
        // Use a single connection for all initialization steps
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            conn.setAutoCommit(false); // Start transaction

            // Products Table
            stmt.execute("CREATE TABLE IF NOT EXISTS products ("
                + "barcode TEXT PRIMARY KEY,"
                + "name TEXT NOT NULL UNIQUE,"
                + "price REAL NOT NULL,"
                + "stock_quantity INTEGER NOT NULL,"
                + "tax_slab REAL NOT NULL"
                + ");");

            // Users Table
            stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                + "username TEXT PRIMARY KEY,"
                + "password_hash TEXT NOT NULL,"
                + "role TEXT NOT NULL"
                + ");");
            
            // Bills Table (Parent)
            stmt.execute("CREATE TABLE IF NOT EXISTS bills ("
                + "bill_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "bill_date TEXT NOT NULL,"
                + "total_amount REAL NOT NULL"
                + ");");

            // Bill Items Table (Child, links products to bills)
            stmt.execute("CREATE TABLE IF NOT EXISTS bill_items ("
                + "item_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "bill_id INTEGER NOT NULL,"
                + "product_barcode TEXT NOT NULL,"
                + "quantity INTEGER NOT NULL,"
                + "price_per_item REAL NOT NULL," // Price at the time of sale
                + "FOREIGN KEY(bill_id) REFERENCES bills(bill_id),"
                + "FOREIGN KEY(product_barcode) REFERENCES products(barcode)"
                + ");");

            // Check if the users table is empty to create default admin
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    addUserInternal(conn, "admin", "admin", "administrator");
                    System.out.println("Default administrator account created. Username: 'admin', Password: 'admin'");
                }
            }
            
            conn.commit(); // Commit transaction

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // --- User Management Methods ---

    private static boolean addUserInternal(Connection conn, String username, String password, String role) throws SQLException {
        // This internal method assumes a transaction is already being managed.
        String sql = "INSERT INTO users(username, password_hash, role) VALUES(?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, PasswordUtil.hashPassword(password));
            pstmt.setString(3, role);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Re-throw to be handled by the calling method's transaction management
            throw new SQLException("Error adding user: " + e.getMessage(), e);
        }
    }

    public static boolean addUser(String username, String password, String role) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            return addUserInternal(conn, username, password, role);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    public static String verifyUser(String username, String password) {
        String sql = "SELECT password_hash, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (PasswordUtil.verifyPassword(password, storedHash)) {
                    return rs.getString("role");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error verifying user: " + e.getMessage());
        }
        return null;
    }

    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT username, role FROM users";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(rs.getString("username"), rs.getString("role")));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all users: " + e.getMessage());
        }
        return users;
    }

    public static boolean resetUserPassword(String username, String newPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, PasswordUtil.hashPassword(newPassword));
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error resetting password: " + e.getMessage());
            return false;
        }
    }

    // --- Product and Inventory Methods ---

    /**
     * NEW: Checks if a product with the given name already exists in the database.
     * This is used to warn the user about potential duplicates.
     * @param name The product name to check.
     * @return true if a product with this name exists, false otherwise.
     */
    public static boolean doesProductNameExist(String name) {
        String sql = "SELECT 1 FROM products WHERE name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // If rs.next() is true, a record was found
            }
        } catch (SQLException e) {
            System.err.println("Error checking for product name: " + e.getMessage());
            return false; // Fail safe
        }
    }

    public static boolean addProduct(Product product) {
        String sql = "INSERT INTO products(barcode, name, price, stock_quantity, tax_slab) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.getBarcode());
            pstmt.setString(2, product.getName());
            pstmt.setDouble(3, product.getPrice());
            pstmt.setInt(4, product.getStockQuantity());
            pstmt.setDouble(5, product.getTaxSlab());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding product: " + e.getMessage());
            return false;
        }
    }

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
                    rs.getInt("stock_quantity"),
                    rs.getDouble("tax_slab")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error finding product: " + e.getMessage());
        }
        return null;
    }

    public static List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(new Product(
                    rs.getString("barcode"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getInt("stock_quantity"),
                    rs.getDouble("tax_slab")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all products: " + e.getMessage());
        }
        return products;
    }

    public static boolean updateStock(String barcode, int quantityChange) {
        String sql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE barcode = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantityChange);
            pstmt.setString(2, barcode);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating stock: " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteProductByBarcode(String barcode) {
        String sql = "DELETE FROM products WHERE barcode = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting product: " + e.getMessage());
            return false;
        }
    }

    // --- Sales and Billing Methods ---
    
    public static long saveBill(Map<Product, Integer> billItems, double totalAmount) {
        String billSql = "INSERT INTO bills(bill_date, total_amount) VALUES(?, ?)";
        String itemSql = "INSERT INTO bill_items(bill_id, product_barcode, quantity, price_per_item) VALUES(?, ?, ?, ?)";
        String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE barcode = ?";
        
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(false); // Start transaction

            // 1. Insert into bills table
            long billId;
            try (PreparedStatement billPstmt = conn.prepareStatement(billSql, Statement.RETURN_GENERATED_KEYS)) {
                billPstmt.setString(1, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
                billPstmt.setDouble(2, totalAmount);
                billPstmt.executeUpdate();

                try (ResultSet generatedKeys = billPstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        billId = generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating bill failed, no ID obtained.");
                    }
                }
            }

            // 2. Insert into bill_items and 3. Update stock for each item
            try (PreparedStatement itemPstmt = conn.prepareStatement(itemSql);
                 PreparedStatement stockPstmt = conn.prepareStatement(updateStockSql)) {

                for (Map.Entry<Product, Integer> entry : billItems.entrySet()) {
                    Product product = entry.getKey();
                    int quantity = entry.getValue();

                    // Insert item into bill_items
                    itemPstmt.setLong(1, billId);
                    itemPstmt.setString(2, product.getBarcode());
                    itemPstmt.setInt(3, quantity);
                    itemPstmt.setDouble(4, product.getPrice());
                    itemPstmt.addBatch();

                    // Update stock in products
                    stockPstmt.setInt(1, quantity);
                    stockPstmt.setString(2, product.getBarcode());
                    stockPstmt.addBatch();
                }
                itemPstmt.executeBatch();
                stockPstmt.executeBatch();
            }

            conn.commit(); // Commit the transaction
            return billId;

        } catch (SQLException e) {
            System.err.println("Transaction failed. Rolling back changes. Error: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error during rollback: " + ex.getMessage());
                }
            }
            return -1;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("Error closing connection: " + ex.getMessage());
                }
            }
        }
    }


    public static List<Object[]> getSalesHistory(String filter) {
        List<Object[]> history = new ArrayList<>();
        String sql = "SELECT bill_id, bill_date, total_amount FROM bills ";

        LocalDate now = LocalDate.now();
        String startDate = "";
        String endDate = now.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 23:59:59";

        switch (filter) {
            case "Today":
                startDate = now.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00";
                break;
            case "This Week":
                startDate = now.with(java.time.DayOfWeek.MONDAY).format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00";
                break;
            case "This Month":
                startDate = now.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00";
                break;
        }
        sql += "WHERE bill_date BETWEEN ? AND ? ORDER BY bill_date DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                history.add(new Object[]{
                    rs.getLong("bill_id"),
                    rs.getString("bill_date"),
                    rs.getDouble("total_amount")
                });
            }
        } catch (SQLException e) {
            System.err.println("Error fetching sales history: " + e.getMessage());
        }
        return history;
    }
    
    public static List<Object[]> getBillDetails(long billId) {
        List<Object[]> items = new ArrayList<>();
        String sql = "SELECT p.name, bi.quantity, bi.price_per_item, (bi.quantity * bi.price_per_item) AS total " +
                     "FROM bill_items bi " +
                     "JOIN products p ON bi.product_barcode = p.barcode " +
                     "WHERE bi.bill_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, billId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                items.add(new Object[] {
                    rs.getString("name"),
                    rs.getInt("quantity"),
                    rs.getDouble("price_per_item"),
                    rs.getDouble("total")
                });
            }
        } catch (SQLException e) {
             System.err.println("Error fetching bill details: " + e.getMessage());
        }
        return items;
    }
}

