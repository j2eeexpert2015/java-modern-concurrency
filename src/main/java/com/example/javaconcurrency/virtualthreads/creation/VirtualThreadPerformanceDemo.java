package com.example.javaconcurrency.virtualthreads.creation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualThreadPerformanceDemo {

    public static void main(String[] args) {
        int TASK_COUNT = 100_000;

        // Benchmark platform threads
        long platformTime = measurePerformance(
            Executors.newFixedThreadPool(200), TASK_COUNT);

        // Benchmark virtual threads
        long virtualTime = measurePerformance(
            Executors.newVirtualThreadPerTaskExecutor(), TASK_COUNT);

        System.out.printf("Platform threads: %d ms%n", platformTime);
        System.out.printf("Virtual threads:  %d ms%n", virtualTime);
        System.out.printf("Virtual threads were %.2fx faster%n", 
                          (double) platformTime / virtualTime);
    }

    private static long measurePerformance(ExecutorService executor, int taskCount) {
        long start = System.currentTimeMillis();
        try (executor) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
        return System.currentTimeMillis() - start;
    }
}
