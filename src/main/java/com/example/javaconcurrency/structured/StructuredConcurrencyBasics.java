package com.example.javaconcurrency.structured;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This example demonstrates the basic concepts of Structured Concurrency.
 * It shows how to create child tasks, manage their lifecycle, and handle results.
 */
public class StructuredConcurrencyBasics {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("Structured Concurrency Basics");
        System.out.println("============================");
        
        basicExample();
        errorHandlingExample();
        
        System.out.println("\nAll examples completed.");
    }
    
    /**
     * Basic example of using StructuredTaskScope to create and manage subtasks.
     */
    private static void basicExample() throws InterruptedException, ExecutionException {
        System.out.println("\n1. Basic StructuredTaskScope Example:");
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Fork subtasks - these run concurrently
            Subtask<String> task1 = scope.fork(() -> {
                System.out.println("Task 1 running on: " + Thread.currentThread());
                Thread.sleep(Duration.ofMillis(500));
                return "Result from Task 1";
            });
            
            Subtask<String> task2 = scope.fork(() -> {
                System.out.println("Task 2 running on: " + Thread.currentThread());
                Thread.sleep(Duration.ofMillis(300));
                return "Result from Task 2";
            });
            
            // Wait for both tasks to complete
            scope.join();
            
            // Process results
            System.out.println("Task 1 result: " + task1.get());
            System.out.println("Task 2 result: " + task2.get());
        }
    }
    
    /**
     * Example showing how errors are propagated in structured concurrency.
     */
    private static void errorHandlingExample() {
        System.out.println("\n2. Error Handling Example:");
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            Subtask<String> successTask = scope.fork(() -> {
                Thread.sleep(Duration.ofMillis(300));
                return "Success task completed";
            });
            
            Subtask<String> failingTask = scope.fork(() -> {
                Thread.sleep(Duration.ofMillis(200));
                if (ThreadLocalRandom.current().nextBoolean()) {
                    throw new RuntimeException("Simulated error in task");
                }
                return "This might not complete";
            });
            
            try {
                // Join and handle potential errors
                scope.join().throwIfFailed();
                
                // If we get here, both tasks succeeded
                System.out.println("All tasks completed successfully:");
                System.out.println("- " + successTask.get());
                System.out.println("- " + failingTask.get());
                
            } catch (ExecutionException e) {
                System.out.println("One of the tasks failed: " + e.getCause().getMessage());
                System.out.println("Success task state: " + successTask.state());
                System.out.println("Failing task state: " + failingTask.state());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Operation was interrupted");
            }
        }
    }
}