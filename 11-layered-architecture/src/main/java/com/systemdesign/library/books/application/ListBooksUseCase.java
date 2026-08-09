package com.systemdesign.library.books.application;

import java.util.List;
import org.springframework.stereotype.Service;
import com.systemdesign.library.books.domain.Book;
import com.systemdesign.library.books.domain.BookRepositoryPort;

@Service
public class ListBooksUseCase {

    private final BookRepositoryPort bookRepository;

    public ListBooksUseCase(BookRepositoryPort bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> execute() {
        return bookRepository.findAll();
    }
}
