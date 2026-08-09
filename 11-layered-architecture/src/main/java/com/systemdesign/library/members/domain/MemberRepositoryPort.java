package com.systemdesign.library.members.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * A port (interface). The Domain and Application layers depend on this abstraction only. The
 * Data Access layer ({@code members.infrastructure.MemberRepositoryAdapter}) is the sole place
 * that implements it with a real ORM/database call.
 */
public interface MemberRepositoryPort {

    Member save(Member member);

    Optional<Member> findById(UUID id);
}
