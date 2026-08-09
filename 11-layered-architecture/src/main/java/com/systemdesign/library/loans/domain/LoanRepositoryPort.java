package com.systemdesign.library.loans.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A port (interface). The Domain and Application layers depend on this abstraction only. The
 * Data Access layer ({@code loans.infrastructure.LoanRepositoryAdapter}) is the sole place that
 * implements it with a real ORM/database call.
 */
public interface LoanRepositoryPort {

    Loan save(Loan loan);

    Optional<Loan> findById(UUID id);

    List<Loan> findByMemberId(UUID memberId);

    /** Unreturned loans only -- used by the eligibility rules. */
    List<Loan> findActiveByMemberId(UUID memberId);
}
