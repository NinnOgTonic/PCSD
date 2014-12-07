/**
 *
 */
package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 *
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 *
 */
public class CertainWorkload {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        int numConcurrentWorkloadThreads = 10;
        String serverAddress = "http://localhost:8081";
        boolean localTest = true;
        List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
        List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();

        // Initialize the RPC interfaces if its not a localTest, the variable is
        // overriden if the property is set
        String localTestProperty = System
                .getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
        localTest = (localTestProperty != null) ? Boolean
                .parseBoolean(localTestProperty) : localTest;

        BookStore bookStore = null;
        StockManager stockManager = null;
        if (localTest) {
            CertainBookStore store = new CertainBookStore();
            bookStore = store;
            stockManager = store;
        } else {
            stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
            bookStore = new BookStoreHTTPProxy(serverAddress);
        }

        // Generate data in the bookstore before running the workload
        initializeBookStoreData(bookStore, stockManager);

        ExecutorService exec = Executors
                .newFixedThreadPool(numConcurrentWorkloadThreads);

        for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
            WorkloadConfiguration config = new WorkloadConfiguration(bookStore,
                    stockManager);
            Worker workerTask = new Worker(config);
            // Keep the futures to wait for the result from the thread
            runResults.add(exec.submit(workerTask));
        }

        // Get the results from the threads using the futures returned
        for (Future<WorkerRunResult> futureRunResult : runResults) {
            WorkerRunResult runResult = futureRunResult.get(); // blocking call
            workerRunResults.add(runResult);
        }

        exec.shutdownNow(); // shutdown the executor

        // Finished initialization, stop the clients if not localTest
        if (!localTest) {
            ((BookStoreHTTPProxy) bookStore).stop();
            ((StockManagerHTTPProxy) stockManager).stop();
        }

        reportMetric(workerRunResults);
    }

    /**
     * Computes the metrics and prints them
     *
     * @param workerRunResults
     */
    public static void reportMetric(List<WorkerRunResult> results) {
        double countSuccess = 0.0;
        double time = 0.0;
        double timeSeconds = 0.0;
        double totalRuns = 0.0;
        double numCustomerSuccess = 0.0;
        double numCustomerTotal = 0.0;
        double threads = 0.0;

        for(WorkerRunResult res : results) {
            countSuccess += res.getSuccessfulInteractions();
            time += res.getElapsedTimeInNanoSecs();
            totalRuns += res.getTotalRuns();
            numCustomerTotal += res.getTotalFrequentBookStoreInteractionRuns();
            numCustomerSuccess += res.getSuccessfulFrequentBookStoreInteractionRuns();
            threads += 1.0;
        }

        countSuccess /= threads;
        time /= threads;
        timeSeconds = time / 1000000000.0;
        totalRuns /= threads;
        numCustomerTotal /= threads;
        numCustomerSuccess /= threads;

        double goodput = countSuccess / timeSeconds;
        double errorRate = 100 - (countSuccess / totalRuns * 100.0);
        double customerPercentage = numCustomerTotal / totalRuns * 100.0;
        double latency = time / totalRuns / threads;

        System.out.format("Statistics averaged over threads:\n");
        System.out.format("Number of successful runs: %f.\n", countSuccess);
        System.out.format("Time elapsed: %f seconds.\n", timeSeconds);
        System.out.format("Total runs: %f.\n", totalRuns);
        System.out.format("Number of customer runs: %f.\n", numCustomerTotal);
        System.out.format("Number of successful customer runs: %f.\n", numCustomerSuccess);
        System.out.format("Number of threads: %f.\n\n", threads);

        System.out.format("Error rate: %.02f%%\n", errorRate);
        System.out.format("Goodput   : %.02f requests per second.\n", goodput);
        System.out.format("Latency   : %.02f nano seconds.\n", latency);
        System.out.format("Customers : %.02f%%\n", customerPercentage);
    }

    /**
     * Generate the data in bookstore before the workload interactions are run
     *
     * Ignores the serverAddress if its a localTest
     *
     */
    public static void initializeBookStoreData(BookStore bookStore,
            StockManager stockManager) throws BookStoreException {

        // TODO: You should initialize data for your bookstore here

    }
}
