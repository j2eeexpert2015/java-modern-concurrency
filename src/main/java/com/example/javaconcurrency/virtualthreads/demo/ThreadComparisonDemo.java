package com.example.javaconcurrency.virtualthreads.demo;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadComparisonDemo {
    
    private static final int TASK_COUNT = 10000;
    private static final int SLEEP_DURATION_MS = 100; // Shorter for quicker demo
    
    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Thread Comparison Demo ===");
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
        
        System.out.println("⏸️  Press ENTER to start the comparison tests...");
        scanner.nextLine();
        
        // Test Platform Threads
        runPlatformThreadsTest(scanner);
        
        // Give JVM time to settle
        Thread.sleep(2000);
        System.gc();
        Thread.sleep(1000);
        
        // Test Virtual Threads
        runVirtualThreadsTest(scanner);
        
        System.out.println("🎉 All tests completed!");
        System.out.println("⏸️  Press ENTER to exit (this keeps the process alive for final monitoring)...");
        scanner.nextLine();
        
        System.out.println("👋 Exiting thread comparison demo.");
        scanner.close();
    }
    
    private static void runPlatformThreadsTest(Scanner scanner) throws InterruptedException {
        System.out.println("🔄 Testing Platform Threads...");
        System.out.println("⏸️  Press ENTER to start platform threads execution...");
        scanner.nextLine();
        
        long startTime = System.currentTimeMillis();
        
        Runtime runtime = Runtime.getRuntime();
        long startMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        
        ExecutorService executor = Executors.newFixedThreadPool(200);
        
        for (int i = 0; i < TASK_COUNT; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(SLEEP_DURATION_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        
        long endTime = System.currentTimeMillis();
        long endMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        
        System.out.println("✅ Platform Threads Results:");
        System.out.println("   Execution time: " + (endTime - startTime) + "ms");
        System.out.println("   Memory used: " + (endMemory - startMemory) + " MB");
        System.out.println("   Throughput: " + (TASK_COUNT * 1000.0 / (endTime - startTime)) + " tasks/sec");
        System.out.println();
        
        System.out.println("⏸️  Press ENTER to continue to virtual threads test...");
        scanner.nextLine();
    }
    
    private static void runVirtualThreadsTest(Scanner scanner) throws InterruptedException {
        System.out.println("🔄 Testing Virtual Threads...");
        System.out.println("⏸️  Press ENTER to start virtual threads execution...");
        scanner.nextLine();
        
        long startTime = System.currentTimeMillis();
        
        Runtime runtime = Runtime.getRuntime();
        long startMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        
        for (int i = 0; i < TASK_COUNT; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(SLEEP_DURATION_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        
        long endTime = System.currentTimeMillis();
        long endMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        
        System.out.println("✅ Virtual Threads Results:");
        System.out.println("   Execution time: " + (endTime - startTime) + "ms");
        System.out.println("   Memory used: " + (endMemory - startMemory) + " MB");
        System.out.println("   Throughput: " + (TASK_COUNT * 1000.0 / (endTime - startTime)) + " tasks/sec");
        System.out.println();
    }
}