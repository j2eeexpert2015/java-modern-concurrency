package com.example.javaconcurrency.virtualthreads;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Section 1: Introduction to Virtual Threads
 * 
 * This demo shows the different ways to create virtual threads in Java,
 * and contrasts them with traditional platform threads.
 */
public class VirtualThreadCreationDemo {

    private static final int THREAD_COUNT = 10_000;
    private static final AtomicInteger counter = new AtomicInteger();

    public static void main(String[] args) {
        System.out.println("Java Virtual Threads Demo");
        System.out.println("------------------------");
        System.out.println("JDK Version: " + System.getProperty("java.version"));
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println();

        // Demo 1: Direct creation
        demoDirectCreation();
        
        // Demo 2: Using Thread.Builder API
        demoBuilderAPI();
        
        // Demo 3: Using ExecutorService
        demoExecutorService();
        
        // Demo 4: Performance comparison
        demoPerformanceComparison();
    }

    private static void demoDirectCreation() {
        System.out.println("Demo 1: Direct Thread Creation");
        System.out.println("-----------------------------");
        
        // Method 1: Traditional platform thread
        Thread platformThread = new Thread(() -> {
            System.out.println("  Running in a platform thread: " + Thread.currentThread());
        });
        platformThread.start();
        
        // Method 2: Virtual thread using startVirtualThread
        Thread virtualThread = Thread.startVirtualThread(() -> {
            System.out.println("  Running in a virtual thread: " + Thread.currentThread());
        });
        
        try {
            platformThread.join();
            virtualThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("  Platform thread is virtual: " + platformThread.isVirtual());
        System.out.println("  Virtual thread is virtual: " + virtualThread.isVirtual());
        System.out.println();
    }
    
    private static void demoBuilderAPI() {
        System.out.println("Demo 2: Using Thread.Builder API");
        System.out.println("------------------------------");
        
        // Create unstarted virtual thread
        Thread unstartedVirtual = Thread.ofVirtual()
                .name("custom-virtual-", 1)
                .unstarted(() -> {
                    System.out.println("  Custom named virtual thread: " + Thread.currentThread());
                });
        
        // Create unstarted platform thread
        Thread unstartedPlatform = Thread.ofPlatform()
                .name("custom-platform-", 1)
                .unstarted(() -> {
                    System.out.println("  Custom named platform thread: " + Thread.currentThread());
                });
        
        // Start both threads
        unstartedVirtual.start();
        unstartedPlatform.start();
        
        try {
            unstartedVirtual.join();
            unstartedPlatform.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Creating thread factories
        System.out.println("\n  Creating threads with ThreadFactory:");
        
        ThreadFactory virtualFactory = Thread.ofVirtual().factory();
        ThreadFactory platformFactory = Thread.ofPlatform().factory();
        
        Thread vThread = virtualFactory.newThread(() -> 
            System.out.println("  Thread from virtual factory: " + Thread.currentThread()));
        
        Thread pThread = platformFactory.newThread(() -> 
            System.out.println("  Thread from platform factory: " + Thread.currentThread()));
        
        vThread.start();
        pThread.start();
        
        try {
            vThread.join();
            pThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println();
    }
    
    private static void demoExecutorService() {
        System.out.println("Demo 3: Using ExecutorService");
        System.out.println("----------------------------");
        
        // Reset counter
        counter.set(0);
        
        // Using platform thread executor
        System.out.println("  Using newFixedThreadPool (platform threads):");
        try (ExecutorService platformExecutor = 
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            
            for (int i = 0; i < 5; i++) {
                platformExecutor.submit(() -> {
                    int taskId = counter.incrementAndGet();
                    System.out.println("    Task " + taskId + " running in: " + Thread.currentThread());
                    try {
                        Thread.sleep(100); // Simulate work
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return taskId;
                });
            }
            
            // Allow executor to complete
            sleep(1000);
        }
        
        // Reset counter
        counter.set(0);
        
        // Using virtual thread per task executor
        System.out.println("\n  Using newVirtualThreadPerTaskExecutor:");
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            for (int i = 0; i < 5; i++) {
                virtualExecutor.submit(() -> {
                    int taskId = counter.incrementAndGet();
                    System.out.println("    Task " + taskId + " running in: " + Thread.currentThread());
                    try {
                        Thread.sleep(100); // Simulate work
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return taskId;
                });
            }
            
            // Allow executor to complete
            sleep(1000);
        }
        
        System.out.println();
    }
    
    private static void demoPerformanceComparison() {
        System.out.println("Demo 4: Performance Comparison");
        System.out.println("----------------------------");
        
        System.out.println("  Creating " + THREAD_COUNT + " threads...");
        
        // Measure platform thread creation
        Instant platformStart = Instant.now();
        try (ExecutorService platformExecutor = 
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            
            IntStream.range(0, THREAD_COUNT).forEach(i -> {
                platformExecutor.submit(() -> {
                    try {
                        Thread.sleep(Duration.ofMillis(10));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return i;
                });
            });
            
            // Shutdown and wait for termination
            platformExecutor.shutdown();
            while (!platformExecutor.isTerminated()) {
                try {
                    platformExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        Duration platformDuration = Duration.between(platformStart, Instant.now());
        System.out.println("  Platform threads completed in: " + platformDuration.toMillis() + " ms");
        
        // Measure virtual thread creation
        Instant virtualStart = Instant.now();
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            IntStream.range(0, THREAD_COUNT).forEach(i -> {
                virtualExecutor.submit(() -> {
                    try {
                        Thread.sleep(Duration.ofMillis(10));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return i;
                });
            });
            
            // Shutdown and wait for termination
            virtualExecutor.shutdown();
            while (!virtualExecutor.isTerminated()) {
                try {
                    virtualExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        Duration virtualDuration = Duration.between(virtualStart, Instant.now());
        System.out.println("  Virtual threads completed in: " + virtualDuration.toMillis() + " ms");
        
        // Calculate improvement
        double improvement = (double) platformDuration.toMillis() / virtualDuration.toMillis();
        System.out.printf("  Virtual threads were %.2fx faster for %d tasks%n", improvement, THREAD_COUNT);
    }
    
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}