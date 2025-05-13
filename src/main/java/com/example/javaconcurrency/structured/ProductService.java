package com.example.javaconcurrency.structured;


import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ExecutionException;
import java.time.Duration;

/**
 * This example demonstrates practical structured concurrency with
 * a simulated product service that aggregates data from multiple backend services.
 */
public class ProductService {

    public static void main(String[] args) throws Exception {
        var service = new ProductService();
        
        // Fetch and display product details
        try {
            var productId = "PROD-12345";
            System.out.println("Fetching details for product: " + productId);
            
            var productDetails = service.getProductDetails(productId);
            System.out.println("\nProduct Details:");
            System.out.println("===================");
            System.out.println("ID: " + productDetails.id());
            System.out.println("Name: " + productDetails.name());
            System.out.println("Price: $" + productDetails.price());
            System.out.println("Stock: " + productDetails.stock() + " units");
            System.out.println("Rating: " + productDetails.rating() + "/5");
            System.out.println("Reviews: " + productDetails.reviews().size());
            System.out.println("Similar Products: " + productDetails.similarProducts().size());
            
        } catch (Exception e) {
            System.out.println("Error retrieving product details: " + e.getMessage());
        }
    }
    
    /**
     * Fetches complete product information by aggregating data from multiple services.
     * Uses structured concurrency to parallelize service calls and handle errors.
     */
    public ProductDetails getProductDetails(String productId) 
            throws InterruptedException, ExecutionException {
        
        // Use ShutdownOnFailure to cancel all tasks if any fail
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Fork parallel calls to different services
            var basicInfoTask = scope.fork(() -> getBasicProductInfo(productId));
            var inventoryTask = scope.fork(() -> getInventoryInfo(productId));
            var reviewsTask = scope.fork(() -> getProductReviews(productId));
            var recommendationsTask = scope.fork(() -> getSimilarProducts(productId));
            
            // Wait for all tasks to complete or fail
            scope.join().throwIfFailed();
            
            // If we get here, all tasks completed successfully
            var basicInfo = basicInfoTask.get();
            var inventory = inventoryTask.get();
            var reviews = reviewsTask.get();
            var similarProducts = recommendationsTask.get();
            
            // Combine results
            return new ProductDetails(
                productId,
                basicInfo.name(),
                basicInfo.price(),
                inventory.stockLevel(),
                basicInfo.rating(),
                reviews,
                similarProducts
            );
        }
    }
    
    // Simulated service calls to various backends
    
    private ProductBasicInfo getBasicProductInfo(String productId) throws Exception {
        System.out.println("Fetching basic product info...");
        simulateServiceCall(800);
        
        // Simulate a service response
        return new ProductBasicInfo(
            "Ergonomic Office Chair",
            299.99,
            4.7
        );
    }
    
    private InventoryInfo getInventoryInfo(String productId) throws Exception {
        System.out.println("Fetching inventory information...");
        simulateServiceCall(500);
        
        return new InventoryInfo(42, true);
    }
    
    private java.util.List<Review> getProductReviews(String productId) throws Exception {
        System.out.println("Fetching customer reviews...");
        simulateServiceCall(1000);
        
        // Simulate reviews
        return java.util.List.of(
            new Review("Great chair!", 5, "John D."),
            new Review("Very comfortable for long work days", 5, "Sarah M."),
            new Review("Good but expensive", 4, "Mike T.")
        );
    }
    
    private java.util.List<SimilarProduct> getSimilarProducts(String productId) throws Exception {
        System.out.println("Fetching similar products...");
        simulateServiceCall(700);
        
        // Simulate similar products
        return java.util.List.of(
            new SimilarProduct("PROD-12346", "Executive Office Chair", 399.99),
            new SimilarProduct("PROD-10987", "Budget Office Chair", 149.99),
            new SimilarProduct("PROD-11567", "Standing Desk Converter", 199.99)
        );
    }
    
    private void simulateServiceCall(long millis) throws Exception {
        Thread.sleep(Duration.ofMillis(millis));
        
        // Occasionally fail to simulate real-world issues
        if (java.util.concurrent.ThreadLocalRandom.current().nextInt(20) == 0) {
            throw new Exception("Service temporarily unavailable");
        }
    }
    
    // Data classes
    
    record ProductDetails(
        String id, 
        String name, 
        double price, 
        int stock, 
        double rating,
        java.util.List<Review> reviews,
        java.util.List<SimilarProduct> similarProducts
    ) {}
    
    record ProductBasicInfo(String name, double price, double rating) {}
    
    record InventoryInfo(int stockLevel, boolean inStock) {}
    
    record Review(String text, int rating, String reviewer) {}
    
    record SimilarProduct(String id, String name, double price) {}
}