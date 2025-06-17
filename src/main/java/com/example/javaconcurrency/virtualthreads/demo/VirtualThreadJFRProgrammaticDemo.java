package com.example.javaconcurrency.virtualthreads.demo;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Programmatically starts a JFR recording and captures virtual thread events.
 * No need for command-line VM arguments or external .jfc files.
 * Output file: vt-demo.jfr (in working directory)
 *
 * Requires:
 * - Java 21+
 * - Compile and run with --enable-preview
 */
public class VirtualThreadJFRProgrammaticDemo {

    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        System.out.println("🔁 Starting JFR recording...");

        // Use built-in default config and override only needed events
        Recording recording = new Recording(Configuration.getConfiguration("default"));
        recording.setName("VirtualThreadsRecording");
        recording.setDuration(Duration.ofSeconds(30));
        recording.setToDisk(true);
        recording.setDestination(new File("vt-demo.jfr").toPath());

        // Enable key virtual thread-related events
        Map<String, String> settings = Map.of(
                "jdk.VirtualThreadStart#enabled", "true",
                "jdk.VirtualThreadEnd#enabled", "true",
                "jdk.VirtualThreadMount#enabled", "true",
                "jdk.VirtualThreadUnmount#enabled", "true",
                "jdk.VirtualThreadPinned#enabled", "true",
                "jdk.ThreadSleep#enabled", "true",
                "jdk.JavaMonitorEnter#enabled", "true"
        );
        recording.setSettings(settings);
        recording.start();

        System.out.println("📦 JFR started. Submitting virtual threads...");

        var executor = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                System.out.println("➡️ VT Started: " + Thread.currentThread());
                try {
                    Thread.sleep(1000); // Triggers unmount
                    synchronized (VirtualThreadJFRProgrammaticDemo.class) {
                        Thread.sleep(300); // Triggers pinning
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("⬅️ VT Completed: " + Thread.currentThread());
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Keep alive to allow background events to flush
        System.out.println("🕒 Waiting before stopping recording...");
        Thread.sleep(15000);

        System.out.println("🛑 Stopping JFR and saving to vt-demo.jfr");
        recording.stop();
        recording.close();

        System.out.println("✅ Done! Open vt-demo.jfr in JMC to analyze.");
    }
}
