package com.itsm.web;

import com.itsm.intelligence.ai.LlmClient;
import com.itsm.repository.NotificationRepository;
import com.itsm.repository.UserRepository;
import com.itsm.security.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds the logged-in user, unread count and AI status to every page. */
@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {

    private final CurrentUserService currentUser;
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final LlmClient llm;

    public GlobalModelAdvice(CurrentUserService currentUser, NotificationRepository notifications,
                             UserRepository users, LlmClient llm) {
        this.currentUser = currentUser;
        this.notifications = notifications;
        this.users = users;
        this.llm = llm;
    }

    @ModelAttribute
    public void addGlobals(Model model) {
        model.addAttribute("aiAvailable", llm.isAvailable());
        currentUser.principal().ifPresent(p -> {
            model.addAttribute("me", p);
            model.addAttribute("roleTheme", p.getRole().name().toLowerCase());
            users.findById(p.getId()).ifPresent(u ->
                    model.addAttribute("unreadCount", notifications.countByRecipientAndReadFalse(u)));
        });
    }
}
