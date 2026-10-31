package com.itsm.notification;

import com.itsm.domain.notification.ChannelType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory pattern: callers ask for a channel by {@link ChannelType} and never name a
 * concrete class. An EnumMap gives constant-time lookup keyed by the enum.
 */
@Component
public class NotificationChannelFactory {

    private final Map<ChannelType, NotificationChannel> channels = new EnumMap<>(ChannelType.class);

    public NotificationChannelFactory(List<NotificationChannel> available) {
        available.forEach(c -> channels.put(c.type(), c));
    }

    public Optional<NotificationChannel> get(ChannelType type) {
        return Optional.ofNullable(channels.get(type));
    }
}
