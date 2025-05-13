package com.example.javaconcurrency.retaildemo.model;

import java.util.List;

public class ProductDetails {
    private final double price;
    private final int inventory;
    private final List<String> reviews;

    public ProductDetails(double price, int inventory, List<String> reviews) {
        this.price = price;
        this.inventory = inventory;
        this.reviews = reviews;
    }

    @Override
    public String toString() {
        return "ProductDetails{price=" + price + ", inventory=" + inventory + ", reviews=" + reviews + "}";
    }
}