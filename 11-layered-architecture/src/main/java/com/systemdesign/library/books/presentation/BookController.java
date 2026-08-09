package com.systemdesign.library.books.presentation;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.systemdesign.library.books.application.CreateBookUseCase;
import com.systemdesign.library.books.application.GetBookUseCase;
import com.systemdesign.library.books.application.ListBooksUseCase;
import com.systemdesign.library.books.presentation.dto.BookResponse;
import com.systemdesign.library.books.presentation.dto.CreateBookRequest;

/**
 * Presentation layer. Talks only to the Application layer's use cases -- never touches the
 * Domain layer or a repository directly.
 */
@Tag(name = "books")
@RestController
@RequestMapping("/books")
public class BookController {

    private final CreateBookUseCase createBookUseCase;
    private final ListBooksUseCase listBooksUseCase;
    private final GetBookUseCase getBookUseCase;

    public BookController(
            CreateBookUseCase createBookUseCase, ListBooksUseCase listBooksUseCase, GetBookUseCase getBookUseCase) {
        this.createBookUseCase = createBookUseCase;
        this.listBooksUseCase = listBooksUseCase;
        this.getBookUseCase = getBookUseCase;
    }

    @PostMapping
    @Operation(summary = "Add a book to the catalog")
    @ApiResponse(responseCode = "201", description = "Book created.")
    public ResponseEntity<BookResponse> create(@Valid @RequestBody CreateBookRequest request) {
        BookResponse response = BookResponse.from(createBookUseCase.execute(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List every book in the catalog")
    public List<BookResponse> list() {
        return listBooksUseCase.execute().stream().map(BookResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single book by id")
    @ApiResponse(responseCode = "404", description = "No book with this id.")
    public BookResponse get(@PathVariable UUID id) {
        return BookResponse.from(getBookUseCase.execute(id));
    }
}
