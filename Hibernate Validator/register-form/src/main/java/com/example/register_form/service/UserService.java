package com.example.register_form.service;

import com.example.register_form.dto.RegisterForm;
import com.example.register_form.model.User;
import com.example.register_form.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    public User register(
            RegisterForm registerForm
    ) {
        User user = new User();
        user.setMail(registerForm.getMail());
        user.setUsername(registerForm.getUsername());
        user.setPassword(registerForm.getPassword());
        return userRepository.save(user);
    }
}
