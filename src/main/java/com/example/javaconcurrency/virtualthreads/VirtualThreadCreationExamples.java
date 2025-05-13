package com.example.javaconcurrency.virtualthreads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualThreadCreationExamples {

	public static void main(String[] args) throws InterruptedException {

		System.out.println("Main thread: " + Thread.currentThread());
		System.out.println("=".repeat(40));

		// --- Common Task for all Threads ---
		Runnable simpleTask = () -> {
			System.out.println("Running task in: " + Thread.currentThread());
			try {
				// Simulate some work
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				System.err.println("Task interrupted in " + Thread.currentThread());
			}
		};

		// --- Method 1: Thread.startVirtualThread(Runnable) ---
		// Simplest way to create and start a virtual thread immediately.
		System.out.println("\nMethod 1: Thread.startVirtualThread()");
		Thread vThread1 = Thread.startVirtualThread(simpleTask);
		System.out.println("Started vThread1: " + vThread1); // Note: May not have finished executing yet
		System.out.println("=".repeat(40));

		// --- Method 2: Using Thread.Builder (Thread.ofVirtual().start()) ---
		// Use a builder to potentially configure the thread (e.g., name) before
		// starting.
		System.out.println("\nMethod 2: Thread.ofVirtual().start()");
		Thread vThread2 = Thread.ofVirtual().name("my-virtual-thread-", 1) // Name pattern with counter
				.start(simpleTask);
		System.out.println("Started vThread2: " + vThread2);
		System.out.println("=".repeat(40));

		// --- Method 3: Using Thread.Builder (Thread.ofVirtual().unstarted()) ---
		// Create a virtual thread but don't start it immediately. You control when
		// .start() is called.
		System.out.println("\nMethod 3: Thread.ofVirtual().unstarted()");
		Thread vThread3 = Thread.ofVirtual().name("unstarted-virtual").unstarted(simpleTask);
		System.out.println("Created (unstarted) vThread3: " + vThread3);
		// Start it manually later
		vThread3.start();
		System.out.println("Manually started vThread3.");
		System.out.println("=".repeat(40));

		// --- Method 4: Using ExecutorService
		// (Executors.newVirtualThreadPerTaskExecutor()) ---
		// Recommended for managing multiple tasks. Creates a new virtual thread for
		// each submitted task.
		// Use try-with-resources to ensure the executor is shut down properly.
		System.out.println("\nMethod 4: Executors.newVirtualThreadPerTaskExecutor()");
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			System.out.println("Submitting 3 tasks to ExecutorService...");
			executor.submit(simpleTask);
			executor.submit(simpleTask);
			executor.submit(simpleTask);
			System.out.println("Tasks submitted. Executor will handle thread creation.");
			// The executor will wait for submitted tasks to complete upon exiting
			// the try-with-resources block (implicitly calls shutdown and awaitTermination)
		} // executor.close() is called automatically here
		System.out.println("ExecutorService finished.");
		System.out.println("=".repeat(40));

		vThread1.join();
		vThread2.join();
		vThread3.join();

		System.out.println("\nMain thread finished.");
	}
}