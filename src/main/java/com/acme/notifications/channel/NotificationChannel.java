package com.acme.notifications.channel;

import com.acme.notifications.model.ChannelType;

/**
 * Strategy-pattern contract: each channel provides its own delivery behavior
 * behind the same send operation.
 */
public interface NotificationChannel {
  ChannelType type();

  String send(String user, String message);
}
