package com.acme.notifications.model;

import com.acme.notifications.service.exception.InvalidChannelException;
import java.util.Locale;

/**
 * Domain enum for a user's delivery preference. This is not a design pattern;
 * it gives the channel factory a small, validated set of supported values.
 */
public enum ChannelType {
  EMAIL("Email"),
  SMS("SMS"),
  PUSH("Push");

  private final String displayName;

  ChannelType(String displayName) {
    this.displayName = displayName;
  }

  public String displayName() {
    return displayName;
  }

  public static ChannelType parse(String value) {
    if (value == null || value.isBlank()) {
      throw new InvalidChannelException("Choose Email, SMS, or Push.");
    }

    try {
      return ChannelType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new InvalidChannelException(
          "Unsupported channel: " + value + ". Choose Email, SMS, or Push.");
    }
  }
}
