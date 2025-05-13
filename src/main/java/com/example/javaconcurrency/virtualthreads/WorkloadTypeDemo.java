package com.example.javaconcurrency.virtualthreads;


import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 * Section 5: CPU-Bound vs. IO-Bound Workloads
 * 
 * This demo illustrates when virtual threads are beneficial and when they're not.
 * Virtual threads excel at IO-bound workloads but provide little benefit for 
 * CPU-bound tasks.
 */
public class WorkloadTypeDemo {

    // Number of tasks to run
    private static final int TASK_COUNT = 64;
    private static final int IO_TASK_COUNT = 1000;
    
    public static void main(String[] args) throws Exception {
        System.out.println("Java Virtual Threads Workload Type Demo");
        System.out.println("--------------------------------------");
        System.out.println("This demonstration compares virtual and platform threads");
        System.out.println("under different types of workloads.");
        System.out.println();
        
        // Demo 1: CPU-bound workload comparison
        demoCpuBoundWorkload();
        
        // Demo 2: IO-bound workload comparison
        demoIoBoundWorkload();
        
        // Demo 3: Mixed workload performance
        demoMixedWorkload();
    }
    
    private static void demoCpuBoundWorkload() throws Exception {
        System.out.println("Demo 1: CPU-Bound Workload");
        System.out.println("-------------------------");
        System.out.println("For CPU-bound tasks, virtual threads don't provide much advantage");
        System.out.println("because the bottleneck is computational power, not thread-switching.");
        System.out.println();
        
        // Tasks to perform CPU-intensive work (calculate large Fibonacci numbers)
        System.out.println("Running " + TASK_COUNT + " CPU-intensive tasks...");
        System.out.println("Each task calculates the sum of big integers from 0 to 10 million");
        
        // Platform thread test
        System.out.println("\nUsing platform threads (newFixedThreadPool):");
        List<Long> platformTimes = new ArrayList<>();
        
        try (ExecutorService platformExecutor = 
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            
            List<Future<Long>> futures = new ArrayList<>();
            Instant start = Instant.now();
            
            for (int i = 0; i < TASK_COUNT; i++) {
                final int taskId = i;
                Future<Long> future = platformExecutor.submit(() -> {
                    Instant taskStart = Instant.now();
                    
                    // CPU-bound computation (sum a lot of big integers)
                    BigInteger result = IntStream.range(0, 10_000_000)
                            .mapToObj(BigInteger::valueOf)
                            .reduce(BigInteger.ZERO, BigInteger::add);
                    
                    long duration = Duration.between(taskStart, Instant.now()).toMillis();
                    System.out.printf("  Platform task #%02d completed in %d ms%n", taskId, duration);
                    return duration;
                });
                
                futures.add(future);
            }
            
            // Collect the results
            for (Future<Long> future : futures) {
                platformTimes.add(future.get());
            }
            
            Duration totalDuration = Duration.between(start, Instant.now());
            System.out.println("Platform threads total execution time: " + totalDuration.toMillis() + " ms");
        }
        
        // Virtual thread test
        System.out.println("\nUsing virtual threads (newVirtualThreadPerTaskExecutor):");
        List<Long> virtualTimes = new ArrayList<>();
        
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            List<Future<Long>> futures = new ArrayList<>();
            Instant start = Instant.now();
            
            for (int i = 0; i < TASK_COUNT; i++) {
                final int taskId = i;
                Future<Long> future = virtualExecutor.submit(() -> {
                    Instant taskStart = Instant.now();
                    
                    // Same CPU-bound computation
                    BigInteger result = IntStream.range(0, 10_000_000)
                            .mapToObj(BigInteger::valueOf)
                            .reduce(BigInteger.ZERO, BigInteger::add);
                    
                    long duration = Duration.between(taskStart, Instant.now()).toMillis();
                    System.out.printf("  Virtual task #%02d completed in %d ms%n", taskId, duration);
                    return duration;
                });
                
                futures.add(future);
            }
            
            // Collect the results
            for (Future<Long> future : futures) {
                virtualTimes.add(future.get());
            }
            
            Duration totalDuration = Duration.between(start, Instant.now());
            System.out.println("Virtual threads total execution time: " + totalDuration.toMillis() + " ms");
        }
        
        // Calculate averages
        double platformAvg = platformTimes.stream().mapToLong(Long::valueOf).average().orElse(0);
        double virtualAvg = virtualTimes.stream().mapToLong(Long::valueOf).average().orElse(0);
        
        System.out.println("\nCPU-bound workload comparison:");
        System.out.printf("  • Platform thread average task time: %.1f ms%n", platformAvg);
        System.out.printf("  • Virtual thread average task time: %.1f ms%n", virtualAvg);
        System.out.printf("  • Ratio: %.2fx%n", virtualAvg / platformAvg);
        
        System.out.println("\nConclusion: For CPU-bound tasks, virtual threads don't provide");
        System.out.println("a significant performance advantage, and may actually be slightly slower");
        System.out.println("due to the additional scheduling overhead.");
        
        System.out.println();
    }
    
    private static void demoIoBoundWorkload() throws Exception {
        System.out.println("Demo 2: IO-Bound Workload");
        System.out.println("------------------------");
        System.out.println("For IO-bound tasks, virtual threads shine because they can efficiently");
        System.out.println("yield the CPU while waiting for IO operations to complete.");
        System.out.println();
        
        // Tasks to perform IO-bound work (simulate network/disk operations with sleep)
        System.out.println("Running " + IO_TASK_COUNT + " IO-intensive tasks...");
        System.out.println("Each task simulates IO operations with Thread.sleep()");
        
        // Platform thread test - limited to processor count
        System.out.println("\nUsing platform threads (newFixedThreadPool):");
        AtomicLong platformTotalTime = new AtomicLong(0);
        
        Instant platformStart = Instant.now();
        try (ExecutorService platformExecutor = 
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            
            List<Future<Long>> futures = new ArrayList<>();
            
            for (int i = 0; i < IO_TASK_COUNT; i++) {
                Future<Long> future = platformExecutor.submit(() -> {
                    Instant taskStart = Instant.now();
                    
                    // Simulate IO operation
                    Thread.sleep(100);
                    
                    long duration = Duration.between(taskStart, Instant.now()).toMillis();
                    platformTotalTime.addAndGet(duration);
                    return duration;
                });
                
                futures.add(future);
            }
            
            // Wait for completion
            for (Future<Long> future : futures) {
                future.get();
            }
        }
        
        Duration platformDuration = Duration.between(platformStart, Instant.now());
        System.out.println("Platform threads total execution time: " + platformDuration.toMillis() + " ms");
        
        // Virtual thread test
        System.out.println("\nUsing virtual threads (newVirtualThreadPerTaskExecutor):");
        AtomicLong virtualTotalTime = new AtomicLong(0);
        
        Instant virtualStart = Instant.now();
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            List<Future<Long>> futures = new ArrayList<>();
            
            for (int i = 0; i < IO_TASK_COUNT; i++) {
                Future<Long> future = virtualExecutor.submit(() -> {
                    Instant taskStart = Instant.now();
                    
                    // Simulate IO operation
                    Thread.sleep(100);
                    
                    long duration = Duration.between(taskStart, Instant.now()).toMillis();
                    virtualTotalTime.addAndGet(duration);
                    return duration;
                });
                
                futures.add(future);
            }
            
            // Wait for completion
            for (Future<Long> future : futures) {
                future.get();
            }
        }
        
        Duration virtualDuration = Duration.between(virtualStart, Instant.now());
        System.out.println("Virtual threads total execution time: " + virtualDuration.toMillis() + " ms");
        
        // Calculate improvement ratio
        double improvementRatio = (double) platformDuration.toMillis() / virtualDuration.toMillis();
        
        System.out.println("\nIO-bound workload comparison:");
        System.out.println("  • Platform thread execution time: " + platformDuration.toMillis() + " ms");
        System.out.println("  • Virtual thread execution time: " + virtualDuration.toMillis() + " ms");
        System.out.printf("  • Virtual threads were %.2fx faster!%n", improvementRatio);
        
        System.out.println("\nConclusion: For IO-bound tasks, virtual threads provide a dramatic");
        System.out.println("performance improvement by efficiently utilizing CPU resources during IO waits.");
        
        System.out.println();
    }
    
    private static void demoMixedWorkload() throws Exception {
        System.out.println("Demo 3: Mixed Workload Performance");
        System.out.println("---------------------------------");
        System.out.println("In real applications, you'll have a mix of CPU-bound and IO-bound tasks.");
        System.out.println("Let's see how virtual threads perform in a more realistic scenario.");
        System.out.println();
        
        int ioTasks = 200; // IO-bound tasks
        int cpuTasks = 10; // CPU-bound tasks
        
        System.out.println("Running a mixed workload of:");
        System.out.println("  • " + ioTasks + " IO-bound tasks (100ms sleep each)");
        System.out.println("  • " + cpuTasks + " CPU-bound tasks (compute-intensive)");
        
        // Platform thread test
        System.out.println("\nUsing platform threads:");
        Instant platformStart = Instant.now();
        
        try (ExecutorService platformExecutor = 
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            
            List<Future<?>> futures = new ArrayList<>();
            
            // Submit IO-bound tasks
            for (int i = 0; i < ioTasks; i++) {
                futures.add(platformExecutor.submit(() -> {
                    try {
                        Thread.sleep(100); // Simulate IO
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }));
            }
            
            // Submit CPU-bound tasks
            for (int i = 0; i < cpuTasks; i++) {
                final int taskId = i;
                futures.add(platformExecutor.submit(() -> {
                    System.out.println("  Platform CPU task #" + taskId + " starting");
                    // CPU-intensive computation
                    BigInteger result = IntStream.range(0, 5_000_000)
                            .mapToObj(BigInteger::valueOf)
                            .reduce(BigInteger.ZERO, BigInteger::add);
                    System.out.println("  Platform CPU task #" + taskId + " completed");
                    return null;
                }));
            }
            
            // Wait for all tasks to complete
            for (Future<?> future : futures) {
                future.get();
            }
        }
        
        Duration platformDuration = Duration.between(platformStart, Instant.now());
        System.out.println("Platform threads total execution time: " + platformDuration.toMillis() + " ms");
        
        // Virtual thread test
        System.out.println("\nUsing virtual threads:");
        Instant virtualStart = Instant.now();
        
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            List<Future<?>> futures = new ArrayList<>();
            
            // Submit IO-bound tasks
            for (int i = 0; i < ioTasks; i++) {
                futures.add(virtualExecutor.submit(() -> {
                    try {
                        Thread.sleep(100); // Simulate IO
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }));
            }
            
            // Submit CPU-bound tasks
            for (int i = 0; i < cpuTasks; i++) {
                final int taskId = i;
                futures.add(virtualExecutor.submit(() -> {
                    System.out.println("  Virtual CPU task #" + taskId + " starting");
                    // CPU-intensive computation
                    BigInteger result = IntStream.range(0, 5_000_000)
                            .mapToObj(BigInteger::valueOf)
                            .reduce(BigInteger.ZERO, BigInteger::add);
                    System.out.println("  Virtual CPU task #" + taskId + " completed");
                    return null;
                }));
            }
            
            // Wait for all tasks to complete
            for (Future<?> future : futures) {
                future.get();
            }
        }
        
        Duration virtualDuration = Duration.between(virtualStart, Instant.now());
        System.out.println("Virtual threads total execution time: " + virtualDuration.toMillis() + " ms");
        
        // Calculate improvement
        double improvementRatio = (double) platformDuration.toMillis() / virtualDuration.toMillis();
        
        System.out.println("\nMixed workload comparison:");
        System.out.println("  • Platform thread execution time: " + platformDuration.toMillis() + " ms");
        System.out.println("  • Virtual thread execution time: " + virtualDuration.toMillis() + " ms");
        System.out.printf("  • Virtual threads were %.2fx %s!%n", 
                         Math.abs(improvementRatio), 
                         improvementRatio > 1 ? "faster" : "slower");
        
        System.out.println("\nConclusion:");
        System.out.println("  • Virtual threads excel at IO-bound tasks due to efficient unmounting");
        System.out.println("  • For CPU-bound tasks, the benefit is minimal or even negative");
        System.out.println("  • In mixed workloads, the overall benefit depends on the IO-to-CPU ratio");
        System.out.println("  • Applications with high IO-to-CPU ratios benefit most from virtual threads");
    }
}