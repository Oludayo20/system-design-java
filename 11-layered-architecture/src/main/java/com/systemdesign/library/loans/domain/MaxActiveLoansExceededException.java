package com.systemdesign.library.loans.domain;

/** Domain error. Plain {@link RuntimeException} subclass -- see {@link BookUnavailableException}. */
public class MaxActiveLoansExceededException extends RuntimeException {

    public MaxActiveLoansExceededException(String message) {
        super(message);
    }
}
