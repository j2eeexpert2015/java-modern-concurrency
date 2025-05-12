package com.example.virtualthreads;

public class ThreadTimingComparison {
    private static final int TASK_COUNT = 100_000; 
    private static final int SLEEP_MS = 1000;

    public static void main(String[] args) {
        System.out.println("Comparing execution time for " + TASK_COUNT + " tasks with " + SLEEP_MS + "ms sleep");

        // Platform Threads
        long ptStart = System.currentTimeMillis();
        try {
            runWithPlatformThreads();
        } catch (Exception e) {
            System.out.println("Platform threads failed: " + e.getMessage());
        }
        long ptEnd = System.currentTimeMillis();
        long platformTime = ptEnd - ptStart;
        System.out.println("Platform threads execution time: " + platformTime + " ms");

        // Brief pause to stabilize system
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Virtual Threads
        long vtStart = System.currentTimeMillis();
        try {
            runWithVirtualThreads();
        } catch (Exception e) {
            System.out.println("Virtual threads failed: " + e.getMessage());
        }
        long vtEnd = System.currentTimeMillis();
        long virtualTime = vtEnd - vtStart;
        System.out.println("Virtual threads execution time: " + virtualTime + " ms");

        // Calculate improvement
        if (virtualTime > 0 && platformTime > 0) {
            double improvement = ((double) (platformTime - virtualTime) / platformTime) * 100;
            System.out.printf("Virtual threads were %.2f%% faster than platform threads%n", improvement);
        }
    }

    private static void runWithPlatformThreads() throws InterruptedException {
        Thread[] threads = new Thread[TASK_COUNT];
        for (int i = 0; i < TASK_COUNT; i++) {
            threads[i] = new Thread(() -> {
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException ignored) {}
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }

    private static void runWithVirtualThreads() throws InterruptedException {
        Thread[] threads = new Thread[TASK_COUNT];
        for (int i = 0; i < TASK_COUNT; i++) {
            threads[i] = Thread.startVirtualThread(() -> {
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException ignored) {}
            });
        }
        for (Thread t : threads) {
            t.join();
        }
    }
}