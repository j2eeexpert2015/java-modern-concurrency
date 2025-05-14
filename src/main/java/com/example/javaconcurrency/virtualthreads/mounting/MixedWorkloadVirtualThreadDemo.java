package com.example.javaconcurrency.virtualthreads.mounting;

import java.time.Duration;
import java.util.concurrent.Executors;

public class MixedWorkloadVirtualThreadDemo {
    
    // Configuration
    private static final int TOTAL_TASKS = 10;
    private static final int CPU_TASKS = 4;
    private static final int IO_TASKS = TOTAL_TASKS - CPU_TASKS;
    private static final int CPU_ITERATIONS = 100_000_000;
    private static final int IO_DELAY_MS = 100;
    
    // Formatting
    private static final String HEADER = "========================================";
    private static final String DIVIDER = "----------------------------------------";
    private static final String CPU_LABEL = "CPU";
    private static final String IO_LABEL = "IO ";

    public static void main(String[] args) {
        printHeader();
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // Create CPU-bound tasks
            for (int i = 0; i < CPU_TASKS; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    String initialThread = getCarrierThread();
                    printTaskStart(CPU_LABEL, taskId, initialThread);
                    
                    doCpuWork();
                    
                    String currentThread = getCarrierThread();
                    printTaskResult(CPU_LABEL, taskId, initialThread, currentThread);
                    return null;
                });
            }
            
            // Create IO-bound tasks
            for (int i = 0; i < IO_TASKS; i++) {
                final int taskId = CPU_TASKS + i;
                executor.submit(() -> {
                    String initialThread = getCarrierThread();
                    printTaskStart(IO_LABEL, taskId, initialThread);
                    
                    simulateIoWork();
                    
                    String currentThread = getCarrierThread();
                    printTaskResult(IO_LABEL, taskId, initialThread, currentThread);
                    return null;
                });
            }
        }
        printFooter();
    }

    private static void printHeader() {
        System.out.println(HEADER);
        System.out.println("       VIRTUAL THREAD MOUNTING DEMO");
        System.out.printf("  Running %d tasks (%d CPU, %d IO with blocking)%n", 
                        TOTAL_TASKS, CPU_TASKS, IO_TASKS);
        System.out.println(HEADER);
        System.out.println("  TASK   TYPE  STATUS       CARRIER THREAD");
        System.out.println(DIVIDER);
    }

    private static void printTaskStart(String type, int taskId, String thread) {
        System.out.printf("  #%-2d    %s    STARTED     %s%n", taskId, type, thread);
    }

    private static void printTaskResult(String type, int taskId, 
                                     String initialThread, String currentThread) {
        String status = initialThread.equals(currentThread) ? 
                       "COMPLETED   " : "MOVED TO    ";
        System.out.printf("  #%-2d    %s    %s %s%n",
                       taskId, type, status, formatThreadChange(initialThread, currentThread));
    }

    private static String formatThreadChange(String initial, String current) {
        if (initial.equals(current)) {
            return current;
        }
        return String.format("%s (from %s)", current, initial);
    }

    private static void printFooter() {
        System.out.println(DIVIDER);
        System.out.println("  KEY OBSERVATIONS:");
        System.out.println("  • CPU tasks stay on their original thread");
        System.out.println("  • IO tasks often move to different threads after blocking");
        System.out.println("  • Fewer carrier threads than virtual threads used");
        System.out.println(HEADER);
    }

    private static String getCarrierThread() {
        String fullThread = Thread.currentThread().toString();
        return "Worker-" + fullThread.substring(fullThread.lastIndexOf('-') + 1);
    }

    private static void simulateIoWork() throws InterruptedException {
        Thread.sleep(Duration.ofMillis(IO_DELAY_MS));
    }

    private static void doCpuWork() {
        long result = 0;
        for (int i = 0; i < CPU_ITERATIONS; i++) {
            result += Math.sqrt(i);
        }
    }
}