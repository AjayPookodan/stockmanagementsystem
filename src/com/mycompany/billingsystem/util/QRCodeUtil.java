package com.mycompany.billingsystem.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;

/**
 * A utility class to generate QR Code images using the ZXing library.
 */
public class QRCodeUtil {

    /**
     * Generates a QR code image for the given text.
     *
     * @param text The text to encode in the QR code (e.g., a URL).
     * @param width The desired width of the QR code image.
     * @param height The desired height of the QR code image.
     * @return An ImageIcon containing the QR code, or null if generation fails.
     */
    public static ImageIcon generateQRCodeImage(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            return new ImageIcon(qrImage);
        } catch (WriterException e) {
            System.err.println("Could not generate QR Code, WriterException :: " + e.getMessage());
        }
        return null;
    }
}
