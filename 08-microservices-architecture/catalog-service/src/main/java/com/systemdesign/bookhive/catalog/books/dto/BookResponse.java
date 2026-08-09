package com.systemdesign.bookhive.catalog.books.dto;

import com.systemdesign.bookhive.catalog.books.entity.Book;

import java.util.UUID;

public record BookResponse(UUID id, String title, String author, Integer priceCents, Integer stock) {

    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getPriceCents(), book.getStock());
    }
}
