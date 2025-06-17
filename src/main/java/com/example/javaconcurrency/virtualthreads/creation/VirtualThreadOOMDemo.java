package com.example.javaconcurrency.virtualthreads.creation;


import java.util.ArrayList;
import java.util.List;

/*
 * Demonstrates that creating too many platform threads may lead to
 * OutOfMemoryError: "unable to create native thread" under tight memory conditions.
 * Virtual threads handle large numbers efficiently.
 *
 * To simulate memory constraints only, use:
 *   -Xmx256m -Xms256m
 *
 * To generate a flame graph using Java Flight Recorder (JFR), use:
 *   -Xmx256m -Xms256m -XX:StartFlightRecording=filename=thread-test.jfr,duration=60s,settings=profile
 *
 * After running with JFR enabled, open `thread-test.jfr` in Java Mission Control (JMC)
 * and inspect Flame Graph / Method Profiling views to visualize thread behavior.
 */

public class VirtualThreadOOMDemo {

    // Change this value to test how many threads your system can handle
    private static final int THREAD_COUNT = 300_000;

    public static void main(String[] args) throws InterruptedException {
        measureVirtualThreads(); // Typically succeeds even with high thread counts
    }

    /* Creates and joins many platform threads */
    private static void measurePlatformThreads() throws InterruptedException {
        long startTime = System.nanoTime();
        List<Thread> threads = new ArrayList<>(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread t = Thread.ofPlatform().unstarted(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        long elapsed = System.nanoTime() - startTime;
        System.out.println("✅ Platform Threads completed in " + (elapsed / 1_000_000) / 1000.0 + " seconds");
    }

    /* Creates and joins many virtual threads */
    private static void measureVirtualThreads() throws InterruptedException {
        long startTime = System.nanoTime();
        List<Thread> threads = new ArrayList<>(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread t = Thread.ofVirtual().unstarted(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        long elapsed = System.nanoTime() - startTime;
        System.out.println("✅ Virtual Threads completed in " + (elapsed / 1_000_000) / 1000.0 + " seconds");
    }
}
