package com.systemdesign.library.loans.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.systemdesign.library.books.domain.Book;
import com.systemdesign.library.books.domain.BookRepositoryPort;
import com.systemdesign.library.loans.domain.BookUnavailableException;
import com.systemdesign.library.loans.domain.Loan;
import com.systemdesign.library.loans.domain.LoanEligibilityRules;
import com.systemdesign.library.loans.domain.LoanRepositoryPort;
import com.systemdesign.library.loans.domain.MaxActiveLoansExceededException;
import com.systemdesign.library.loans.domain.OverdueLoanExistsException;
import com.systemdesign.library.loans.presentation.dto.BorrowBookRequest;
import com.systemdesign.library.members.domain.Member;
import com.systemdesign.library.members.domain.MemberRepositoryPort;

/**
 * Application layer. Orchestrates the borrow flow: fetch the book and member through their
 * repository ports, ask the Domain layer whether borrowing is allowed, and -- only if the Domain
 * layer doesn't object -- mutate stock and persist a new loan.
 *
 * Note what this class does NOT do: it never checks {@code availableCopies} or counts active
 * loans itself. Deciding "can this member borrow" is entirely {@link LoanEligibilityRules}' job
 * (Domain). This use case only coordinates the steps and translates domain errors into
 * HTTP-shaped ones.
 */
@Service
public class BorrowBookUseCase {

    private static final long LOAN_PERIOD_DAYS = 14;

    private final LoanRepositoryPort loanRepository;
    private final BookRepositoryPort bookRepository;
    private final MemberRepositoryPort memberRepository;

    public BorrowBookUseCase(
            LoanRepositoryPort loanRepository, BookRepositoryPort bookRepository, MemberRepositoryPort memberRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
    }

    public Loan execute(BorrowBookRequest request) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No book with id " + request.bookId() + "."));

        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No member with id " + request.memberId() + "."));

        List<Loan> activeLoans = loanRepository.findActiveByMemberId(request.memberId());

        // Ask the domain. It throws if any of the 4 business rules is violated.
        try {
            LoanEligibilityRules.assertCanBorrow(book, activeLoans, Instant.now());
        } catch (BookUnavailableException | MaxActiveLoansExceededException | OverdueLoanExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        book.borrowOneCopy();
        bookRepository.save(book);

        Instant borrowedAt = Instant.now();
        Instant dueAt = borrowedAt.plus(LOAN_PERIOD_DAYS, ChronoUnit.DAYS);
        Loan loan = new Loan(UUID.randomUUID(), book.getId(), member.getId(), borrowedAt, dueAt, null);

        return loanRepository.save(loan);
    }
}
