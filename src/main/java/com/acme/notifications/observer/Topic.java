package com.acme.notifications.observer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Observer-pattern Subject: it owns subscribers and broadcasts each published
 * message to a stable subscriber snapshot.
 */
public final class Topic {
  private final String name;
  private final Set<Subscriber> subscribers = new LinkedHashSet<>();

  public Topic(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public synchronized boolean subscribe(Subscriber subscriber) {
    return subscribers.add(subscriber);
  }

  public synchronized boolean unsubscribe(Subscriber subscriber) {
    return subscribers.remove(subscriber);
  }

  public synchronized boolean hasSubscriber(Subscriber subscriber) {
    return subscribers.contains(subscriber);
  }

  public synchronized List<Subscriber> getSubscribersSnapshot() {
    return new ArrayList<>(subscribers);
  }

  public synchronized List<Subscriber> publish(String message) {
    List<Subscriber> snapshot = getSubscribersSnapshot();
    for (Subscriber subscriber : snapshot) {
      subscriber.notify(name, message);
    }
    return snapshot;
  }
}
