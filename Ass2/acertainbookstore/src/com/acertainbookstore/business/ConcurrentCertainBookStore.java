/**
 *
 */
package com.acertainbookstore.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;

/**
 * ConcurrentCertainBookStore implements the bookstore and its functionality which is
 * defined in the BookStore
 */
public class ConcurrentCertainBookStore implements BookStore, StockManager {
    private Map<Integer, BookStoreBook> bookMap;
    private Map<Integer, ReadWriteLock> why_cant_I_hold_all_these_locks;
    ReadWriteLock I_like_big_locks_and_I_cannot_lie;

    public ConcurrentCertainBookStore() {
        // Constructors are not synchronized
        bookMap = new HashMap<Integer, BookStoreBook>();
        why_cant_I_hold_all_these_locks = new HashMap<Integer, ReadWriteLock>();
        I_like_big_locks_and_I_cannot_lie = new ReentrantReadWriteLock(true);
    }

    private void take_global(boolean write) {
        if(write) {
            I_like_big_locks_and_I_cannot_lie.writeLock().lock();
        } else {
            I_like_big_locks_and_I_cannot_lie.readLock().lock();
        }
    }

    private void release_global(boolean write) {
        if(write) {
            I_like_big_locks_and_I_cannot_lie.writeLock().unlock();
        } else {
            I_like_big_locks_and_I_cannot_lie.readLock().unlock();
        }
    }

    private void take_local(int isbn, boolean write)
        throws BookStoreException {
        ReadWriteLock 눈사람 = why_cant_I_hold_all_these_locks.get(isbn);
        if (눈사람 == null)
            throw new BookStoreException(BookStoreConstants.ISBN + isbn
                                         + BookStoreConstants.NOT_AVAILABLE);

        if(write) {
            눈사람.writeLock().lock();
        } else {
            눈사람.readLock().lock();
        }
    }

    private void release_local(int isbn, boolean write)
        throws BookStoreException {
        ReadWriteLock 눈사람 = why_cant_I_hold_all_these_locks.get(isbn);
        if (눈사람 == null)
            throw new BookStoreException(BookStoreConstants.ISBN + isbn
                                         + BookStoreConstants.NOT_AVAILABLE);

        if(write) {
            눈사람.writeLock().unlock();
        } else {
            눈사람.readLock().unlock();
        }
    }

    public void addBooks(Set<StockBook> bookSet)
        throws BookStoreException {

        take_global(true);

        try {
            if (bookSet == null) {
                throw new BookStoreException(BookStoreConstants.NULL_INPUT);
            }
            // Check if all are there
            for (StockBook book : bookSet) {
                int ISBN = book.getISBN();
                String bookTitle = book.getTitle();
                String bookAuthor = book.getAuthor();
                int noCopies = book.getNumCopies();
                float bookPrice = book.getPrice();
                if (BookStoreUtility.isInvalidISBN(ISBN)
                    || BookStoreUtility.isEmpty(bookTitle)
                    || BookStoreUtility.isEmpty(bookAuthor)
                    || BookStoreUtility.isInvalidNoCopies(noCopies)
                    || bookPrice < 0.0) {
                    throw new BookStoreException(BookStoreConstants.BOOK
                                                 + book.toString() + BookStoreConstants.INVALID);
                } else if (bookMap.containsKey(ISBN)) {
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.DUPLICATED);
                }
            }

            for (StockBook book : bookSet) {
                int ISBN = book.getISBN();
                bookMap.put(ISBN, new BookStoreBook(book));
                why_cant_I_hold_all_these_locks.put(ISBN, new ReentrantReadWriteLock(true));
            }
        } finally {
            release_global(true);
        }
        return;
    }

    public void addCopies(Set<BookCopy> bookCopiesSet)
        throws BookStoreException {
        int ISBN, numCopies;

        take_global(false);

        try {
            if (bookCopiesSet == null) {
                throw new BookStoreException(BookStoreConstants.NULL_INPUT);
            }

            for (BookCopy bookCopy : bookCopiesSet) {
                ISBN = bookCopy.getISBN();
                numCopies = bookCopy.getNumCopies();
                if (BookStoreUtility.isInvalidISBN(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.INVALID);
                if (!bookMap.containsKey(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.NOT_AVAILABLE);
                if (BookStoreUtility.isInvalidNoCopies(numCopies))
                    throw new BookStoreException(BookStoreConstants.NUM_COPIES
                                                 + numCopies + BookStoreConstants.INVALID);

            }

            BookStoreBook book;
            // Update the number of copies
            for (BookCopy bookCopy : bookCopiesSet) {
                ISBN = bookCopy.getISBN();
                numCopies = bookCopy.getNumCopies();
                take_local(ISBN, true);
                try {
                    book = bookMap.get(ISBN);
                    book.addCopies(numCopies);
                } finally {
                    release_local(ISBN, true);
                }
            }
        } finally {
            release_global(false);
        }
    }

    public List<StockBook> getBooks() {
        take_global(false);

        try {
            List<StockBook> listBooks = new ArrayList<StockBook>();
            Collection<Integer> bookMapKeys = bookMap.keys();
            for (Integer ISBN : bookMapValues) {
                take_local(ISBN, true);
                try {
                    listBooks.add(bookMap.get(ISBN).immutableStockBook());
                } finally {
                    release_local(ISBN, true);
                }
            }
            return listBooks;
        } finally {
            release_global(false);
        }
    }

    public void updateEditorPicks(Set<BookEditorPick> editorPicks)
        throws BookStoreException {
        // Check that all ISBNs that we add/remove are there first.
        if (editorPicks == null) {
            throw new BookStoreException(BookStoreConstants.NULL_INPUT);
        }

    	List<BookEditorPick> sortedEditorPicks = new ArrayList<BookEditorPick>(editorPicks);
		Collections.sort
             (sortedEditorPicks,
              new Comparator<BookEditorPick>() {
                 @Override
                 public int compare(BookEditorPick b1, BookEditorPick b2) {
                     int f1 = b1.getISBN();
                     int f2 = b2.getISBN();
                     if      (f1  < f2) return -1;
                     else if (f1 == f2) return  0;
                     else               return  1;
                 }
             });


        int ISBNVal;

        take_global(false);
        List<int> taken = new ArrayList<int>();

        try {
            for (BookEditorPick editorPickArg : editorPicks) {
                ISBNVal = editorPickArg.getISBN();
                if (BookStoreUtility.isInvalidISBN(ISBNVal))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
                                                 + BookStoreConstants.INVALID);
                if (!bookMap.containsKey(ISBNVal))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
                                                 + BookStoreConstants.NOT_AVAILABLE);
            }

            for (BookEditorPick editorPickArg : editorPicks) {
                Integer ISBN = editorPickArg.getISBN();
                take_local(ISBN, true);
                taken.add(ISBN);
                bookMap.get(ISBN).setEditorPick(editorPickArg.isEditorPick());
            }
            return;
        } finally {
            for(int ISBN : taken) {
                release_local(ISBN, true);
            }
            release_global(false);
        }
    }

    public void buyBooks(Set<BookCopy> bookCopiesToBuy)
        throws BookStoreException {
        if (bookCopiesToBuy == null) {
            throw new BookStoreException(BookStoreConstants.NULL_INPUT);
        }

        // Check that all ISBNs that we buy are there first.
        int ISBN;
        BookStoreBook book;
        Boolean saleMiss = false;

        take_global(true);
        List<int> taken = new ArrayList<int>();

        try {
            for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
                ISBN = bookCopyToBuy.getISBN();
                if (bookCopyToBuy.getNumCopies() < 0)
                    throw new BookStoreException(BookStoreConstants.NUM_COPIES
                                                 + bookCopyToBuy.getNumCopies()
                                                 + BookStoreConstants.INVALID);
                if (BookStoreUtility.isInvalidISBN(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.INVALID);
                if (!bookMap.containsKey(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.NOT_AVAILABLE);
                take_local(ISBN, true);
                taken.add(ISBN);
                book = bookMap.get(ISBN);
                if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
                    book.addSaleMiss(); // If we cannot sell the copies of the book
                    // its a miss
                    saleMiss = true;
                }
            }

            // We throw exception now since we want to see how many books in the
            // order incurred misses which is used by books in demand
            if (saleMiss)
                throw new BookStoreException(BookStoreConstants.BOOK
                                             + BookStoreConstants.NOT_AVAILABLE);

            // Then make purchase
            for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
                book = bookMap.get(bookCopyToBuy.getISBN());
                book.buyCopies(bookCopyToBuy.getNumCopies());
            }
            return;
        } finally {
            for(int ISBN : taken) {
                release_local(ISBN, true);
            }
            release_global(true);
        }
    }


    public List<StockBook> getBooksByISBN(Set<Integer> isbnSet)
        throws BookStoreException {
        take_global(false);
        try {
            if (isbnSet == null) {
                throw new BookStoreException(BookStoreConstants.NULL_INPUT);
            }
            for (Integer ISBN : isbnSet) {
                if (BookStoreUtility.isInvalidISBN(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.INVALID);
                if (!bookMap.containsKey(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.NOT_AVAILABLE);
            }

            List<StockBook> listBooks = new ArrayList<StockBook>();

            for (Integer ISBN : isbnSet) {
                take_local(ISBN, false);
                try {
                    listBooks.add(bookMap.get(ISBN).immutableStockBook());
                } finally {
                    release_local(ISBN, false);
                }
            }

            return listBooks;
        } finally {
            release_global(false);
        }
    }

    public List<Book> getBooks(Set<Integer> isbnSet)
        throws BookStoreException {
        take_global(false);
        try {
            if (isbnSet == null) {
                throw new BookStoreException(BookStoreConstants.NULL_INPUT);
            }
            // Check that all ISBNs that we rate are there first.
            for (Integer ISBN : isbnSet) {
                if (BookStoreUtility.isInvalidISBN(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.INVALID);
                if (!bookMap.containsKey(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.NOT_AVAILABLE);
            }

            List<Book> listBooks = new ArrayList<Book>();

            // Get the books
            for (Integer ISBN : isbnSet) {
                take_local(ISBN, false);
                try {
                    listBooks.add(bookMap.get(ISBN).immutableBook());
                } finally {
                    release_local(ISBN, false);
                }
            }
            return listBooks;
        } finally {
            release_global(false);
        }
    }

    public List<Book> getEditorPicks(int numBooks)
        throws BookStoreException {
        take_global(false);
        ArrayList<int> taken = new ArrayList<int>();
        try {
            if (numBooks < 0) {
                throw new BookStoreException("numBooks = " + numBooks
                                             + ", but it must be positive");
            }

            List<Book> listEditorPicks = new ArrayList<Book>();
            Collection<int> bookMapKeys = bookMap.keys();
            List<int> toBeAdded = List<Integer>();
            for (int ISBN : bookMapValues) {
                take_local(ISBN, false);
                taken.add(ISBN);

                if (bookMap.get(ISBN).isEditorPick()) {
                    toBeAdded.add(ISBN);
                }
            }

            // Find numBooks random indices of books that will be picked
            Random rand = new Random();
            int totalCount = toBeAdded.size();
            for(int totalCount = toBeAdded.size(); totalcount > 0 && numBooks > 0; totalcount--, numBooks--) {
                int nextIndex = rand.nextInt(totalCount);
                int nextISBN  = toBeAdded.get(nextIndex);
                int lastISBN  = toBeAdded.get(totalCount-1);
                listEditorPicks.add(bookMap.get(nextISBN));
                toBeAdded.set(nextIndex, lastISBN);

            }

            return listEditorPicks;
        } finally {
            for(int ISBN : taken) {
                release_local(ISBN, true);
            }
            release_global(false);
        }

    }

    public void removeAllBooks() throws BookStoreException {
        take_global(true);
        try {
            bookMap.clear();
        } finally {
            release_global(true);
        }
    }

    public void removeBooks(Set<Integer> isbnSet)
        throws BookStoreException {
        take_global(true);
        try {

            if (isbnSet == null) {
                throw new BookStoreException(BookStoreConstants.NULL_INPUT);
            }
            for (Integer ISBN : isbnSet) {
                if (BookStoreUtility.isInvalidISBN(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.INVALID);
                if (!bookMap.containsKey(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.NOT_AVAILABLE);
            }

            for (int isbn : isbnSet) {
                bookMap.remove(isbn);
            }
        } finally {
            release_global(true);
        }
    }
}
