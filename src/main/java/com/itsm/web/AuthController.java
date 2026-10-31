package com.itsm.web;

import com.itsm.exception.ValidationException;
import com.itsm.service.UserService;
import com.itsm.web.form.RegistrationForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult result,
                           RedirectAttributes redirect) {
        if (!result.hasErrors() && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.registerEmployee(form.getName(), form.getEmail(), form.getPassword(), form.getDepartment());
        } catch (ValidationException e) {
            result.reject("invalid", e.getMessage());
            return "auth/register";
        }
        redirect.addFlashAttribute("success", "Account created. You can now log in.");
        return "redirect:/login";
    }
}
