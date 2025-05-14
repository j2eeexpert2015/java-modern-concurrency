package com.example.javaconcurrency.virtualthreads.creation;

public class VirtualThreadBasicCreation {
    public static void main(String[] args) throws InterruptedException {
        // 1. Using Thread.startVirtualThread()
        Thread vThread1 = Thread.startVirtualThread(() -> 
            System.out.println("Running in virtual thread: " + Thread.currentThread()));
        
        // 2. Using Thread.ofVirtual().start()
        Thread vThread2 = Thread.ofVirtual()
            .name("my-vthread-1")
            .start(() -> System.out.println("Named virtual thread: " + Thread.currentThread()));
        
        vThread1.join();
        vThread2.join();
    }
}
