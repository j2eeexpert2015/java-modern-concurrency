package com.example.javaconcurrency.virtualthreads.demo;

import java.io.*;
import java.util.concurrent.*;
import java.util.Random;

/**
 * LOW BLOCKING I/O (~10%)
 * This code spends most time in CPU-intensive calculations
 */
public class LowBlockingIOExample {
    
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Low Blocking I/O Example...");
        System.out.println("Process ID (PID): " + ProcessHandle.current().pid());
        System.out.println("Attach JMC now! Press Enter to continue...");
        System.in.read(); // Wait for user input
        
        ExecutorService executor = Executors.newFixedThreadPool(20);
        
        long startTime = System.currentTimeMillis();
        
        // Submit 100 CPU-intensive tasks
        for (int i = 0; i < 100; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // ~90% CPU-intensive work
                    long result = performHeavyCalculation(taskId);
                    
                    // ~10% I/O - Quick file write
                    saveResultToFile(taskId, result);
                    
                    System.out.println("Task " + taskId + " result: " + result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        
        long endTime = System.currentTimeMillis();
        System.out.println("Total time: " + (endTime - startTime) + "ms");
        System.out.println("Most time spent in CPU calculations!");
    }
    
    // CPU-INTENSIVE work (90% of execution time)
    private static long performHeavyCalculation(int taskId) {
        // Simulate heavy mathematical computation
        long result = 0;
        Random random = new Random(taskId);
        
        // Prime number calculation + matrix operations
        for (int i = 0; i < 1000000; i++) {
            result += isPrime(random.nextInt(10000)) ? 1 : 0;
            result += fibonacci(25); // Recursive fibonacci
            result += matrixMultiplication(); // Matrix operations
        }
        
        return result;
    }
    
    // Quick I/O operation (10% of execution time)
    private static void saveResultToFile(int taskId, long result) throws IOException {
        // Fast, non-blocking file write
        String filename = "result_" + taskId + ".txt";
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Task " + taskId + ": " + result);
        }
        // File is small, write completes quickly
    }
    
    // CPU-intensive helper methods
    private static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i <= Math.sqrt(n); i++) {
            if (n % i == 0) return false;
        }
        return true;
    }
    
    private static long fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
    
    private static long matrixMultiplication() {
        // Simple 3x3 matrix multiplication
        int[][] a = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        int[][] b = {{9, 8, 7}, {6, 5, 4}, {3, 2, 1}};
        long sum = 0;
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    sum += a[i][k] * b[k][j];
                }
            }
        }
        return sum;
    }
}
