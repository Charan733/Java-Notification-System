package com.acme.notifications.service.exception;

/**
 * Not a design pattern: this custom domain exception lets the HTTP adapter map
 * a missing user to a clear 404 instead of a low-level lookup failure.
 */
public final class UnknownUserException extends RuntimeException {
  public UnknownUserException(String userName) {
    super("Unknown user: " + userName);
  }
}
