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

    private boolean take_local(int isbn, boolean write) {
        ReadWriteLock 눈사람 = why_cant_I_hold_all_these_locks.get(isbn);
        if (눈사람 == null) return false;

        if(write) {
            눈사람.writeLock().lock();
        } else {
            눈사람.readLock().lock();
        }

        return true;
    }

    private boolean release_local(int isbn, boolean write) {
        ReadWriteLock 눈사람 = why_cant_I_hold_all_these_locks.get(isbn);
        if (눈사람 == null) return false;

        if(write) {
            눈사람.writeLock().unlock();
        } else {
            눈사람.readLock().unlock();
        }

        return true;
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
        BookStoreBook book;

        if (bookCopiesSet == null) {
            throw new BookStoreException(BookStoreConstants.NULL_INPUT);
        }

        List<Integer> taken = new ArrayList<Integer>();
        List<BookCopy> sortedBookCopiesSet = new ArrayList<BookCopy>(bookCopiesSet);
        Collections.sort
            (sortedBookCopiesSet,
             new Comparator<BookCopy>() {
                @Override
                    public int compare(BookCopy b1, BookCopy b2) {
                    int f1 = b1.getISBN();
                    int f2 = b2.getISBN();
                    if      (f1  < f2) return -1;
                    else if (f1 == f2) return  0;
                    else               return  1;
                }
            });

        take_global(false);

        try {
            for (BookCopy bookCopy : sortedBookCopiesSet) {
                int ISBN = bookCopy.getISBN();
                int numCopies = bookCopy.getNumCopies();
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

            // Update the number of copies
            for (BookCopy bookCopy : sortedBookCopiesSet) {
                int ISBN = bookCopy.getISBN();
                int numCopies = bookCopy.getNumCopies();
                take_local(ISBN, true);
                taken.add(ISBN);
                book = bookMap.get(ISBN);
                book.addCopies(numCopies);
            }
        } finally {
            for(int ISBN : taken) {
                release_local(ISBN, true);
            }
            release_global(false);
        }
    }

    public List<StockBook> getBooks() {
        List<StockBook> listBooks = new ArrayList<StockBook>();
        List<Integer> sortedBookMapKeys = new ArrayList<Integer>(bookMap.keySet());
        Collections.sort(sortedBookMapKeys);
        List<Integer> taken = new ArrayList<Integer>();

        take_global(false);

        try {
            for (Integer ISBN : sortedBookMapKeys) {
                take_local(ISBN, false);
                taken.add(ISBN);
                listBooks.add(bookMap.get(ISBN).immutableStockBook());
            }
            return listBooks;
        } finally {
            for(int ISBN : taken) {
                release_local(ISBN, false);
            }
            release_global(false);
        }
    }

    public void updateEditorPicks(Set<BookEditorPick> editorPicks)
        throws BookStoreException {

        List<Integer> taken = new ArrayList<Integer>();
        int ISBNVal;

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


        take_global(false);

        try {
            for (BookEditorPick editorPickArg : sortedEditorPicks) {
                ISBNVal = editorPickArg.getISBN();
                if (BookStoreUtility.isInvalidISBN(ISBNVal))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
                                                 + BookStoreConstants.INVALID);
                if (!bookMap.containsKey(ISBNVal))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBNVal
                                                 + BookStoreConstants.NOT_AVAILABLE);
            }

            for (BookEditorPick editorPickArg : sortedEditorPicks) {
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
        BookStoreBook book;
        Boolean saleMiss = false;
        List<Integer> taken = new ArrayList<Integer>();

        List<BookCopy> sortedBookCopiesToBuy = new ArrayList<BookCopy>(bookCopiesToBuy);
        Collections.sort
            (sortedBookCopiesToBuy,
             new Comparator<BookCopy>() {
                @Override
                    public int compare(BookCopy b1, BookCopy b2) {
                    int f1 = b1.getISBN();
                    int f2 = b2.getISBN();
                    if      (f1  < f2) return -1;
                    else if (f1 == f2) return  0;
                    else               return  1;
                }
            });

        take_global(false);

        try {
            for (BookCopy bookCopyToBuy : sortedBookCopiesToBuy) {
                int ISBN = bookCopyToBuy.getISBN();
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
            for (BookCopy bookCopyToBuy : sortedBookCopiesToBuy) {
                book = bookMap.get(bookCopyToBuy.getISBN());
                book.buyCopies(bookCopyToBuy.getNumCopies());
            }
            return;
        } finally {
            for(int ISBN : taken) {
                release_local(ISBN, true);
            }
            release_global(false);
        }
    }


    public List<StockBook> getBooksByISBN(Set<Integer> isbnSet)
        throws BookStoreException {

        if (isbnSet == null) {
            throw new BookStoreException(BookStoreConstants.NULL_INPUT);
        }


        List<Integer> sortedIsbnSet = new ArrayList<Integer>(isbnSet);
        List<Integer> taken = new ArrayList<Integer>();
        Collections.sort(sortedIsbnSet);

        take_global(false);

        try {
            for (Integer ISBN : sortedIsbnSet) {
                if (BookStoreUtility.isInvalidISBN(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.INVALID);
                if (!bookMap.containsKey(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.NOT_AVAILABLE);
            }

            List<StockBook> listBooks = new ArrayList<StockBook>();

            for (Integer ISBN : sortedIsbnSet) {
                take_local(ISBN, false);
                taken.add(ISBN);
                listBooks.add(bookMap.get(ISBN).immutableStockBook());
            }

            return listBooks;
        } finally {
            for(int ISBN : taken) {
                release_local(ISBN, false);
            }
            release_global(false);
        }
    }

    public List<Book> getBooks(Set<Integer> isbnSet)
        throws BookStoreException {

        if (isbnSet == null) {
            throw new BookStoreException(BookStoreConstants.NULL_INPUT);
        }

        List<Integer> sortedIsbnSet = new ArrayList<Integer>(isbnSet);
        List<Integer> taken = new ArrayList<Integer>();
        Collections.sort(sortedIsbnSet);

        take_global(false);

        try {
            // Check that all ISBNs that we rate are there first.
            for (Integer ISBN : sortedIsbnSet) {
                if (BookStoreUtility.isInvalidISBN(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.INVALID);
                if (!bookMap.containsKey(ISBN))
                    throw new BookStoreException(BookStoreConstants.ISBN + ISBN
                                                 + BookStoreConstants.NOT_AVAILABLE);
            }

            List<Book> listBooks = new ArrayList<Book>();

            // Get the books
            for (Integer ISBN : sortedIsbnSet) {
                take_local(ISBN, false);
                taken.add(ISBN);
                listBooks.add(bookMap.get(ISBN).immutableBook());
            }
            return listBooks;
        } finally {
            for(int ISBN : taken) {
                release_local(ISBN, false);
            }
            release_global(false);
        }
    }

    public List<Book> getEditorPicks(int numBooks)
        throws BookStoreException {
        take_global(false);
        ArrayList<Integer> taken = new ArrayList<Integer>();
        try {
            if (numBooks < 0) {
                throw new BookStoreException("numBooks = " + numBooks
                                             + ", but it must be positive");
            }

            List<Book> listEditorPicks = new ArrayList<Book>();
            List<Integer> sortedBookMapKeys = new ArrayList<Integer>(bookMap.keySet());
            List<Integer> toBeAdded = new ArrayList<Integer>();
            Collections.sort(sortedBookMapKeys);
            for (int ISBN : sortedBookMapKeys) {
                take_local(ISBN, false);
                taken.add(ISBN);

                if (bookMap.get(ISBN).isEditorPick()) {
                    toBeAdded.add(ISBN);
                }
            }

            // Find numBooks random indices of books that will be picked
            Random rand = new Random();
            for(int totalCount = toBeAdded.size(); totalCount > 0 && numBooks > 0; totalCount--, numBooks--) {
                int nextIndex = rand.nextInt(totalCount);
                int nextISBN  = toBeAdded.get(nextIndex);
                int lastISBN  = toBeAdded.get(totalCount-1);
                listEditorPicks.add(bookMap.get(nextISBN));
                toBeAdded.set(nextIndex, lastISBN);

            }

            return listEditorPicks;
        } finally {
            for(int ISBN : taken) {
                release_local(ISBN, false);
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
