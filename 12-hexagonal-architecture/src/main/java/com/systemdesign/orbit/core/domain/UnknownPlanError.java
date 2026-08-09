package com.systemdesign.orbit.core.domain;

public class UnknownPlanError extends DomainError {
    public UnknownPlanError(String planId) {
        super("Unknown plan \"" + planId + "\". Valid plans: basic, pro, enterprise.", "UNKNOWN_PLAN");
    }
}
