package com.mycompany.billingsystem.model;

/**
 * Represents a single product in the inventory.
 * This class acts as a simple data holder (model).
 */
public class Product {

    private final String barcode;
    private final String name;
    private final double price;
    private int stockQuantity;

    public Product(String barcode, String name, double price, int stockQuantity) {
        this.barcode = barcode;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    // --- Getter methods ---

    public String getBarcode() {
        return barcode;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    // --- Setter method ---

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}
