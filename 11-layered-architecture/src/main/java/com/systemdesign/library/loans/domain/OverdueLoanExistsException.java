package com.systemdesign.library.loans.domain;

/** Domain error. Plain {@link RuntimeException} subclass -- see {@link BookUnavailableException}. */
public class OverdueLoanExistsException extends RuntimeException {

    public OverdueLoanExistsException(String message) {
        super(message);
    }
}
