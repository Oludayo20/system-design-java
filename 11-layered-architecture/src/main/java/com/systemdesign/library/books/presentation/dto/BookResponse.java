package com.systemdesign.library.books.presentation.dto;

import java.util.UUID;
import com.systemdesign.library.books.domain.Book;

public record BookResponse(
        UUID id, String title, String author, String isbn, int totalCopies, int availableCopies) {

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getTotalCopies(),
                book.getAvailableCopies());
    }
}
