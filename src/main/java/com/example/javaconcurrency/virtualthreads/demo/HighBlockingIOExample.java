package com.example.javaconcurrency.virtualthreads.demo;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

/**
 * HIGH BLOCKING I/O (~80%)
 * This code spends most time waiting for network responses
 */
public class HighBlockingIOExample {
    
    public static void main(String[] args) throws Exception {
        System.out.println("Starting High Blocking I/O Example...");
        System.out.println("Process ID (PID): " + ProcessHandle.current().pid());
        System.out.println("Attach JMC now! Press Enter to continue...");
        new BufferedReader(new InputStreamReader(System.in)).readLine(); // Wait for Enter
        
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<String> urls = List.of(
            "https://httpbin.org/delay/2",
            "https://httpbin.org/delay/3", 
            "https://httpbin.org/delay/1",
            "https://httpbin.org/delay/2",
            "https://httpbin.org/delay/4"
        );
        
        long startTime = System.currentTimeMillis();
        
        // Submit 100 tasks that mostly wait for I/O
        for (int i = 0; i < 100; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // This will spend ~80% time in blocking I/O
                    String result = fetchDataWithDelay(urls.get(taskId % urls.size()));
                    
                    // Minimal CPU work (~20%)
                    processData(result);
                    
                    System.out.println("Task " + taskId + " completed");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        
        long endTime = System.currentTimeMillis();
        System.out.println("Total time: " + (endTime - startTime) + "ms");
        System.out.println("Most time spent waiting for network I/O!");
        
        System.out.println("\n=== EXECUTION COMPLETE ===");
        System.out.println("Stop your JFR recording now and analyze the results!");
        System.out.println("Press Enter to exit...");
        new BufferedReader(new InputStreamReader(System.in)).readLine(); // Wait for Enter before exit
    }
    
    // BLOCKING I/O - Network call with delay
    private static String fetchDataWithDelay(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        // This blocks the thread waiting for response
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    // Minimal CPU processing
    private static void processData(String data) {
        // Just some light string processing
        data.toUpperCase().length();
    }
}