package com.acme.notifications.channel;

import com.acme.notifications.model.ChannelType;

/**
 * Concrete Strategy for simulated push delivery; the shared channel
 * interface keeps it interchangeable with email and SMS.
 */
public final class PushChannel implements NotificationChannel {
  @Override
  public ChannelType type() {
    return ChannelType.PUSH;
  }

  @Override
  public String send(String user, String message) {
    return "Push notification sent to " + user + ": " + message;
  }
}
