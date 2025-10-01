package com.mycompany.billingsystem.net;

/**
 * An interface for receiving barcode data from a source (like a network server).
 */
public interface BarcodeReceiver {
    void onBarcodeReceived(String barcode);
    void setServerStatus(String status);
}

