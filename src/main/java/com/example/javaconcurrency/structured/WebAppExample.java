package com.example.javaconcurrency.structured;


import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * This example demonstrates using Scoped Values in a web application
 * for securely propagating authentication and request context.
 */
public class WebAppExample {

    // Scoped Values for request context
    private static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();
    private static final ScopedValue<RequestData> REQUEST_DATA = ScopedValue.newInstance();
    
    // User database (for demo purposes)
    private static final Map<String, User> USERS = Map.of(
        "alice", new User("alice", "Alice Smith", new String[]{"user"}),
        "bob", new User("bob", "Bob Johnson", new String[]{"user", "editor"}),
        "admin", new User("admin", "Admin User", new String[]{"user", "admin"})
    );
    
    // Session storage
    private static final Map<String, String> SESSIONS = new ConcurrentHashMap<>();
    
    // Resource permissions
    private static final Map<String, String[]> RESOURCE_PERMISSIONS = Map.of(
        "/api/profile", new String[]{"user"},
        "/api/articles", new String[]{"user"},
        "/api/articles/edit", new String[]{"editor"},
        "/api/admin", new String[]{"admin"}
    );
    
    public static void main(String[] args) {
        // Simulate several HTTP requests
        simulateRequest("GET", "/api/profile", "session=abc123");
        simulateRequest("GET", "/api/admin", "session=xyz789");
        simulateRequest("POST", "/api/articles/edit", "session=def456");
        simulateRequest("GET", "/api/articles", null); // No session
    }
    
    /**
     * Simulates handling an HTTP request with middleware pattern.
     */
    private static void simulateRequest(String method, String path, String sessionHeader) {
        System.out.println("\n--- New Request: " + method + " " + path + " ---");
        
        // Create request data
        var requestId = UUID.randomUUID().toString().substring(0, 8);
        var requestData = new RequestData(requestId, method, path);
        
        // Start with request context binding
        ScopedValue.where(REQUEST_DATA, requestData).run(() -> {
            // Apply middleware chain
            logRequestMiddleware(() -> 
                authenticationMiddleware(() -> 
                    authorizationMiddleware(() ->
                        routeHandler()
                    )
                )
            );
        });
    }
    
    /**
     * Middleware that logs request information.
     */
    private static void logRequestMiddleware(Runnable next) {
        var request = REQUEST_DATA.get();
        System.out.println("Request started: " + request.method() + " " + 
                           request.path() + " (ID: " + request.id() + ")");
        
        // Continue to next middleware
        next.run();
        
        System.out.println("Request completed: " + request.id());
    }
    
    /**
     * Middleware that handles authentication.
     */
    private static void authenticationMiddleware(Runnable next) {
        var request = REQUEST_DATA.get();
        
        // Check for session cookie
        String sessionId = getSessionFromRequest();
        String username = sessionId != null ? SESSIONS.get(sessionId) : null;
        
        if (username != null && USERS.containsKey(username)) {
            // User is authenticated - establish user context for downstream components
            User user = USERS.get(username);
            System.out.println("Authenticated as: " + user.displayName() + 
                              " (Roles: " + String.join(", ", user.roles()) + ")");
            
            // Bind the user to scoped value for the rest of the request handling
            ScopedValue.where(CURRENT_USER, user).run(next);
        } else {
            // No valid session - continue as anonymous
            System.out.println("Anonymous request");
            next.run();
        }
    }
    
    /**
     * Middleware that checks authorization for protected resources.
     */
    private static void authorizationMiddleware(Runnable next) {
        var request = REQUEST_DATA.get();
        User user = null;
        
        // Try to get current user if authenticated
        try {
            user = CURRENT_USER.get();
        } catch (IllegalStateException e) {
            // No user bound to CURRENT_USER (anonymous)
        }
        
        // Check if resource requires permissions
        String[] requiredRoles = RESOURCE_PERMISSIONS.get(request.path());
        
        if (requiredRoles != null) {
            // Resource is protected
            if (user == null) {
                // Not authenticated but resource requires auth
                System.out.println("Access denied: Authentication required for " + request.path());
                return;
            }
            
            // Check if user has required role
            boolean hasAccess = false;
            for (String role : user.roles()) {
                for (String requiredRole : requiredRoles) {
                    if (role.equals(requiredRole)) {
                        hasAccess = true;
                        break;
                    }
                }
            }
            
            if (!hasAccess) {
                System.out.println("Access denied: Insufficient permissions for " + request.path());
                return;
            }
        }
        
        // Continue to handler
        next.run();
    }
    
    /**
     * The actual request handler for the route.
     */
    private static void routeHandler() {
        var request = REQUEST_DATA.get();
        
        System.out.println("Handling request: " + request.path());
        
        // Access user information from the bound ScopedValue
        User user = null;
        try {
            user = CURRENT_USER.get();
            System.out.println("Processing for user: " + user.username());
            
            // Example of calling a service method that uses the current user
            if ("/api/profile".equals(request.path())) {
                userProfileService();
            } else if ("/api/articles/edit".equals(request.path())) {
                editArticleService(request.method().equals("POST"));
            } else if ("/api/admin".equals(request.path())) {
                adminDashboardService();
            } else {
                // Generic endpoint
                System.out.println("Response: Success - Data for path " + request.path());
            }
            
        } catch (IllegalStateException e) {
            // No authenticated user
            System.out.println("Processing for anonymous user");
            System.out.println("Response: Public data only");
        }
    }
    
    // Service methods that use the scoped value context
    
    private static void userProfileService() {
        User user = CURRENT_USER.get();
        System.out.println("Response: User profile for " + user.displayName());
    }
    
    private static void editArticleService(boolean isPost) {
        User user = CURRENT_USER.get();
        if (isPost) {
            System.out.println("Article edited by " + user.username());
            System.out.println("Response: Article updated successfully");
        } else {
            System.out.println("Response: Article edit form for " + user.username());
        }
    }
    
    private static void adminDashboardService() {
        System.out.println("Accessing admin features as " + CURRENT_USER.get().username());
        System.out.println("Response: Admin dashboard data");
    }
    
    // Helper methods
    
    private static String getSessionFromRequest() {
        // Simulate extracting session from request header
        var request = REQUEST_DATA.get();
        
        // For demo purposes, create some sessions
        if (!SESSIONS.containsKey("abc123")) {
            SESSIONS.put("abc123", "alice");
        }
        if (!SESSIONS.containsKey("def456")) {
            SESSIONS.put("def456", "bob");
        }
        if (!SESSIONS.containsKey("xyz789")) {
            SESSIONS.put("xyz789", "admin");
        }
        
        // Simulated header parsing
        if (request.path().equals("/api/profile")) {
            return "abc123";  // Alice's session
        } else if (request.path().equals("/api/articles/edit")) {
            return "def456";  // Bob's session
        } else if (request.path().equals("/api/admin")) {
            return "xyz789";  // Admin's session
        }
        
        return null;
    }
    
    // Data classes
    
    record User(String username, String displayName, String[] roles) {}
    
    record RequestData(String id, String method, String path) {}
}
