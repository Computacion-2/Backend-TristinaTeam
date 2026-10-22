package com.icesi.cuidapetproyect.controller;

import com.icesi.cuidapetproyect.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegistrationForm registrationForm,
                           BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getAllErrors().getFirst().getDefaultMessage());
            return registrationError(registrationForm, redirect);
        }
        try {
            authService.register(registrationForm.getEmail(), registrationForm.getPassword());
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return registrationError(registrationForm, redirect);
        } catch (IllegalStateException | DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("error", "No fue posible registrar la cuenta. Contacta al administrador.");
            return registrationError(registrationForm, redirect);
        }
        return "redirect:/login?registered";
    }

    private String registrationError(RegistrationForm form, RedirectAttributes redirect) {
        // No guardar la contraseña en los atributos de sesión usados por la redirección.
        form.setPassword(null);
        redirect.addFlashAttribute("registrationForm", form);
        return "redirect:/register";
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        model.addAttribute("email", authentication.getName());
        model.addAttribute("authorities", authentication.getAuthorities());
        return "home";
    }
}
