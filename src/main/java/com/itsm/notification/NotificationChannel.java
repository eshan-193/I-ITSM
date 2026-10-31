package com.itsm.notification;

import com.itsm.domain.notification.ChannelType;

/**
 * One delivery mechanism (in-app, live push, email, SMS).
 * Adding a new mechanism, such as Slack or Teams, means implementing this interface
 * as a Spring bean; the factory picks it up automatically.
 */
public interface NotificationChannel {

    ChannelType type();

    void send(NotificationMessage message);
}
