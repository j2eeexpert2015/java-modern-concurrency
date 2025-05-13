package com.example.javaconcurrency.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api/basic")
public class VirtualThreadBasicController {

    private static final Logger logger = LoggerFactory.getLogger(VirtualThreadBasicController.class);
    private static final ExecutorService platformExecutor = Executors.newFixedThreadPool(5);

    @GetMapping("/virtual")
    public String handleVirtual() {
        log("Virtual Endpoint");
        simulateBlockingCall();
        return "Handled by Virtual Thread";
    }

    @GetMapping("/platform")
    public String handlePlatform() throws Exception {
        Future<String> result = platformExecutor.submit(() -> {
            log("Platform Endpoint");
            simulateBlockingCall();
            return "Handled by Platform Thread";
        });
        return result.get();
    }

    private void log(String tag) {
        Thread t = Thread.currentThread();
        logger.info("[{}] Thread: {}, virtual={}", tag, t.getName(), t.isVirtual());
    }

    private void simulateBlockingCall() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

