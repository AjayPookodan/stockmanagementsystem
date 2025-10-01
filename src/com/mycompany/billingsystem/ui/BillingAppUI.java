package com.mycompany.billingsystem.ui;

import com.mycompany.billingsystem.db.DatabaseManager;
import com.mycompany.billingsystem.model.Product;
import com.mycompany.billingsystem.net.BarcodeReceiver;
import com.mycompany.billingsystem.net.BarcodeServer;
import com.mycompany.billingsystem.util.NetworkUtil;
import com.mycompany.billingsystem.util.QRCodeUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main UI, now featuring a QR code for easy mobile connection.
 */
public class BillingAppUI extends JFrame implements BarcodeReceiver {

    private JTextField barcodeField;
    private JTable billTable;
    private DefaultTableModel tableModel;
    private JLabel totalAmountLabel;
    private JLabel serverStatusLabel;

    private final Map<String, Product> billedProducts = new HashMap<>();
    private final Map<String, Integer> billQuantities = new HashMap<>();

    public BillingAppUI() {
        setTitle("Retail Billing System (QR Code Connect)");
        setSize(850, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setupUI();
        setupListeners();
        startBarcodeServer();
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));

        // --- Top Panel for QR Code and Status ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createTitledBorder("Connect Your Phone"));

        // Generate QR Code
        String ipAddress = NetworkUtil.getLocalIpAddress();
        String connectionUrl = "http://" + ipAddress + ":" + BarcodeServer.PORT;
        ImageIcon qrCodeIcon = QRCodeUtil.generateQRCodeImage(connectionUrl, 200, 200);
        
        JLabel qrCodeLabel = new JLabel();
        if (qrCodeIcon != null) {
            qrCodeLabel.setIcon(qrCodeIcon);
        } else {
            qrCodeLabel.setText("Could not generate QR Code.");
        }
        
        String instructions = "<html><h3>Scan this QR code with your phone's camera to connect.</h3></html>";
        JLabel instructionLabel = new JLabel(instructions);
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        serverStatusLabel = new JLabel("Server Status: Initializing...");
        serverStatusLabel.setForeground(Color.BLUE);
        serverStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(instructionLabel, BorderLayout.NORTH);
        infoPanel.add(serverStatusLabel, BorderLayout.SOUTH);
        
        topPanel.add(qrCodeLabel, BorderLayout.WEST);
        topPanel.add(infoPanel, BorderLayout.CENTER);

        // --- Center Panel for Bill Table ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        
        JPanel usbInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        usbInputPanel.add(new JLabel("Scan with USB Handheld Scanner:"));
        barcodeField = new JTextField(20);
        usbInputPanel.add(barcodeField);

        String[] columnNames = {"Barcode", "Product Name", "Price", "Quantity", "Total"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        billTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(billTable);
        
        centerPanel.add(usbInputPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Bottom Panel ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        totalAmountLabel = new JLabel("Total: $0.00");
        totalAmountLabel.setFont(new Font("Arial", Font.BOLD, 24));

        JButton generateBillButton = new JButton("Generate Bill");
        generateBillButton.setFont(new Font("Arial", Font.BOLD, 18));

        bottomPanel.add(totalAmountLabel, BorderLayout.WEST);
        bottomPanel.add(generateBillButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        generateBillButton.addActionListener(e -> generateBill());
    }

    private void setupListeners() {
        barcodeField.addActionListener(e -> {
            String barcode = barcodeField.getText().trim();
            if (!barcode.isEmpty()) {
                addProductToBill(barcode);
                barcodeField.setText("");
            }
        });
    }

    private void startBarcodeServer() {
        BarcodeServer server = new BarcodeServer(this);
        Thread serverThread = new Thread(server);
        serverThread.setDaemon(true);
        serverThread.start();
    }
    
    // --- BarcodeReceiver Implementation ---
    @Override
    public void onBarcodeReceived(String barcode) {
        SwingUtilities.invokeLater(() -> addProductToBill(barcode));
    }
    
    @Override
    public void setServerStatus(String status) {
        SwingUtilities.invokeLater(() -> serverStatusLabel.setText("Server Status: " + status));
    }
    
    // --- Core Logic Methods ---
    private void addProductToBill(String barcode) {
        if (billQuantities.containsKey(barcode)) {
            int currentQuantity = billQuantities.get(barcode);
            Product product = billedProducts.get(barcode);
            if (product.getStockQuantity() > currentQuantity) {
                 billQuantities.put(barcode, currentQuantity + 1);
            } else {
                JOptionPane.showMessageDialog(this, "No more stock for " + product.getName(), "Stock Alert", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            Product product = DatabaseManager.findProductByBarcode(barcode);
            if (product != null) {
                if(product.getStockQuantity() > 0) {
                    billedProducts.put(barcode, product);
                    billQuantities.put(barcode, 1);
                } else {
                    JOptionPane.showMessageDialog(this, "Product out of stock!", "Stock Alert", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Product not found!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        updateBillTable();
    }

    private void updateBillTable() {
        tableModel.setRowCount(0);
        double totalAmount = 0.0;
        for (String barcode : billQuantities.keySet()) {
            Product product = billedProducts.get(barcode);
            int quantity = billQuantities.get(barcode);
            double itemTotal = product.getPrice() * quantity;
            totalAmount += itemTotal;
            tableModel.addRow(new Object[]{ product.getBarcode(), product.getName(), String.format("%.2f", product.getPrice()), quantity, String.format("%.2f", itemTotal) });
        }
        totalAmountLabel.setText(String.format("Total: $%.2f", totalAmount));
    }

    private void generateBill() {
        if (billedProducts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items in the bill.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<Product> productsToUpdate = new ArrayList<>();
        for (String barcode : billedProducts.keySet()) {
            Product product = billedProducts.get(barcode);
            int newStock = product.getStockQuantity() - billQuantities.get(barcode);
            product.setStockQuantity(newStock);
            productsToUpdate.add(product);
        }
        if (DatabaseManager.updateStock(productsToUpdate)) {
            JOptionPane.showMessageDialog(this, "Bill generated and stock updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
            resetBill();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update stock.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void resetBill() {
        tableModel.setRowCount(0);
        billedProducts.clear();
        billQuantities.clear();
        totalAmountLabel.setText("Total: $0.00");
        barcodeField.requestFocusInWindow();
    }
}

