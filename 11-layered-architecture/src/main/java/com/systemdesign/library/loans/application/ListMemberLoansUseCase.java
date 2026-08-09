package com.systemdesign.library.loans.application;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.systemdesign.library.loans.domain.Loan;
import com.systemdesign.library.loans.domain.LoanRepositoryPort;
import com.systemdesign.library.members.domain.MemberRepositoryPort;

@Service
public class ListMemberLoansUseCase {

    private final LoanRepositoryPort loanRepository;
    private final MemberRepositoryPort memberRepository;

    public ListMemberLoansUseCase(LoanRepositoryPort loanRepository, MemberRepositoryPort memberRepository) {
        this.loanRepository = loanRepository;
        this.memberRepository = memberRepository;
    }

    public List<Loan> execute(UUID memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No member with id " + memberId + "."));
        return loanRepository.findByMemberId(memberId);
    }
}
