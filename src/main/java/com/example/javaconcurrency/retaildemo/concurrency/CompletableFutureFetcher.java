package com.example.javaconcurrency.retaildemo.concurrency;

import com.example.javaconcurrency.retaildemo.model.ProductDetails;
import com.example.javaconcurrency.retaildemo.service.RetailService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CompletableFutureFetcher {
    public static ProductDetails fetch(String productId) {
        CompletableFuture<Double> priceFuture = CompletableFuture.supplyAsync(
            () -> RetailService.fetchPrice(productId)
        );
        CompletableFuture<Integer> inventoryFuture = CompletableFuture.supplyAsync(
            () -> RetailService.fetchInventory(productId)
        );
        CompletableFuture<List<String>> reviewsFuture = CompletableFuture.supplyAsync(
            () -> RetailService.fetchReviews(productId)
        );

        return CompletableFuture.allOf(priceFuture, inventoryFuture, reviewsFuture)
            .thenApply(v -> new ProductDetails(
                priceFuture.join(),
                inventoryFuture.join(),
                reviewsFuture.join()
            ))
            .join();
    }
}