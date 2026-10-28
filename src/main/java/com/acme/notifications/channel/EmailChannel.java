package com.acme.notifications.channel;

import com.acme.notifications.model.ChannelType;

/**
 * Concrete Strategy for simulated email delivery; it keeps channel-specific
 * delivery formatting out of users and topics.
 */
public final class EmailChannel implements NotificationChannel {
  @Override
  public ChannelType type() {
    return ChannelType.EMAIL;
  }

  @Override
  public String send(String user, String message) {
    return "Email sent to " + user + ": " + message;
  }
}
