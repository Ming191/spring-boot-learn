package com.example.register_form.controller;

import com.example.register_form.dto.RegisterForm;
import com.example.register_form.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/register-form")
    public String initRegisterForm(
            @ModelAttribute("registerForm") RegisterForm registerForm
    ) {
        return "register";
    }

    @PostMapping("/register-form")
    public String submitForm(
            @Valid @ModelAttribute("registerForm") RegisterForm registerForm,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        if (!registerForm.getPassword().equals(registerForm.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "password.mismatch", "Passwords do not match");
            return "register";
        }

        userService.register(registerForm);
        return "confirmation";
    }
}
