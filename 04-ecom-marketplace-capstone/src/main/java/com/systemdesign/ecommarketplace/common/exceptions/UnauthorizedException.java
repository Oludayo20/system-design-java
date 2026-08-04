package com.systemdesign.ecommarketplace.common.exceptions;

/** Mirrors Nest's UnauthorizedException (HTTP 401). */
public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException(String message) {
    super(message);
  }
}
