package com.example.javaconcurrency.structured;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This example demonstrates using ScopedValue for securely passing context
 * through different components without thread locals.
 */
public class ScopedValueExample {

    // Define ScopedValue for user context
    private static final ScopedValue<UserContext> CURRENT_USER = 
        ScopedValue.newInstance();
    
    // Define ScopedValue for request tracing
    private static final ScopedValue<RequestContext> REQUEST_CONTEXT = 
        ScopedValue.newInstance();
    
    public static void main(String[] args) {
        System.out.println("Scoped Values Example");
        System.out.println("====================");
        
        // Simulate processing multiple user requests
        processUserRequest("user123", "GET /api/products");
        processUserRequest("admin456", "GET /api/admin/users");
        
        System.out.println("\nParallel processing example:");
        parallelProcessingExample();
    }
    
    /**
     * Processes a user request with proper context propagation using ScopedValue.
     */
    private static void processUserRequest(String userId, String requestInfo) {
        System.out.println("\nProcessing request for user: " + userId);
        
        // Create user context
        var user = new UserContext(userId, userId.startsWith("admin"));
        
        // Create request context with tracing info
        var reqContext = new RequestContext("REQ-" + System.nanoTime(), requestInfo);
        
        // Bind both contexts for the duration of request processing
        ScopedValue.where(CURRENT_USER, user)
            .where(REQUEST_CONTEXT, reqContext)
            .run(() -> {
                // Process the request with the scoped values available
                authenticateRequest();
                var result = handleRequest();
                logRequestCompletion(result);
            });
    }
    
    private static void authenticateRequest() {
        // Access the user context from the ScopedValue
        var user = CURRENT_USER.get();
        var request = REQUEST_CONTEXT.get();
        
        System.out.println("Authenticating request: " + request.requestId() + 
                           " for user: " + user.userId());
        
        // Check authorization for admin-only paths
        if (request.path().contains("/admin/") && !user.isAdmin()) {
            System.out.println("⚠️ Access denied: User is not an admin");
        }
    }
    
    private static String handleRequest() {
        var user = CURRENT_USER.get();
        var request = REQUEST_CONTEXT.get();
        
        System.out.println("Handling request: " + request.requestId() + 
                           " - " + request.path() +
                           " (Thread: " + Thread.currentThread() + ")");
        
        // Simulate accessing a service that uses the current user context
        accessSecureService();
        
        return "Success";
    }
    
    private static void accessSecureService() {
        // The service method can access the user context without it being passed
        // explicitly as a parameter
        var user = CURRENT_USER.get();
        System.out.println("Secure service accessed by: " + user.userId() + 
                          " (Admin: " + user.isAdmin() + ")");
    }
    
    private static void logRequestCompletion(String result) {
        var request = REQUEST_CONTEXT.get();
        System.out.println("Request " + request.requestId() + " completed with status: " + result);
    }
    
    /**
     * Example showing scoped values in parallel processing contexts.
     */
    private static void parallelProcessingExample() {
        // Create a user context
        var user = new UserContext("batch-processor", true);
        
        // Bind the user context
        ScopedValue.where(CURRENT_USER, user).run(() -> {
            // Create an executor with virtual threads
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                // Submit multiple tasks that will inherit the scoped value
                for (int i = 0; i < 3; i++) {
                    final int taskId = i;
                    
                    // Each task creates its own request context
                    var reqContext = new RequestContext(
                        "BATCH-" + taskId, 
                        "PROCESS /api/batch/" + taskId
                    );
                    
                    // Bind the request context for this specific task
                    executor.submit(() -> 
                        ScopedValue.where(REQUEST_CONTEXT, reqContext).run(() -> {
                            System.out.println("\nBatch task " + taskId + " started");
                            // Both CURRENT_USER and REQUEST_CONTEXT are available here
                            handleRequest();
                        })
                    );
                }
                
                // Wait for a moment to see the results
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
    
    // Data classes
    
    record UserContext(String userId, boolean isAdmin) {}
    
    record RequestContext(String requestId, String path) {}
}
