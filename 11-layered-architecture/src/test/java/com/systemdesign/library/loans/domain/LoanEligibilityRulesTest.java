package com.systemdesign.library.loans.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.systemdesign.library.books.domain.Book;

/**
 * These tests instantiate plain {@link Book} and {@link Loan} objects directly -- no
 * {@code @SpringBootTest}, no {@code @DataJpaTest}, no application context, not even an HTTP
 * request. This is the payoff of the layering described in the README: the actual business rules
 * run and assert in milliseconds, and a reviewer can trust that borrowing eligibility is entirely
 * captured by this one class.
 */
class LoanEligibilityRulesTest {

    private static final UUID BOOK_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();

    private static Book book(int availableCopies) {
        return new Book(BOOK_ID, "Domain-Driven Design", "Eric Evans", "978-0321125217", 3, availableCopies);
    }

    private static Loan activeLoan(Instant dueAt) {
        return new Loan(UUID.randomUUID(), BOOK_ID, MEMBER_ID, Instant.now(), dueAt, null);
    }

    private static Loan returnedLoan(Instant dueAt) {
        return new Loan(
                UUID.randomUUID(),
                BOOK_ID,
                MEMBER_ID,
                Instant.parse("2020-01-01T00:00:00Z"),
                dueAt,
                Instant.parse("2020-01-10T00:00:00Z"));
    }

    private static Instant future() {
        return Instant.now().plus(7, ChronoUnit.DAYS);
    }

    private static Instant past() {
        return Instant.now().minus(7, ChronoUnit.DAYS);
    }

    @Test
    void allowsBorrowingWhenACopyIsAvailableAndTheMemberHasNoActiveLoans() {
        assertThatCode(() -> LoanEligibilityRules.assertCanBorrow(book(1), List.of())).doesNotThrowAnyException();
    }

    @Test
    void rejectsBorrowingWhenAvailableCopiesIsZero_rule1() {
        assertThatThrownBy(() -> LoanEligibilityRules.assertCanBorrow(book(0), List.of()))
                .isInstanceOf(BookUnavailableException.class);
    }

    @Test
    void rejectsBorrowingWhenTheMemberAlreadyHasThreeActiveLoans_rule2() {
        List<Loan> threeActiveLoans = List.of(activeLoan(future()), activeLoan(future()), activeLoan(future()));
        assertThatThrownBy(() -> LoanEligibilityRules.assertCanBorrow(book(1), threeActiveLoans))
                .isInstanceOf(MaxActiveLoansExceededException.class);
    }

    @Test
    void allowsBorrowingWhenTheMemberHasOnlyTwoActiveLoans() {
        List<Loan> twoActiveLoans = List.of(activeLoan(future()), activeLoan(future()));
        assertThatCode(() -> LoanEligibilityRules.assertCanBorrow(book(1), twoActiveLoans)).doesNotThrowAnyException();
    }

    @Test
    void rejectsBorrowingWhenTheMemberHasAnOverdueUnreturnedLoan_rule3() {
        List<Loan> overdue = List.of(activeLoan(past()));
        assertThatThrownBy(() -> LoanEligibilityRules.assertCanBorrow(book(1), overdue))
                .isInstanceOf(OverdueLoanExistsException.class);
    }

    @Test
    void doesNotCountReturnedLoansTowardTheActiveLoanLimitOrOverdueCheck() {
        List<Loan> returnedButPastDue = List.of(returnedLoan(past()));
        assertThatCode(() -> LoanEligibilityRules.assertCanBorrow(book(1), returnedButPastDue))
                .doesNotThrowAnyException();
    }

    @Test
    void checksCopyAvailabilityBeforeTheActiveLoanCount() {
        // Even with 0 active loans, no copies means no borrow.
        assertThatThrownBy(() -> LoanEligibilityRules.assertCanBorrow(book(0), List.of()))
                .isInstanceOf(BookUnavailableException.class);
    }
}
