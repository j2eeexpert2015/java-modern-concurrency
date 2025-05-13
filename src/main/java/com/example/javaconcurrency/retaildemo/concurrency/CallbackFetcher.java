package com.example.javaconcurrency.retaildemo.concurrency;

import com.example.javaconcurrency.retaildemo.model.ProductDetails;
import com.example.javaconcurrency.retaildemo.service.RetailService;

import java.util.List;

public class CallbackFetcher {
    public static ProductDetails fetch(String productId) throws InterruptedException {
        final Double[] price = {null};
        final Integer[] inventory = {null};
        final List<String>[] reviews = new List[]{null};
        final Throwable[] error = {null};
        final Object lock = new Object();
        final int[] completedTasks = {0};

        RetailService.fetchPriceAsync(productId,
            result -> {
                synchronized (lock) {
                    price[0] = result;
                    completedTasks[0]++;
                    lock.notifyAll();
                }
            },
            err -> {
                synchronized (lock) {
                    error[0] = err;
                    lock.notifyAll();
                }
            });

        RetailService.fetchInventoryAsync(productId,
            result -> {
                synchronized (lock) {
                    inventory[0] = result;
                    completedTasks[0]++;
                    lock.notifyAll();
                }
            },
            err -> {
                synchronized (lock) {
                    error[0] = err;
                    lock.notifyAll();
                }
            });

        RetailService.fetchReviewsAsync(productId,
            result -> {
                synchronized (lock) {
                    reviews[0] = result;
                    completedTasks[0]++;
                    lock.notifyAll();
                }
            },
            err -> {
                synchronized (lock) {
                    error[0] = err;
                    lock.notifyAll();
                }
            });

        synchronized (lock) {
            while (completedTasks[0] < 3 && error[0] == null) {
                lock.wait();
            }
        }

        if (error[0] != null) {
            throw new RuntimeException("Callback fetch failed", error[0]);
        }

        return new ProductDetails(price[0], inventory[0], reviews[0]);
    }
}