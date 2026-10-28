package com.acme.notifications.service.exception;

/**
 * Not a design pattern: this custom domain exception lets the HTTP adapter map
 * a missing topic to a clear 404 instead of a low-level lookup failure.
 */
public final class UnknownTopicException extends RuntimeException {
  public UnknownTopicException(String topicName) {
    super("Unknown topic: " + topicName);
  }
}
