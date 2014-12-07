/**
 *
 */
package com.acertainbookstore.client.workloads;

import java.util.Random;
import java.util.concurrent.Callable;

import com.acertainbookstore.utils.BookStoreException;




import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * Worker represents the workload runner which runs the workloads with
 * parameters using WorkloadConfiguration and then reports the results
 *
 */
public class Worker implements Callable<WorkerRunResult> {
    private WorkloadConfiguration configuration = null;
    private int numSuccessfulFrequentBookStoreInteraction = 0;
    private int numTotalFrequentBookStoreInteraction = 0;

    public Worker(WorkloadConfiguration config) {
        configuration = config;
    }

    /**
     * Run the appropriate interaction while trying to maintain the configured
     * distributions
     *
     * Updates the counts of total runs and successful runs for customer
     * interaction
     *
     * @param chooseInteraction
     * @return
     */
    private boolean runInteraction(float chooseInteraction) {
        try {
            if (chooseInteraction < configuration
                    .getPercentRareStockManagerInteraction()) {
                runRareStockManagerInteraction();
            } else if (chooseInteraction < configuration
                    .getPercentFrequentStockManagerInteraction()) {
                runFrequentStockManagerInteraction();
            } else {
                numTotalFrequentBookStoreInteraction++;
                runFrequentBookStoreInteraction();
                numSuccessfulFrequentBookStoreInteraction++;
            }
        } catch (BookStoreException ex) {
            return false;
        }
        return true;
    }

    /**
     * Run the workloads trying to respect the distributions of the interactions
     * and return result in the end
     */
    public WorkerRunResult call() throws Exception {
        int count = 1;
        long startTimeInNanoSecs = 0;
        long endTimeInNanoSecs = 0;
        int successfulInteractions = 0;
        long timeForRunsInNanoSecs = 0;

        Random rand = new Random();
        float chooseInteraction;

        // Perform the warmup runs
        while (count++ <= configuration.getWarmUpRuns()) {
            chooseInteraction = rand.nextFloat() * 100f;
            runInteraction(chooseInteraction);
        }

        count = 1;
        numTotalFrequentBookStoreInteraction = 0;
        numSuccessfulFrequentBookStoreInteraction = 0;

        // Perform the actual runs
        startTimeInNanoSecs = System.nanoTime();
        while (count++ <= configuration.getNumActualRuns()) {
            chooseInteraction = rand.nextFloat() * 100f;
            if (runInteraction(chooseInteraction)) {
                successfulInteractions++;
            }
        }
        endTimeInNanoSecs = System.nanoTime();
        timeForRunsInNanoSecs += (endTimeInNanoSecs - startTimeInNanoSecs);
        return new WorkerRunResult(successfulInteractions,
                timeForRunsInNanoSecs, configuration.getNumActualRuns(),
                numSuccessfulFrequentBookStoreInteraction,
                numTotalFrequentBookStoreInteraction);
    }

    /**
     * Runs the new stock acquisition interaction
     *
     * @throws BookStoreException
     */
    private void runRareStockManagerInteraction() throws BookStoreException {
        // TODO: Add code for New Stock Acquisition Interaction
        /*allBooks  = getBooks();
        randBooks = nextSetOfStockBooks();
        for(book : randBooks) {
            if(!book.isbn in isbns(allBooks)) {
                addBook(book);
            }
        }*/

    }

    /**
     * Runs the stock replenishment interaction
     *
     * @throws BookStoreException
     */
    private void runFrequentStockManagerInteraction() throws BookStoreException {
        // TODO: Add code for Stock Replenishment Interaction
    }

    /**
     * Runs the customer interaction
     *
     * @throws BookStoreException
     */
    private void runFrequentBookStoreInteraction() throws BookStoreException {
        BookStore bs = configuration.getBookStore();
        List<Book> editorPicks = bs.getEditorPicks(configuration.getNumBooksToBuy()*2);

        Set<Integer> picksToBeSampled = new HashSet<Integer>();

        for(Book n : editorPicks){
            picksToBeSampled.add(n.getISBN());
        }

        BookSetGenerator generator = configuration.getBookSetGenerator();

        Set<Integer> sampledPicks = generator.sampleFromSetOfISBNs(picksToBeSampled, configuration.getNumBooksToBuy());

        // A set is a subset of itself so...
        Set<BookCopy> booksToBuy = new HashSet<BookCopy>();

        for(Integer isbn : sampledPicks){
            booksToBuy.add(new BookCopy(isbn, configuration.getNumBookCopiesToBuy()));
        }

        bs.buyBooks(booksToBuy);

    }

}
