package com.mycompany.billingsystem.ui;

import com.mycompany.billingsystem.db.DatabaseManager;
import com.mycompany.billingsystem.model.Product;
import com.mycompany.billingsystem.model.User;
import com.mycompany.billingsystem.util.PdfGenerator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * The main UI for the billing system.
 * This version includes an Admin Dashboard, discount feature, and has been
 * refactored for robustness, performance, and security using SwingWorker.
 */
public class BillingAppUI extends JFrame {

    // --- Component Declarations ---
    private JTextField barcodeField;
    private DefaultTableModel billTableModel;
    private JTable billTable;
    private JLabel totalAmountLabel;
    private final Map<Product, Integer> billItems = new HashMap<>();
    private final List<Product> productsInBillTable = new ArrayList<>();
    private JTextField discountField;
    private JComboBox<String> discountTypeComboBox;

    private JTable inventoryTable;
    private DefaultTableModel inventoryTableModel;
    private JTextField newBarcodeField, newNameField, newMrpField, newTaxSlabField, newStockField;
    private JTextField updateBarcodeField, updateQuantityField;
    private JTextField deleteBarcodeField;

    private JTable salesHistoryTable;
    private DefaultTableModel salesHistoryTableModel;
    private JComboBox<String> salesFilterComboBox;

    private JTable usersTable;
    private DefaultTableModel usersTableModel;
    private JTextField newStaffUsernameField;
    private JPasswordField newStaffPasswordField;
    private JComboBox<String> userSelectionComboBox;
    private JPasswordField resetPasswordField;

    private JLabel todaysSalesValueLabel, todaysBillsValueLabel;
    private DefaultTableModel lowStockTableModel;
    private DefaultTableModel topSellingTableModel;

    private final String currentUserRole;

    public BillingAppUI(String userRole) {
        this.currentUserRole = userRole;
        setTitle("Stock and Billing System - Role: " + userRole.toUpperCase());
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        // --- Tab Setup Based on Role ---
        if ("administrator".equals(currentUserRole)) {
            tabbedPane.addTab("Dashboard", null, createDashboardPanel(), "Business overview");
        }
        tabbedPane.addTab("Billing", null, createBillingPanel(), "Point of Sale");

        if ("administrator".equals(currentUserRole)) {
            tabbedPane.addTab("Manage Products", null, createFullManageProductsPanel(), "Full inventory control");
            tabbedPane.addTab("Sales History", null, createSalesHistoryPanel(), "View past bills");
            tabbedPane.addTab("Manage Users", null, createManageUsersPanel(), "Add and view users");
        } else if ("staff".equals(currentUserRole)) {
            tabbedPane.addTab("Add Product", null, createAddOnlyProductsPanel(), "Add new products to inventory");
        }
        
        tabbedPane.addChangeListener(e -> {
            String selectedTitle = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
            switch (selectedTitle) {
                case "Dashboard": refreshDashboard(); break;
                case "Manage Products": refreshInventoryTable(); break;
                case "Sales History": refreshSalesHistoryTable(); break;
                case "Manage Users": refreshUsersTable(); break;
            }
        });
        
        add(tabbedPane);
        setVisible(true);

        // Initial load for the first visible tab
        if ("administrator".equals(currentUserRole)) {
            refreshDashboard();
        }
    }
    
    // --- Panel Creation Methods ---
    
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel metricsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        metricsPanel.add(createMetricCard("Today's Sales", "₹ 0.00", todaysSalesValueLabel = new JLabel()));
        metricsPanel.add(createMetricCard("Today's Bills", "0", todaysBillsValueLabel = new JLabel()));
        panel.add(metricsPanel, BorderLayout.NORTH);

        JPanel listsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        
        String[] lowStockColumns = {"Product Name", "Stock Left"};
        lowStockTableModel = new DefaultTableModel(lowStockColumns, 0);
        JTable lowStockTable = new JTable(lowStockTableModel);
        JPanel lowStockPanel = new JPanel(new BorderLayout());
        lowStockPanel.setBorder(BorderFactory.createTitledBorder("Low Stock Alerts (<= 10 items)"));
        lowStockPanel.add(new JScrollPane(lowStockTable), BorderLayout.CENTER);
        
        String[] topSellingColumns = {"Product Name", "Units Sold (Month)"};
        topSellingTableModel = new DefaultTableModel(topSellingColumns, 0);
        JTable topSellingTable = new JTable(topSellingTableModel);
        JPanel topSellingPanel = new JPanel(new BorderLayout());
        topSellingPanel.setBorder(BorderFactory.createTitledBorder("Top Selling Products (This Month)"));
        topSellingPanel.add(new JScrollPane(topSellingTable), BorderLayout.CENTER);

        listsPanel.add(lowStockPanel);
        listsPanel.add(topSellingPanel);
        panel.add(listsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMetricCard(String title, String initialValue, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        valueLabel.setText(initialValue);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 28));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createBillingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Scan Barcode:"));
        barcodeField = new JTextField(20);
        topPanel.add(barcodeField);
        JButton removeItemButton = new JButton("Remove Selected Item");
        topPanel.add(removeItemButton);
        panel.add(topPanel, BorderLayout.NORTH);

        String[] billColumns = {"Name", "MRP", "Tax Slab", "Quantity", "Total"};
        billTableModel = new DefaultTableModel(billColumns, 0) {
             @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        billTable = new JTable(billTableModel);
        panel.add(new JScrollPane(billTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        totalAmountLabel = new JLabel("Total: ₹ 0.00");
        totalAmountLabel.setFont(new Font("Arial", Font.BOLD, 20));
        
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlsPanel.add(new JLabel("Discount:"));
        discountField = new JTextField(5);
        controlsPanel.add(discountField);
        discountTypeComboBox = new JComboBox<>(new String[]{"₹", "%"});
        controlsPanel.add(discountTypeComboBox);
        JButton finalizeButton = new JButton("Finalize Bill");
        controlsPanel.add(finalizeButton);

        bottomPanel.add(totalAmountLabel, BorderLayout.WEST);
        bottomPanel.add(controlsPanel, BorderLayout.EAST);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        barcodeField.addActionListener(e -> addProductToBill());
        removeItemButton.addActionListener(e -> removeSelectedItemFromBill());
        finalizeButton.addActionListener(e -> finalizeBill());
        
        DocumentListener discountListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateTotalAmount(); }
            public void removeUpdate(DocumentEvent e) { updateTotalAmount(); }
            public void insertUpdate(DocumentEvent e) { updateTotalAmount(); }
        };
        discountField.getDocument().addDocumentListener(discountListener);
        discountTypeComboBox.addActionListener(e -> updateTotalAmount());

        return panel;
    }

    private JPanel createFullManageProductsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel formsPanel = new JPanel();
        formsPanel.setLayout(new BoxLayout(formsPanel, BoxLayout.Y_AXIS));
        formsPanel.add(createAddNewProductPanel());
        formsPanel.add(createUpdateStockPanel());
        formsPanel.add(createDeleteProductPanel());
        mainPanel.add(formsPanel, BorderLayout.NORTH);
        
        JPanel inventoryPanel = new JPanel(new BorderLayout());
        inventoryPanel.setBorder(BorderFactory.createTitledBorder("Full Stock Inventory"));
        String[] inventoryColumns = {"Barcode", "Name", "MRP", "Stock", "Tax Slab (%)"};
        inventoryTableModel = new DefaultTableModel(inventoryColumns, 0) {
             @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        inventoryTable = new JTable(inventoryTableModel);
        inventoryPanel.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);
        JButton refreshInventoryButton = new JButton("Refresh Inventory");
        inventoryPanel.add(refreshInventoryButton, BorderLayout.SOUTH);
        mainPanel.add(inventoryPanel, BorderLayout.CENTER);

        refreshInventoryButton.addActionListener(e -> refreshInventoryTable());

        return mainPanel;
    }
    
    private JPanel createAddNewProductPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Add New Product"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Barcode (Optional):"), gbc);
        gbc.gridx = 1; newBarcodeField = new JTextField(15); panel.add(newBarcodeField, gbc);
        gbc.gridx = 2; panel.add(new JLabel("Product Name:"), gbc);
        gbc.gridx = 3; newNameField = new JTextField(20); panel.add(newNameField, gbc);
        gbc.gridy = 1; gbc.gridx = 0; panel.add(new JLabel("MRP (₹):"), gbc);
        gbc.gridx = 1; newMrpField = new JTextField(10); panel.add(newMrpField, gbc);
        gbc.gridx = 2; panel.add(new JLabel("Tax Slab (%):"), gbc);
        gbc.gridx = 3; newTaxSlabField = new JTextField(10); panel.add(newTaxSlabField, gbc);
        gbc.gridy = 2; gbc.gridx = 0; panel.add(new JLabel("Initial Stock:"), gbc);
        gbc.gridx = 1; newStockField = new JTextField(10); panel.add(newStockField, gbc);
        
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.NONE;
        JButton addProductButton = new JButton("Add Product");
        panel.add(addProductButton, gbc);
        addProductButton.addActionListener(e -> addNewProduct());
        
        return panel;
    }
    
    private JPanel createUpdateStockPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Update Stock Quantity"));
        panel.add(new JLabel("Product Barcode:"));
        updateBarcodeField = new JTextField(15);
        panel.add(updateBarcodeField);
        panel.add(new JLabel("Qty to Add/Remove (+/-):"));
        updateQuantityField = new JTextField(5);
        panel.add(updateQuantityField);
        JButton updateStockButton = new JButton("Update Stock");
        panel.add(updateStockButton);
        updateStockButton.addActionListener(e -> updateExistingStock());
        return panel;
    }
    
    private JPanel createDeleteProductPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Delete Product Permanently"));
        panel.add(new JLabel("Product Barcode:"));
        deleteBarcodeField = new JTextField(15);
        panel.add(deleteBarcodeField);
        JButton deleteProductButton = new JButton("Delete Product");
        panel.add(deleteProductButton);
        deleteProductButton.addActionListener(e -> deleteProduct());
        return panel;
    }

    private JPanel createAddOnlyProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(createAddNewProductPanel(), BorderLayout.NORTH);
        return panel;
    }
    
    private JPanel createSalesHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Filter by:"));
        String[] filters = {"Today", "This Week", "This Month"};
        salesFilterComboBox = new JComboBox<>(filters);
        topPanel.add(salesFilterComboBox);
        JButton refreshButton = new JButton("Refresh");
        topPanel.add(refreshButton);
        panel.add(topPanel, BorderLayout.NORTH);

        String[] historyColumns = {"Bill ID", "Date", "Total Amount (₹)"};
        salesHistoryTableModel = new DefaultTableModel(historyColumns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        salesHistoryTable = new JTable(salesHistoryTableModel);
        salesHistoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(salesHistoryTable), BorderLayout.CENTER);

        refreshButton.addActionListener(e -> refreshSalesHistoryTable());
        salesFilterComboBox.addActionListener(e -> refreshSalesHistoryTable());
        
        salesHistoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && salesHistoryTable.getSelectedRow() != -1) {
                showSelectedBillDetails();
            }
        });
        return panel;
    }

    private JPanel createManageUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    
        JPanel formsContainer = new JPanel();
        formsContainer.setLayout(new BoxLayout(formsContainer, BoxLayout.Y_AXIS));
    
        JPanel addStaffPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addStaffPanel.setBorder(BorderFactory.createTitledBorder("Add New Staff Member"));
        addStaffPanel.add(new JLabel("Username:"));
        newStaffUsernameField = new JTextField(15);
        addStaffPanel.add(newStaffUsernameField);
        addStaffPanel.add(new JLabel("Password:"));
        newStaffPasswordField = new JPasswordField(15);
        addStaffPanel.add(newStaffPasswordField);
        JButton addStaffButton = new JButton("Add Staff");
        addStaffPanel.add(addStaffButton);
        addStaffButton.addActionListener(e -> addStaffMember());
    
        JPanel resetPasswordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        resetPasswordPanel.setBorder(BorderFactory.createTitledBorder("Reset User Password"));
        resetPasswordPanel.add(new JLabel("Select User:"));
        userSelectionComboBox = new JComboBox<>();
        userSelectionComboBox.setPreferredSize(new Dimension(150, 25));
        resetPasswordPanel.add(userSelectionComboBox);
        resetPasswordPanel.add(new JLabel("New Password:"));
        resetPasswordField = new JPasswordField(15);
        resetPasswordPanel.add(resetPasswordField);
        JButton resetPasswordButton = new JButton("Reset Password");
        resetPasswordPanel.add(resetPasswordButton);
        resetPasswordButton.addActionListener(e -> resetUserPassword());
    
        formsContainer.add(addStaffPanel);
        formsContainer.add(resetPasswordPanel);
    
        panel.add(formsContainer, BorderLayout.NORTH);
    
        JPanel userListPanel = new JPanel(new BorderLayout());
        userListPanel.setBorder(BorderFactory.createTitledBorder("Current Users"));
        String[] userColumns = {"Username", "Role"};
        usersTableModel = new DefaultTableModel(userColumns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        usersTable = new JTable(usersTableModel);
        userListPanel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
    
        panel.add(userListPanel, BorderLayout.CENTER);
        
        return panel;
    }

    // --- Action Handler Methods ---

    private void addProductToBill() {
        String barcode = barcodeField.getText().trim();
        if (barcode.isEmpty()) return;

        new SwingWorker<Product, Void>() {
            @Override
            protected Product doInBackground() {
                return DatabaseManager.findProductByBarcode(barcode);
            }

            @Override
            protected void done() {
                try {
                    Product product = get();
                    if (product == null) {
                        JOptionPane.showMessageDialog(BillingAppUI.this, "Product not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    } else if (product.getStockQuantity() <= billItems.getOrDefault(product, 0)) {
                        JOptionPane.showMessageDialog(BillingAppUI.this, "Not enough stock for " + product.getName(), "Stock Alert", JOptionPane.WARNING_MESSAGE);
                    } else {
                        billItems.put(product, billItems.getOrDefault(product, 0) + 1);
                        refreshBillTable();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    handleWorkerException(e, "Error fetching product");
                } finally {
                    barcodeField.setText("");
                    barcodeField.requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void removeSelectedItemFromBill() {
        int selectedRow = billTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item from the bill to remove.", "No Item Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Product productToRemove = productsInBillTable.get(selectedRow);
        int currentQuantity = billItems.get(productToRemove);
        if (currentQuantity > 1) {
            billItems.put(productToRemove, currentQuantity - 1);
        } else {
            billItems.remove(productToRemove);
        }
        refreshBillTable();
    }

    private void refreshBillTable() {
        billTableModel.setRowCount(0);
        productsInBillTable.clear();
        for (Map.Entry<Product, Integer> entry : billItems.entrySet()) {
            Product p = entry.getKey();
            int qty = entry.getValue();
            billTableModel.addRow(new Object[]{ p.getName(), p.getPrice(), p.getTaxSlab(), qty, p.getPrice() * qty });
            productsInBillTable.add(p);
        }
        updateTotalAmount();
    }
    
    private void updateTotalAmount() {
        double subtotal = billItems.entrySet().stream().mapToDouble(e -> e.getKey().getPrice() * e.getValue()).sum();
        double discountValue = 0;
        try {
            String discountText = discountField.getText().trim();
            if (!discountText.isEmpty()) {
                discountValue = Double.parseDouble(discountText);
            }
        } catch (NumberFormatException e) {
            // Ignore invalid format during real-time updates
        }

        double discountAmount = 0;
        if (discountValue > 0) {
            if ("%".equals(discountTypeComboBox.getSelectedItem())) {
                discountAmount = (subtotal * discountValue) / 100.0;
            } else { // "₹"
                discountAmount = discountValue;
            }
        }
        
        double grandTotal = subtotal - discountAmount;
        if (grandTotal < 0) grandTotal = 0;
        
        totalAmountLabel.setText(String.format("Total: ₹ %.2f", grandTotal));
    }

    private void finalizeBill() {
        if (billItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cannot finalize an empty bill.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double subtotal = billItems.entrySet().stream().mapToDouble(e -> e.getKey().getPrice() * e.getValue()).sum();
        double discountValue = 0;
        try {
            String discountText = discountField.getText().trim();
            if (!discountText.isEmpty()) {
                discountValue = Double.parseDouble(discountText);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid discount value. Please enter a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        double discountAmount = 0;
        if (discountValue > 0) {
            if ("%".equals(discountTypeComboBox.getSelectedItem())) {
                discountAmount = (subtotal * discountValue) / 100.0;
            } else {
                discountAmount = discountValue;
            }
        }
        
        double grandTotal = subtotal - discountAmount;
        if (grandTotal < 0) grandTotal = 0;

        int confirm = JOptionPane.showConfirmDialog(this,
            String.format("Subtotal: ₹ %.2f\nDiscount: - ₹ %.2f\nGrand Total: ₹ %.2f\n\nFinalize this bill?", subtotal, discountAmount, grandTotal),
            "Confirm Bill", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            final double finalDiscount = discountAmount;
            final double finalGrandTotal = grandTotal;
            new SwingWorker<Long, Void>() {
                @Override protected Long doInBackground() { return DatabaseManager.saveBill(billItems, finalGrandTotal); }
                @Override protected void done() {
                    try {
                        long billId = get();
                        if (billId != -1) {
                            PdfGenerator.generateBillPdf(billId, billItems, subtotal, finalDiscount, finalGrandTotal);
                            
                            List<Object> message = new ArrayList<>();
                            message.add("Bill finalized successfully!");
                            message.add("PDF saved in the 'bills' directory.");
                            
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                                JButton openFolderButton = new JButton("Open Bill Location");
                                openFolderButton.addActionListener(e -> {
                                    try {
                                        Desktop.getDesktop().open(new File("bills"));
                                    } catch (IOException | IllegalArgumentException ex) {
                                        JOptionPane.showMessageDialog(BillingAppUI.this, "Could not open bills directory.", "Error", JOptionPane.ERROR_MESSAGE);
                                    }
                                });
                                message.add(openFolderButton);
                            }
                            
                            JOptionPane.showMessageDialog(BillingAppUI.this, message.toArray(), "Success", JOptionPane.INFORMATION_MESSAGE);

                            billItems.clear();
                            discountField.setText("");
                            refreshBillTable();
                            
                            if (inventoryTableModel != null) {
                                refreshInventoryTable();
                            }
                            if (todaysSalesValueLabel != null) {
                                refreshDashboard();
                            }
                        } else {
                            throw new Exception("saveBill returned -1");
                        }
                    } catch (Exception e) {
                        handleWorkerException(e, "Failed to finalize bill");
                    }
                }
            }.execute();
        }
    }
    
    private void refreshDashboard() {
        if (todaysSalesValueLabel == null) return;
        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() {
                Map<String, Object> data = new HashMap<>();
                data.put("sales", DatabaseManager.getTodaysTotalSales());
                data.put("bills", DatabaseManager.getTodaysBillCount());
                data.put("lowStock", DatabaseManager.getLowStockProducts(10));
                data.put("topSelling", DatabaseManager.getTopSellingProductsThisMonth(5));
                return data;
            }
            @Override
            protected void done() {
                try {
                    Map<String, Object> data = get();
                    todaysSalesValueLabel.setText(String.format("₹ %.2f", (double) data.get("sales")));
                    todaysBillsValueLabel.setText(String.valueOf((int) data.get("bills")));

                    lowStockTableModel.setRowCount(0);
                    @SuppressWarnings("unchecked")
                    List<Product> lowStock = (List<Product>) data.get("lowStock");
                    for (Product p : lowStock) {
                        lowStockTableModel.addRow(new Object[]{p.getName(), p.getStockQuantity()});
                    }

                    topSellingTableModel.setRowCount(0);
                    @SuppressWarnings("unchecked")
                    List<Object[]> topSelling = (List<Object[]>) data.get("topSelling");
                    for (Object[] row : topSelling) {
                        topSellingTableModel.addRow(row);
                    }
                } catch (Exception e) {
                    handleWorkerException(e, "Failed to load dashboard data");
                }
            }
        }.execute();
    }
    
    private void refreshInventoryTable() {
        if (inventoryTableModel == null) return;
        new SwingWorker<List<Product>, Void>() {
            @Override protected List<Product> doInBackground() { return DatabaseManager.getAllProducts(); }
            @Override protected void done() {
                try {
                    inventoryTableModel.setRowCount(0);
                    for (Product p : get()) {
                        inventoryTableModel.addRow(new Object[]{p.getBarcode(), p.getName(), p.getPrice(), p.getStockQuantity(), p.getTaxSlab()});
                    }
                } catch (Exception e) {
                    handleWorkerException(e, "Failed to load inventory");
                }
            }
        }.execute();
    }

    private void addNewProduct() {
        String barcode = newBarcodeField.getText().trim();
        String name = newNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Product Name cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double mrp, taxSlab;
        int stock;
        try {
            mrp = Double.parseDouble(newMrpField.getText().trim());
            taxSlab = Double.parseDouble(newTaxSlabField.getText().trim());
            stock = Integer.parseInt(newStockField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "MRP, Tax Slab, and Stock must be valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (mrp <= 0 || stock < 0 || taxSlab < 0 || mrp > 1_000_000 || stock > 1_000_000) {
             JOptionPane.showMessageDialog(this, "Please enter valid, reasonable values.\nMRP must be positive. Stock and Tax cannot be negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (barcode.isEmpty()) {
            barcode = "N/A-" + UUID.randomUUID().toString().substring(0, 8);
        }

        Product product = new Product(barcode, name, mrp, stock, taxSlab);
        
        new SwingWorker<Boolean, Void>() {
            private boolean nameExists = false;

            @Override
            protected Boolean doInBackground() {
                if (DatabaseManager.doesProductNameExist(name)) {
                    nameExists = true;
                    return false;
                }
                return DatabaseManager.addProduct(product);
            }

            @Override
            protected void done() {
                try {
                    if (nameExists) {
                        int confirm = JOptionPane.showConfirmDialog(BillingAppUI.this,
                                "A product with the name '" + name + "' already exists.\nDo you still want to add this new product?",
                                "Duplicate Product Name", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (confirm == JOptionPane.YES_OPTION) {
                            addProductAfterConfirmation(product);
                        }
                        return;
                    }

                    if (get()) {
                        JOptionPane.showMessageDialog(BillingAppUI.this, "Product added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        clearAddProductFields();
                        refreshInventoryTableIfNeeded();
                    } else {
                        throw new Exception("A product with this barcode may already exist.");
                    }
                } catch (Exception e) {
                     handleWorkerException(e, "Failed to add product");
                }
            }
        }.execute();
    }
    
    private void addProductAfterConfirmation(Product product) {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { return DatabaseManager.addProduct(product); }
            @Override protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(BillingAppUI.this, "Product added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        clearAddProductFields();
                        refreshInventoryTableIfNeeded();
                    } else {
                        throw new Exception("A product with this barcode may already exist.");
                    }
                } catch (Exception e) {
                    handleWorkerException(e, "Failed to add product");
                }
            }
        }.execute();
    }
    
    private void clearAddProductFields() {
        newBarcodeField.setText(""); newNameField.setText(""); newMrpField.setText("");
        newTaxSlabField.setText(""); newStockField.setText("");
    }

    private void updateExistingStock() {
        String barcode = updateBarcodeField.getText().trim();
        if (barcode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a product barcode.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(updateQuantityField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantity must be a valid integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { return DatabaseManager.updateStock(barcode, quantity); }
            @Override protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(BillingAppUI.this, "Stock updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        updateBarcodeField.setText(""); updateQuantityField.setText("");
                        refreshInventoryTable();
                    } else {
                        throw new Exception("Product with this barcode may not exist.");
                    }
                } catch (Exception e) {
                    handleWorkerException(e, "Failed to update stock");
                }
            }
        }.execute();
    }

    private void deleteProduct() {
        String barcode = deleteBarcodeField.getText().trim();
        if (barcode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the barcode of the product to delete.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to permanently delete this product?\nThis action cannot be undone.",
            "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() { return DatabaseManager.deleteProductByBarcode(barcode); }
                @Override protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(BillingAppUI.this, "Product deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                            deleteBarcodeField.setText("");
                            refreshInventoryTable();
                        } else {
                             throw new Exception("Product with this barcode may not exist.");
                        }
                    } catch (Exception e) {
                        handleWorkerException(e, "Failed to delete product");
                    }
                }
            }.execute();
        }
    }

    private void showSelectedBillDetails() {
        int selectedRow = salesHistoryTable.getSelectedRow();
        if (selectedRow < 0) return;
        
        long billId;
        double totalAmount;
        try {
            billId = Long.parseLong(salesHistoryTableModel.getValueAt(selectedRow, 0).toString());
            totalAmount = Double.parseDouble(salesHistoryTableModel.getValueAt(selectedRow, 2).toString());
        } catch (NumberFormatException | NullPointerException e) {
            JOptionPane.showMessageDialog(this, "Could not parse bill details from table.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String billDate = salesHistoryTableModel.getValueAt(selectedRow, 1).toString();
        
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() { return DatabaseManager.getBillDetails(billId); }
            @Override protected void done() {
                try {
                    List<Object[]> items = get();
                    StringBuilder details = new StringBuilder();
                    details.append("--- Bill Details ---\n");
                    details.append("Bill ID: ").append(billId).append("\n");
                    details.append("Date: ").append(billDate).append("\n");
                    details.append("Total: ₹").append(String.format("%.2f", totalAmount)).append("\n\n");
                    details.append("--- Items Purchased ---\n");
                    details.append(String.format("%-25s %5s %10s %10s\n", "Name", "Qty", "Price", "Total"));
                    details.append("----------------------------------------------------------\n");

                    for (Object[] item : items) {
                        details.append(String.format("%-25.25s %5d %10.2f %10.2f\n", item[0], item[1], item[2], item[3]));
                    }
                    
                    JTextArea detailsArea = new JTextArea(details.toString());
                    detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    detailsArea.setEditable(false);
                    JScrollPane scrollPane = new JScrollPane(detailsArea);
                    scrollPane.setPreferredSize(new Dimension(450, 300));
                    JOptionPane.showMessageDialog(BillingAppUI.this, scrollPane, "Bill Details - #" + billId, JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    handleWorkerException(e, "Could not fetch bill details");
                }
            }
        }.execute();
    }
    
    private void refreshSalesHistoryTable() {
        if (salesFilterComboBox == null || salesHistoryTableModel == null) return;
        String filter = (String) salesFilterComboBox.getSelectedItem();
        
        new SwingWorker<List<Object[]>, Void>() {
            @Override protected List<Object[]> doInBackground() { return DatabaseManager.getSalesHistory(filter); }
            @Override protected void done() {
                try {
                    salesHistoryTableModel.setRowCount(0);
                    for (Object[] row : get()) {
                        salesHistoryTableModel.addRow(row);
                    }
                } catch (Exception e) {
                    handleWorkerException(e, "Could not fetch sales history");
                }
            }
        }.execute();
    }

    private void refreshUsersTable() {
        if (usersTableModel == null || userSelectionComboBox == null) return;
        
        new SwingWorker<List<User>, Void>() {
            @Override protected List<User> doInBackground() { return DatabaseManager.getAllUsers(); }
            @Override protected void done() {
                try {
                    List<User> users = get();
                    usersTableModel.setRowCount(0);
                    for (User user : users) {
                        usersTableModel.addRow(new Object[]{user.getUsername(), user.getRole()});
                    }
                    
                    Object selectedItem = userSelectionComboBox.getSelectedItem();
                    userSelectionComboBox.removeAllItems();
                    for (User user : users) {
                        userSelectionComboBox.addItem(user.getUsername());
                    }
                    if (selectedItem != null) {
                        userSelectionComboBox.setSelectedItem(selectedItem);
                    }
                } catch (Exception e) {
                     handleWorkerException(e, "Could not fetch user list");
                }
            }
        }.execute();
    }

    private void addStaffMember() {
        String username = newStaffUsernameField.getText().trim();
        char[] passwordChars = newStaffPasswordField.getPassword();

        if (username.isEmpty() || passwordChars.length == 0) {
            JOptionPane.showMessageDialog(this, "Username and password cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String password = new String(passwordChars);
        
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { return DatabaseManager.addUser(username, password, "staff"); }
            @Override protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(BillingAppUI.this, "Staff member added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        newStaffUsernameField.setText(""); newStaffPasswordField.setText("");
                        refreshUsersTable();
                    } else {
                        throw new Exception("Username may already exist.");
                    }
                } catch (Exception e) {
                     handleWorkerException(e, "Failed to add staff member");
                } finally {
                    java.util.Arrays.fill(passwordChars, ' ');
                }
            }
        }.execute();
    }

    private void resetUserPassword() {
        String selectedUser = (String) userSelectionComboBox.getSelectedItem();
        char[] newPasswordChars = resetPasswordField.getPassword();
    
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Please select a user to reset their password.", "No User Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newPasswordChars.length == 0) {
            JOptionPane.showMessageDialog(this, "The new password cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String newPassword = new String(newPasswordChars);
    
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to reset the password for user '" + selectedUser + "'?",
            "Confirm Password Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    
        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() { return DatabaseManager.resetUserPassword(selectedUser, newPassword); }
                @Override protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(BillingAppUI.this, "Password for user '" + selectedUser + "' has been reset successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                            resetPasswordField.setText("");
                        } else {
                            throw new Exception("Failed to reset password in database.");
                        }
                    } catch (Exception e) {
                        handleWorkerException(e, "Failed to reset password");
                    } finally {
                        java.util.Arrays.fill(newPasswordChars, ' ');
                    }
                }
            }.execute();
        } else {
             java.util.Arrays.fill(newPasswordChars, ' ');
        }
    }
    
    // --- Helper Methods ---
    
    private void refreshInventoryTableIfNeeded() {
        if ("administrator".equals(currentUserRole) && inventoryTableModel != null) {
            refreshInventoryTable();
        }
    }
    
    private void handleWorkerException(Exception e, String title) {
        e.printStackTrace();
        // Unwrap the exception if it's from the worker's background thread
        Throwable cause = (e instanceof ExecutionException) ? e.getCause() : e;
        JOptionPane.showMessageDialog(this, "An error occurred: " + cause.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }
}

