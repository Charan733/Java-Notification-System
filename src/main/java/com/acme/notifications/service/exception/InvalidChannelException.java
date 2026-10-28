package com.acme.notifications.service.exception;

/**
 * Not a design pattern: this custom domain exception rejects unsupported
 * preferred-channel values before a channel Strategy is selected.
 */
public final class InvalidChannelException extends RuntimeException {
  public InvalidChannelException(String message) {
    super(message);
  }
}
