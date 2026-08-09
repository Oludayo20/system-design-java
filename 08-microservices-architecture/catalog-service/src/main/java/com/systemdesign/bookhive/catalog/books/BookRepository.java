package com.systemdesign.bookhive.catalog.books;

import com.systemdesign.bookhive.catalog.books.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {

    List<Book> findAllByOrderByCreatedAtDesc();

    /**
     * Atomically decrements stock in a single conditional UPDATE (WHERE id = :id AND stock >=
     * :quantity) so two concurrent reservations can never both succeed against the same last
     * unit - the second one's WHERE clause simply matches zero rows (this method returns 0).
     * This is the operation order-service calls over HTTP instead of touching catalog-db
     * directly.
     */
    @Modifying
    @Query("UPDATE Book b SET b.stock = b.stock - :quantity WHERE b.id = :id AND b.stock >= :quantity")
    int reserveStock(@Param("id") UUID id, @Param("quantity") int quantity);
}
