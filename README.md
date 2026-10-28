# Java Notification System

A small, deployable notification system written in plain Java. Create topics and users, choose each user's preferred delivery channel, manage subscriptions, and publish messages that are simulated through email, SMS, or push.

The notification engine uses only the JDK. The web server is `com.sun.net.httpserver.HttpServer`; the interface is plain HTML, CSS, and browser JavaScript. There is no Spring Boot, database, or Java library dependency. Data and the delivery log live in memory and reset when the server restarts.

## Live demo

[Open the live Replit preview](https://945a8404-56d7-485f-924b-b5cc1f7d2fa3-00-74bdt4d5jjkb.pike.replit.dev/) while the project is running. Publish the project to create a stable production demo URL.

## Patterns and where they are used

- **Observer:** `observer.Subscriber` defines the notification contract. `observer.Topic` is the subject that manages subscribers and broadcasts a published message. `observer.User` is a concrete subscriber.
- **Strategy:** `channel.NotificationChannel` defines the send behavior. `EmailChannel`, `SmsChannel`, and `PushChannel` implement interchangeable delivery strategies.
- **Factory:** `factory.ChannelFactory` maps a channel type to the correct channel implementation when a user is created.
- **Singleton:** `service.NotificationService` has one shared instance and synchronized operations, so the console and web layer use the same thread-safe state.

## Text class diagram

```text
Main
 ├── console demo ───────────────────────────┐
 └── NotificationHttpServer                  │
      ├── ApiHandler ── delegates to ────────┤
      └── StaticFileHandler                   │
                                               ▼
                                  NotificationService (Singleton)
                                   ├── Topic (Subject)
                                   │    └── Subscriber (Observer)
                                   │         └── User (Concrete Observer)
                                   │              └── NotificationChannel (Strategy)
                                   │                   ├── EmailChannel
                                   │                   ├── SmsChannel
                                   │                   └── PushChannel
                                   ├── ChannelFactory ── creates channel strategies
                                   └── Delivery log

Topic ── publishes ──> Subscriber.notify(topicName, message)
User ── sends through ──> preferred NotificationChannel
```

## Run

Requirements: JDK 17 or newer.

From the repository root:

```bash
# Build Java classes and copy the web files
bash scripts/build.sh

# Run the console demo
java --add-modules jdk.httpserver \
  -cp build/classes \
  com.acme.notifications.Main

# Start the web app on PORT (defaults to 8080 if unset)
PORT=8080 bash scripts/dev.sh
```

The Replit web workflow supplies its assigned port automatically. Open the app preview to use the web interface.

The console demo and web app both preload:

- Topics: `Offers`, `Exam Alerts`
- Users: `Ravi` (Email), `Maya` (SMS), and `Arjun` (Push)
- Subscriptions and a sample delivery on `Exam Alerts`

## HTTP endpoints

- `GET /notify-api/state` — list topics, subscribers, users, and user subscriptions
- `GET /notify-api/deliveries` — read the recent delivery log
- `POST /notify-api/topics` — body: `{"name":"New topic"}`
- `POST /notify-api/users` — body: `{"name":"Sam","channel":"PUSH"}`
- `POST /notify-api/subscriptions` — body: `{"topic":"Offers","user":"Ravi"}`
- `DELETE /notify-api/subscriptions` — body: `{"topic":"Offers","user":"Ravi"}`
- `POST /notify-api/publish` — body: `{"topic":"Offers","message":"A message"}`

Unknown topics and users return `404`; duplicate subscriptions return `409`; invalid values return `400`.
