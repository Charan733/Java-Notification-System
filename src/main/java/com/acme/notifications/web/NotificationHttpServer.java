package com.acme.notifications.web;

import com.acme.notifications.service.NotificationService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * HTTP server setup using the JDK's built-in HttpServer; this is a thin
 * transport layer, not a design pattern, and delegates behavior to the service.
 */
public final class NotificationHttpServer {
  private final HttpServer server;

  public NotificationHttpServer(int port, Path webRoot) throws IOException {
    server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
    server.createContext("/notify-api", new ApiHandler(NotificationService.getInstance()));
    server.createContext("/", new StaticFileHandler(webRoot));
    server.setExecutor(Executors.newFixedThreadPool(8));
  }

  public void start() {
    server.start();
  }
}
