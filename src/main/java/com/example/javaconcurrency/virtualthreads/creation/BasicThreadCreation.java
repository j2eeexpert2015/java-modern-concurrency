package com.example.javaconcurrency.virtualthreads.creation;

/**
 * This class demonstrates all ways to create both platform and virtual threads,
 * showing the evolution from legacy approaches to modern Java 21+ styles.
 * 
 * Key concepts covered:
 * 1. Legacy thread creation (pre-Java 21)
 * 2. Modern platform thread creation (Java 21+)
 * 3. Virtual thread creation (Java 21+)
 * 4. Thread customization (naming, daemon status)
 */
public class BasicThreadCreation {
    public static void main(String[] args) throws InterruptedException {
        // Shared task for all thread types
        Runnable task = () -> {
            System.out.println("Executing in: " + Thread.currentThread());
            try {
                Thread.sleep(100); // Simulate work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        /*
         * 1. LEGACY PLATFORM THREAD CREATION (Pre-Java 21)
         */
        Thread legacyThread = new Thread(task, "legacy-platform-thread");
        legacyThread.start();
        System.out.println("\n[Legacy] Started platform thread: " + legacyThread.getName());
        
       
        /*
         * 2. MODERN PLATFORM THREAD CREATION (Java 21+) - Using Builder Pattern
         */
        Thread modernPlatformThread = Thread.ofPlatform()
            .name("modern-platform-thread")
            .daemon(false)  // Configured during creation
            .unstarted(task); // Create without starting

        modernPlatformThread.start();
        System.out.println("[Modern] Started platform thread: " + modernPlatformThread.getName());

        /*
         * 3. VIRTUAL THREAD CREATION (Java 21+)
         */
       
        // Method 1: Quick start
        Thread virtualThread1 = Thread.startVirtualThread(task);
        System.out.println("\n[Virtual] Started unnamed virtual thread");

        // Method 2: Configured creation - Using Builder Pattern
        Thread virtualThread2 = Thread.ofVirtual() 
            .name("configured-virtual-thread")
            .unstarted(task);

        virtualThread2.start();
        System.out.println("[Virtual] Started named virtual thread: " + virtualThread2.getName());

        /*
         * 4. THREAD PROPERTIES VERIFICATION
         */
        System.out.println("\n=== Thread Properties ===");
        System.out.println("Legacy thread is virtual? " + legacyThread.isVirtual());
        System.out.println("Modern platform thread is virtual? " + modernPlatformThread.isVirtual());
        System.out.println("Virtual thread 2 is virtual? " + virtualThread2.isVirtual());

        // Wait for all threads to complete
        legacyThread.join();
        modernPlatformThread.join();
        virtualThread1.join();
        virtualThread2.join();

        System.out.println("\n All threads completed");
    }
}