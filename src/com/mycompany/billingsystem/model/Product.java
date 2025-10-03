package com.mycompany.billingsystem.model;

import java.util.Objects;

/**
 * Represents a single product in the inventory.
 * This is a simple Plain Old Java Object (POJO) that holds product information.
 */
public class Product {
    private String barcode;
    private String name;
    private double price;
    private int stockQuantity;
    private double taxSlab;

    public Product(String barcode, String name, double price, int stockQuantity, double taxSlab) {
        this.barcode = barcode;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.taxSlab = taxSlab;
    }

    // --- Getters ---
    public String getBarcode() { return barcode; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
    public double getTaxSlab() { return taxSlab; }

    // --- Overridden equals and hashCode ---
    // These are crucial for using Product objects as keys in a HashMap.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(barcode, product.barcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barcode);
    }
}

