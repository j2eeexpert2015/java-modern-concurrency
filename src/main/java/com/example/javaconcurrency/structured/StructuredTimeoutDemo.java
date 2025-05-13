package com.example.javaconcurrency.structured;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

/**
 * This example demonstrates using structured concurrency with timeouts
 * to implement resilient parallel operations.
 */
public class StructuredTimeoutDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("Structured Concurrency with Timeout");
        System.out.println("==================================");
        
        // Run different timeout scenarios
        runWithTimeout(1000); // Should complete normally
        runWithTimeout(300);  // Should timeout
        
        System.out.println("\nKey takeaways:");
        System.out.println("1. Use joinUntil() to set timeouts with structured concurrency");
        System.out.println("2. Timeouts automatically cancel all subtasks");
        System.out.println("3. You can implement fallbacks for timeout scenarios");
        System.out.println("4. This is useful for implementing resilient systems");
    }
    
    /**
     * Run structured concurrency with a timeout.
     */
    private static void runWithTimeout(long timeoutMs) {
        System.out.println("\nRunning with " + timeoutMs + "ms timeout:");
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            Instant start = Instant.now();
            
            // Fork multiple service calls
            var serviceA = scope.fork(() -> callExternalService("Service A", 
                    ThreadLocalRandom.current().nextInt(400, 600)));
            
            var serviceB = scope.fork(() -> callExternalService("Service B", 
                    ThreadLocalRandom.current().nextInt(200, 800)));
            
            var serviceC = scope.fork(() -> callExternalService("Service C", 
                    ThreadLocalRandom.current().nextInt(300, 700)));
            
            try {
                // Join with timeout
                scope.joinUntil(Instant.now().plusMillis(timeoutMs));
                
                // If we get here, all tasks completed within the timeout
                scope.throwIfFailed();
                
                System.out.println("All services responded successfully within timeout!");
                System.out.println("Service A response: " + serviceA.get());
                System.out.println("Service B response: " + serviceB.get());
                System.out.println("Service C response: " + serviceC.get());
                
            } catch (TimeoutException e) {
                // Handle timeout - tasks are automatically cancelled
                System.out.println("Timeout occurred! Some services did not respond in time.");
                System.out.println("Service A state: " + serviceA.state());
                System.out.println("Service B state: " + serviceB.state());
                System.out.println("Service C state: " + serviceC.state());
                
                // Implement fallback strategy
                System.out.println("Using fallback strategy...");
                
                // Example: Use any completed results, default values for others
                var resultA = serviceA.state() == StructuredTaskScope.Subtask.State.SUCCESS 
                        ? serviceA.get() : "DEFAULT_A";
                var resultB = serviceB.state() == StructuredTaskScope.Subtask.State.SUCCESS 
                        ? serviceB.get() : "DEFAULT_B";
                var resultC = serviceC.state() == StructuredTaskScope.Subtask.State.SUCCESS 
                        ? serviceC.get() : "DEFAULT_C";
                
                System.out.println("Final results with fallbacks:");
                System.out.println("Service A: " + resultA);
                System.out.println("Service B: " + resultB);
                System.out.println("Service C: " + resultC);
            }
            
            // Print execution time
            Duration duration = Duration.between(start, Instant.now());
            System.out.println("Execution time: " + duration.toMillis() + "ms");
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Simulate calling an external service with variable response time.
     */
    private static String callExternalService(String serviceName, long responseTimeMs) 
            throws Exception {
        
        System.out.println(serviceName + " called, expected response time: " + responseTimeMs + "ms");
        
        // Simulate service call
        Thread.sleep(responseTimeMs);
        
        // Generate response
        String response = serviceName + " response at " + Instant.now();
        System.out.println(serviceName + " responded after " + responseTimeMs + "ms");
        
        return response;
    }
}
