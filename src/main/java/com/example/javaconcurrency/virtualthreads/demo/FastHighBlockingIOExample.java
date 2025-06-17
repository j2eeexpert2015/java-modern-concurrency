package com.example.javaconcurrency.virtualthreads.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * FAST HIGH BLOCKING I/O (~80%) - Completes in ~60 seconds
 * Pattern: CPU → BLOCK → CPU (80% blocking)
 */
public class FastHighBlockingIOExample {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Fast High Blocking I/O Example...");
        System.out.println("Process ID (PID): " + ProcessHandle.current().pid());
        System.out.println("Attach JMC now! Press Enter to continue...");
        new BufferedReader(new InputStreamReader(System.in)).readLine();

        ExecutorService executor = Executors.newFixedThreadPool(20);
        Object sharedLock = new Object();
        BlockingQueue<String> taskQueue = new ArrayBlockingQueue<>(10);
        
        long startTime = System.currentTimeMillis();
        System.out.println("=== ALL THREAD STATES DEMO - 80% BLOCKING ===");

        // Submit 50 tasks showing all thread states
        for (int i = 0; i < 50; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // GREEN: CPU work (5% of time)
                    doCpuWork(300);
                    
                    // YELLOW: Monitor wait - simulating waiting for tasks (5% of time)
                    waitForTask(taskQueue, taskId);
                    
                    // SALMON: Monitor blocked - competing for shared resource (10% of time)
                    synchronizedWork(sharedLock, taskId);
                    
                    // RED: Socket I/O - network blocking (70% of time) 
                    System.out.println("Task " + taskId + " starting network I/O...");
                    blockingNetworkCall();
                    
                    // GREEN: Final CPU work (10% of time)
                    doCpuWork(600);
                    
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
        System.out.println("Thread States: GREEN(15%) + YELLOW(5%) + SALMON(10%) + RED(70%) = 80% BLOCKING");
        
        System.out.println("\n=== EXECUTION COMPLETE ===");
        System.out.println("Stop your JFR recording now and analyze the results!");
        System.out.println("Press Enter to exit...");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    // YELLOW: Monitor wait - waiting for tasks/objects
    private static void waitForTask(BlockingQueue<String> queue, int taskId) throws Exception {
        try {
            // Simulate waiting for work - creates YELLOW (monitor wait)
            String task = queue.poll(500, TimeUnit.MILLISECONDS);
            if (task == null) {
                // Add some work for others to pick up
                queue.offer("work-" + taskId, 100, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // SALMON: Monitor blocked - synchronization bottleneck  
    private static void synchronizedWork(Object lock, int taskId) {
        synchronized (lock) {
            try {
                // Simulate critical section work - creates SALMON (monitor blocked)
                Thread.sleep(200); // Intentional bottleneck
                
                // Some CPU work while holding lock
                double result = 0;
                for (int i = 0; i < 100000; i++) {
                    result += Math.sqrt(i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Controlled blocking I/O - exactly 8 seconds per call (RED)
    private static void blockingNetworkCall() throws Exception {
        try {
            // Use a reliable delay service - exactly 8 second delay
            URL url = new URL("https://httpbin.org/delay/8");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);  // 15s timeout
            conn.setReadTimeout(15000);     // 15s timeout
            
            // This blocks the thread for exactly 8 seconds (RED - Socket Read)
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Read response (minimal processing)
                }
            }
        } catch (SocketTimeoutException e) {
            // Even timeouts create blocking I/O pattern (RED)
            System.out.println("Network timeout (still blocking I/O)");
        }
    }
    
    // CPU-intensive work for specified milliseconds
    private static void doCpuWork(long durationMs) {
        long startTime = System.currentTimeMillis();
        long result = 0;
        
        // CPU-bound loop for specified duration
        while (System.currentTimeMillis() - startTime < durationMs) {
            // Mathematical operations to consume CPU
            for (int i = 0; i < 10000; i++) {
                result += Math.sqrt(i) * Math.sin(i) * Math.cos(i);
            }
        }
        
        // Prevent optimization
        if (result == Long.MAX_VALUE) {
            System.out.println("Unlikely result: " + result);
        }
    }
}