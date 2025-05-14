package com.example.javaconcurrency.virtualthreads.creation;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Java Virtual Threads & Structured Concurrency: The Complete Guide
 * 
 * This comprehensive program demonstrates the full lifecycle and capabilities of 
 * Java Virtual Threads, organized as a series of modules that can be taught
 * separately or as a complete course.
 */
public class VirtualThreadsMasterClass {

    // Configuration constants
    private static final int THREAD_COUNT = 10_000;
    private static final int TASK_DURATION_MS = 10;
    private static final AtomicInteger counter = new AtomicInteger();
    
    public static void main(String[] args) throws Exception {
        printHeader("JAVA VIRTUAL THREADS MASTERCLASS");
        printEnvironmentInfo();
        
        // Module 1: Basic Virtual Thread Creation
        moduleBasicThreadCreation();
        
        // Module 2: Thread Builder API
        moduleThreadBuilderAPI();
        
        // Module 3: ExecutorService with Virtual Threads
        moduleExecutorService();
        
        // Module 4: Thread Characteristics & Debugging
        moduleThreadCharacteristics();
        
        // Module 5: Performance Comparison
        modulePerformanceComparison();
        
        // Module 6: Structured Concurrency (Preview)
        moduleStructuredConcurrency();
        
        // Module 7: Real-world Patterns
        moduleRealWorldPatterns();
        
        System.out.println("\nCourse completed successfully!");
    }

    /**
     * MODULE 1: Basic Virtual Thread Creation
     * 
     * This module covers the fundamental ways to create virtual threads
     * and compares them with traditional platform threads.
     */
    private static void moduleBasicThreadCreation() throws Exception {
        printModuleHeader("MODULE 1: BASIC VIRTUAL THREAD CREATION");
        
        // LESSON 1.1: Creating a basic platform thread
        System.out.println("\nLesson 1.1: Creating a basic platform thread");
        System.out.println("------------------------------------------");
        
        // Traditional way to create a platform thread
        Thread platformThread = new Thread(() -> {
            System.out.println("  Executing in platform thread: " + Thread.currentThread());
        });
        platformThread.start();
        platformThread.join();
        System.out.println("  Platform thread is virtual: " + platformThread.isVirtual());
        
        // LESSON 1.2: Creating a basic virtual thread
        System.out.println("\nLesson 1.2: Creating a basic virtual thread");
        System.out.println("------------------------------------------");
        
        // The simplest way to create and start a virtual thread
        Thread virtualThread = Thread.startVirtualThread(() -> {
            System.out.println("  Executing in virtual thread: " + Thread.currentThread());
        });
        virtualThread.join();
        System.out.println("  Virtual thread is virtual: " + virtualThread.isVirtual());
        
        // LESSON 1.3: Virtual thread naming and inheritance
        System.out.println("\nLesson 1.3: Virtual thread naming and inheritance");
        System.out.println("-----------------------------------------------");
        
        // Creating a virtual thread with a specified name
        Thread namedVirtualThread = Thread.ofVirtual()
                .name("my-custom-vthread")
                .start(() -> {
                    System.out.println("  Thread name: " + Thread.currentThread().getName());
                    
                    // Demonstrate thread inheritance by creating a child thread
                    Thread childThread = Thread.startVirtualThread(() -> {
                        System.out.println("  Child thread: " + Thread.currentThread().getName());
                    });
                    
                    try {
                        childThread.join();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
        
        namedVirtualThread.join();
    }

    /**
     * MODULE 2: Thread Builder API
     * 
     * This module explores the Thread.Builder API for more advanced
     * thread configuration options.
     */
    private static void moduleThreadBuilderAPI() throws Exception {
        printModuleHeader("MODULE 2: THREAD BUILDER API");
        
        // LESSON 2.1: Thread.ofVirtual() options
        System.out.println("\nLesson 2.1: Thread.ofVirtual() options");
        System.out.println("-------------------------------------");
        
        // Creating an unstarted virtual thread
        Thread unstartedThread = Thread.ofVirtual()
                .name("unstarted-thread")
                .unstarted(() -> {
                    System.out.println("  Running in unstarted thread after manual start");
                });
        
        System.out.println("  Created unstarted thread: " + unstartedThread);
        System.out.println("  Thread state before start: " + unstartedThread.getState());
        
        // Manually start the thread when ready
        unstartedThread.start();
        unstartedThread.join();
        
        // LESSON 2.2: Thread Factory
        System.out.println("\nLesson 2.2: Thread Factory");
        System.out.println("-------------------------");
        
        // Creating a ThreadFactory for virtual threads with custom naming
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
                .name("factory-vthread-", 0) // Base name with counter
                .factory();
        
        List<Thread> factoryThreads = new ArrayList<>();
        
        // Create multiple threads using the factory
        for (int i = 0; i < 5; i++) {
            Thread t = virtualThreadFactory.newThread(() -> {
                System.out.println("  Factory-created thread: " + Thread.currentThread());
            });
            factoryThreads.add(t);
            t.start();
        }
        
        // Wait for all factory-created threads to complete
        for (Thread t : factoryThreads) {
            t.join();
        }
        
        // LESSON 2.3: Thread InheritableThreadLocal
        System.out.println("\nLesson 2.3: InheritableThreadLocal with virtual threads");
        System.out.println("---------------------------------------------------");
        
        // Create an InheritableThreadLocal variable
        InheritableThreadLocal<String> inheritableContext = new InheritableThreadLocal<>();
        inheritableContext.set("PARENT_CONTEXT_VALUE");
        
        // Demonstrate context inheritance in virtual threads
        Thread parentThread = Thread.startVirtualThread(() -> {
            // Access the inherited value
            String value = inheritableContext.get();
            System.out.println("  Parent thread got inherited value: " + value);
            
            // Child thread should inherit this value
            Thread childThread = Thread.startVirtualThread(() -> {
                String childValue = inheritableContext.get();
                System.out.println("  Child thread got inherited value: " + childValue);
            });
            
            try {
                childThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        parentThread.join();
    }

    /**
     * MODULE 3: ExecutorService with Virtual Threads
     * 
     * This module demonstrates how to use virtual threads with 
     * ExecutorService for task management.
     */
    private static void moduleExecutorService() throws Exception {
        printModuleHeader("MODULE 3: EXECUTORSERVICE WITH VIRTUAL THREADS");
        
        // LESSON 3.1: Traditional thread pool vs. virtual thread per task
        System.out.println("\nLesson 3.1: Traditional thread pool vs. virtual thread per task");
        System.out.println("-----------------------------------------------------------");
        
        // Reset the counter for this module
        counter.set(0);
        
        // Traditional fixed thread pool with platform threads
        System.out.println("  Traditional thread pool (platform threads):");
        try (ExecutorService platformExecutor = 
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            
            // Submit several tasks to the platform thread pool
            for (int i = 0; i < 5; i++) {
                platformExecutor.submit(() -> {
                    int taskId = counter.incrementAndGet();
                    System.out.println("    Task " + taskId + " running in: " + Thread.currentThread());
                    sleepMillis(100); // Simulate work
                    return taskId;
                });
            }
            
            // Allow tasks to complete
            sleepMillis(500);
        } // Executor is closed automatically
        
        // Reset the counter
        counter.set(0);
        
        // Virtual thread per task executor
        System.out.println("\n  Virtual thread per task executor:");
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // Submit the same number of tasks
            for (int i = 0; i < 5; i++) {
                virtualExecutor.submit(() -> {
                    int taskId = counter.incrementAndGet();
                    System.out.println("    Task " + taskId + " running in: " + Thread.currentThread());
                    sleepMillis(100); // Simulate work
                    return taskId;
                });
            }
            
            // Allow tasks to complete
            sleepMillis(500);
        } // Executor is closed automatically
        
        // LESSON 3.2: Handling many concurrent tasks
        System.out.println("\nLesson 3.2: Handling many concurrent tasks");
        System.out.println("----------------------------------------");
        
        // Define the number of concurrent tasks
        final int NUM_TASKS = 100;
        
        System.out.println("  Executing " + NUM_TASKS + " concurrent tasks...");
        
        Instant start = Instant.now();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Create a list to hold all the submitted tasks
            List<Runnable> tasks = new ArrayList<>();
            
            for (int i = 0; i < NUM_TASKS; i++) {
                final int taskId = i;
                tasks.add(() -> {
                    // Simulate a task that does some work and may block
                    sleepMillis(50); // Simulate I/O or blocking
                    if (taskId % 20 == 0) {
                        System.out.println("    Completed task " + taskId);
                    }
                });
            }
            
            // Submit all tasks at once
            tasks.forEach(executor::submit);
            
            // The executor will wait for tasks to complete when it's closed
        } // Executor is closed automatically
        
        Duration duration = Duration.between(start, Instant.now());
        System.out.println("  All " + NUM_TASKS + " tasks completed in " + duration.toMillis() + " ms");
    }

    /**
     * MODULE 4: Thread Characteristics & Debugging
     * 
     * This module explores the characteristics of virtual threads
     * and demonstrates debugging techniques.
     */
    private static void moduleThreadCharacteristics() throws Exception {
        printModuleHeader("MODULE 4: THREAD CHARACTERISTICS & DEBUGGING");
        
        // LESSON 4.1: Examining thread properties
        System.out.println("\nLesson 4.1: Examining thread properties");
        System.out.println("-------------------------------------");
        
        // Create threads for comparison
        Thread platformThread = new Thread(() -> {
            sleepMillis(10);
        });
        
        Thread virtualThread = Thread.startVirtualThread(() -> {
            sleepMillis(10);
        });
        
        // Start the platform thread
        platformThread.start();
        
        // Wait for threads to complete
        platformThread.join();
        virtualThread.join();
        
        // Compare thread properties
        System.out.println("  Platform Thread Properties:");
        System.out.println("    - Is Virtual: " + platformThread.isVirtual());
        System.out.println("    - Thread ID: " + platformThread.threadId());
        System.out.println("    - Thread Name: " + platformThread.getName());
        System.out.println("    - Thread Priority: " + platformThread.getPriority());
        System.out.println("    - Thread State: " + platformThread.getState());
        
        System.out.println("\n  Virtual Thread Properties:");
        System.out.println("    - Is Virtual: " + virtualThread.isVirtual());
        System.out.println("    - Thread ID: " + virtualThread.threadId());
        System.out.println("    - Thread Name: " + virtualThread.getName());
        System.out.println("    - Thread Priority: " + virtualThread.getPriority());
        System.out.println("    - Thread State: " + virtualThread.getState());
        
        // LESSON 4.2: Thread stack traces and debugging
        System.out.println("\nLesson 4.2: Thread stack traces and debugging");
        System.out.println("--------------------------------------------");
        
        // Create a virtual thread that we'll examine
        Thread debugThread = Thread.ofVirtual().name("debug-thread").start(() -> {
            // Call some methods to create a call stack
            methodA();
        });
        
        // Give the thread time to reach the sleep point
        sleepMillis(100);
        
        // Get and print the stack trace
        System.out.println("  Stack Trace for virtual thread:");
        StackTraceElement[] stackTrace = debugThread.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            System.out.println("    " + element);
        }
        
        // Wait for the debug thread to finish
        debugThread.join();
        
        // LESSON 4.3: Thread pinning awareness
        System.out.println("\nLesson 4.3: Thread pinning awareness");
        System.out.println("-----------------------------------");
        
        System.out.println("  Virtual threads can be 'pinned' to carrier threads when");
        System.out.println("  executing synchronized blocks on objects that aren't virtual");
        System.out.println("  thread aware, or when using native methods.");
        
        Thread pinnedThread = Thread.ofVirtual().name("potentially-pinned").start(() -> {
            System.out.println("  Entering code that could cause pinning...");
            
            // Example of code that could cause pinning in real applications
            synchronized (VirtualThreadsMasterClass.class) {
                System.out.println("  Inside synchronized block");
                sleepMillis(100); // Simulate work while holding the lock
            }
            
            System.out.println("  Exited potentially pinning code");
        });
        
        pinnedThread.join();
        
        System.out.println("  Best practice: Avoid synchronized blocks on objects in virtual threads");
        System.out.println("  Use java.util.concurrent locks instead (ReentrantLock, etc.)");
    }
    
    /**
     * Helper methods to create a call stack for demonstration
     */
    private static void methodA() {
        methodB();
    }
    
    private static void methodB() {
        methodC();
    }
    
    private static void methodC() {
        // Sleep to simulate work and to make it easier to capture the stack trace
        sleepMillis(5000);
    }

    /**
     * MODULE 5: Performance Comparison
     * 
     * This module provides a detailed performance comparison between
     * platform threads and virtual threads.
     */
    private static void modulePerformanceComparison() throws Exception {
        printModuleHeader("MODULE 5: PERFORMANCE COMPARISON");
        
        // LESSON 5.1: Throughput comparison
        System.out.println("\nLesson 5.1: Throughput comparison");
        System.out.println("--------------------------------");
        
        System.out.println("  Creating " + THREAD_COUNT + " threads to measure throughput...");
        
        // Measure platform thread throughput
        Instant platformStart = Instant.now();
        try (ExecutorService platformExecutor = 
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            
            IntStream.range(0, THREAD_COUNT).forEach(i -> {
                platformExecutor.submit(() -> {
                    sleepMillis(TASK_DURATION_MS); // Simulate I/O-bound work
                    return i;
                });
            });
            
            // Await termination
            platformExecutor.shutdown();
            platformExecutor.awaitTermination(1, TimeUnit.MINUTES);
        }
        Duration platformDuration = Duration.between(platformStart, Instant.now());
        
        // Measure virtual thread throughput
        Instant virtualStart = Instant.now();
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            IntStream.range(0, THREAD_COUNT).forEach(i -> {
                virtualExecutor.submit(() -> {
                    sleepMillis(TASK_DURATION_MS); // Simulate I/O-bound work
                    return i;
                });
            });
            
            // Await termination
            virtualExecutor.shutdown();
            virtualExecutor.awaitTermination(1, TimeUnit.MINUTES);
        }
        Duration virtualDuration = Duration.between(virtualStart, Instant.now());
        
        // Report results
        System.out.println("  Results:");
        System.out.println("    Platform threads: " + platformDuration.toMillis() + " ms");
        System.out.println("    Virtual threads:  " + virtualDuration.toMillis() + " ms");
        
        double speedup = (double) platformDuration.toMillis() / virtualDuration.toMillis();
        System.out.printf("    Virtual threads were %.2fx faster%n", speedup);
        
        // LESSON 5.2: Memory usage comparison
        System.out.println("\nLesson 5.2: Memory usage comparison");
        System.out.println("---------------------------------");
        
        System.out.println("  Platform threads typically consume 1-2 MB of stack space each,");
        System.out.println("  while virtual threads require only a few KB when not actively running.");
        
        // Force garbage collection to get more accurate memory readings
        System.gc();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // Create many idle virtual threads
        final int IDLE_THREADS = 100_000;
        List<Thread> threads = new ArrayList<>();
        
        for (int i = 0; i < IDLE_THREADS; i++) {
            Thread t = Thread.ofVirtual().unstarted(() -> {
                try {
                    Thread.sleep(60000); // 1 minute sleep
                } catch (InterruptedException e) {
                    // Expected when we interrupt the threads
                }
            });
            threads.add(t);
        }
        
        // Start all threads
        for (Thread t : threads) {
            t.start();
        }
        
        // Let the threads initialize
        sleepMillis(1000);
        
        // Check memory usage
        System.gc();
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryPerThread = (memoryAfter - memoryBefore) / IDLE_THREADS;
        
        System.out.println("  Created " + IDLE_THREADS + " idle virtual threads");
        System.out.println("  Approximate memory usage per thread: " + memoryPerThread + " bytes");
        System.out.println("  Total memory used: " + (memoryAfter - memoryBefore) / 1024 / 1024 + " MB");
        
        // Clean up threads
        for (Thread t : threads) {
            t.interrupt();
        }
    }

    /**
     * MODULE 6: Structured Concurrency
     * 
     * This module provides an introduction to Structured Concurrency,
     * which brings structure and reliability to concurrent code.
     * 
     * Note: This module uses preview features and is therefore commented out.
     * Uncomment and adjust imports when using with JDK 19+ with preview features.
     */
    private static void moduleStructuredConcurrency() throws Exception {
        printModuleHeader("MODULE 6: STRUCTURED CONCURRENCY");
        
        System.out.println("\nThis module demonstrates Structured Concurrency, a preview feature in JDK.");
        System.out.println("To use this feature, you need to run Java with --enable-preview flag.");
        System.out.println("\nStructured Concurrency key principles:");
        System.out.println("1. Child tasks should not outlive their parent scope");
        System.out.println("2. If a child task fails, the parent should fail");
        System.out.println("3. Cancellation should propagate down from parent to children");
        
        // Note: This is commented out since it uses preview features
        // To use this, uncomment and add:
        // import jdk.incubator.concurrent.*;
        
        /*
        System.out.println("\nExample: Parallel data processing with Structured Concurrency");
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Fork two subtasks
            StructuredTaskScope.Subtask<String> task1 = 
                scope.fork(() -> processData("Part 1"));
            
            StructuredTaskScope.Subtask<String> task2 = 
                scope.fork(() -> processData("Part 2"));
            
            // Wait for both tasks to complete or one to fail
            scope.join();
            // Propagate any exceptions
            scope.throwIfFailed();
            
            // Combine the results
            String combined = task1.get() + " & " + task2.get();
            System.out.println("  Combined result: " + combined);
        }
        */
    }
    
    /*
    // Helper method for structured concurrency example
    private static String processData(String input) throws Exception {
        System.out.println("  Processing " + input);
        sleepMillis(100); // Simulate work
        return input + " processed";
    }
    */

    /**
     * MODULE 7: Real-world Patterns
     * 
     * This module explores practical patterns for using virtual threads
     * in real-world applications.
     */
    private static void moduleRealWorldPatterns() throws Exception {
        printModuleHeader("MODULE 7: REAL-WORLD PATTERNS");
        
        // LESSON 7.1: Parallel HTTP requests pattern
        System.out.println("\nLesson 7.1: Parallel HTTP requests pattern");
        System.out.println("-----------------------------------------");
        
        System.out.println("  This demonstrates how to make many HTTP requests in parallel.");
        System.out.println("  Below is how you would implement it (simulated):");
        
        // Simulating a list of URLs to fetch
        List<String> urls = List.of(
            "https://example.com/api/data/1",
            "https://example.com/api/data/2",
            "https://example.com/api/data/3",
            "https://example.com/api/data/4",
            "https://example.com/api/data/5"
        );
        
        // Create a list to hold the results
        List<String> results = new ArrayList<>();
        
        // Using virtual threads to fetch the URLs in parallel
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // Submit all tasks and collect futures
            List<Thread> tasks = new ArrayList<>();
            
            for (String url : urls) {
                Thread task = Thread.ofVirtual().start(() -> {
                    // Simulate HTTP request
                    String result = simulateHttpRequest(url);
                    synchronized (results) {
                        results.add(result);
                    }
                });
                tasks.add(task);
            }
            
            // Wait for all tasks to complete
            for (Thread task : tasks) {
                task.join();
            }
        }
        
        // Display the results
        System.out.println("\n  Results from parallel requests:");
        for (String result : results) {
            System.out.println("    " + result);
        }
        
        // LESSON 7.2: Database connection pool pattern
        System.out.println("\nLesson 7.2: Database connection pool pattern");
        System.out.println("-----------------------------------------");
        
        System.out.println("  With virtual threads, traditional connection pools may be less necessary.");
        System.out.println("  Each virtual thread can have its own connection without exhausting resources.");
        System.out.println("  Example pattern (simulated):");
        
        // Simulate concurrent database operations
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            
            // Submit multiple database operations
            for (int i = 0; i < 5; i++) {
                final int id = i;
                executor.submit(() -> {
                    // Simulate getting a direct connection (no pool needed)
                    String result = simulateDatabaseQuery("SELECT data FROM table WHERE id = " + id);
                    System.out.println("    Query " + id + " result: " + result);
                });
            }
            
            // Wait for completion
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        
        // LESSON 7.3: Virtual thread best practices
        System.out.println("\nLesson 7.3: Virtual thread best practices");
        System.out.println("---------------------------------------");
        
        System.out.println("  1. Use virtual threads for I/O-bound tasks, not CPU-bound work");
        System.out.println("  2. Avoid thread-local variables with high memory footprint");
        System.out.println("  3. Minimize synchronized blocks that could cause pinning");
        System.out.println("  4. Prefer java.util.concurrent locks over synchronized");
        System.out.println("  5. Don't create your own executors - use newVirtualThreadPerTaskExecutor()");
        System.out.println("  6. Structured Concurrency provides better error handling and cleanup");
    }
    
    /**
     * Simulate an HTTP request (for demonstration purposes)
     */
    private static String simulateHttpRequest(String url) {
        // Simulate network latency
        sleepMillis(100 + (int)(Math.random() * 200));
        return "Response from " + url + ": {\"status\": \"success\", \"data\": \"...\" }";
    }
    
    /**
     * Simulate a database query (for demonstration purposes)
     */
    private static String simulateDatabaseQuery(String sql) {
        // Simulate database latency
        sleepMillis(150 + (int)(Math.random() * 100));
        return "{ id: " + sql.hashCode() % 100 + ", name: \"Record " + sql.hashCode() % 10 + "\" }";
    }
    
    /**
     * Print environment information
     */
    private static void printEnvironmentInfo() {
        System.out.println("JDK Version: " + System.getProperty("java.version"));
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Max memory: " + 
                           (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
        System.out.println();
    }
    
    /**
     * Helper method to sleep without checked exceptions
     */
    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Print a formatted module header
     */
    private static void printModuleHeader(String header) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(header);
        System.out.println("=".repeat(80));
    }
    
    /**
     * Print a formatted main header
     */
    private static void printHeader(String header) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(" ".repeat((80 - header.length()) / 2) + header);
        System.out.println("=".repeat(80));
    }
}
