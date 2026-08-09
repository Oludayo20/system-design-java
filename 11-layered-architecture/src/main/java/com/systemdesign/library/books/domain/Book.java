package com.systemdesign.library.books.domain;

import java.util.UUID;

/**
 * Domain layer. Plain Java only -- no Spring, no JPA/Hibernate, no HTTP concepts. This class must
 * be constructible and testable with nothing more than {@code javac}/JUnit; {@code java.util.UUID}
 * is a JDK type, not a framework one, so it's fine to use here.
 */
public class Book {

    private final UUID id;
    private String title;
    private String author;
    private String isbn;
    private int totalCopies;
    private int availableCopies;

    public Book(UUID id, String title, String author, String isbn, int totalCopies, int availableCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    /** Business rule #1 (part of it): a book can only be borrowed if a copy is available. */
    public boolean hasAvailableCopies() {
        return availableCopies > 0;
    }

    /** Decrements available stock. Callers are expected to have already checked eligibility. */
    public void borrowOneCopy() {
        if (!hasAvailableCopies()) {
            throw new IllegalStateException("Cannot borrow \"" + title + "\": no copies available.");
        }
        availableCopies -= 1;
    }

    /** Business rule #4: returning a book increments availableCopies. */
    public void returnOneCopy() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("Cannot return \"" + title + "\": all copies are already accounted for.");
        }
        availableCopies += 1;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }
}
