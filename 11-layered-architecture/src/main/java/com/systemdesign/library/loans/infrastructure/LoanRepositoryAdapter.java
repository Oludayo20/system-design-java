package com.systemdesign.library.loans.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import com.systemdesign.library.loans.domain.Loan;
import com.systemdesign.library.loans.domain.LoanRepositoryPort;

/**
 * Data Access layer. The only class that implements {@link LoanRepositoryPort} with a real
 * database call. Because it's the only bean implementing the port, Spring autowires it wherever
 * {@code LoanRepositoryPort} is requested -- the equivalent of the {@code useClass} binding in
 * the NestJS module files.
 */
@Repository
public class LoanRepositoryAdapter implements LoanRepositoryPort {

    private final LoanJpaRepository jpaRepository;

    public LoanRepositoryAdapter(LoanJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Loan save(Loan loan) {
        LoanJpaEntity saved = jpaRepository.save(toEntity(loan));
        return toDomain(saved);
    }

    @Override
    public Optional<Loan> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Loan> findByMemberId(UUID memberId) {
        return jpaRepository.findByMemberIdOrderByBorrowedAtDesc(memberId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Loan> findActiveByMemberId(UUID memberId) {
        return jpaRepository.findByMemberIdAndReturnedAtIsNull(memberId).stream().map(this::toDomain).toList();
    }

    private Loan toDomain(LoanJpaEntity row) {
        return new Loan(row.getId(), row.getBookId(), row.getMemberId(), row.getBorrowedAt(), row.getDueAt(), row.getReturnedAt());
    }

    private LoanJpaEntity toEntity(Loan loan) {
        return new LoanJpaEntity(
                loan.getId(), loan.getBookId(), loan.getMemberId(), loan.getBorrowedAt(), loan.getDueAt(), loan.getReturnedAt());
    }
}
