package com.systemdesign.library.books.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A port (interface). The Domain and Application layers depend on this abstraction only. The
 * Data Access layer ({@code books.infrastructure.BookRepositoryAdapter}) is the sole place that
 * implements it with a real ORM/database call.
 */
public interface BookRepositoryPort {

    Book save(Book book);

    Optional<Book> findById(UUID id);

    List<Book> findAll();
}
