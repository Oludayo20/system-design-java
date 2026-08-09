package com.systemdesign.library.members.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, UUID> {
}
