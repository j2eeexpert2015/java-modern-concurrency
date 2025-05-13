package com.example.javaconcurrency.structured;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This example demonstrates a hybrid executor service that combines
 * virtual threads for I/O-bound tasks and platform threads for CPU-bound tasks.
 */
public class HybridExecutorDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("Hybrid Executor Service Demo");
        System.out.println("==========================");
        
        // Create two different executor services
        int cpuCores = Runtime.getRuntime().availableProcessors();
        ExecutorService cpuBoundExecutor = Executors.newFixedThreadPool(cpuCores);
        ExecutorService ioBoundExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        try {
            // Run a mixed workload
            runMixedWorkload(cpuBoundExecutor, ioBoundExecutor);
            
            // Compare with all-platform-thread approach
            runTraditionalApproach();
            
        } finally {
            // Shutdown executors
            cpuBoundExecutor.shutdown();
            ioBoundExecutor.shutdown();
        }
    }
    
    /**
     * Run a workload with a mix of I/O-bound and CPU-bound tasks.
     */
    private static void runMixedWorkload(
            ExecutorService cpuBoundExecutor, 
            ExecutorService ioBoundExecutor) throws Exception {
        
        System.out.println("\nRunning with hybrid executor approach:");
        
        int taskCount = 100;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger activeIoTasks = new AtomicInteger(0);
        AtomicInteger activeCpuTasks = new AtomicInteger(0);
        
        Instant start = Instant.now();
        
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            
            // Use virtual threads for I/O-bound tasks
            ioBoundExecutor.submit(() -> {
                try {
                    int active = activeIoTasks.incrementAndGet();
                    System.out.printf("I/O task %d started on %s (active: %d)%n", 
                                     taskId, Thread.currentThread(), active);
                    
                    // Simulate I/O operation (e.g., database query, API call)
                    Thread.sleep(Duration.ofMillis(100));
                    
                    // Now submit CPU-intensive work to the CPU-bound executor
                    cpuBoundExecutor.submit(() -> {
                        try {
                            int cpuActive = activeCpuTasks.incrementAndGet();
                            System.out.printf("CPU task %d started on %s (active: %d)%n", 
                                           taskId, Thread.currentThread(), cpuActive);
                            
                            // Simulate CPU-intensive computation
                            // Calculate the result and store in a final variable
                            final long computationResult = performComputation();
                            
                            // Final I/O operation to "save" the result
                            ioBoundExecutor.submit(() -> {
                                try {
                                    // Simulate writing results back
                                    Thread.sleep(Duration.ofMillis(50));
                                    System.out.printf("Task %d completed with result: %d%n", 
                                                    taskId, computationResult);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                } finally {
                                    activeCpuTasks.decrementAndGet();
                                    latch.countDown();
                                }
                            });
                            
                        } catch (Exception e) {
                            e.printStackTrace();
                            latch.countDown();
                        }
                    });
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    latch.countDown();
                } finally {
                    activeIoTasks.decrementAndGet();
                }
            });
        }
        
        // Wait for all tasks to complete
        latch.await();
        Duration duration = Duration.between(start, Instant.now());
        System.out.println("Hybrid approach completed in: " + duration.toMillis() + "ms");
    }
    
    /**
     * Perform CPU-intensive computation
     */
    private static long performComputation() {
        long result = 0;
        for (int j = 0; j < 10_000_000; j++) {
            result += j;
        }
        return result;
    }
    
    /**
     * Run the same workload using only a fixed thread pool.
     */
    private static void runTraditionalApproach() throws Exception {
        System.out.println("\nRunning with traditional thread pool approach:");
        
        int taskCount = 100;
        CountDownLatch latch = new CountDownLatch(taskCount);
        
        // Create a fixed thread pool
        int cpuCores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cpuCores);
        
        Instant start = Instant.now();
        
        try {
            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                
                executor.submit(() -> {
                    try {
                        // Simulate I/O operation
                        Thread.sleep(Duration.ofMillis(100));
                        
                        // Simulate CPU-intensive computation
                        long result = performComputation();
                        
                        // Simulate writing results back
                        Thread.sleep(Duration.ofMillis(50));
                        
                        // Optional: print completion (similar to hybrid approach)
                        System.out.printf("Traditional task %d completed with result: %d%n", 
                                         taskId, result);
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // Wait for all tasks to complete
            latch.await();
            Duration duration = Duration.between(start, Instant.now());
            System.out.println("Traditional approach completed in: " + duration.toMillis() + "ms");
            
        } finally {
            executor.shutdown();
        }
        
        System.out.println("\nKey takeaways:");
        System.out.println("1. Use virtual threads for I/O-bound work");
        System.out.println("2. Use platform threads for CPU-bound work");
        System.out.println("3. A hybrid approach gives the best of both worlds");
        System.out.println("4. Virtual threads excel when there's a lot of blocking I/O");
    }
}