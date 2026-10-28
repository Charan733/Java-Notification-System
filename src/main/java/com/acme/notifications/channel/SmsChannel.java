package com.acme.notifications.channel;

import com.acme.notifications.model.ChannelType;

/**
 * Concrete Strategy for simulated SMS delivery; it can be exchanged for
 * another channel without changing the notification flow.
 */
public final class SmsChannel implements NotificationChannel {
  @Override
  public ChannelType type() {
    return ChannelType.SMS;
  }

  @Override
  public String send(String user, String message) {
    return "SMS sent to " + user + ": " + message;
  }
}
