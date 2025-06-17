package com.example.javaconcurrency.virtualthreads.creation;

import java.io.IOException;
import java.util.concurrent.Executors;

public class VirtualThreadDemo {

    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("📢 Attach JMC now. Press ENTER to continue...");
        System.in.read();

        var executor = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                System.out.println("➡️ Thread started: " + Thread.currentThread());
                try {
                    Thread.sleep(1000); // Causes unmount
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("⬅️ Thread resumed: " + Thread.currentThread());
            });
        }

        executor.shutdown();
        Thread.sleep(3000); // Wait before exit
    }
}

