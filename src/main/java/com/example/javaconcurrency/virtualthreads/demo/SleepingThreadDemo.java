package com.example.javaconcurrency.virtualthreads.demo;

import java.util.Scanner;

public class SleepingThreadDemo {
    private static volatile boolean keepRunning = true;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Starting SleepingThreadDemo to show purple 'Sleeping' state in VisualVM.");
        System.out.println("Connect VisualVM to this process, go to Threads tab, and look for 'Sleeping-Thread'.");

        // Create a thread that sleeps for a long duration
        Thread sleepingThread = new Thread(() -> {
            while (keepRunning) {
                try {
                    System.out.println("Sleeping-Thread is sleeping for 15 seconds...");
                    Thread.sleep(15000); // 15-second sleep to ensure visibility
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Sleeping-Thread interrupted. Exiting...");
                    break;
                }
            }
        });
        sleepingThread.setName("Sleeping-Thread");
        sleepingThread.setDaemon(true); // Daemon to allow JVM exit
        sleepingThread.start();

        System.out.println("Observe 'Sleeping-Thread' in VisualVM (should show purple 'Sleeping' state).");
        System.out.println("Press Enter to terminate the program or wait 15 seconds to see the next sleep cycle...");
        scanner.nextLine();

        // Cleanup: Stop the sleeping thread
        keepRunning = false;
        sleepingThread.interrupt();

        // Wait briefly to ensure thread terminates
        try {
            sleepingThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        scanner.close();
        System.out.println("Program ended. Check VisualVM to confirm 'Sleeping-Thread' is terminated.");
    }
}