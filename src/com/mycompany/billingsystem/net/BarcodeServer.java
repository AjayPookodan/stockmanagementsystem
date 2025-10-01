package com.mycompany.billingsystem.net;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Scanner;

/**
 * A lightweight HTTP server that serves the mobile scanner HTML and JS files
 * and listens for POST requests containing barcode data.
 */
public class BarcodeServer implements Runnable {
    public static final int PORT = 9999;
    private final BarcodeReceiver receiver;

    public BarcodeServer(BarcodeReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void run() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", new BarcodeHandler(receiver));
            server.setExecutor(null); // creates a default executor
            server.start();
            receiver.setServerStatus("Server started on port " + PORT + ". Listening...");
        } catch (IOException e) {
            receiver.setServerStatus("Error: Could not start server on port " + PORT);
            e.printStackTrace();
        }
    }

    static class BarcodeHandler implements HttpHandler {
        private final BarcodeReceiver receiver;

        public BarcodeHandler(BarcodeReceiver receiver) {
            this.receiver = receiver;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equalsIgnoreCase(requestMethod)) {
                // Serve the correct file based on the path
                String filePath;
                String contentType;

                if ("/".equals(path)) {
                    filePath = "mobile_scanner.html";
                    contentType = "text/html";
                } else if ("/html5-qrcode.min.js".equals(path)) {
                    // This is the crucial change: serve the JS file from the local 'web' folder
                    filePath = "web/html5-qrcode.min.js";
                    contentType = "application/javascript";
                } else {
                    sendResponse(exchange, 404, "Not Found");
                    return;
                }
                
                File file = new File(filePath);
                if (file.exists()) {
                    exchange.sendResponseHeaders(200, file.length());
                    exchange.getResponseHeaders().set("Content-Type", contentType);
                    try (OutputStream os = exchange.getResponseBody()) {
                        Files.copy(file.toPath(), os);
                    }
                } else {
                    // Send a clearer error message if a file is missing
                    String errorMessage = "Error: File not found on server at path: " + filePath;
                    System.err.println(errorMessage);
                    sendResponse(exchange, 404, errorMessage);
                }

            } else if ("POST".equalsIgnoreCase(requestMethod)) {
                // Handle barcode submission
                try (InputStream is = exchange.getRequestBody()) {
                    Scanner scanner = new Scanner(is).useDelimiter("\\A");
                    String barcode = scanner.hasNext() ? scanner.next() : "";
                    if (!barcode.isEmpty()) {
                        receiver.onBarcodeReceived(barcode);
                    }
                }
                sendResponse(exchange, 200, "OK");
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.sendResponseHeaders(statusCode, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}

