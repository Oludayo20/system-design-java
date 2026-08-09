package com.systemdesign.library.members.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain tests -- no Spring context, no database. */
class MemberTest {

    @Test
    void isActiveWhenMembershipStatusIsActive() {
        Member member = new Member(UUID.randomUUID(), "Ada Lovelace", "ada@example.com", MembershipStatus.ACTIVE);
        assertThat(member.isActive()).isTrue();
    }

    @Test
    void isNotActiveWhenMembershipStatusIsSuspended() {
        Member member = new Member(UUID.randomUUID(), "Ada Lovelace", "ada@example.com", MembershipStatus.SUSPENDED);
        assertThat(member.isActive()).isFalse();
    }
}
