package com.example.javaconcurrency.retaildemo.concurrency;

import com.example.javaconcurrency.retaildemo.model.ProductDetails;
import com.example.javaconcurrency.retaildemo.service.RetailService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VirtualThreadFetcher {
    public static ProductDetails fetch(String productId) throws Exception {
        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Double> priceFuture = virtualThreadExecutor.submit(() -> RetailService.fetchPrice(productId));
            Future<Integer> inventoryFuture = virtualThreadExecutor.submit(() -> RetailService.fetchInventory(productId));
            Future<List<String>> reviewsFuture = virtualThreadExecutor.submit(() -> RetailService.fetchReviews(productId));

            return new ProductDetails(
                priceFuture.get(),
                inventoryFuture.get(),
                reviewsFuture.get()
            );
        }
    }
}