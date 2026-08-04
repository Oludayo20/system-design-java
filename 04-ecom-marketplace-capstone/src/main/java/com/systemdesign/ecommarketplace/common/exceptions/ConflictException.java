package com.systemdesign.ecommarketplace.common.exceptions;

/** Mirrors Nest's ConflictException (HTTP 409). */
public class ConflictException extends RuntimeException {
  public ConflictException(String message) {
    super(message);
  }
}
