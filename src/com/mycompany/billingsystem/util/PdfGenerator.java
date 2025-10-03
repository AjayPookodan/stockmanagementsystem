package com.mycompany.billingsystem.util;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.mycompany.billingsystem.model.Product;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * A utility class to generate PDF bills using the iText library.
 * This class requires iText 7 libraries (kernel, io, layout) in the project's 'lib' folder.
 */
public class PdfGenerator {

    private static final String BILLS_DIRECTORY = "bills";

    /**
     * Generates a PDF receipt for a finalized bill with specific formatting requirements.
     * This method is synchronized to prevent race conditions if called by multiple threads.
     *
     * @param billId      The unique ID for the bill.
     * @param billItems   A map containing the products and quantities sold.
     * @param totalAmount The final total amount of the bill.
     * @return The file path of the generated PDF, or null if an error occurred.
     */
    public static synchronized String generateBillPdf(long billId, Map<Product, Integer> billItems, double totalAmount) {
        // --- Input Validation ---
        if (billItems == null || billItems.isEmpty()) {
            System.err.println("Cannot generate PDF for an empty bill.");
            return null;
        }

        // --- Directory Setup ---
        // Ensure the 'bills' directory exists. If it fails, the application might not have write permissions.
        try {
            File dir = new File(BILLS_DIRECTORY);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } catch (SecurityException se) {
            System.err.println("Security Exception: Failed to create '" + BILLS_DIRECTORY + "' directory. Check folder permissions.");
            se.printStackTrace();
            return null;
        }

        String filePath = BILLS_DIRECTORY + File.separator + "Bill_" + billId + ".pdf";

        // --- PDF Generation using try-with-resources and broad exception handling ---
        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A5)) {

            // --- Header: TEAM 5 ---
            document.add(new Paragraph("TEAM 5")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(20)
                .setMarginBottom(5));

            document.add(new Paragraph("Mavoor Road, Kozhikode, Kerala")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10));
            
            document.add(createSeparator());

            // --- Bill Details: Bill No. and Date/Time in IST ---
            // Set up date format for Indian Standard Time (IST)
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a");
            dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
            String formattedDate = dateFormat.format(new Date());

            Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
            detailsTable.setWidth(UnitValue.createPercentValue(100));
            detailsTable.addCell(createDetailCell("Bill No: " + billId, TextAlignment.LEFT));
            detailsTable.addCell(createDetailCell("Date: " + formattedDate, TextAlignment.RIGHT));
            document.add(detailsTable);
            
            document.add(createSeparator());

            // --- Items Table ---
            // The number of floats in the array (5) matches the number of columns added.
            Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{4, 2, 1, 2, 2}));
            itemsTable.setWidth(UnitValue.createPercentValue(100));

            // Headers
            itemsTable.addHeaderCell(createHeaderCell("Item Name"));
            itemsTable.addHeaderCell(createHeaderCell("MRP"));
            itemsTable.addHeaderCell(createHeaderCell("Qty"));
            itemsTable.addHeaderCell(createHeaderCell("Tax"));
            itemsTable.addHeaderCell(createHeaderCell("Amount"));

            // Body
            for (Map.Entry<Product, Integer> entry : billItems.entrySet()) {
                Product product = entry.getKey();
                int quantity = entry.getValue();

                // FIX: Added null check for product to prevent NullPointerException
                if (product == null) {
                    System.err.println("Skipping a null product in the bill item list.");
                    continue;
                }
                
                double itemTotal = product.getPrice() * quantity;
                String productName = product.getName() != null ? product.getName() : "Unknown Product";

                itemsTable.addCell(createItemCell(productName, TextAlignment.LEFT));
                itemsTable.addCell(createItemCell(String.format("₹%.2f", product.getPrice()), TextAlignment.RIGHT));
                itemsTable.addCell(createItemCell(String.valueOf(quantity), TextAlignment.CENTER));
                itemsTable.addCell(createItemCell(String.format("%.1f%%", product.getTaxSlab()), TextAlignment.RIGHT));
                itemsTable.addCell(createItemCell(String.format("₹%.2f", itemTotal), TextAlignment.RIGHT));
            }
            document.add(itemsTable);
            
            document.add(createSeparator());

            // --- Total Amount ---
            document.add(new Paragraph("Grand Total:  ₹ " + String.format("%.2f", totalAmount))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBold()
                .setFontSize(14));

            document.close();
            System.out.println("PDF generated successfully at: " + filePath);
            return filePath;

        } catch (IOException e) {
            // Enhanced error message for better debugging
            System.err.println("Error generating PDF. This can be caused by missing iText libraries in the 'lib' folder, or a lack of file system permissions to write to the '" + BILLS_DIRECTORY + "' directory.");
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            // FIX: Catch any other unexpected runtime exceptions (like NullPointerException)
            System.err.println("An unexpected error occurred during PDF generation: " + e.getClass().getSimpleName());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Helper method to create a styled separator line.
     * @return A Paragraph object representing a horizontal line.
     */
    private static Paragraph createSeparator() {
        return new Paragraph("--------------------------------------------------")
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(5)
            .setMarginBottom(5);
    }

    /**
     * Helper method to create styled header cells for the items table.
     * @param text The header text.
     * @return A styled Cell object.
     */
    private static Cell createHeaderCell(String text) {
        return new Cell().add(new Paragraph(text).setBold())
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(Border.NO_BORDER);
    }

    /**
     * Helper method to create styled data cells for the items table.
     * @param text The cell text.
     * @param alignment The horizontal alignment for the text.
     * @return A styled Cell object.
     */
    private static Cell createItemCell(String text, TextAlignment alignment) {
        return new Cell().add(new Paragraph(text))
            .setTextAlignment(alignment)
            .setBorder(Border.NO_BORDER)
            .setPaddingTop(2)
            .setPaddingBottom(2);
    }
    
    /**
     * Helper method for bill detail cells (Bill No. and Date).
     * @param text The cell text.
     * @param alignment The horizontal alignment for the text.
     * @return A styled Cell object.
     */
    private static Cell createDetailCell(String text, TextAlignment alignment) {
        return new Cell().add(new Paragraph(text).setFontSize(9))
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(alignment);
    }
}

