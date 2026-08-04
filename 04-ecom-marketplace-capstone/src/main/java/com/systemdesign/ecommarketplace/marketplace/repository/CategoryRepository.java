package com.systemdesign.ecommarketplace.marketplace.repository;

import com.systemdesign.ecommarketplace.marketplace.entity.Category;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
  List<Category> findAllByOrderByNameAsc();
}
