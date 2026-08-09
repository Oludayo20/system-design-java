package com.systemdesign.library.loans.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain tests -- no Spring context, no database. */
class LoanTest {

    @Test
    void isActiveUntilReturned() {
        Loan loan = new Loan(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(1), null);
        assertThat(loan.isActive()).isTrue();
    }

    @Test
    void isOverdueWhenUnreturnedAndPastItsDueDate() {
        Loan loan = new Loan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-15T00:00:00Z"),
                null);
        assertThat(loan.isOverdue(Instant.parse("2024-02-01T00:00:00Z"))).isTrue();
    }

    @Test
    void isNotOverdueWhenReturnedEvenIfPastTheOriginalDueDate() {
        Loan loan = new Loan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-15T00:00:00Z"),
                Instant.parse("2024-01-10T00:00:00Z"));
        assertThat(loan.isOverdue(Instant.parse("2024-02-01T00:00:00Z"))).isFalse();
    }

    @Test
    void setsReturnedAtWhenMarkedReturned() {
        Loan loan = new Loan(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(1), null);
        Instant now = Instant.now().plus(1, ChronoUnit.MINUTES);
        loan.markReturned(now);
        assertThat(loan.getReturnedAt()).isEqualTo(now);
        assertThat(loan.isActive()).isFalse();
    }

    @Test
    void refusesToReturnALoanThatWasAlreadyReturned() {
        Loan loan = new Loan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now().plusSeconds(1),
                Instant.now());
        assertThatThrownBy(loan::markReturned).isInstanceOf(LoanAlreadyReturnedException.class);
    }
}
