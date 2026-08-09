package com.systemdesign.library.books.application;

import java.util.UUID;
import org.springframework.stereotype.Service;
import com.systemdesign.library.books.domain.Book;
import com.systemdesign.library.books.domain.BookRepositoryPort;
import com.systemdesign.library.books.presentation.dto.CreateBookRequest;

/**
 * Application layer. Orchestrates a single use case -- it does not itself decide any business
 * rule; here there isn't even a rule to ask, just a step to run: build the domain object and hand
 * it to the repository port.
 */
@Service
public class CreateBookUseCase {

    private final BookRepositoryPort bookRepository;

    public CreateBookUseCase(BookRepositoryPort bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Book execute(CreateBookRequest request) {
        Book book = new Book(
                UUID.randomUUID(),
                request.title(),
                request.author(),
                request.isbn(),
                request.totalCopies(),
                request.totalCopies() // a freshly catalogued book starts fully available
        );
        return bookRepository.save(book);
    }
}
