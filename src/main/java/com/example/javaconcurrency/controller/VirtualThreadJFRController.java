package com.example.javaconcurrency.controller;


import jdk.jfr.Event;
import jdk.jfr.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api/jfr")
public class VirtualThreadJFRController {

    private static final Logger logger = LoggerFactory.getLogger(VirtualThreadJFRController.class);
    private static final ExecutorService platformExecutor = Executors.newFixedThreadPool(5);

    @GetMapping("/virtual")
    public String handleVirtualWithJFR() {
        logAndRecordJfr("Virtual Endpoint");
        simulateBlockingCall();
        return "Handled by Virtual Thread with JFR";
    }

    @GetMapping("/platform")
    public String handlePlatformWithJFR() throws Exception {
        Future<String> result = platformExecutor.submit(() -> {
            logAndRecordJfr("Platform Endpoint");
            simulateBlockingCall();
            return "Handled by Platform Thread with JFR";
        });
        return result.get();
    }

    private void logAndRecordJfr(String tag) {
        Thread t = Thread.currentThread();
        logger.info("[{}] Thread: {}, virtual={}", tag, t.getName(), t.isVirtual());

        ThreadExecutionEvent event = new ThreadExecutionEvent();
        event.name = t.getName();
        event.isVirtual = t.isVirtual();
        event.endpoint = tag;
        event.commit();
    }

    private void simulateBlockingCall() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Label("ThreadExecutionEvent")
    static class ThreadExecutionEvent extends Event {
        @Label("Thread Name")
        String name;

        @Label("Is Virtual")
        boolean isVirtual;

        @Label("Endpoint")
        String endpoint;
    }
}

