package com.systemdesign.library.books.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data Access layer. Spring Data JPA does the SQL; nothing above this package sees it. */
interface BookJpaRepository extends JpaRepository<BookJpaEntity, UUID> {
}
