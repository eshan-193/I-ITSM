package com.itsm.web;

import com.itsm.exception.ItsmException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Turns exceptions from browser pages into friendly feedback.
 * <ul>
 *   <li>Business-rule violations after a form POST: redirect back with a red banner.</li>
 *   <li>Otherwise: an error page with the right HTTP status.</li>
 *   <li>Database failures and concurrent edits get their own messages.</li>
 * </ul>
 */
@ControllerAdvice(annotations = Controller.class)
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(ItsmException.class)
    public String handleBusiness(ItsmException ex, HttpServletRequest request, HttpServletResponse response,
                                 RedirectAttributes redirect, Model model) {
        return respond(ex.getMessage(), ex.httpStatus(), request, response, redirect, model);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String handleConcurrentEdit(HttpServletRequest request, HttpServletResponse response,
                                       RedirectAttributes redirect, Model model) {
        return respond("Someone else updated this incident at the same time. Please review and try again.",
                409, request, response, redirect, model);
    }

    @ExceptionHandler(DataAccessException.class)
    public String handleDatabase(DataAccessException ex, HttpServletRequest request, HttpServletResponse response,
                                 RedirectAttributes redirect, Model model) {
        log.error("Database error", ex);
        return respond("The database is temporarily unavailable. Your change was not saved; please try again.",
                503, request, response, redirect, model);
    }

    private String respond(String message, int status, HttpServletRequest request, HttpServletResponse response,
                           RedirectAttributes redirect, Model model) {
        String referer = request.getHeader("Referer");
        if ("POST".equalsIgnoreCase(request.getMethod()) && referer != null && isSameOrigin(referer, request)) {
            redirect.addFlashAttribute("error", message);
            return "redirect:" + referer;
        }
        response.setStatus(status);
        model.addAttribute("status", status);
        model.addAttribute("message", message);
        return "error";
    }

    private static boolean isSameOrigin(String referer, HttpServletRequest request) {
        String origin = request.getScheme() + "://" + request.getServerName();
        return referer.startsWith(origin);
    }
}
