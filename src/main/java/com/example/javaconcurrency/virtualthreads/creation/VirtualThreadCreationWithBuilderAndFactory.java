package com.example.javaconcurrency.virtualthreads.creation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

public class VirtualThreadCreationWithBuilderAndFactory {

    public static void main(String[] args) throws InterruptedException {

        // 1. Directly started virtual thread with builder
        Thread startedThread = Thread.ofVirtual()
            .name("started-thread")
            .start(() -> {
                System.out.println("  Started thread is running: " + Thread.currentThread());
                sleepMillis(100);
            });

        startedThread.join();

        // 2. Unstarted virtual thread with builder
        Thread unstartedThread = Thread.ofVirtual()
            .name("unstarted-thread")
            .unstarted(() -> {
                System.out.println("  Unstarted thread started manually: " + Thread.currentThread());
                sleepMillis(100);
            });

        System.out.println("  Created unstarted thread: " + unstartedThread.getName());
        System.out.println("  Thread state before start: " + unstartedThread.getState());

        unstartedThread.start();
        unstartedThread.join();

        // 3. Virtual threads using ThreadFactory
        ThreadFactory virtualThreadFactory = Thread.ofVirtual()
            .name("factory-thread-", 0)
            .factory();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Thread t = virtualThreadFactory.newThread(() ->
                System.out.println("  Factory-created thread running in: " + Thread.currentThread()));
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("\nAll virtual threads completed.");
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
