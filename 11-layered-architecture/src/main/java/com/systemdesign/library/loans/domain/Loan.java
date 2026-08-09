package com.systemdesign.library.loans.domain;

import java.time.Instant;
import java.util.UUID;

/** Domain layer. Plain Java only -- no Spring, no JPA/Hibernate. */
public class Loan {

    private final UUID id;
    private final UUID bookId;
    private final UUID memberId;
    private final Instant borrowedAt;
    private final Instant dueAt;
    private Instant returnedAt;

    public Loan(UUID id, UUID bookId, UUID memberId, Instant borrowedAt, Instant dueAt, Instant returnedAt) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.borrowedAt = borrowedAt;
        this.dueAt = dueAt;
        this.returnedAt = returnedAt;
    }

    public boolean isActive() {
        return returnedAt == null;
    }

    /** Business rule #3 (part of it): a loan is overdue if unreturned and past its due date. */
    public boolean isOverdue(Instant asOf) {
        return isActive() && asOf.isAfter(dueAt);
    }

    public boolean isOverdue() {
        return isOverdue(Instant.now());
    }

    /** Business rule #4: returning a book sets returnedAt. */
    public void markReturned(Instant asOf) {
        if (!isActive()) {
            throw new LoanAlreadyReturnedException("Loan " + id + " has already been returned.");
        }
        this.returnedAt = asOf;
    }

    public void markReturned() {
        markReturned(Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getBookId() {
        return bookId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public Instant getBorrowedAt() {
        return borrowedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }
}
