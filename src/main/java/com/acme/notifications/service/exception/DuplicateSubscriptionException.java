package com.acme.notifications.service.exception;

/**
 * Not a design pattern: this custom domain exception gives the console and
 * web layer a precise error for a duplicate subscription.
 */
public final class DuplicateSubscriptionException extends RuntimeException {
  public DuplicateSubscriptionException(String userName, String topicName) {
    super(userName + " is already subscribed to " + topicName + ".");
  }
}
