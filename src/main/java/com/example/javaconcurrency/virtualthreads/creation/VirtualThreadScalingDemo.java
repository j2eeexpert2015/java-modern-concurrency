package com.example.javaconcurrency.virtualthreads.creation;


import java.util.ArrayList;
import java.util.List;

public class VirtualThreadScalingDemo {
    private static final int THREAD_COUNT = 10_000;

    public static void main(String[] args) throws Exception {
        System.out.println("Creating " + THREAD_COUNT + " virtual threads...");

        long startCreate = System.currentTimeMillis();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread t = Thread.ofVirtual().unstarted(() -> {
                try {
                    Thread.sleep(1000); // simulate I/O-bound task
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(t);
        }
        long endCreate = System.currentTimeMillis();
        System.out.println("Created in: " + (endCreate - startCreate) + " ms");

        System.out.println("Used heap memory before starting:");
        printMemoryUsage();

        long startRun = System.currentTimeMillis();
        threads.forEach(Thread::start);
        for (Thread t : threads) t.join();
        long endRun = System.currentTimeMillis();

        System.out.println("Used heap memory after completion:");
        printMemoryUsage();

        System.out.println("Virtual thread execution time: " + (endRun - startRun) + " ms");
    }

    private static void printMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        long used = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("Heap used: %d KB (%d MB)%n", used / 1024, used / 1024 / 1024);
    }
}

