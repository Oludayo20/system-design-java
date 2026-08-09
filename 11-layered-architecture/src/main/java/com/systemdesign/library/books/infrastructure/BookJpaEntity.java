package com.systemdesign.library.books.infrastructure;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Data Access layer. This is the ONLY book-shaped class allowed to know about JPA annotations or
 * the database schema. It is deliberately separate from {@code books.domain.Book} (the pure
 * domain class) -- the ORM row shape and the business object shape are allowed to diverge without
 * either layer noticing.
 */
@Entity
@Table(name = "books")
public class BookJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, length = 20)
    private String isbn;

    @Column(name = "total_copies", nullable = false)
    private int totalCopies;

    @Column(name = "available_copies", nullable = false)
    private int availableCopies;

    protected BookJpaEntity() {
        // required by JPA
    }

    public BookJpaEntity(UUID id, String title, String author, String isbn, int totalCopies, int availableCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
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
