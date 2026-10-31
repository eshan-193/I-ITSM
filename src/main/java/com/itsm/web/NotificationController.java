package com.itsm.web;

import com.itsm.domain.user.User;
import com.itsm.notification.LiveUpdateHub;
import com.itsm.security.CurrentUserService;
import com.itsm.service.NotificationService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
public class NotificationController {

    private final CurrentUserService currentUser;
    private final NotificationService notifications;
    private final LiveUpdateHub hub;

    public NotificationController(CurrentUserService currentUser, NotificationService notifications, LiveUpdateHub hub) {
        this.currentUser = currentUser;
        this.notifications = notifications;
        this.hub = hub;
    }

    @GetMapping("/notifications")
    public String list(Model model) {
        model.addAttribute("notifications", notifications.latest(currentUser.require()));
        return "notifications";
    }

    @GetMapping("/notifications/{id}/open")
    public String open(@PathVariable Long id) {
        Long incidentId = notifications.open(id, currentUser.require());
        return incidentId == null ? "redirect:/notifications" : "redirect:/incidents/" + incidentId;
    }

    @PostMapping("/notifications/read-all")
    public String readAll() {
        notifications.markAllRead(currentUser.require());
        return "redirect:/notifications";
    }

    /** Server-Sent Events stream: one long-lived connection per open browser tab. */
    @GetMapping(path = "/live/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream() {
        User me = currentUser.require();
        return hub.subscribe(me.getId());
    }
}
