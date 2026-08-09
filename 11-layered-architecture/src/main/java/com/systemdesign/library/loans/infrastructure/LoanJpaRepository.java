package com.systemdesign.library.loans.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface LoanJpaRepository extends JpaRepository<LoanJpaEntity, UUID> {

    List<LoanJpaEntity> findByMemberIdOrderByBorrowedAtDesc(UUID memberId);

    List<LoanJpaEntity> findByMemberIdAndReturnedAtIsNull(UUID memberId);
}
