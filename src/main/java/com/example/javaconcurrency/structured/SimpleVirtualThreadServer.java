package com.example.javaconcurrency.structured;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;

/**
 * A simple HTTP server that uses virtual threads to handle requests.
 * This demonstrates how to use Project Loom for web applications.
 */
public class SimpleVirtualThreadServer {

    public static void main(String[] args) throws IOException {
        // Create and configure HTTP server
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Register endpoints
        server.createContext("/hello", new HelloHandler());
        server.createContext("/sleep", new SleepingHandler());
        
        // Use virtual threads for request handling
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        
        // Start the server
        server.start();
        System.out.println("Server started on port " + port);
        System.out.println("Try: http://localhost:" + port + "/hello");
        System.out.println("     http://localhost:" + port + "/sleep?ms=2000");
    }
    
    /**
     * A simple handler that returns a greeting.
     */
    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String response = """
                    {
                      "message": "Hello from Virtual Thread!",
                      "thread": "%s",
                      "threadId": %d,
                      "isVirtual": %b
                    }
                    """.formatted(
                        Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().isVirtual()
                    );
                
                sendJsonResponse(exchange, 200, response);
                
            } finally {
                exchange.close();
            }
        }
    }
    
    /**
     * A handler that sleeps for the specified number of milliseconds.
     * This demonstrates how virtual threads handle blocking operations.
     */
    static class SleepingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Instant start = Instant.now();
            String query = exchange.getRequestURI().getQuery();
            
            try {
                // Parse sleep duration from query parameter
                long sleepMs = 1000; // Default
                if (query != null && query.startsWith("ms=")) {
                    sleepMs = Long.parseLong(query.substring(3));
                }
                
                // Limit maximum sleep time to 10 seconds
                sleepMs = Math.min(sleepMs, 10000);
                
                System.out.printf("Thread %s sleeping for %d ms%n", 
                        Thread.currentThread().getName(), sleepMs);
                
                // Sleep - this would block a platform thread, but not a virtual thread
                Thread.sleep(sleepMs);
                
                // Calculate actual duration
                Duration duration = Duration.between(start, Instant.now());
                
                String response = """
                    {
                      "message": "Slept for %d milliseconds",
                      "requestedSleepMs": %d,
                      "actualDurationMs": %d,
                      "thread": "%s",
                      "isVirtual": %b
                    }
                    """.formatted(
                        sleepMs,
                        sleepMs,
                        duration.toMillis(),
                        Thread.currentThread().getName(),
                        Thread.currentThread().isVirtual()
                    );
                
                sendJsonResponse(exchange, 200, response);
                
            } catch (InterruptedException e) {
                String response = """
                    {
                      "error": "Sleep interrupted",
                      "message": "%s"
                    }
                    """.formatted(e.getMessage());
                
                sendJsonResponse(exchange, 500, response);
                Thread.currentThread().interrupt();
                
            } catch (NumberFormatException e) {
                String response = """
                    {
                      "error": "Invalid sleep parameter", 
                      "message": "Please provide a valid number of milliseconds (e.g., /sleep?ms=2000)"
                    }
                    """;
                
                sendJsonResponse(exchange, 400, response);
                
            } finally {
                exchange.close();
            }
        }
    }
    
    /**
     * Helper method to send JSON response.
     */
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String response) 
            throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
