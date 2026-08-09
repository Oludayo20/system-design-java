package com.systemdesign.library.loans.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.systemdesign.library.books.domain.Book;

/**
 * The actual business rules for borrowing, as pure static methods over plain domain objects.
 * Nothing in this file imports Spring or JPA, and nothing here touches a database -- it operates
 * entirely on {@link Book} and {@link Loan} instances handed to it by the Application layer.
 *
 * This is the part of the codebase reviewers should scrutinize most closely: it is the reason the
 * other four layers exist. See {@code LoanEligibilityRulesTest} for tests that exercise this
 * class with zero setup -- no Spring context, no database, no HTTP server.
 */
public final class LoanEligibilityRules {

    private static final int MAX_ACTIVE_LOANS = 3;

    private LoanEligibilityRules() {
    }

    /**
     * Throws a domain error if the member is not allowed to borrow {@code book} right now.
     * Returns normally (no throw) if borrowing is allowed.
     */
    public static void assertCanBorrow(Book book, List<Loan> memberActiveLoans, Instant now) {
        // Rule 1: no available copies.
        if (!book.hasAvailableCopies()) {
            throw new BookUnavailableException("\"" + book.getTitle() + "\" has no available copies right now.");
        }

        // Rule 2: at most 3 active (unreturned) loans at a time.
        if (memberActiveLoans.size() >= MAX_ACTIVE_LOANS) {
            throw new MaxActiveLoansExceededException(
                    "Member already has " + memberActiveLoans.size() + " active loan(s) (limit is " + MAX_ACTIVE_LOANS + ").");
        }

        // Rule 3: any overdue, unreturned loan blocks further borrowing.
        Optional<Loan> overdueLoan = memberActiveLoans.stream().filter(loan -> loan.isOverdue(now)).findFirst();
        if (overdueLoan.isPresent()) {
            Loan loan = overdueLoan.get();
            throw new OverdueLoanExistsException(
                    "Member has an overdue loan (loan " + loan.getId() + ", due " + loan.getDueAt()
                            + ") and cannot borrow further books until it is returned.");
        }
    }

    public static void assertCanBorrow(Book book, List<Loan> memberActiveLoans) {
        assertCanBorrow(book, memberActiveLoans, Instant.now());
    }
}
