package com.example.javaconcurrency.virtualthreads.creation;


import java.util.concurrent.Executors;

/**
 * Demonstrates creation of multiple Virtual Threads and sleep-based blocking
 * to trigger mount/unmount events. Use JFR to record thread lifecycle.
 *
 * 🔧 VM Arguments (Eclipse or command line):
 * --enable-preview -XX:StartFlightRecording=duration=30s,filename=vt-simple.jfr,settings=profile
 *
 * 📁 Output: vt-simple.jfr (Open with JDK Mission Control)
 *
 * 🔍 Check for: VirtualThreadStart, VirtualThreadEnd, VirtualThreadMount, VirtualThreadUnmount
 */
public class VirtualThreadDemoSimple {

    public static void main(String[] args) throws InterruptedException {
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < 5000; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(200); // Triggers unmount
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        Thread.sleep(2000); // Allow time for completion
    }
}

