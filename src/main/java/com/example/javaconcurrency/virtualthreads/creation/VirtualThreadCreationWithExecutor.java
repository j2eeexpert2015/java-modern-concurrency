package com.example.javaconcurrency.virtualthreads.creation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualThreadCreationWithExecutor {

    public static void main(String[] args) throws InterruptedException {

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Submit multiple tasks
            for (int i = 1; i <= 5; i++) {
                int taskId = i;
                executor.submit(() -> {
                    System.out.println("  Task " + taskId + " running in: " + Thread.currentThread());
                    sleepMillis(100);
                });
            }

            // Executor will auto-close and wait for tasks to finish
        }

        System.out.println("\nAll tasks completed using virtual thread executor.");
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

  
}
