package com.example.javaconcurrency.retaildemo.service;

import java.util.List;
import java.util.function.Consumer;

public class RetailService {
    public static double fetchPrice(String productId) {
        try {
            Thread.sleep(1000); // Simulate I/O delay
            return 99.99;
        } catch (InterruptedException e) {
            throw new RuntimeException("Price fetch failed", e);
        }
    }

    public static int fetchInventory(String productId) {
        try {
            Thread.sleep(1000); // Simulate I/O delay
            return 50;
        } catch (InterruptedException e) {
            throw new RuntimeException("Inventory fetch failed", e);
        }
    }

    public static List<String> fetchReviews(String productId) {
        try {
            Thread.sleep(1000); // Simulate I/O delay
            return List.of("Great product!", "Highly recommended");
        } catch (InterruptedException e) {
            throw new RuntimeException("Reviews fetch failed", e);
        }
    }

    public static void fetchPriceAsync(String productId, Consumer<Double> callback, Consumer<Throwable> errorCallback) {
        new Thread(() -> {
            try {
                double price = fetchPrice(productId);
                callback.accept(price);
            } catch (Exception e) {
                errorCallback.accept(e);
            }
        }).start();
    }

    public static void fetchInventoryAsync(String productId, Consumer<Integer> callback, Consumer<Throwable> errorCallback) {
        new Thread(() -> {
            try {
                int inventory = fetchInventory(productId);
                callback.accept(inventory);
            } catch (Exception e) {
                errorCallback.accept(e);
            }
        }).start();
    }

    public static void fetchReviewsAsync(String productId, Consumer<List<String>> callback, Consumer<Throwable> errorCallback) {
        new Thread(() -> {
            try {
                List<String> reviews = fetchReviews(productId);
                callback.accept(reviews);
            } catch (Exception e) {
                errorCallback.accept(e);
            }
        }).start();
    }
}