package com.systemdesign.orbit.core.domain;

/**
 * Base class for every error the core can throw. A plain RuntimeException subclass — no HTTP
 * status codes, no framework knowledge. Inbound adapters (REST controller, CLI) decide how to
 * present these to their caller; see adapters/in/http/DomainErrorHandler.java for the REST
 * mapping and adapters/in/cli/OrbitCliRunner.java for the CLI's own presentation.
 */
public abstract class DomainError extends RuntimeException {

    private final String code;

    protected DomainError(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
