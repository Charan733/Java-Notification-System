package com.acme.notifications.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable delivery-log data. This is a model object, not a design pattern;
 * it records the result produced by a channel strategy.
 */
public record Delivery(
    long id,
    String topic,
    String user,
    ChannelType channel,
    String message,
    String providerMessage,
    String deliveredAt,
    String deliveryText) {

  public Map<String, Object> toMap() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", id);
    result.put("topic", topic);
    result.put("user", user);
    result.put("channel", channel.name());
    result.put("message", message);
    result.put("providerMessage", providerMessage);
    result.put("deliveredAt", deliveredAt);
    result.put("deliveryText", deliveryText);
    return result;
  }
}
