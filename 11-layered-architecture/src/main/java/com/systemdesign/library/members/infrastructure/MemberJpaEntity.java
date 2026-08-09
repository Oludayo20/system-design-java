package com.systemdesign.library.members.infrastructure;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.systemdesign.library.members.domain.MembershipStatus;

/** Data Access layer. TypeORM/JPA row shape -- kept separate from the pure domain Member class. */
@Entity
@Table(name = "members")
public class MemberJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 20)
    private MembershipStatus membershipStatus;

    protected MemberJpaEntity() {
        // required by JPA
    }

    public MemberJpaEntity(UUID id, String name, String email, MembershipStatus membershipStatus) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.membershipStatus = membershipStatus;
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
