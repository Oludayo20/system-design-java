package com.systemdesign.library.members.domain;

import java.util.UUID;

/** Domain layer. Plain Java only -- no Spring, no JPA/Hibernate. */
public class Member {

    private final UUID id;
    private String name;
    private String email;
    private MembershipStatus membershipStatus;

    public Member(UUID id, String name, String email, MembershipStatus membershipStatus) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.membershipStatus = membershipStatus;
    }

    public boolean isActive() {
        return membershipStatus == MembershipStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public MembershipStatus getMembershipStatus() {
        return membershipStatus;
    }
}
