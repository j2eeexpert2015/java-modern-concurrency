package com.example.javaconcurrency.virtualthreads.demo;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

public class SingleThreadStateDemo {
    private static final Object monitor = new Object();
    private static final Object sleepMonitor = new Object();
    private static volatile boolean wakeUpSignal = false;
    private static volatile boolean shouldContinue = true;
    private static CountDownLatch parkLatch = new CountDownLatch(1);
    private static Scanner scanner = new Scanner(System.in);
    private static Thread demoThread;
    
    public static void main(String[] args) {
        System.out.println("=== Single Thread State Demo for VisualVM ===");
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("One thread will go through all states sequentially");
        System.out.println("===============================================\n");
        
        // Step 1: NEW state
        demonstrateNewState();
        
        // Step 2: Start thread and observe other states
        startThreadAndDemonstrateStates();
        
        System.out.println("Demo Complete!");
        scanner.close();
    }
    
    private static void demonstrateNewState() {
        System.out.println("1. NEW STATE");
        System.out.println("Creating thread but not starting it...");
        
        demoThread = new Thread(() -> {
            System.out.println("Thread started - now in RUNNABLE");
            
            // RUNNABLE state - CPU intensive work
            runCpuIntensiveWork();
            
            // WAIT state - Thread.sleep
            sleepForDemo();
            
            // MONITOR state - try to acquire lock (will be blocked)
            tryToAcquireMonitor();
            
            // PARK state - wait on latch
            try {
                waitOnLatch();
            } catch (InterruptedException e) {
                System.out.println("Park was interrupted");
            }
            
            System.out.println("Thread completing...");
        }, "DEMO-THREAD");
        
        System.out.println("Check: DEMO-THREAD should show NEW state");
        waitForInput("Press Enter to start the thread...");
        System.out.println();
    }
    
    private static void startThreadAndDemonstrateStates() {
        // Acquire monitor lock in main thread first
        Thread mainLockHolder = new Thread(() -> {
            synchronized (monitor) {
                try {
                    Thread.sleep(120000); // Hold lock for longer demo
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "MAIN-LOCK-HOLDER");
        mainLockHolder.start();
        
        // Small delay to ensure lock is acquired
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        // Now start our demo thread
        demoThread.start();
        
        // Demo RUNNABLE state
        System.out.println("2. RUNNABLE STATE");
        System.out.println("Check: DEMO-THREAD should show RUNNABLE state (green)");
        waitForInput("Press Enter when observed (thread is doing CPU work)...");
        
        // Signal thread to stop CPU work and move to sleep
        shouldContinue = false;
        try { Thread.sleep(1000); } catch (InterruptedException e) {} // Give more time to transition
        
        System.out.println("3. SLEEPING STATE (Object.wait())");
        System.out.println("Check: DEMO-THREAD should show PURPLE 'Sleeping' state");
        System.out.println("Using Object.wait() instead of Thread.sleep() - should trigger purple!");
        System.out.println("Look for the PURPLE color in the timeline!");
        waitForInput("Press Enter ONLY after you've seen the PURPLE sleeping state...");
        
        // Wake up the waiting thread
        synchronized (sleepMonitor) {
            wakeUpSignal = true;
            sleepMonitor.notify();
        }
        try { Thread.sleep(1000); } catch (InterruptedException e) {} // Give time to transition
        
        System.out.println("4. MONITOR STATE (blocked on synchronized)");
        System.out.println("Check: DEMO-THREAD should show 'Monitor' state (light blue)");
        waitForInput("Press Enter when observed...");
        
        // Release the lock so thread can acquire it and move to PARK state
        mainLockHolder.interrupt();
        try { Thread.sleep(1000); } catch (InterruptedException e) {} // Give time to transition
        
        System.out.println("5. PARK STATE");
        System.out.println("Check: DEMO-THREAD should show 'Park' state (orange)");
        waitForInput("Press Enter when observed...");
        
        // Release the latch to let thread complete
        parkLatch.countDown();
        
        try {
            demoThread.join();
            mainLockHolder.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("6. Thread has completed (TERMINATED state)");
        System.out.println("Java Thread.State: " + demoThread.getState());
        System.out.println();
    }
    
    private static void runCpuIntensiveWork() {
        long count = 0;
        while (shouldContinue) {
            count++;
            if (count % 100000 == 0) {
                Thread.yield();
            }
        }
    }
    
    private static void sleepForDemo() {
        synchronized (sleepMonitor) {
            try {
                System.out.println("Thread entering Object.wait() - should show PURPLE sleeping...");
                while (!wakeUpSignal) {
                    sleepMonitor.wait(); // Use Object.wait() instead of Thread.sleep()
                }
            } catch (InterruptedException e) {
                System.out.println("Object.wait() interrupted, continuing to monitor demo...");
                Thread.interrupted();
            }
        }
    }
    
    private static void tryToAcquireMonitor() {
        synchronized (monitor) {
            // Once we get here, we've acquired the lock
            System.out.println("Acquired monitor lock, moving to park state...");
        }
    }
    
    private static void waitOnLatch() throws InterruptedException {
        try {
            parkLatch.await(); // This will park the thread
        } catch (InterruptedException e) {
            System.out.println("Latch wait interrupted");
            throw e;
        }
    }
    
    private static void waitForInput(String message) {
        System.out.print(message + " ");
        scanner.nextLine();
    }
}