(() => {
  "use strict";

  const state = { topics: [], users: [], deliveries: [], loading: true, busy: false };
  const $ = (selector) => document.querySelector(selector);
  const escapeHTML = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[char]);

  const icons = {
    EMAIL: '<svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><rect x="2.5" y="4" width="15" height="12" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="m3.5 5 6.5 5 6.5-5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    SMS: '<svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M4 3.5h12a1.5 1.5 0 0 1 1.5 1.5v8a1.5 1.5 0 0 1-1.5 1.5H9l-4.5 2v-2.1A1.5 1.5 0 0 1 3 13V5a1.5 1.5 0 0 1 1-1.5Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/><path d="M6.5 8h7M6.5 11h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>',
    PUSH: '<svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M15.5 7.5a5.5 5.5 0 0 0-11 0c0 6-2 6-2 7h15c0-1-2-1-2-7ZM8 17h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  };
  const topicIcon = '<svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M10 2.5v15M2.5 10h15M4.7 4.7l10.6 10.6m0-10.6L4.7 15.3" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>';

  async function request(url, options = {}) {
    const response = await fetch(url, {
      ...options,
      headers: { "Content-Type": "application/json", ...(options.headers || {}) }
    });
    const text = await response.text();
    let data = {};
    if (text) {
      try { data = JSON.parse(text); } catch { data = { message: text }; }
    }
    if (!response.ok) throw new Error(data.message || data.error || `Request failed (${response.status})`);
    return data;
  }

  function showToast(message, isError = false) {
    const toast = $("#toast");
    $("#toast-message").textContent = message;
    toast.classList.toggle("error", isError);
    toast.classList.add("show");
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => toast.classList.remove("show"), 3600);
  }

  function setBusy(busy) {
    state.busy = busy;
    document.querySelectorAll("form button[type='submit']").forEach((button) => {
      button.disabled = busy || button.dataset.unavailable === "true";
    });
    updateSubscriptionButton();
  }

  async function loadAll(showLoading = false) {
    if (showLoading) {
      state.loading = true;
      $("#topics-list").innerHTML = '<div class="loading-box"><div class="skeleton"></div><div class="skeleton"></div></div>';
      $("#users-list").innerHTML = '<div class="loading-box"><div class="skeleton"></div><div class="skeleton"></div></div>';
      $("#deliveries-list").innerHTML = '<div class="loading-box"><div class="skeleton"></div><div class="skeleton"></div></div>';
    }
    try {
      const [snapshot, deliveryData] = await Promise.all([
        request("/notify-api/state"),
        request("/notify-api/deliveries")
      ]);
      state.topics = Array.isArray(snapshot.topics) ? snapshot.topics : [];
      state.users = Array.isArray(snapshot.users) ? snapshot.users : [];
      state.deliveries = Array.isArray(deliveryData.deliveries) ? deliveryData.deliveries : [];
      state.loading = false;
      $("#page-error").hidden = true;
      $("#content").setAttribute("aria-busy", "false");
      $("#connection-status").textContent = "system online";
      render();
    } catch (error) {
      state.loading = false;
      $("#connection-status").textContent = "connection issue";
      $("#content").setAttribute("aria-busy", "false");
      $("#page-error").hidden = false;
      $("#page-error").innerHTML = `<strong>Couldn’t reach the notification service.</strong><br>${escapeHTML(error.message)}<br><button class="button ghost" type="button" id="retry-load">Try again</button>`;
      $("#retry-load").addEventListener("click", () => loadAll(true));
      if (showLoading) {
        $("#topics-list").innerHTML = emptyMarkup("No topics to show yet", "Reconnect to load your workspace.");
        $("#users-list").innerHTML = emptyMarkup("No recipients to show", "Reconnect to load your workspace.");
        $("#deliveries-list").innerHTML = emptyMarkup("Delivery trail unavailable", "Reconnect to load recent activity.");
      }
    }
  }

  function emptyMarkup(title, detail) {
    return `<div class="empty-state"><span class="empty-mark">${topicIcon}</span><div><strong>${escapeHTML(title)}</strong><span>${escapeHTML(detail)}</span></div></div>`;
  }

  function topicSubscribers(topic) {
    if (Array.isArray(topic.subscribers)) return topic.subscribers.length;
    return Number(topic.subscriberCount) || 0;
  }

  function renderTopics() {
    $("#topic-count").textContent = state.topics.length;
    const list = $("#topics-list");
    if (!state.topics.length) {
      list.innerHTML = emptyMarkup("No topics yet", "Create one below to start routing messages.");
    } else {
      list.innerHTML = state.topics.map((topic) => `
        <div class="topic-row">
          <div class="topic-name"><span class="topic-symbol">${topicIcon}</span><span>${escapeHTML(topic.name)}</span></div>
          <span class="topic-side">${topicSubscribers(topic)} ${topicSubscribers(topic) === 1 ? "subscriber" : "subscribers"}</span>
        </div>`).join("");
    }
  }

  function normalizeName(value) {
    return String(value ?? "").trim().toLocaleLowerCase();
  }

  function subscriptionsOf(user) {
    return Array.isArray(user.topics) ? user.topics : [];
  }

  function hasSubscription(topicName, userName) {
    const user = state.users.find((item) => normalizeName(item.name) === normalizeName(userName));
    if (user && subscriptionsOf(user).some((item) => normalizeName(typeof item === "string" ? item : item.name) === normalizeName(topicName))) return true;
    const topic = state.topics.find((item) => normalizeName(item.name) === normalizeName(topicName));
    if (topic && Array.isArray(topic.subscribers)) {
      return topic.subscribers.some((item) => normalizeName(typeof item === "string" ? item : item.name) === normalizeName(userName));
    }
    return false;
  }

  function renderUsers() {
    $("#user-count").textContent = state.users.length;
    const list = $("#users-list");
    if (!state.users.length) {
      list.innerHTML = emptyMarkup("No recipients yet", "Add a person and choose how they hear from you.");
    } else {
      list.innerHTML = state.users.map((user) => {
        const channel = String(user.channel || "EMAIL").toUpperCase();
        const topics = subscriptionsOf(user).map((topic) => typeof topic === "string" ? topic : topic.name).filter(Boolean);
        const initials = String(user.name || "?").trim().split(/\s+/).slice(0, 2).map((part) => part.charAt(0)).join("").toUpperCase();
        return `<div class="user-row">
          <span class="user-avatar" aria-hidden="true">${escapeHTML(initials)}</span>
          <div class="user-info"><div class="user-name">${escapeHTML(user.name)}</div><div class="user-topics">${topics.length ? topics.map(escapeHTML).join(" · ") : "Not subscribed yet"}</div></div>
          <span class="channel-label">${icons[channel] || icons.EMAIL}${escapeHTML(channel)}</span>
        </div>`;
      }).join("");
    }
    populateSelects();
  }

  function fillSelect(selector, items, placeholder) {
    const select = $(selector);
    const previous = select.value;
    select.innerHTML = `<option value="">${escapeHTML(placeholder)}</option>` + items.map((item) =>
      `<option value="${escapeHTML(item.name)}">${escapeHTML(item.name)}</option>`).join("");
    if (items.some((item) => item.name === previous)) select.value = previous;
  }

  function populateSelects() {
    fillSelect("#subscription-topic", state.topics, "Choose topic");
    fillSelect("#publish-topic", state.topics, "Choose a topic");
    fillSelect("#subscription-user", state.users, "Choose person");
    ["#subscription-topic", "#subscription-user", "#publish-topic"].forEach((selector) => {
      const select = $(selector);
      select.disabled = !select.options.length || (selector.includes("topic") && !state.topics.length) || (selector === "#subscription-user" && !state.users.length);
    });
    updateSubscriptionButton();
    const topic = state.topics.find((item) => item.name === $("#publish-topic").value);
    $("#publish-helper").textContent = topic
      ? `${topicSubscribers(topic)} subscriber${topicSubscribers(topic) === 1 ? "" : "s"} will receive this message.`
      : "Subscribers receive this on their preferred channel.";
  }

  function updateSubscriptionButton() {
    const topic = $("#subscription-topic").value;
    const user = $("#subscription-user").value;
    const button = $("#subscription-action");
    const subscribed = topic && user && hasSubscription(topic, user);
    button.textContent = subscribed ? "Unsubscribe" : "Subscribe";
    button.classList.toggle("secondary", Boolean(subscribed));
    button.classList.toggle("ghost", !subscribed);
    button.disabled = state.busy || !topic || !user;
  }

  function formatDate(value) {
    if (!value) return "Just now";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return escapeHTML(value);
    return new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric", hour: "numeric", minute: "2-digit" }).format(date);
  }

  function renderDeliveries() {
    const list = $("#deliveries-list");
    $("#delivery-count").textContent = state.deliveries.length;
    if (!state.deliveries.length) {
      list.innerHTML = emptyMarkup("Nothing delivered yet", "Publish a message to see each recipient appear here.");
      return;
    }
    list.innerHTML = state.deliveries.slice().reverse().map((delivery) => {
      const channel = String(delivery.channel || "EMAIL").toUpperCase();
      return `<div class="delivery-item">
        <time class="delivery-time">${formatDate(delivery.deliveredAt)}</time>
        <div class="delivery-main"><strong>${escapeHTML(delivery.user)}</strong><span>${icons[channel] || icons.EMAIL} ${escapeHTML(channel)} · ${escapeHTML(delivery.topic)}</span></div>
        <div class="delivery-message" title="${escapeHTML(delivery.message || delivery.deliveryText)}">${escapeHTML(delivery.message || delivery.deliveryText || "Message delivered")}</div>
        <span class="delivery-state">${escapeHTML(delivery.deliveryText || "Delivered")}</span>
      </div>`;
    }).join("");
  }

  function render() {
    renderTopics();
    renderUsers();
    renderDeliveries();
    populateSelects();
  }

  async function mutate(action, successMessage) {
    if (state.busy) return;
    setBusy(true);
    try {
      await action();
      await loadAll(false);
      showToast(successMessage);
    } catch (error) {
      showToast(error.message || "Something went wrong. Please try again.", true);
    } finally {
      setBusy(false);
    }
  }

  $("#topic-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const input = $("#topic-name");
    const name = input.value.trim();
    if (!name) return;
    mutate(() => request("/notify-api/topics", { method: "POST", body: JSON.stringify({ name }) }), `Topic “${name}” created.`);
    input.value = "";
  });

  $("#user-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const input = $("#user-name");
    const name = input.value.trim();
    const channel = $("#user-channel").value;
    if (!name) return;
    mutate(() => request("/notify-api/users", { method: "POST", body: JSON.stringify({ name, channel }) }), `${name} added on ${channel}.`);
    input.value = "";
  });

  $("#subscription-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const topic = $("#subscription-topic").value;
    const user = $("#subscription-user").value;
    if (!topic || !user) return;
    const subscribed = hasSubscription(topic, user);
    mutate(
      () => request("/notify-api/subscriptions", { method: subscribed ? "DELETE" : "POST", body: JSON.stringify({ topic, user }) }),
      `${user} ${subscribed ? "unsubscribed from" : "subscribed to"} ${topic}.`
    );
  });

  $("#publish-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const topic = $("#publish-topic").value;
    const message = $("#publish-message").value.trim();
    if (!topic || !message) return;
    mutate(
      async () => {
        const result = await request("/notify-api/publish", { method: "POST", body: JSON.stringify({ topic, message }) });
        if (Array.isArray(result.deliveries)) state.deliveries = result.deliveries;
        await loadAll(false);
      },
      `Message published to ${topic}.`
    );
    $("#publish-message").value = "";
  });

  $("#subscription-topic").addEventListener("change", updateSubscriptionButton);
  $("#subscription-user").addEventListener("change", updateSubscriptionButton);
  $("#publish-topic").addEventListener("change", populateSelects);

  loadAll(true);
})();
