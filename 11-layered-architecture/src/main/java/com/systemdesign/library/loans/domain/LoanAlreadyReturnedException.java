package com.systemdesign.library.loans.domain;

/** Domain error. Plain {@link RuntimeException} subclass -- see {@link BookUnavailableException}. */
public class LoanAlreadyReturnedException extends RuntimeException {

    public LoanAlreadyReturnedException(String message) {
        super(message);
    }
}
