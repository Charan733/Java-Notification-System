package com.acme.notifications.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * JDK-only static file handler, not a design pattern: it serves the plain
 * HTML/CSS/JavaScript interface while preventing paths from escaping webRoot.
 */
public final class StaticFileHandler implements HttpHandler {
  private static final Map<String, String> CONTENT_TYPES = Map.of(
      ".html", "text/html; charset=utf-8",
      ".css", "text/css; charset=utf-8",
      ".js", "text/javascript; charset=utf-8",
      ".svg", "image/svg+xml",
      ".ico", "image/x-icon");

  private final Path webRoot;

  public StaticFileHandler(Path webRoot) {
    this.webRoot = webRoot.toAbsolutePath().normalize();
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
      HttpSupport.error(exchange, 405, "method_not_allowed", "Only GET is supported for page files.");
      return;
    }

    String requestPath = exchange.getRequestURI().getPath();
    String relativePath = "/".equals(requestPath) ? "index.html" : requestPath.substring(1);
    Path target = webRoot.resolve(relativePath).normalize();
    if (!target.startsWith(webRoot) || !Files.isRegularFile(target)) {
      HttpSupport.error(exchange, 404, "not_found", "Page file not found.");
      return;
    }

    byte[] body = Files.readAllBytes(target);
    if ("HEAD".equals(exchange.getRequestMethod())) {
      body = new byte[0];
    }
    String contentType = contentType(target);
    HttpSupport.send(exchange, 200, contentType, body);
  }

  private static String contentType(Path file) {
    String name = file.getFileName().toString();
    int extensionIndex = name.lastIndexOf('.');
    String extension = extensionIndex < 0 ? "" : name.substring(extensionIndex).toLowerCase();
    return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
  }
}
