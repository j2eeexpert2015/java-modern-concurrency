package com.example.javaconcurrency.structured;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

/**
 * This class demonstrates different implementations of StructuredTaskScope
 * and their use cases for different concurrent scenarios.
 * 
 * Note: Run with JDK 21 or later with preview features enabled:
 * java --enable-preview TaskScopePatterns.java
 */
public class TaskScopePatterns {

    public static void main(String[] args) throws Exception {
        System.out.println("StructuredTaskScope Patterns");
        System.out.println("===========================");
        
        shutdownOnFailureExample();
        shutdownOnSuccessExample();
        customShutdownPolicyExample();
        
        System.out.println("\nAll examples completed.");
    }
    
    /**
     * Example demonstrating the ShutdownOnFailure policy.
     * All subtasks must complete successfully, or the first failure
     * cancels all other subtasks.
     */
    private static void shutdownOnFailureExample() throws InterruptedException, ExecutionException {
        System.out.println("\n1. ShutdownOnFailure Example:");
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Fork multiple price check tasks
            var usdTask = scope.fork(() -> fetchPrice("USD", 1000));
            var eurTask = scope.fork(() -> fetchPrice("EUR", 1200));
            var gbpTask = scope.fork(() -> fetchPrice("GBP", 800));
            
            try {
                // Wait for all tasks and throw if any failed
                scope.join().throwIfFailed();
                
                // All tasks completed successfully
                System.out.println("All price checks succeeded:");
                System.out.println("- USD: " + usdTask.get());
                System.out.println("- EUR: " + eurTask.get());
                System.out.println("- GBP: " + gbpTask.get());
                
                // Calculate an aggregate
                var total = usdTask.get().add(eurTask.get()).add(gbpTask.get());
                System.out.println("Total: " + total);
                
            } catch (ExecutionException e) {
                System.out.println("Price check failed: " + e.getCause().getMessage());
                System.out.println("USD task state: " + usdTask.state());
                System.out.println("EUR task state: " + eurTask.state());
                System.out.println("GBP task state: " + gbpTask.state());
            }
        }
    }
    
    /**
     * Example demonstrating the ShutdownOnSuccess policy.
     * As soon as one subtask completes successfully, all other
     * subtasks are cancelled.
     */
    private static void shutdownOnSuccessExample() throws InterruptedException, ExecutionException {
        System.out.println("\n2. ShutdownOnSuccess Example:");
        
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
            
            // Fork multiple tasks to check different servers
            scope.fork(() -> checkServer("server1.example.com", 1000));
            scope.fork(() -> checkServer("server2.example.com", 1500));
            scope.fork(() -> checkServer("server3.example.com", 800));
            
            // Wait for the first successful result
            scope.join();
            
            // Get the successful result
            String firstResponse = scope.result();
            System.out.println("Got first successful response: " + firstResponse);
        }
    }
    
    /**
     * Example demonstrating a custom shutdown policy by extending
     * StructuredTaskScope.
     */
    private static void customShutdownPolicyExample() throws InterruptedException, TimeoutException {
        System.out.println("\n3. Custom Shutdown Policy Example:");
        
        // Create custom scope that completes when we've received at least
        // 2 results or exceeded a timeout
        try (var scope = new ThresholdTaskScope<String>(2)) {
            
            // Fork multiple tasks
            scope.fork(() -> processData("source-1", 500));
            scope.fork(() -> processData("source-2", 1000));
            scope.fork(() -> processData("source-3", 300));
            scope.fork(() -> processData("source-4", 1200));
            
            // Wait until we have at least 2 results or timeout
            scope.joinUntil(Instant.now().plusMillis(2000));
            
            // Print the collected results
            var results = scope.getResults();
            System.out.println("Collected " + results.size() + " results:");
            results.forEach(result -> System.out.println("- " + result));
        }
    }
    
    // Simulated service calls

    private static BigDecimal fetchPrice(String currency, long delay) throws Exception {
        System.out.println("Fetching price for " + currency + "...");
        Thread.sleep(delay);
        
        // Randomly generate errors for demonstration
        if (currency.equals("EUR") && ThreadLocalRandom.current().nextInt(10) < 3) {
            throw new Exception("Error fetching " + currency + " price");
        }
        
        return new BigDecimal(ThreadLocalRandom.current().nextDouble(10, 100));
    }
    
    private static String checkServer(String server, long delay) throws Exception {
        System.out.println("Checking server " + server + "...");
        Thread.sleep(delay);
        
        // Sometimes fail to simulate unreliable servers
        if (ThreadLocalRandom.current().nextInt(10) < 3) {
            throw new Exception("Server " + server + " failed to respond");
        }
        
        return "Response from " + server + " at " + Instant.now();
    }
    
    private static String processData(String source, long delay) throws Exception {
        System.out.println("Processing data from " + source + "...");
        Thread.sleep(delay);
        
        // Sometimes fail to simulate data processing errors
        if (ThreadLocalRandom.current().nextInt(10) < 2) {
            throw new Exception("Failed to process data from " + source);
        }
        
        return "Processed data from " + source + ": " + 
               ThreadLocalRandom.current().nextInt(100, 1000) + " records";
    }
    
    /**
     * A custom StructuredTaskScope that collects successful results
     * and completes when a threshold number of results is obtained.
     */
    static class ThresholdTaskScope<T> extends StructuredTaskScope<T> {
        private final int threshold;
        private final List<T> results = new ArrayList<>();
        
        public ThresholdTaskScope(int threshold) {
            super("ThresholdTaskScope", Thread.ofVirtual().factory());
            this.threshold = threshold;
        }
        
        /**
         * This method is called when a subtask completes - this is where we implement
         * our custom shutdown policy.
         */
        @Override
        protected void handleComplete(StructuredTaskScope.Subtask<? extends T> subtask) {
            // Check if the subtask completed successfully
            if (subtask.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                try {
                    // Get the result and add it to our list
                    results.add(subtask.get());
                    
                    // If we've reached the threshold, shut down remaining tasks
                    if (results.size() >= threshold) {
                        shutdown();
                    }
                } catch (Exception e) {
                    // This shouldn't happen since we check for SUCCESS state
                    Thread.currentThread().interrupt();
                }
            }
            // For failed or cancelled tasks, we don't do anything special
        }
        
        public List<T> getResults() {
            return new ArrayList<>(results);
        }
    }
}