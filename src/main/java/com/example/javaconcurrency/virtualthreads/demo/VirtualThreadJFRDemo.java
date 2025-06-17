package com.example.javaconcurrency.virtualthreads.demo;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates virtual thread scheduling, unmounting, and pinning behavior.
 *
 * To capture JFR events, run with one of the following VM arguments:
 *
 * --enable-preview -XX:StartFlightRecording=duration=60s,filename=vt-demo.jfr,settings=profile
 * --enable-preview -XX:StartFlightRecording=duration=60s,filename=vt-demo.jfr,settings=default
 * --enable-preview -XX:StartFlightRecording=duration=60s,filename=vt-demo.jfr,settings=virtual-threads.jfc
 *
 * Ensure enough work runs during the recording window to generate events.
 */

public class VirtualThreadJFRDemo {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("🔍 Attach JMC or wait for JFR to auto-start. Press ENTER to begin virtual threads...");
        System.in.read();

        var executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                System.out.println("➡️ VT Started: " + Thread.currentThread());
                try {
                    // This causes virtual thread to unmount (blocking simulation)
                    Thread.sleep(2000);

                    // This causes the thread to become pinned (due to monitor lock)
                    synchronized (VirtualThreadJFRDemo.class) {
                        Thread.sleep(500);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("⬅️ VT Completed: " + Thread.currentThread());
            });
        }

        // Gracefully shutdown executor and wait for tasks to finish
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        // Keep JVM alive to allow JFR recording to complete
        System.out.println("🕒 Waiting to ensure JFR events are flushed...");
        Thread.sleep(20000);

        System.out.println("✅ Program complete. You can now open vt-demo.jfr in JMC.");
    }
}
