package com.acme.notifications;

import com.acme.notifications.model.Delivery;
import com.acme.notifications.service.NotificationService;
import com.acme.notifications.observer.Topic;
import com.acme.notifications.observer.User;
import com.acme.notifications.web.NotificationHttpServer;
import java.nio.file.Path;
import java.util.List;

/**
 * Application entry point, not a design pattern: it offers a console demo
 * without arguments and starts the JDK HTTP server when passed --server.
 */
public final class Main {
  private Main() {}

  public static void main(String[] args) throws Exception {
    if (args.length == 0 || !"--server".equals(args[0])) {
      runConsoleDemo();
      return;
    }

    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    String configuredRoot = System.getProperty(
        "notification.webRoot",
        "src/main/resources/web");
    NotificationHttpServer server = new NotificationHttpServer(port, Path.of(configuredRoot));
    server.start();
    System.out.println("Java Notification System listening on port " + port);
  }

  private static void runConsoleDemo() {
    NotificationService service = NotificationService.getInstance();
    System.out.println("Java Notification System — console demo");
    System.out.println("1. Sample topics and subscriptions:");
    for (Topic topic : service.getTopicsSnapshot()) {
      String subscriberNames = topic.getSubscribersSnapshot().stream()
          .filter(User.class::isInstance)
          .map(User.class::cast)
          .map(User::getName)
          .reduce((first, second) -> first + ", " + second)
          .orElse("no subscribers");
      System.out.println("   " + topic.getName() + " → " + subscriberNames);
    }

    System.out.println("2. Users and preferred channels:");
    for (User user : service.getUsersSnapshot()) {
      System.out.println(
          "   " + user.getName() + " → " + user.getPreferredChannel().displayName());
    }

    System.out.println("3. Publish a message to Exam Alerts:");

    List<Delivery> deliveries = service.publish(
        "Exam Alerts",
        "Room 204 is open for the practice exam.");
    for (Delivery delivery : deliveries) {
      System.out.println(delivery.deliveryText());
      System.out.println("  " + delivery.providerMessage());
    }
  }
}
