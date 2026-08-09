package com.systemdesign.library.loans.application;

import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.systemdesign.library.books.domain.BookRepositoryPort;
import com.systemdesign.library.loans.domain.Loan;
import com.systemdesign.library.loans.domain.LoanAlreadyReturnedException;
import com.systemdesign.library.loans.domain.LoanRepositoryPort;

/**
 * Application layer. Orchestrates the return flow: load the loan, ask the Domain layer to mark it
 * returned (rule 4), then free up a copy on the book. Again, the decision ("is this loan even
 * returnable right now?") lives in {@code Loan.markReturned()}, not here.
 */
@Service
public class ReturnBookUseCase {

    private final LoanRepositoryPort loanRepository;
    private final BookRepositoryPort bookRepository;

    public ReturnBookUseCase(LoanRepositoryPort loanRepository, BookRepositoryPort bookRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
    }

    public Loan execute(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No loan with id " + loanId + "."));

        try {
            loan.markReturned(Instant.now());
        } catch (LoanAlreadyReturnedException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        bookRepository.findById(loan.getBookId()).ifPresent(book -> {
            book.returnOneCopy();
            bookRepository.save(book);
        });

        return loanRepository.save(loan);
    }
}
