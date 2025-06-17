package com.example.javaconcurrency.virtualthreads.demo;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Demonstrates Virtual Thread Mount/Unmount using JFR with a custom config.
 *
 * Run with:
 * --enable-preview -XX:StartFlightRecording=filename=vt-mount-unmount.jfr,settings=virtual-thread.jfc,duration=30s
 */
public class VirtualThreadMountUnmountDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Virtual Thread Mount/Unmount JFR Demo ===");
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("Attach JMC or wait for JFR to finish...\n");

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    System.out.println("➡️ Started: " + Thread.currentThread());
                    // Triggers unmount/mount
                    Thread.sleep(Duration.ofSeconds(1));
                    System.out.println("⬅️ Resumed: " + Thread.currentThread());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        Thread.sleep(5000); // Ensure recording continues during execution
        System.out.println("✅ Main completed.");
    }
}
