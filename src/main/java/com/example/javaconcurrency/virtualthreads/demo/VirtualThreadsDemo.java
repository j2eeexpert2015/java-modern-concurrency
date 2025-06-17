package com.example.javaconcurrency.virtualthreads.demo;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VirtualThreadsDemo {
    
    private static final int TASK_COUNT = 10000;
    private static final int SLEEP_DURATION_MS = 1000;
    
    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Virtual Threads Demo ===");
        System.out.println("Tasks: " + TASK_COUNT);
        System.out.println("Sleep per task: " + SLEEP_DURATION_MS + "ms");
        System.out.println();
        
        // Display process info for VisualVM/JMC connection
        String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        String pid = processName.split("@")[0];
        System.out.println("🔍 Process ID: " + pid);
        System.out.println("📊 To monitor with VisualVM: Connect to PID " + pid);
        System.out.println("📊 To monitor with JMC: Connect to local process or PID " + pid);
        System.out.println();
        
        System.out.println("⏸️  Press ENTER to start the virtual threads test...");
        scanner.nextLine();
        
        long startTime = System.currentTimeMillis();
        
        // Create virtual thread executor (Java 21+)
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        
        System.out.println("Starting tasks with virtual threads...");
        
        // Submit tasks
        for (int i = 0; i < TASK_COUNT; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // Simulate I/O bound work (like network call, database query)
                    Thread.sleep(SLEEP_DURATION_MS);
                    
                    if (taskId % 1000 == 0) {
                        System.out.println("Virtual thread completed task " + taskId + 
                                         " on thread: " + Thread.currentThread().getName());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Task " + taskId + " was interrupted");
                }
            });
        }
        
        // Shutdown executor and wait for completion
        executor.shutdown();
        boolean finished = executor.awaitTermination(5, TimeUnit.MINUTES);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("=== Virtual Threads Results ===");
        System.out.println("All tasks completed: " + finished);
        System.out.println("Total execution time: " + duration + "ms");
        System.out.println("Average time per task: " + (duration / (double) TASK_COUNT) + "ms");
        
        // Memory usage info
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        System.out.println("Memory used: " + usedMemory + " MB");
        System.out.println();
        
        System.out.println("✅ Virtual threads test completed!");
        System.out.println("⏸️  Press ENTER to exit (this keeps the process alive for monitoring)...");
        scanner.nextLine();
        
        System.out.println("👋 Exiting virtual threads demo.");
        scanner.close();
    }
}