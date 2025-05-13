package com.example.javaconcurrency.virtualthreads;


import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Section 4: Monitoring Virtual Threads
 * 
 * This demo shows various ways to monitor virtual threads and their performance,
 * including thread pools, memory usage, and the impact of the virtual thread scheduler.
 */
public class VirtualThreadMonitoringDemo {

    private static final int THREAD_COUNT = 10_000;
    private static final AtomicInteger completedTasks = new AtomicInteger(0);
    private static final Set<String> carrierThreadNames = ConcurrentHashMap.newKeySet();
    private static final Set<String> poolNames = ConcurrentHashMap.newKeySet();
    private static final Pattern WORKER_PATTERN = Pattern.compile("worker-\\d+");
    private static final Pattern POOL_PATTERN = Pattern.compile("@ForkJoinPool-\\d+");
    
    public static void main(String[] args) {
        System.out.println("Java Virtual Thread Monitoring Demo");
        System.out.println("---------------------------------");
        System.out.println("This demonstration shows how to monitor virtual threads");
        System.out.println("and observe their behavior in the JVM.");
        System.out.println();
        
        // Demo 1: Observing platform thread usage by virtual threads
        demoCarrierThreadUsage();
        
        // Demo 2: Memory usage comparison
        demoMemoryUsage();
        
        // Demo 3: Virtual thread scheduler monitoring
        demoSchedulerMonitoring();
    }
    
    private static void demoCarrierThreadUsage() {
        System.out.println("Demo 1: Observing Carrier Thread Usage");
        System.out.println("-------------------------------------");
        System.out.println("We'll create " + THREAD_COUNT + " virtual threads and observe how many");
        System.out.println("platform threads (carriers) are used to execute them.");
        System.out.println();
        
        // Clear collections
        carrierThreadNames.clear();
        poolNames.clear();
        
        Instant start = Instant.now();
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    // Extract platform thread information
                    String threadInfo = Thread.currentThread().toString();
                    
                    // Extract carrier thread name
                    Matcher workerMatcher = WORKER_PATTERN.matcher(threadInfo);
                    if (workerMatcher.find()) {
                        carrierThreadNames.add(workerMatcher.group());
                    }
                    
                    // Extract pool name
                    Matcher poolMatcher = POOL_PATTERN.matcher(threadInfo);
                    if (poolMatcher.find()) {
                        poolNames.add(poolMatcher.group());
                    }
                    
                    // Simulate work
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    return null;
                });
            }
            
            executor.shutdown();
            try {
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        Duration duration = Duration.between(start, Instant.now());
        
        // Report results
        System.out.println("Results:");
        System.out.println("  • Total virtual threads: " + THREAD_COUNT);
        System.out.println("  • CPU cores available: " + Runtime.getRuntime().availableProcessors());
        System.out.println("  • Thread pools used: " + poolNames.size());
        System.out.println("  • Platform threads used: " + carrierThreadNames.size());
        System.out.println("  • Execution time: " + duration.toMillis() + " ms");
        System.out.println("  • Virtual:Carrier thread ratio: " + 
                         (THREAD_COUNT / Math.max(1, carrierThreadNames.size())) + ":1");
        
        System.out.println("\n  This demonstrates that a small number of carrier threads");
        System.out.println("  can efficiently execute a large number of virtual threads.");
        
        System.out.println();
    }
    
    private static void demoMemoryUsage() {
        System.out.println("Demo 2: Memory Usage Comparison");
        System.out.println("------------------------------");
        System.out.println("Let's examine the memory overhead of virtual threads vs platform threads.");
        System.out.println();
        
        // Memory usage before creating any threads
        System.gc(); // Request garbage collection
        sleepMillis(200); // Give GC time to run
        
        long initialMemory = getUsedMemory();
        System.out.println("Initial memory usage: " + formatMemory(initialMemory));
        
        // Test with platform threads (limit to a reasonable number)
        int platformThreadCount = Math.min(1000, THREAD_COUNT); // Avoid OOM
        System.out.println("\nCreating " + platformThreadCount + " platform threads...");
        
        Thread[] platformThreads = new Thread[platformThreadCount];
        for (int i = 0; i < platformThreadCount; i++) {
            platformThreads[i] = Thread.ofPlatform().daemon().unstarted(() -> {
                try {
                    Thread.sleep(1000); // Keep thread alive for measurement
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Start platform threads
        for (Thread thread : platformThreads) {
            thread.start();
        }
        
        // Give the JVM a chance to allocate all thread resources
        sleepMillis(500);
        System.gc();
        sleepMillis(200);
        
        long platformMemory = getUsedMemory() - initialMemory;
        double platformThreadOverhead = (double) platformMemory / platformThreadCount;
        
        System.out.println("Platform thread memory usage:");
        System.out.println("  • Total: " + formatMemory(platformMemory));
        System.out.println("  • Per thread: " + formatMemory((long)platformThreadOverhead));
        
        // Wait for platform threads to finish to free memory
        for (Thread thread : platformThreads) {
            try {
                thread.interrupt();
                thread.join(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.gc();
        sleepMillis(500);
        
        // Reset to baseline
        long baselineMemory = getUsedMemory();
        
        // Test with virtual threads
        System.out.println("\nCreating " + THREAD_COUNT + " virtual threads...");
        
        Thread[] virtualThreads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            virtualThreads[i] = Thread.ofVirtual().unstarted(() -> {
                try {
                    Thread.sleep(1000); // Keep thread alive for measurement
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Start virtual threads
        for (Thread thread : virtualThreads) {
            thread.start();
        }
        
        // Give the JVM a chance to allocate all thread resources
        sleepMillis(500);
        System.gc();
        sleepMillis(200);
        
        long virtualMemory = getUsedMemory() - baselineMemory;
        double virtualThreadOverhead = (double) virtualMemory / THREAD_COUNT;
        
        System.out.println("Virtual thread memory usage:");
        System.out.println("  • Total: " + formatMemory(virtualMemory));
        System.out.println("  • Per thread: " + formatMemory((long)virtualThreadOverhead));
        
        // Comparison
        double memoryRatio = platformThreadOverhead / Math.max(1, virtualThreadOverhead);
        System.out.println("\nPlatform threads use approximately " + 
                          String.format("%.1fx", memoryRatio) + 
                          " more memory than virtual threads!");
        
        // Clean up
        for (Thread thread : virtualThreads) {
            try {
                thread.interrupt();
            } catch (Exception ignored) {
                // Ignore
            }
        }
        
        System.out.println();
    }
    
    private static void demoSchedulerMonitoring() {
        System.out.println("Demo 3: Virtual Thread Scheduler Monitoring");
        System.out.println("------------------------------------------");
        System.out.println("The virtual thread scheduler can be configured with JVM flags:");
        System.out.println("  • jdk.virtualThreadScheduler.parallelism: Number of carrier threads");
        System.out.println("  • jdk.virtualThreadScheduler.maxPoolSize: Maximum threads in the pool");
        System.out.println("  • jdk.virtualThreadScheduler.minRunnable: Adaptive parallelism threshold");
        System.out.println();
        
        // Show default scheduler properties
        System.out.println("Current scheduler configuration (from system properties):");
        String parallelism = System.getProperty("jdk.virtualThreadScheduler.parallelism", 
                            String.valueOf(Runtime.getRuntime().availableProcessors()));
        String maxPoolSize = System.getProperty("jdk.virtualThreadScheduler.maxPoolSize", 
                            String.valueOf(256));
        String minRunnable = System.getProperty("jdk.virtualThreadScheduler.minRunnable", 
                            String.valueOf(1));
        
        System.out.println("  • Parallelism: " + parallelism);
        System.out.println("  • Max pool size: " + maxPoolSize);
        System.out.println("  • Min runnable: " + minRunnable);
        
        System.out.println("\nTo modify these values, use JVM flags when starting the application:");
        System.out.println("java -Djdk.virtualThreadScheduler.parallelism=4 -Djdk.virtualThreadScheduler.maxPoolSize=16 ...");
        
        // Demonstrate thread count monitoring
        System.out.println("\nMonitoring active thread counts:");
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        
        // Initial thread count
        int initialThreadCount = threadMXBean.getThreadCount();
        System.out.println("  Initial platform thread count: " + initialThreadCount);
        
        // Create virtual threads and monitor count
        System.out.println("\nCreating and running 1000 virtual threads...");
        
        Instant start = Instant.now();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 1000; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(Duration.ofMillis(100));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                });
            }
            
            // Sample thread counts during execution
            for (int i = 0; i < 5; i++) {
                sleepMillis(100);
                int currentCount = threadMXBean.getThreadCount();
                int deltaFromInitial = currentCount - initialThreadCount;
                System.out.println("  Platform threads: " + currentCount + 
                                  " (+" + deltaFromInitial + " from initial)");
            }
            
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("  Final platform thread count: " + threadMXBean.getThreadCount());
        System.out.println("  Execution time: " + Duration.between(start, Instant.now()).toMillis() + " ms");
        
        System.out.println("\nObservation: The platform thread count increased by only a small amount");
        System.out.println("despite running 1000 virtual threads. This demonstrates the efficiency of");
        System.out.println("the virtual thread scheduler in reusing a small pool of carrier threads.");
        
        System.out.println("\nFor more detailed monitoring, consider using JFR (Java Flight Recorder)");
        System.out.println("and JDK-specific flags like -Djdk.tracePinnedThreads to detect pinning events.");
    }
    
    // Helper methods
    
    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    private static String formatMemory(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
