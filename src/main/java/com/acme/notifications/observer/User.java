package com.acme.notifications.observer;

import com.acme.notifications.channel.NotificationChannel;
import com.acme.notifications.factory.ChannelFactory;
import com.acme.notifications.model.ChannelType;

/**
 * Observer-pattern concrete subscriber: a user reacts to a topic update by
 * sending it through the channel Strategy created by ChannelFactory.
 */
public final class User implements Subscriber {
  private final String name;
  private final NotificationChannel channel;
  private volatile String lastProviderMessage = "";

  public User(String name, String preferredChannel) {
    this.name = name;
    this.channel = ChannelFactory.create(preferredChannel);
  }

  public String getName() {
    return name;
  }

  public ChannelType getPreferredChannel() {
    return channel.type();
  }

  public String getLastProviderMessage() {
    return lastProviderMessage;
  }

  @Override
  public void notify(String topicName, String message) {
    lastProviderMessage = channel.send(name, message);
  }
}
