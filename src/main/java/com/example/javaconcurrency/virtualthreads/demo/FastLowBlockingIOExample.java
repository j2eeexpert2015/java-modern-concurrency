package com.example.javaconcurrency.virtualthreads.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * FAST LOW BLOCKING I/O (~10%) - Completes in ~60 seconds
 * Pattern: Intensive CPU work with minimal I/O
 * NOT suitable for virtual threads
 */
public class FastLowBlockingIOExample {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Fast Low Blocking I/O Example...");
        System.out.println("Process ID (PID): " + ProcessHandle.current().pid());
        System.out.println("Attach JMC now! Press Enter to continue...");
        new BufferedReader(new InputStreamReader(System.in)).readLine();

        ExecutorService executor = Executors.newFixedThreadPool(20);
        Object sharedLock = new Object();
        BlockingQueue<String> taskQueue = new ArrayBlockingQueue<>(10);
        
        long startTime = System.currentTimeMillis();
        System.out.println("=== ALL THREAD STATES DEMO - 10% BLOCKING ===");

        // Submit 100 CPU-intensive tasks with minimal blocking
        for (int i = 0; i < 100; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // GREEN: Major CPU-intensive work (80% of time)
                    performComplexCalculations(taskId, 2000);
                    
                    // YELLOW: Brief monitor wait (3% of time) 
                    if (taskId % 20 == 0) {
                        briefWaitForTask(taskQueue, taskId);
                    }
                    
                    // SALMON: Quick synchronized work (4% of time)
                    if (taskId % 15 == 0) {
                        quickSynchronizedWork(sharedLock, taskId);
                    }
                    
                    // RED: Minimal network I/O (3% of time)
                    if (taskId % 25 == 0) {
                        minimalNetworkCall(taskId);
                    }
                    
                    // GREEN: More CPU work (10% of time)
                    performComplexCalculations(taskId, 400);
                    
                    System.out.println("Task " + taskId + " completed");
                } catch (Exception e) {
                    System.out.println("Task " + taskId + " failed: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        
        long endTime = System.currentTimeMillis();
        System.out.println("Total time: " + (endTime - startTime) + "ms");
        System.out.println("Thread States: GREEN(90%) + YELLOW(3%) + SALMON(4%) + RED(3%) = 10% BLOCKING");
        System.out.println("NOT suitable for virtual threads - CPU-bound workload!");
        
        System.out.println("\n=== EXECUTION COMPLETE ===");
        System.out.println("Stop your JFR recording now and analyze the results!");
        System.out.println("Press Enter to exit...");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    // YELLOW: Brief monitor wait (Low Blocking version)
    private static void briefWaitForTask(BlockingQueue<String> queue, int taskId) throws Exception {
        try {
            // Very brief wait - minimal blocking
            String task = queue.poll(50, TimeUnit.MILLISECONDS);
            if (task == null) {
                queue.offer("work-" + taskId, 10, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // SALMON: Quick synchronized work (Low Blocking version)
    private static void quickSynchronizedWork(Object lock, int taskId) {
        synchronized (lock) {
            try {
                // Very brief critical section - minimal contention
                Thread.sleep(20);
                
                // Quick CPU work while holding lock
                double result = 0;
                for (int i = 0; i < 5000; i++) {
                    result += Math.sqrt(i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // RED: Minimal network call (Low Blocking version)
    private static void minimalNetworkCall(int taskId) {
        try {
            // Very quick network call - 1 second delay
            URL url = new URL("https://httpbin.org/delay/1");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                reader.readLine(); // Read just one line quickly
            }
        } catch (Exception e) {
            // Even failures are quick
            System.out.println("Quick network timeout for task " + taskId);
        }
    }

    // CPU-intensive calculations - mathematical processing (GREEN)
    private static void performComplexCalculations(int taskId, long durationMs) {
        long startTime = System.currentTimeMillis();
        double result = 0;
        
        // Run CPU-intensive work for specified duration
        while (System.currentTimeMillis() - startTime < durationMs) {
            // Complex mathematical operations
            for (int i = 0; i < 50000; i++) {
                result += Math.sqrt(i) * Math.sin(i) * Math.cos(i);
                result += Math.pow(i % 100, 2.5);
                result += Math.log(i + 1) * Math.exp(i % 10);
            }
            
            // Matrix multiplication simulation
            double[][] matrix = generateMatrix(30);
            result += matrixMultiplication(matrix, matrix)[0][0];
            
            // Prime number calculation
            result += findPrimesBelow(500).size();
        }
        
        // Prevent optimization
        if (result < 0) {
            System.out.println("Impossible result: " + result);
        }
    }
    
    // Minimal I/O operation - removed, replaced with network call above
    
    // Generate matrix for calculations
    private static double[][] generateMatrix(int size) {
        double[][] matrix = new double[size][size];
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] = random.nextDouble() * 100;
            }
        }
        return matrix;
    }
    
    // Matrix multiplication (CPU-intensive)
    private static double[][] matrixMultiplication(double[][] a, double[][] b) {
        int size = a.length;
        double[][] result = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }
    
    // Find prime numbers (CPU-intensive)
    private static List<Integer> findPrimesBelow(int limit) {
        List<Integer> primes = new ArrayList<>();
        for (int i = 2; i < limit; i++) {
            boolean isPrime = true;
            for (int j = 2; j <= Math.sqrt(i); j++) {
                if (i % j == 0) {
                    isPrime = false;
                    break;
                }
            }
            if (isPrime) {
                primes.add(i);
            }
        }
        return primes;
    }
}