package com.systemdesign.library.loans.domain;

/**
 * Domain error. A plain {@link RuntimeException} subclass -- no Spring {@code ResponseStatusException}
 * here. The Application layer ({@code loans.application.*UseCase}) catches these and translates
 * them into HTTP-shaped exceptions; the Domain layer itself has no idea what HTTP is.
 */
public class BookUnavailableException extends RuntimeException {

    public BookUnavailableException(String message) {
        super(message);
    }
}
