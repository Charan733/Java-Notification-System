package com.acme.notifications.observer;

/**
 * Observer-pattern contract: a topic calls this method whenever it publishes
 * a message, without needing to know how a subscriber handles it.
 */
public interface Subscriber {
  void notify(String topicName, String message);
}
