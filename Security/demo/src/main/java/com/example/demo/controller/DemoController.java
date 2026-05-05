package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class DemoController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void initData() {
        if (userRepository.count() == 0) {
            Role rAdmin = new Role();
            rAdmin.setName("ADMIN");
            roleRepository.save(rAdmin);
            Role rUser = new Role();
            rUser.setName("USER");
            roleRepository.save(rUser);

            User admin = new User();
            admin.setUsername("boss");
            admin.setPassword(passwordEncoder.encode("123"));
            admin.setRoles(Set.of(rAdmin));
            userRepository.save(admin);

            User user = new User();
            user.setUsername("minh");
            user.setPassword(passwordEncoder.encode("123"));
            user.setRoles(Set.of(rUser));
            userRepository.save(user);
        }
    }

    @RequestMapping("/public")
    public String publicEndpoint() {
        return "this is a public endpoint";
    }

    @RequestMapping("/private")
    public String privateEndpoint() {
        return "this is a private endpoint";
    }

    @GetMapping("/users")
    @PreAuthorize( "hasRole('ADMIN')")
    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }
}
