package com.example.javaconcurrency.retaildemo;


import com.example.javaconcurrency.retaildemo.concurrency.*;
import com.example.javaconcurrency.retaildemo.model.ProductDetails;

public class Main {
    private static final String PRODUCT_ID = "12345";

    public static void main(String[] args) {
        try {
            System.out.println("ExecutorService: " + ExecutorServiceFetcher.fetch(PRODUCT_ID));
            System.out.println("Virtual Threads: " + VirtualThreadFetcher.fetch(PRODUCT_ID));
            System.out.println("CompletableFuture: " + CompletableFutureFetcher.fetch(PRODUCT_ID));
            //System.out.println("Reactor: " + ReactorFetcher.fetch(PRODUCT_ID));
            //System.out.println("HttpClient: " + HttpClientFetcher.fetch(PRODUCT_ID));
            System.out.println("Callbacks: " + CallbackFetcher.fetch(PRODUCT_ID));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
