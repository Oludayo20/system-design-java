package com.systemdesign.library.books.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain tests -- no Spring context, no database. */
class BookTest {

    private static Book book(int totalCopies, int availableCopies) {
        return new Book(UUID.randomUUID(), "Clean Architecture", "Robert C. Martin", "9780134494166", totalCopies, availableCopies);
    }

    @Test
    void hasAvailableCopiesWhenAvailableCopiesIsPositive() {
        assertThat(book(3, 1).hasAvailableCopies()).isTrue();
    }

    @Test
    void hasNoAvailableCopiesWhenAvailableCopiesIsZero() {
        assertThat(book(3, 0).hasAvailableCopies()).isFalse();
    }

    @Test
    void borrowOneCopyDecrementsAvailableCopies() {
        Book book = book(3, 2);
        book.borrowOneCopy();
        assertThat(book.getAvailableCopies()).isEqualTo(1);
    }

    @Test
    void borrowOneCopyRefusesWhenNoCopiesAvailable() {
        Book book = book(3, 0);
        assertThatThrownBy(book::borrowOneCopy).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void returnOneCopyIncrementsAvailableCopies() {
        Book book = book(3, 1);
        book.returnOneCopy();
        assertThat(book.getAvailableCopies()).isEqualTo(2);
    }

    @Test
    void returnOneCopyRefusesWhenAllCopiesAreAlreadyAccountedFor() {
        Book book = book(3, 3);
        assertThatThrownBy(book::returnOneCopy).isInstanceOf(IllegalStateException.class);
    }
}
