package com.systemdesign.ecommarketplace.auth.repository;

import com.systemdesign.ecommarketplace.auth.entity.UserDirectory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Bound to the primary EntityManagerFactory via PrimaryDataSourceConfig's @EnableJpaRepositories. */
public interface UserDirectoryRepository extends JpaRepository<UserDirectory, UUID> {
  Optional<UserDirectory> findByEmail(String email);
}
