package com.systemdesign.library.members.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.systemdesign.library.members.domain.Member;
import com.systemdesign.library.members.domain.MemberRepositoryPort;

@Service
public class GetMemberUseCase {

    private final MemberRepositoryPort memberRepository;

    public GetMemberUseCase(MemberRepositoryPort memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member execute(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No member with id " + id + "."));
    }
}
