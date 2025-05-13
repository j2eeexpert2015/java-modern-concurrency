package com.example.javaconcurrency.structured;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This example shows how to implement controlled parallelism with virtual threads
 * to limit resource consumption for operations like database connections.
 */
public class ControlledParallelismDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("Controlled Parallelism with Virtual Threads");
        System.out.println("=========================================");
        
        // Create a large number of tasks
        int totalTasks = 1000;
        
        // Test with different parallelism limits
        testWithParallelismLimit(totalTasks, 10);
        testWithParallelismLimit(totalTasks, 50);
        testWithParallelismLimit(totalTasks, 200);
        
        System.out.println("\nKey takeaways:");
        System.out.println("1. Even with virtual threads, you may want to limit parallelism");
        System.out.println("2. Use Semaphore to control access to limited resources");
        System.out.println("3. This is useful for database connection pools or API rate limits");
        System.out.println("4. Find the right balance for your specific workload");
    }
    
    /**
     * Test with a specific parallelism limit.
     */
    private static void testWithParallelismLimit(int totalTasks, int maxConcurrent) 
            throws Exception {
        
        System.out.println("\nRunning " + totalTasks + " tasks with max concurrency of " + 
                         maxConcurrent);
        
        // Create a semaphore to limit concurrency
        Semaphore semaphore = new Semaphore(maxConcurrent);
        
        // Track metrics
        AtomicInteger activeTasks = new AtomicInteger(0);
        AtomicInteger maxActiveTasks = new AtomicInteger(0);
        AtomicInteger completedTasks = new AtomicInteger(0);
        ConcurrentHashMap<Integer, Long> taskDurations = new ConcurrentHashMap<>();
        
        Instant start = Instant.now();
        
        // Create and start virtual threads
        List<Thread> threads = new ArrayList<>();
        
        for (int i = 0; i < totalTasks; i++) {
            final int taskId = i;
            
            Thread vt = Thread.ofVirtual().start(() -> {
                Instant taskStart = Instant.now();
                
                try {
                    // Acquire a permit before proceeding
                    semaphore.acquire();
                    
                    try {
                        // Track concurrency
                        int currentActive = activeTasks.incrementAndGet();
                        maxActiveTasks.updateAndGet(max -> Math.max(max, currentActive));
                        
                        // Simulate task work (database query, API call, etc.)
                        Thread.sleep(ThreadLocalRandom.current().nextInt(50, 150));
                        
                    } finally {
                        // Always release the permit and decrement active count
                        activeTasks.decrementAndGet();
                        semaphore.release();
                    }
                    
                    // Record metrics
                    completedTasks.incrementAndGet();
                    long duration = Duration.between(taskStart, Instant.now()).toMillis();
                    taskDurations.put(taskId, duration);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            threads.add(vt);
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Calculate metrics
        Duration totalDuration = Duration.between(start, Instant.now());
        double avgDuration = taskDurations.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        
        // Print results
        System.out.println("Completed in: " + totalDuration.toMillis() + "ms");
        System.out.println("Maximum concurrent tasks: " + maxActiveTasks.get());
        System.out.println("Average task duration: " + avgDuration + "ms");
        System.out.println("Throughput: " + (totalTasks * 1000.0 / totalDuration.toMillis()) + 
                         " tasks/second");
    }
}
