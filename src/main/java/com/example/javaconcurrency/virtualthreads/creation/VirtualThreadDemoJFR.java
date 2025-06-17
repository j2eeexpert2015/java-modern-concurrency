package com.example.javaconcurrency.virtualthreads.creation;

import jdk.jfr.Recording;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Demonstrates Virtual Threads with programmatic control of Java Flight Recorder (JFR).
 * Allows attaching JMC first, then starts recording and triggers thread events.
 *
 * 🔧 VM Arguments (Eclipse or command line):
 * --enable-preview
 *
 * 📁 Output: virtual-thread-demo.jfr (Saved in project root)
 *
 * 🔍 Check for: VirtualThreadStart, VirtualThreadEnd, VirtualThreadMount, VirtualThreadUnmount
 */
public class VirtualThreadDemoJFR {

    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("📢 Attach JMC now. Press ENTER to start recording...");
        System.in.read();

        Recording recording = new Recording();
        recording.setName("VirtualThreads");
        recording.start();

        var executor = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                System.out.println("➡️ Thread started: " + Thread.currentThread());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("⬅️ Thread resumed: " + Thread.currentThread());
            });
        }

        executor.shutdown();
        Thread.sleep(3000);

        recording.stop();
        recording.dump(Path.of("virtual-thread-demo.jfr"));
        System.out.println("✅ JFR recording saved to virtual-thread-demo.jfr");
    }
}
