package com.example.javaconcurrency.virtualthreads.mounting;

import java.time.Duration;
import java.util.concurrent.Executors;

public class VirtualThreadMountingDemo {
    
    // Configuration constants
    private static final int THREAD_COUNT = 10;             // Number of virtual threads to create
    private static final boolean USE_IO_WORKLOAD = false;    // true for IO blocking, false for CPU
    private static final int CPU_ITERATIONS = 100_000_000;   // Iterations for CPU-bound work
    private static final int IO_DELAY_MS = 100;             // Delay for IO-bound simulation

    public static void main(String[] args) {
        System.out.println("Starting demo with " + THREAD_COUNT + " virtual threads");
        System.out.println("Workload type: " + (USE_IO_WORKLOAD ? "IO-Blocking" : "CPU-Only"));
        
        // Create virtual thread executor (auto-closeable)
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // Submit tasks to executor
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    // Capture initial platform thread
                    String initialThread = getCarrierThreadName();
                    System.out.printf("Task %d started on: %s%n", taskId, initialThread);
                    
                    // Execute workload based on configuration
                    if (USE_IO_WORKLOAD) {
                        simulateIoWork();  // Will cause virtual thread unmounting
                    } else {
                        doCpuWork();       // Will stay pinned to carrier thread
                    }
                    
                    // Check if carrier thread changed
                    String currentThread = getCarrierThreadName();
                    printThreadChange(taskId, initialThread, currentThread);
                    
                    return null;
                });
            }
        }
        System.out.println("All tasks completed");
    }
    
    // Simulates IO-bound work by sleeping (causes unmounting)
    private static void simulateIoWork() throws InterruptedException {
        Thread.sleep(Duration.ofMillis(IO_DELAY_MS));
    }
    
    // Simulates CPU-bound work (keeps thread mounted)
    private static void doCpuWork() {
        long result = 0;
        for (int i = 0; i < CPU_ITERATIONS; i++) {
            result += Math.sqrt(i);  // CPU-intensive calculation
        }
    }
    
    // Extracts platform thread name from full thread string
    private static String getCarrierThreadName() {
        String fullThreadName = Thread.currentThread().toString();
        return fullThreadName.substring(fullThreadName.indexOf('@') + 1);
    }
    
    // Prints thread change status with clear comparison
    private static void printThreadChange(int taskId, String initialThread, String currentThread) {
        if (initialThread.equals(currentThread)) {
            System.out.printf("Task %d Completed on SAME thread: %s%n", taskId, currentThread);
        } else {
            System.out.printf("Task %d Completed on DIFFERENT thread: %s (was %s)%n",
                           taskId, currentThread, initialThread);
        }
    }
}