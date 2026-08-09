package com.systemdesign.bookhive.catalog.common;

/** Decoded claims of a BookHive access token. {@code sub} is the user id, as a string UUID. */
public record JwtPayload(String sub, String email) {
}
