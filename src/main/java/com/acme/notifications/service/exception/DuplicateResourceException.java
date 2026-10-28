package com.acme.notifications.service.exception;

/**
 * Not a design pattern: this custom domain exception prevents ambiguous
 * in-memory lookups when a topic or user name already exists.
 */
public final class DuplicateResourceException extends RuntimeException {
  public DuplicateResourceException(String message) {
    super(message);
  }
}
