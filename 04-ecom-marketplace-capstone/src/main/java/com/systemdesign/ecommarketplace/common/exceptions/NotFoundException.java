package com.systemdesign.ecommarketplace.common.exceptions;

/** Mirrors Nest's NotFoundException (HTTP 404). */
public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) {
    super(message);
  }
}
