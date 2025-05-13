package com.example.javaconcurrency.virtualthreads.benchmark;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import com.example.javaconcurrency.virtualthreads.util.ThreadUtil;

/**
 * Benchmark visualizer comparing platform and virtual threads for CPU-bound tasks.
 */
public class CpuBoundBenchmarkVisualizer {

    private static final int NUM_TASKS = 64;
    private static final int ITERATIONS = 50_000_000;
    private static final int CHART_WIDTH = 1200;
    private static final int CHART_HEIGHT = 800;
    private static final String OUTPUT_DIR = "charts/";

    public static void main(String[] args) {
        // Create output directory for charts
        createOutputDirectory();

        System.out.println("Starting CPU-bound benchmark comparison...");

        // Run benchmarks and generate charts
        List<Integer> platformResults = runBenchmark("Platform Threads", Executors.newCachedThreadPool());
        generateChart(platformResults, "Platform Threads (newCachedThreadPool)", "platform_threads_chart.png");

        List<Integer> virtualResults = runBenchmark("Virtual Threads", Executors.newVirtualThreadPerTaskExecutor());
        generateChart(virtualResults, "Virtual Threads (newVirtualThreadPerTaskExecutor)", "virtual_threads_chart.png");

        // Print summary statistics
        printSummary("Platform Threads", platformResults);
        printSummary("Virtual Threads", virtualResults);

        System.out.println("Benchmark complete. Charts saved in " + OUTPUT_DIR);
    }

    private static void createOutputDirectory() {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("Failed to create output directory: " + OUTPUT_DIR);
        }
    }

    private static List<Integer> runBenchmark(String type, ExecutorService executor) {
        System.out.println("Running benchmark for " + type);

        List<Integer> executionTimes = new ArrayList<>(NUM_TASKS);
        List<Future<Integer>> futures = new ArrayList<>(NUM_TASKS);
        AtomicInteger completed = new AtomicInteger(0);

        // Submit tasks
        for (int i = 0; i < NUM_TASKS; i++) {
            final int index = i;
            Future<Integer> future = executor.submit(() -> {
                Instant start = Instant.now();

                // CPU-bound computation
                BigInteger result = IntStream
                        .range(0, ITERATIONS)
                        .mapToObj(BigInteger::valueOf)
                        .reduce(BigInteger.ZERO, BigInteger::add);

                int elapsedTime = (int) ThreadUtil.benchmark(start);
                String id = createTwoDigitId(index + 1);

                int currentCompleted = completed.incrementAndGet();
                System.out.printf("[%s] Task %s completed: %d ms (%d/%d)%n",
                        type, id, elapsedTime, currentCompleted, NUM_TASKS);

                return elapsedTime;
            });
            futures.add(future);
        }

        // Collect results
        for (Future<Integer> future : futures) {
            try {
                executionTimes.add(future.get());
            } catch (Exception e) {
                System.err.println("Error retrieving benchmark result: " + e.getMessage());
                executionTimes.add(0); // Placeholder for failed tasks
            }
        }

        ThreadUtil.shutdownAndAwaitTermination(executor, TimeUnit.MINUTES);
        System.out.println(type + " benchmark complete.");

        return executionTimes;
    }

    private static void generateChart(List<Integer> executionTimes, String title, String filename) {
        System.out.println("Generating chart: " + title);

        // Create dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < executionTimes.size(); i++) {
            String taskId = createTwoDigitId(i + 1);
            dataset.addValue(executionTimes.get(i), "Execution Time (ms)", taskId);
        }

        // Create chart
        JFreeChart chart = ChartFactory.createBarChart(
                title,                  // chart title
                "Task ID",              // domain axis label
                "Execution Time (ms)",  // range axis label
                dataset,                // data
                PlotOrientation.VERTICAL,
                false,                  // include legend
                true,                   // tooltips
                false                   // URLs
        );

        // Save chart to file
        try {
            File outputFile = new File(OUTPUT_DIR + filename);
            ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
            System.out.println("Chart saved to " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving chart: " + e.getMessage());
        }
    }

    private static void printSummary(String type, List<Integer> executionTimes) {
        if (executionTimes.isEmpty()) {
            System.out.println(type + ": No results to summarize.");
            return;
        }

        double average = executionTimes.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        int max = executionTimes.stream().mapToInt(Integer::intValue).max().orElse(0);
        int min = executionTimes.stream().mapToInt(Integer::intValue).min().orElse(0);

        System.out.printf("%s Summary:%n", type);
        System.out.printf("  Average Execution Time: %.2f ms%n", average);
        System.out.printf("  Max Execution Time: %d ms%n", max);
        System.out.printf("  Min Execution Time: %d ms%n", min);
    }

    private static String createTwoDigitId(int index) {
        return String.format("%02d", index);
    }
}