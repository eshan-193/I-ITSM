package com.itsm.notification;

/** Something that can actually deliver an email. */
public interface EmailGateway {

    boolean isEnabled();

    void send(String to, String subject, String body);
}
