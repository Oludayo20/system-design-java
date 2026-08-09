package com.systemdesign.bookhive.catalog.books;

import com.systemdesign.bookhive.catalog.books.dto.CreateBookRequest;
import com.systemdesign.bookhive.catalog.books.dto.ReserveStockResponse;
import com.systemdesign.bookhive.catalog.books.entity.Book;
import com.systemdesign.bookhive.catalog.shared.exception.BadRequestException;
import com.systemdesign.bookhive.catalog.shared.exception.ConflictException;
import com.systemdesign.bookhive.catalog.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BooksService {

    private final BookRepository books;

    public BooksService(BookRepository books) {
        this.books = books;
    }

    public Book create(CreateBookRequest dto) {
        Book book = new Book();
        book.setTitle(dto.title());
        book.setAuthor(dto.author());
        book.setPriceCents(dto.priceCents());
        book.setStock(dto.stock());
        return books.save(book);
    }

    public List<Book> findAll() {
        return books.findAllByOrderByCreatedAtDesc();
    }

    public Book findOne(UUID id) {
        return books.findById(id).orElseThrow(() -> new NotFoundException("Book " + id + " not found"));
    }

    /**
     * Atomically decrements stock in a single conditional UPDATE so two concurrent reservations
     * can never both succeed against the same last unit. This is the ONLY way order-service
     * learns a book's price or stock - it has no catalog-db connection string.
     */
    @Transactional
    public ReserveStockResponse reserve(UUID id, int quantity) {
        if (quantity < 1) {
            throw new BadRequestException("quantity must be at least 1");
        }

        Book book = findOne(id);

        int affected = books.reserveStock(id, quantity);
        if (affected == 0) {
            throw new ConflictException(
                    "Insufficient stock for \"" + book.getTitle() + "\" (have " + book.getStock() + ", requested " + quantity + ")");
        }

        return new ReserveStockResponse(id, book.getPriceCents(), book.getPriceCents() * quantity, book.getStock() - quantity);
    }
}
