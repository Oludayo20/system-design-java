package com.systemdesign.library.loans.presentation.dto;

import java.time.Instant;
import java.util.UUID;
import com.systemdesign.library.loans.domain.Loan;

public record LoanResponse(UUID id, UUID bookId, UUID memberId, Instant borrowedAt, Instant dueAt, Instant returnedAt) {

    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.getId(), loan.getBookId(), loan.getMemberId(), loan.getBorrowedAt(), loan.getDueAt(), loan.getReturnedAt());
    }
}
