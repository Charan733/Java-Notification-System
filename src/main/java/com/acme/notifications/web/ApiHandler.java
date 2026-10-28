package com.acme.notifications.web;

import com.acme.notifications.model.Delivery;
import com.acme.notifications.observer.Topic;
import com.acme.notifications.observer.User;
import com.acme.notifications.service.NotificationService;
import com.acme.notifications.service.exception.DuplicateResourceException;
import com.acme.notifications.service.exception.DuplicateSubscriptionException;
import com.acme.notifications.service.exception.InvalidChannelException;
import com.acme.notifications.service.exception.UnknownTopicException;
import com.acme.notifications.service.exception.UnknownUserException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin HTTP adapter, not a design pattern: it validates JSON, delegates every
 * notification operation to the Singleton service, and maps domain errors to
 * HTTP responses.
 */
public final class ApiHandler implements HttpHandler {
  private static final int MAX_REQUEST_BYTES = 32 * 1024;
  private final NotificationService service;

  public ApiHandler(NotificationService service) {
    this.service = service;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try {
      route(exchange);
    } catch (UnknownTopicException exception) {
      HttpSupport.error(exchange, 404, "unknown_topic", exception.getMessage());
    } catch (UnknownUserException exception) {
      HttpSupport.error(exchange, 404, "unknown_user", exception.getMessage());
    } catch (DuplicateSubscriptionException exception) {
      HttpSupport.error(exchange, 409, "duplicate_subscription", exception.getMessage());
    } catch (DuplicateResourceException exception) {
      HttpSupport.error(exchange, 409, "duplicate_resource", exception.getMessage());
    } catch (InvalidChannelException exception) {
      HttpSupport.error(exchange, 400, "invalid_channel", exception.getMessage());
    } catch (IllegalArgumentException exception) {
      HttpSupport.error(exchange, 400, "invalid_request", exception.getMessage());
    } catch (IOException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      HttpSupport.error(exchange, 500, "internal_error", "The request could not be completed.");
    }
  }

  private void route(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    String method = exchange.getRequestMethod();

    if ("GET".equals(method) && "/notify-api/state".equals(path)) {
      HttpSupport.json(exchange, 200, state());
    } else if ("GET".equals(method) && "/notify-api/deliveries".equals(path)) {
      HttpSupport.json(exchange, 200, deliveryLog());
    } else if ("GET".equals(method) && "/notify-api/health".equals(path)) {
      HttpSupport.json(exchange, 200, Map.of("status", "ok"));
    } else if ("POST".equals(method) && "/notify-api/topics".equals(path)) {
      Map<String, Object> input = readBody(exchange);
      Topic topic = service.addTopic(stringField(input, "name"));
      HttpSupport.json(exchange, 201, Map.of("topic", topicView(topic)));
    } else if ("POST".equals(method) && "/notify-api/users".equals(path)) {
      Map<String, Object> input = readBody(exchange);
      User user = service.addUser(
          stringField(input, "name"),
          stringField(input, "channel"));
      HttpSupport.json(exchange, 201, Map.of("user", userView(user)));
    } else if ("POST".equals(method) && "/notify-api/subscriptions".equals(path)) {
      Map<String, Object> input = readBody(exchange);
      service.subscribe(stringField(input, "topic"), stringField(input, "user"));
      HttpSupport.json(exchange, 200, Map.of("subscribed", true));
    } else if ("DELETE".equals(method) && "/notify-api/subscriptions".equals(path)) {
      Map<String, Object> input = readBody(exchange);
      boolean removed = service.unsubscribe(
          stringField(input, "topic"),
          stringField(input, "user"));
      HttpSupport.json(exchange, 200, Map.of("subscribed", false, "removed", removed));
    } else if ("POST".equals(method) && "/notify-api/publish".equals(path)) {
      Map<String, Object> input = readBody(exchange);
      List<Map<String, Object>> deliveries = service.publish(
              stringField(input, "topic"),
              stringField(input, "message"))
          .stream()
          .map(Delivery::toMap)
          .toList();
      HttpSupport.json(exchange, 200, Map.of("deliveries", deliveries));
    } else {
      HttpSupport.error(exchange, 404, "not_found", "No API endpoint matches this request.");
    }
  }

  private Map<String, Object> state() {
    List<Map<String, Object>> topicViews = service.getTopicsSnapshot().stream()
        .map(this::topicView)
        .toList();
    List<Map<String, Object>> userViews = service.getUsersSnapshot().stream()
        .map(this::userView)
        .toList();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("topics", topicViews);
    result.put("users", userViews);
    return result;
  }

  private Map<String, Object> deliveryLog() {
    List<Map<String, Object>> deliveries = service.getDeliveryLog().stream()
        .map(Delivery::toMap)
        .toList();
    return Map.of("deliveries", deliveries);
  }

  private Map<String, Object> topicView(Topic topic) {
    List<String> subscribers = new ArrayList<>();
    for (var subscriber : topic.getSubscribersSnapshot()) {
      if (subscriber instanceof User user) {
        subscribers.add(user.getName());
      }
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("name", topic.getName());
    result.put("subscriberCount", subscribers.size());
    result.put("subscribers", subscribers);
    return result;
  }

  private Map<String, Object> userView(User user) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("name", user.getName());
    result.put("channel", user.getPreferredChannel().name());
    result.put("topics", service.getTopicsForUser(user.getName()));
    return result;
  }

  private static Map<String, Object> readBody(HttpExchange exchange) throws IOException {
    byte[] body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
    if (body.length > MAX_REQUEST_BYTES) {
      throw new IllegalArgumentException("Request body is too large.");
    }
    if (body.length == 0) {
      throw new IllegalArgumentException("A JSON request body is required.");
    }
    return Json.parseObject(new String(body, StandardCharsets.UTF_8));
  }

  private static String stringField(Map<String, Object> body, String field) {
    Object value = body.get(field);
    if (!(value instanceof String text) || text.isBlank()) {
      throw new IllegalArgumentException("Field '" + field + "' is required.");
    }
    return text;
  }
}
