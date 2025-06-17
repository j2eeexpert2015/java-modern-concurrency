package com.example.javaconcurrency.virtualthreads.demo;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class ThreadStateDemo {
    private static final Object monitor = new Object();
    private static volatile boolean shouldContinue = true;
    private static Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        System.out.println("=== Thread States Demo for VisualVM ===");
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("Connect VisualVM and go to Threads tab");
        System.out.println("========================================\n");
        
        demonstrateNewState();
        demonstrateRunnableState(); 
        demonstrateMonitorState();
        demonstrateParkState();
        demonstrateWaitState();
        
        System.out.println("Demo Complete!");
        scanner.close();
    }
    
    private static void demonstrateNewState() {
        System.out.println("1. NEW STATE");
        Thread newThread = new Thread(() -> {}, "NEW-THREAD");
        System.out.println("Check: NEW-THREAD should show NEW state");
        waitForInput();
        newThread.start();
        try { newThread.join(); } catch (InterruptedException e) {}
        System.out.println();
    }
    
    private static void demonstrateRunnableState() {
        System.out.println("2. RUNNABLE STATE");
        Thread runnableThread = new Thread(() -> {
            long count = 0;
            while (shouldContinue) {
                count++;
                // More CPU work to keep it busy longer
                if (count % 100000 == 0) {
                    Thread.yield();
                }
            }
        }, "RUNNABLE-THREAD");
        
        runnableThread.start();
        // Give it time to get into runnable state
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        System.out.println("Check: RUNNABLE-THREAD should show RUNNABLE state");
        waitForInput();
        shouldContinue = false;
        try { 
            // Give it time to finish gracefully
            Thread.sleep(100);
            runnableThread.join(1000); // Wait max 1 second
        } catch (InterruptedException e) {}
        shouldContinue = true;
        System.out.println();
    }
    
    private static void demonstrateMonitorState() {
        System.out.println("3. MONITOR STATE (blocked on synchronized)");
        
        Thread lockHolder = new Thread(() -> {
            synchronized (monitor) {
                try { Thread.sleep(30000); } catch (InterruptedException e) {}
            }
        }, "LOCK-HOLDER");
        
        Thread blocked = new Thread(() -> {
            synchronized (monitor) {}
        }, "MONITOR-BLOCKED");
        
        lockHolder.start();
        try { Thread.sleep(200); } catch (InterruptedException e) {}
        blocked.start();
        
        System.out.println("Check: LOCK-HOLDER → 'wait', MONITOR-BLOCKED → 'monitor'");
        waitForInput();
        lockHolder.interrupt();
        try { 
            lockHolder.join(); 
            blocked.join(); 
        } catch (InterruptedException e) {}
        System.out.println();
    }
    
    private static void demonstrateParkState() {
        System.out.println("4. PARK STATE");
        CountDownLatch latch = new CountDownLatch(1);
        
        Thread parkThread = new Thread(() -> {
            try { latch.await(); } catch (InterruptedException e) {}
        }, "PARK-THREAD");
        
        parkThread.start();
        try { Thread.sleep(200); } catch (InterruptedException e) {}
        
        System.out.println("Check: PARK-THREAD should show 'park' state");
        waitForInput();
        latch.countDown();
        try { parkThread.join(); } catch (InterruptedException e) {}
        System.out.println();
    }
    
    private static void demonstrateWaitState() {
        System.out.println("5. WAIT STATE (Thread.sleep)");
        
        Thread sleepThread = new Thread(() -> {
            try { Thread.sleep(30000); } catch (InterruptedException e) {}
        }, "SLEEP-THREAD");
        
        sleepThread.start();
        try { Thread.sleep(200); } catch (InterruptedException e) {}
        
        System.out.println("Check: SLEEP-THREAD should show 'wait' state");
        waitForInput();
        sleepThread.interrupt();
        try { sleepThread.join(); } catch (InterruptedException e) {}
        System.out.println();
    }
    
    private static void waitForInput() {
        System.out.print("Press Enter to continue... ");
        scanner.nextLine();
    }
}