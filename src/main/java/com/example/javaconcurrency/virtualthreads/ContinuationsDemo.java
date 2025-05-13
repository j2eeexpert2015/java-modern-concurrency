package com.example.javaconcurrency.virtualthreads;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Section 2: Understanding Continuations
 * 
 * This demo illustrates the concept of Continuations, which are the
 * underlying mechanism behind virtual threads. We'll look at how
 * virtual threads mount and unmount from carrier platform threads.
 */
public class ContinuationsDemo {
    
    private static final int PLATFORM_THREADS = Runtime.getRuntime().availableProcessors();
    private static final int VIRTUAL_THREADS = 1000;
    private static final AtomicInteger completedTasks = new AtomicInteger(0);
    
    public static void main(String[] args) {
        System.out.println("Java Continuations and Carrier Threads Demo");
        System.out.println("------------------------------------------");
        System.out.println("This demonstration shows how virtual threads can exceed");
        System.out.println("the number of platform threads through continuations.");
        System.out.println();
        
        System.out.println("Available processors: " + PLATFORM_THREADS);
        System.out.println("Virtual threads to create: " + VIRTUAL_THREADS);
        System.out.println();
        
        // Demo 1: Show thread jumping
        demoThreadJumping();
        
        // Demo 2: Show unmounting during IO operations
        demoUnmountingDuringIO();
        
        // Demo 3: Scale test - many virtual threads on few carriers
        demoManyVirtualThreads();
    }
    
    private static void demoThreadJumping() {
        System.out.println("Demo 1: Thread Jumping");
        System.out.println("---------------------");
        System.out.println("Virtual threads may 'jump' between carrier platform threads");
        System.out.println("when they are unmounted and mounted again.");
        System.out.println();
        
        List<Thread> threads = new ArrayList<>();
        
        // Create 10 virtual threads that will show their carrier
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            Thread virtualThread = Thread.ofVirtual().unstarted(() -> {
                // First print
                if (threadId == 0) {
                    System.out.println("  Thread #" + threadId + " BEFORE blocking: " + Thread.currentThread());
                }
                
                // Blocking operation triggers unmounting
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Second print after potential carrier thread change
                if (threadId == 0) {
                    System.out.println("  Thread #" + threadId + " AFTER blocking: " + Thread.currentThread());
                    System.out.println("  (Notice the carrier thread may have changed)");
                }
            });
            
            threads.add(virtualThread);
        }
        
        // Start all threads
        threads.forEach(Thread::start);
        
        // Wait for all to complete
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        System.out.println();
    }
    
    private static void demoUnmountingDuringIO() {
        System.out.println("Demo 2: Unmounting During IO Operations");
        System.out.println("--------------------------------------");
        System.out.println("When a virtual thread performs a blocking operation like I/O");
        System.out.println("or sleep, it unmounts from its carrier thread to free system resources.");
        System.out.println();
        
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // Reset counter
            completedTasks.set(0);
            
            // Start time
            Instant start = Instant.now();
            
            // Launch 50 tasks that perform blocking operations
            for (int i = 0; i < 50; i++) {
                final int taskId = i;
                virtualExecutor.submit(() -> {
                    System.out.println("  Task #" + taskId + " starting on " + Thread.currentThread());
                    
                    // Simulate IO operation with sleep
                    try {
                        // Different sleep durations to show overlapping execution
                        Thread.sleep(Duration.ofMillis(100 * (taskId % 5 + 1)));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    int completed = completedTasks.incrementAndGet();
                    System.out.println("  Task #" + taskId + " completed " +
                                      "(Total: " + completed + "/50)");
                    return taskId;
                });
            }
            
            // Shutdown and wait for completion
            virtualExecutor.shutdown();
            try {
                virtualExecutor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Duration duration = Duration.between(start, Instant.now());
            System.out.println("  All tasks completed in: " + duration.toMillis() + " ms");
            System.out.println("  (Note: This is much less than the sum of all sleep durations");
            System.out.println("   because the tasks executed concurrently)");
        }
        
        System.out.println();
    }
    
    private static void demoManyVirtualThreads() {
        System.out.println("Demo 3: Many Virtual Threads on Few Carriers");
        System.out.println("------------------------------------------");
        System.out.println("We'll create " + VIRTUAL_THREADS + " virtual threads but they'll");
        System.out.println("run on only " + PLATFORM_THREADS + " platform threads (your CPU cores).");
        System.out.println();
        
        // Reset counter
        completedTasks.set(0);
        
        // Start time
        Instant start = Instant.now();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Submit many virtual thread tasks
            for (int i = 0; i < VIRTUAL_THREADS; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    // Simulate some work with sleep - this will cause unmounting
                    try {
                        Thread.sleep(Duration.ofMillis(50));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    // Track completion
                    int completed = completedTasks.incrementAndGet();
                    
                    // Print progress periodically
                    if (completed % 100 == 0 || completed == VIRTUAL_THREADS) {
                        System.out.println("  Completed " + completed + "/" + VIRTUAL_THREADS + 
                                          " tasks - " + (completed * 100 / VIRTUAL_THREADS) + "%");
                    }
                    
                    return taskId;
                });
            }
            
            // Shutdown and wait for completion
            executor.shutdown();
            try {
                executor.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        Duration duration = Duration.between(start, Instant.now());
        double throughput = (double) VIRTUAL_THREADS / duration.toMillis() * 1000;
        
        System.out.println("\n  Execution summary:");
        System.out.println("  • Total virtual threads: " + VIRTUAL_THREADS);
        System.out.println("  • Platform threads (carriers): " + PLATFORM_THREADS);
        System.out.println("  • Completion time: " + duration.toMillis() + " ms");
        System.out.printf("  • Throughput: %.2f virtual threads/second%n", throughput);
        System.out.println("  • Virtual:Platform thread ratio: " + (VIRTUAL_THREADS / PLATFORM_THREADS) + ":1");
        
        System.out.println("\n  This demonstrates how continuations allow virtual threads to");
        System.out.println("  efficiently share a small number of platform threads, enabling");
        System.out.println("  highly concurrent applications without excessive resource usage.");
    }
}
