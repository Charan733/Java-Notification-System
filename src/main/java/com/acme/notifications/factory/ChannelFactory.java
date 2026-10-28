package com.acme.notifications.factory;

import com.acme.notifications.channel.EmailChannel;
import com.acme.notifications.channel.NotificationChannel;
import com.acme.notifications.channel.PushChannel;
import com.acme.notifications.channel.SmsChannel;
import com.acme.notifications.model.ChannelType;

/**
 * Factory pattern: centralizes channel selection so callers request a channel
 * type instead of constructing concrete channel classes themselves.
 */
public final class ChannelFactory {
  private ChannelFactory() {}

  public static NotificationChannel create(String type) {
    return create(ChannelType.parse(type));
  }

  public static NotificationChannel create(ChannelType type) {
    return switch (type) {
      case EMAIL -> new EmailChannel();
      case SMS -> new SmsChannel();
      case PUSH -> new PushChannel();
    };
  }
}
