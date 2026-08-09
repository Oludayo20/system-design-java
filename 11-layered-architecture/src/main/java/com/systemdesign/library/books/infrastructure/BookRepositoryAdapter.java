package com.systemdesign.library.books.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import com.systemdesign.library.books.domain.Book;
import com.systemdesign.library.books.domain.BookRepositoryPort;

/**
 * Data Access layer. The only class that implements {@link BookRepositoryPort} with a real
 * database call. It converts between the ORM row shape ({@link BookJpaEntity}) and the pure
 * domain object ({@link Book}) at the boundary, so nothing above this layer ever sees a JPA type.
 *
 * This is also the DI wiring point: because it's the only bean implementing
 * {@code BookRepositoryPort}, Spring autowires it wherever the port is requested (e.g. in
 * {@code books.application.*UseCase} constructors) -- the equivalent of the {@code useClass}
 * binding in the NestJS module files.
 */
@Repository
public class BookRepositoryAdapter implements BookRepositoryPort {

    private final BookJpaRepository jpaRepository;

    public BookRepositoryAdapter(BookJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Book save(Book book) {
        BookJpaEntity saved = jpaRepository.save(toEntity(book));
        return toDomain(saved);
    }

    @Override
    public Optional<Book> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Book> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private Book toDomain(BookJpaEntity row) {
        return new Book(row.getId(), row.getTitle(), row.getAuthor(), row.getIsbn(), row.getTotalCopies(), row.getAvailableCopies());
    }

    private BookJpaEntity toEntity(Book book) {
        return new BookJpaEntity(
                book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(), book.getTotalCopies(), book.getAvailableCopies());
    }
}
