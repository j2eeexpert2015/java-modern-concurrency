package com.example.javaconcurrency.virtualthreads;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Section 3: Thread Pinning
 * 
 * This demo explains thread pinning, which occurs when a virtual thread
 * cannot unmount from its carrier platform thread. This happens primarily
 * in two cases:
 * 1. When executing synchronized code blocks/methods
 * 2. When calling native methods
 */
public class ThreadPinningDemo {

    private static final Object LOCK = new Object();
    private static final Lock REENTRANT_LOCK = new ReentrantLock();
    private static final AtomicInteger completedTasks = new AtomicInteger(0);
    private static final int TASK_COUNT = 500;
    
    public static void main(String[] args) {
        System.out.println("Java Virtual Thread Pinning Demo");
        System.out.println("-------------------------------");
        System.out.println("This demonstration shows how virtual threads can be 'pinned'");
        System.out.println("to their carrier platform threads, reducing scalability.");
        System.out.println();
        
        // Demo 1: Demonstrating basic pinning behavior
        demoPinningBehavior();
        
        // Demo 2: Comparing performance with and without pinning
        demoPerformanceImpact();
        
        // Demo 3: Preventing pinning with explicit locks
        demoPreventingPinning();
    }
    
    private static void demoPinningBehavior() {
        System.out.println("Demo 1: Basic Pinning Behavior");
        System.out.println("-----------------------------");
        System.out.println("We'll create a virtual thread and observe what happens");
        System.out.println("when it enters synchronized blocks during its execution.");
        System.out.println();
        
        // Thread without pinning
        Thread normalThread = Thread.ofVirtual().unstarted(() -> {
            System.out.println("  Normal thread BEFORE sleep: " + Thread.currentThread());
            
            // Regular sleep operation (no pinning)
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            System.out.println("  Normal thread AFTER sleep: " + Thread.currentThread());
            System.out.println("  (Note: carrier thread may have changed after unmounting)");
        });
        
        // Thread with pinning
        Thread pinnedThread = Thread.ofVirtual().unstarted(() -> {
            System.out.println("\n  Pinned thread BEFORE synchronized block: " + Thread.currentThread());
            
            // Synchronized block causes pinning
            synchronized (LOCK) {
                System.out.println("  Pinned thread INSIDE synchronized block");
                
                try {
                    // Sleep inside synchronized block
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            System.out.println("  Pinned thread AFTER synchronized block: " + Thread.currentThread());
            System.out.println("  (Note: carrier thread remained the same due to pinning)");
        });
        
        normalThread.start();
        
        try {
            normalThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        pinnedThread.start();
        
        try {
            pinnedThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\n  In a real application with JVM flags like -Djdk.tracePinnedThreads=full,");
        System.out.println("  you would see warnings about thread pinning in the console output.");
        
        System.out.println();
    }
    
    private static void demoPerformanceImpact() {
        System.out.println("Demo 2: Performance Impact of Thread Pinning");
        System.out.println("------------------------------------------");
        System.out.println("We'll run " + TASK_COUNT + " tasks in three scenarios:");
        System.out.println("1. Without pinning (normal virtual threads)");
        System.out.println("2. With pinning (synchronized blocks)");
        System.out.println();
        
        // Reset counter
        completedTasks.set(0);
        
        // Scenario 1: No pinning
        System.out.println("Running tasks WITHOUT pinning...");
        Instant normalStart = Instant.now();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < TASK_COUNT; i++) {
                executor.submit(() -> {
                    // Regular sleep (no pinning)
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    completedTasks.incrementAndGet();
                    return null;
                });
            }
            
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Duration normalDuration = Duration.between(normalStart, Instant.now());
        System.out.println("  Completed in: " + normalDuration.toMillis() + " ms");
        
        // Reset counter
        completedTasks.set(0);
        
        // Scenario 2: With pinning
        System.out.println("\nRunning tasks WITH pinning (synchronized blocks)...");
        Instant pinnedStart = Instant.now();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < TASK_COUNT; i++) {
                executor.submit(() -> {
                    // Sleep inside synchronized block (causes pinning)
                    synchronized (LOCK) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    
                    completedTasks.incrementAndGet();
                    return null;
                });
            }
            
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Duration pinnedDuration = Duration.between(pinnedStart, Instant.now());
        System.out.println("  Completed in: " + pinnedDuration.toMillis() + " ms");
        
        // Calculate slowdown
        double slowdown = (double) pinnedDuration.toMillis() / normalDuration.toMillis();
        System.out.printf("\nPinning made execution %.2fx slower!%n", slowdown);
        System.out.println("This demonstrates how thread pinning severely limits concurrent execution.");
        
        System.out.println();
    }
    
    private static void demoPreventingPinning() {
        System.out.println("Demo 3: Preventing Thread Pinning");
        System.out.println("--------------------------------");
        System.out.println("We'll run the same tasks using ReentrantLock instead of synchronized blocks");
        System.out.println("to prevent pinning and restore high concurrency.");
        System.out.println();
        
        // Reset counter
        completedTasks.set(0);
        
        // Using ReentrantLock instead of synchronized
        System.out.println("Running tasks with ReentrantLock (no pinning)...");
        Instant lockStart = Instant.now();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < TASK_COUNT; i++) {
                executor.submit(() -> {
                    // Use ReentrantLock instead of synchronized
                    REENTRANT_LOCK.lock();
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        REENTRANT_LOCK.unlock();
                    }
                    
                    completedTasks.incrementAndGet();
                    return null;
                });
            }
            
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Duration lockDuration = Duration.between(lockStart, Instant.now());
        System.out.println("  Completed in: " + lockDuration.toMillis() + " ms");
        
        System.out.println("\nComparison summary:");
        System.out.println("  • Normal execution: " + normalDuration.toMillis() + " ms");
        System.out.println("  • Pinned execution: " + pinnedDuration.toMillis() + " ms");
        System.out.println("  • Unpinned with ReentrantLock: " + lockDuration.toMillis() + " ms");
        
        System.out.println("\nBest practices to avoid pinning:");
        System.out.println("  1. Use java.util.concurrent locks instead of synchronized");
        System.out.println("  2. Keep synchronized blocks short");
        System.out.println("  3. Don't perform blocking operations inside synchronized blocks");
        System.out.println("  4. Use immutable objects when possible to reduce need for synchronization");
        System.out.println("  5. Consider using VarHandle or Atomic* classes for simple concurrency needs");
    }
    
    private static Duration normalDuration;
    private static Duration pinnedDuration;
}
