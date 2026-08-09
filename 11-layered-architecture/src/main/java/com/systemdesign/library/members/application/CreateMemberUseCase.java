package com.systemdesign.library.members.application;

import java.util.UUID;
import org.springframework.stereotype.Service;
import com.systemdesign.library.members.domain.Member;
import com.systemdesign.library.members.domain.MemberRepositoryPort;
import com.systemdesign.library.members.domain.MembershipStatus;
import com.systemdesign.library.members.presentation.dto.CreateMemberRequest;

@Service
public class CreateMemberUseCase {

    private final MemberRepositoryPort memberRepository;

    public CreateMemberUseCase(MemberRepositoryPort memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member execute(CreateMemberRequest request) {
        Member member = new Member(UUID.randomUUID(), request.name(), request.email(), MembershipStatus.ACTIVE);
        return memberRepository.save(member);
    }
}
