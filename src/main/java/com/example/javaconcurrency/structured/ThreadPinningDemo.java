package com.example.javaconcurrency.structured;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This example demonstrates thread pinning with virtual threads
 * and how to avoid it by using proper synchronization mechanisms.
 */
public class ThreadPinningDemo {

    public static void main(String[] args) throws Exception {
        // Set the number of tasks to run
        int taskCount = 1000;
        
        System.out.println("Thread Pinning Demonstration");
        System.out.println("===========================");
        System.out.println("Running " + taskCount + " tasks with different synchronization methods\n");
        
        // Test different synchronization methods
        runWithSynchronized(taskCount);
        runWithReentrantLock(taskCount);
        
        // Compare the results
        System.out.println("\nKey takeaways:");
        System.out.println("1. Virtual threads get pinned to carrier threads when using synchronized blocks");
        System.out.println("2. This pinning can severely limit concurrency, even with many virtual threads");
        System.out.println("3. Using ReentrantLock allows virtual threads to yield during blocking operations");
        System.out.println("4. Always prefer ReentrantLock over synchronized with virtual threads");
    }
    
    /**
     * Run tasks using synchronized blocks, which cause thread pinning.
     */
    private static void runWithSynchronized(int taskCount) throws Exception {
        System.out.println("Running with synchronized blocks (causes pinning):");
        
        // Shared object for synchronization
        final Object lock = new Object();
        final CountDownLatch latch = new CountDownLatch(taskCount);
        
        Instant start = Instant.now();
        
        // Create and start virtual threads
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            Thread vt = Thread.ofVirtual().start(() -> {
                try {
                    synchronized (lock) {
                        // This operation blocks while holding the monitor lock
                        // causing the virtual thread to be pinned to its carrier thread
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
            threads.add(vt);
        }
        
        // Wait for all tasks to complete
        latch.await();
        
        Duration duration = Duration.between(start, Instant.now());
        System.out.println("Completed in: " + duration.toMillis() + "ms");
    }
    
    /**
     * Run tasks using ReentrantLock, which avoids thread pinning.
     */
    private static void runWithReentrantLock(int taskCount) throws Exception {
        System.out.println("\nRunning with ReentrantLock (avoids pinning):");
        
        // ReentrantLock for synchronization
        final ReentrantLock lock = new ReentrantLock();
        final CountDownLatch latch = new CountDownLatch(taskCount);
        
        Instant start = Instant.now();
        
        // Create and start virtual threads
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            Thread vt = Thread.ofVirtual().start(() -> {
                try {
                    lock.lock();
                    try {
                        // This operation blocks but doesn't cause pinning
                        // The virtual thread can yield while waiting
                        Thread.sleep(10);
                    } finally {
                        lock.unlock();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
            threads.add(vt);
        }
        
        // Wait for all tasks to complete
        latch.await();
        
        Duration duration = Duration.between(start, Instant.now());
        System.out.println("Completed in: " + duration.toMillis() + "ms");
    }
}
