package com.example.javaconcurrency.virtualthreads.demo;

import java.util.concurrent.locks.LockSupport;

public class ThreadStateComparisonDemo {

    private static final Object monitor = new Object();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("🧪 Thread State Comparison Demo (PID: " + ProcessHandle.current().pid() + ")");
        System.out.println("Attach VisualVM and switch to Threads → Timeline. Wait 30s before exit.");

        // 1. Sleep thread
        Thread sleepingThread = new Thread(() -> {
            try {
                System.out.println("SleepingThread sleeping for 30s...");
                Thread.sleep(30000); // Try to produce "Sleeping" (🟣)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "SleepingThread");

        // 2. Wait thread (Object.wait)
        Thread waitingThread = new Thread(() -> {
            synchronized (monitor) {
                try {
                    System.out.println("WaitingThread waiting on monitor...");
                    monitor.wait(); // Will show as "Wait" (🟡)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "WaitingThread");

        // 3. Park thread
        Thread parkedThread = new Thread(() -> {
            System.out.println("ParkedThread parked for 30s...");
            LockSupport.parkNanos(30_000_000_000L); // 30 seconds
            System.out.println("ParkedThread resumed");
        }, "ParkedThread");

        // Start all threads
        sleepingThread.start();
        waitingThread.start();
        parkedThread.start();

        // Allow time for threads to reach expected states
        Thread.sleep(1000);

        // Wait long enough to observe all states in VisualVM
        Thread.sleep(35000); // main thread stays alive

        // Notify and unpark to clean up
        synchronized (monitor) {
            monitor.notify();
        }
        LockSupport.unpark(parkedThread);
        sleepingThread.join();
        waitingThread.join();
        parkedThread.join();

        System.out.println("✅ All threads completed. Check timeline colors.");
    }
}

