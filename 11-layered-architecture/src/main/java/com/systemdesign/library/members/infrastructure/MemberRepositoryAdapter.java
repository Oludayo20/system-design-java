package com.systemdesign.library.members.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import com.systemdesign.library.members.domain.Member;
import com.systemdesign.library.members.domain.MemberRepositoryPort;

/**
 * Data Access layer. The only class that implements {@link MemberRepositoryPort} with a real
 * database call. Because it's the only bean implementing the port, Spring autowires it wherever
 * {@code MemberRepositoryPort} is requested -- the equivalent of the {@code useClass} binding in
 * the NestJS module files.
 */
@Repository
public class MemberRepositoryAdapter implements MemberRepositoryPort {

    private final MemberJpaRepository jpaRepository;

    public MemberRepositoryAdapter(MemberJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Member save(Member member) {
        MemberJpaEntity saved = jpaRepository.save(toEntity(member));
        return toDomain(saved);
    }

    @Override
    public Optional<Member> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private Member toDomain(MemberJpaEntity row) {
        return new Member(row.getId(), row.getName(), row.getEmail(), row.getMembershipStatus());
    }

    private MemberJpaEntity toEntity(Member member) {
        return new MemberJpaEntity(member.getId(), member.getName(), member.getEmail(), member.getMembershipStatus());
    }
}
