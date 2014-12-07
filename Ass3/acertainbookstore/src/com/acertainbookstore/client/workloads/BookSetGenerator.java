package com.acertainbookstore.client.workloads;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Random;
import java.util.List;

import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {

    public BookSetGenerator() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Returns num randomly selected isbns from the input set
     *
     * @param num
     * @return
     */
    public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) {
        Set<Integer> res = new HashSet<Integer>();
        List<Integer> isbnsL = new ArrayList<Integer>(isbns);
        Random rand = new Random();

        for(int totalCount = isbnsL.size(); totalCount > 0 && num > 0; totalCount--, num--) {
            int nextIndex = rand.nextInt(totalCount);
            int nextISBN  = isbnsL.get(nextIndex);
            int lastISBN  = isbnsL.get(totalCount-1);
            res.add(nextISBN);
            isbnsL.set(nextIndex, lastISBN);
        }

        return res;
    }

    /**
     * Return num stock books. For now return an ImmutableStockBook
     *
     * @param num
     * @return
     */
    public Set<StockBook> nextSetOfStockBooks(int num) {
        Set<StockBook> res = new HashSet<StockBook>();
        Random rand = new Random();

        for(int n = 0; n < num; n++) {
            int ISBN = 1234 + rand.nextInt(1000);
            String title = "AbstractSingletonProxyFactoryBean" + ISBN;
            String author = "Adolf Hitler";
            res.add(new ImmutableStockBook(ISBN, title, author, 1 + rand.nextInt(1000), 1 + rand.nextInt(200), 0, 0, 0, rand.nextBoolean()));
        }

        return res;
    }

}
