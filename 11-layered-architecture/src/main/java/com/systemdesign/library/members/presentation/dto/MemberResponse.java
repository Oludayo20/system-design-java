package com.systemdesign.library.members.presentation.dto;

import java.util.UUID;
import com.systemdesign.library.members.domain.Member;
import com.systemdesign.library.members.domain.MembershipStatus;

public record MemberResponse(UUID id, String name, String email, MembershipStatus membershipStatus) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getEmail(), member.getMembershipStatus());
    }
}
