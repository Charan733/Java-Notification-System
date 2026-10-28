package com.acme.notifications.web;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP response helper for the JDK web layer. This is not a design pattern; it
 * keeps content types and response closing consistent across handlers.
 */
public final class HttpSupport {
  private HttpSupport() {}

  public static void json(HttpExchange exchange, int status, Object value) throws IOException {
    byte[] body = Json.stringify(value).getBytes(StandardCharsets.UTF_8);
    send(exchange, status, "application/json; charset=utf-8", body);
  }

  public static void text(HttpExchange exchange, int status, String value) throws IOException {
    send(
        exchange,
        status,
        "text/plain; charset=utf-8",
        value.getBytes(StandardCharsets.UTF_8));
  }

  public static void send(
      HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.sendResponseHeaders(status, body.length);
    try (var output = exchange.getResponseBody()) {
      output.write(body);
    }
  }

  public static void error(
      HttpExchange exchange, int status, String code, String message) throws IOException {
    HttpSupport.json(exchange, status, Map.of("error", message, "code", code));
  }
}
