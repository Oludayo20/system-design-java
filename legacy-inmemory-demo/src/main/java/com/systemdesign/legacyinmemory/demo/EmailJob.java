package com.systemdesign.legacyinmemory.demo;

/** Demo-only payload for Act 2's "flash sale emails" queue - {@code { type, to } } in the original. */
public record EmailJob(String to) {
}
