package com.acme.notifications.service;

import com.acme.notifications.model.ChannelType;
import com.acme.notifications.model.Delivery;
import com.acme.notifications.observer.Subscriber;
import com.acme.notifications.observer.Topic;
import com.acme.notifications.observer.User;
import com.acme.notifications.service.exception.DuplicateResourceException;
import com.acme.notifications.service.exception.DuplicateSubscriptionException;
import com.acme.notifications.service.exception.UnknownTopicException;
import com.acme.notifications.service.exception.UnknownUserException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Thread-safe Singleton: one shared service owns users, topics, subscriptions,
 * and delivery history so the console and HTTP entry points use the same rules.
 */
public final class NotificationService {
  private static final int MAX_DELIVERY_LOG = 250;
  private static final NotificationService INSTANCE = new NotificationService();

  private final Map<String, Topic> topics = new LinkedHashMap<>();
  private final Map<String, User> users = new LinkedHashMap<>();
  private final List<Delivery> deliveryLog = new ArrayList<>();
  private long nextDeliveryId = 1;

  private NotificationService() {
    seedDemoData();
  }

  public static NotificationService getInstance() {
    return INSTANCE;
  }

  public synchronized Topic addTopic(String name) {
    String cleanName = requireName(name, "Topic");
    String key = key(cleanName);
    if (topics.containsKey(key)) {
      throw new DuplicateResourceException("A topic named " + cleanName + " already exists.");
    }

    Topic topic = new Topic(cleanName);
    topics.put(key, topic);
    return topic;
  }

  public synchronized User addUser(String name, String preferredChannel) {
    String cleanName = requireName(name, "User");
    String key = key(cleanName);
    if (users.containsKey(key)) {
      throw new DuplicateResourceException("A user named " + cleanName + " already exists.");
    }

    User user = new User(cleanName, preferredChannel);
    users.put(key, user);
    return user;
  }

  public synchronized void subscribe(String topicName, String userName) {
    Topic topic = requireTopic(topicName);
    User user = requireUser(userName);
    if (!topic.subscribe(user)) {
      throw new DuplicateSubscriptionException(user.getName(), topic.getName());
    }
  }

  public synchronized boolean unsubscribe(String topicName, String userName) {
    Topic topic = requireTopic(topicName);
    User user = requireUser(userName);
    return topic.unsubscribe(user);
  }

  public synchronized List<Delivery> publish(String topicName, String message) {
    Topic topic = requireTopic(topicName);
    String cleanMessage = requireMessage(message);
    List<Subscriber> notifiedSubscribers = topic.publish(cleanMessage);
    List<Delivery> publishedDeliveries = new ArrayList<>();

    for (Subscriber subscriber : notifiedSubscribers) {
      if (subscriber instanceof User user) {
        String deliveryText = "Message delivered to " + user.getName()
            + " via " + user.getPreferredChannel().displayName();
        Delivery delivery = new Delivery(
            nextDeliveryId++,
            topic.getName(),
            user.getName(),
            user.getPreferredChannel(),
            cleanMessage,
            user.getLastProviderMessage(),
            Instant.now().toString(),
            deliveryText);
        deliveryLog.add(0, delivery);
        publishedDeliveries.add(delivery);
      }
    }

    if (deliveryLog.size() > MAX_DELIVERY_LOG) {
      deliveryLog.subList(MAX_DELIVERY_LOG, deliveryLog.size()).clear();
    }
    return List.copyOf(publishedDeliveries);
  }

  public synchronized List<Topic> getTopicsSnapshot() {
    return List.copyOf(topics.values());
  }

  public synchronized List<User> getUsersSnapshot() {
    return List.copyOf(users.values());
  }

  public synchronized List<Delivery> getDeliveryLog() {
    return List.copyOf(deliveryLog);
  }

  public synchronized List<String> getTopicsForUser(String userName) {
    User user = requireUser(userName);
    List<String> result = new ArrayList<>();
    for (Topic topic : topics.values()) {
      if (topic.hasSubscriber(user)) {
        result.add(topic.getName());
      }
    }
    return List.copyOf(result);
  }

  private Topic requireTopic(String name) {
    Topic topic = topics.get(key(name));
    if (topic == null) {
      throw new UnknownTopicException(name == null ? "" : name.trim());
    }
    return topic;
  }

  private User requireUser(String name) {
    User user = users.get(key(name));
    if (user == null) {
      throw new UnknownUserException(name == null ? "" : name.trim());
    }
    return user;
  }

  private static String requireName(String value, String entity) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(entity + " name is required.");
    }
    String clean = value.trim();
    if (clean.length() > 80) {
      throw new IllegalArgumentException(entity + " name must be 80 characters or fewer.");
    }
    return clean;
  }

  private static String requireMessage(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Message is required.");
    }
    String clean = value.trim();
    if (clean.length() > 2_000) {
      throw new IllegalArgumentException("Message must be 2,000 characters or fewer.");
    }
    return clean;
  }

  private static String key(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private void seedDemoData() {
    addTopic("Offers");
    addTopic("Exam Alerts");

    addUser("Ravi", ChannelType.EMAIL.name());
    addUser("Maya", ChannelType.SMS.name());
    addUser("Arjun", ChannelType.PUSH.name());

    subscribe("Offers", "Ravi");
    subscribe("Offers", "Maya");
    subscribe("Exam Alerts", "Ravi");
    subscribe("Exam Alerts", "Maya");
    subscribe("Exam Alerts", "Arjun");

    publish("Exam Alerts", "Your final review starts at 9:00 AM tomorrow.");
  }
}
