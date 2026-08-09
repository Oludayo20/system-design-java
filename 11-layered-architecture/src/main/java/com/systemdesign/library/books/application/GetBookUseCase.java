package com.systemdesign.library.books.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.systemdesign.library.books.domain.Book;
import com.systemdesign.library.books.domain.BookRepositoryPort;

@Service
public class GetBookUseCase {

    private final BookRepositoryPort bookRepository;

    public GetBookUseCase(BookRepositoryPort bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Book execute(UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No book with id " + id + "."));
    }
}
