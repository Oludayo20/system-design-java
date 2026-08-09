package com.systemdesign.bookhive.catalog.books;

import com.systemdesign.bookhive.catalog.books.dto.BookResponse;
import com.systemdesign.bookhive.catalog.books.dto.CreateBookRequest;
import com.systemdesign.bookhive.catalog.books.dto.ReserveStockRequest;
import com.systemdesign.bookhive.catalog.books.dto.ReserveStockResponse;
import com.systemdesign.bookhive.catalog.security.JwtVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "books")
@RestController
@RequestMapping("/books")
public class BooksController {

    private final BooksService booksService;
    private final JwtVerifier jwtVerifier;

    public BooksController(BooksService booksService, JwtVerifier jwtVerifier) {
        this.booksService = booksService;
        this.jwtVerifier = jwtVerifier;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add a book to the catalog")
    public BookResponse create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                @Valid @RequestBody CreateBookRequest dto) {
        jwtVerifier.requireBearer(authorization);
        return BookResponse.from(booksService.create(dto));
    }

    @GetMapping
    @Operation(summary = "List all books")
    public List<BookResponse> findAll() {
        return booksService.findAll().stream().map(BookResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single book")
    public BookResponse findOne(@PathVariable UUID id) {
        return BookResponse.from(booksService.findOne(id));
    }

    @PostMapping("/{id}/reserve")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Reserve stock for an order (called by order-service, not end users)",
            description = "Atomically decrements stock and returns price + remaining stock. This is the "
                    + "ONLY way order-service learns a book's price or stock - it has no catalog-db "
                    + "connection string. order-service forwards the caller's bearer token here, so this "
                    + "endpoint enforces the same auth as everything else.")
    public ReserveStockResponse reserve(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @PathVariable UUID id,
                                         @Valid @RequestBody ReserveStockRequest dto) {
        jwtVerifier.requireBearer(authorization);
        return booksService.reserve(id, dto.quantity());
    }
}
