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
 * A utility class to generate PDF bills using the iText 7 library.
 * This class creates a detailed, formatted receipt for each transaction.
 */
public class PdfGenerator {

    private static final String BILLS_DIRECTORY = "bills";

    /**
     * Generates a PDF receipt for a finalized bill.
     * This method is synchronized to prevent file access conflicts if called by multiple threads.
     *
     * @param billId The unique ID for the bill.
     * @param billItems A map containing the products and quantities sold.
     * @param subtotal The total amount before discounts.
     * @param discountAmount The calculated discount amount.
     * @param grandTotal The final amount after discounts.
     * @return The file path of the generated PDF, or null if an error occurred.
     */
    public static synchronized String generateBillPdf(long billId, Map<Product, Integer> billItems, double subtotal, double discountAmount, double grandTotal) {
        if (billItems == null || billItems.isEmpty()) {
            System.err.println("Cannot generate PDF for an empty bill.");
            return null;
        }

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

        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A5)) {

            // --- Header ---
            document.add(new Paragraph("TEAM 5")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(20)
                .setMarginBottom(5));
            
            document.add(new Paragraph("Mavoor Road, Kozhikode, Kerala")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10));

            document.add(createSeparator());

            // --- Bill Details (Bill No. and Date/Time in IST) ---
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
            Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{4, 2, 1, 2, 2}));
            itemsTable.setWidth(UnitValue.createPercentValue(100));

            itemsTable.addHeaderCell(createHeaderCell("Item Name"));
            itemsTable.addHeaderCell(createHeaderCell("MRP"));
            itemsTable.addHeaderCell(createHeaderCell("Qty"));
            itemsTable.addHeaderCell(createHeaderCell("Tax"));
            itemsTable.addHeaderCell(createHeaderCell("Amount"));

            for (Map.Entry<Product, Integer> entry : billItems.entrySet()) {
                Product product = entry.getKey();
                int quantity = entry.getValue();
                
                if (product == null) continue; // Safety check

                itemsTable.addCell(createItemCell(product.getName(), TextAlignment.LEFT));
                itemsTable.addCell(createItemCell(String.format("₹%.2f", product.getPrice()), TextAlignment.RIGHT));
                itemsTable.addCell(createItemCell(String.valueOf(quantity), TextAlignment.CENTER));
                itemsTable.addCell(createItemCell(String.format("%.1f%%", product.getTaxSlab()), TextAlignment.RIGHT));
                itemsTable.addCell(createItemCell(String.format("₹%.2f", product.getPrice() * quantity), TextAlignment.RIGHT));
            }
            document.add(itemsTable);
            document.add(createSeparator());

            // --- Totals Section (Subtotal, Discount, Grand Total) ---
            Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
            totalsTable.setWidth(UnitValue.createPercentValue(100));
            
            totalsTable.addCell(createTotalCell("Subtotal:", TextAlignment.RIGHT));
            totalsTable.addCell(createTotalCell(String.format("₹ %.2f", subtotal), TextAlignment.RIGHT));

            if (discountAmount > 0) {
                totalsTable.addCell(createTotalCell("Discount:", TextAlignment.RIGHT));
                totalsTable.addCell(createTotalCell(String.format("- ₹ %.2f", discountAmount), TextAlignment.RIGHT));
            }

            totalsTable.addCell(createTotalCell("Grand Total:", TextAlignment.RIGHT).setBold().setFontSize(14));
            totalsTable.addCell(createTotalCell(String.format("₹ %.2f", grandTotal), TextAlignment.RIGHT).setBold().setFontSize(14));
            
            document.add(totalsTable);

            document.close();
            System.out.println("PDF generated successfully at: " + filePath);
            return filePath;

        } catch (IOException e) {
            System.err.println("Error generating PDF. Check iText libraries and file permissions.");
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during PDF generation: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static Paragraph createSeparator() {
        return new Paragraph("--------------------------------------------------").setTextAlignment(TextAlignment.CENTER).setMarginTop(5).setMarginBottom(5);
    }
    
    private static Cell createHeaderCell(String text) {
        return new Cell().add(new Paragraph(text).setBold()).setTextAlignment(TextAlignment.CENTER).setBorder(Border.NO_BORDER);
    }

    private static Cell createItemCell(String text, TextAlignment alignment) {
        return new Cell().add(new Paragraph(text)).setTextAlignment(alignment).setBorder(Border.NO_BORDER).setPaddingTop(2).setPaddingBottom(2);
    }
    
    private static Cell createDetailCell(String text, TextAlignment alignment) {
        return new Cell().add(new Paragraph(text).setFontSize(9)).setBorder(Border.NO_BORDER).setTextAlignment(alignment);
    }

    private static Cell createTotalCell(String text, TextAlignment alignment) {
        return new Cell().add(new Paragraph(text)).setBorder(Border.NO_BORDER).setTextAlignment(alignment);
    }
}

