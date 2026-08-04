package com.systemdesign.ecommarketplace.common.exceptions;

/** Mirrors Nest's BadRequestException (HTTP 400). */
public class BadRequestException extends RuntimeException {
  public BadRequestException(String message) {
    super(message);
  }
}
