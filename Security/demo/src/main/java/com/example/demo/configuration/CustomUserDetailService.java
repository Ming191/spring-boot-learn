package com.example.demo.configuration;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private static final String USER_NOT_FOUND_MESSAGE = "User not found";
    private static final String ROLE_PREFIX = "ROLE_";

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findUserByUsername(username);

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(
                        ROLE_PREFIX + role.getName()
                ))
                .toList();

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), authorities
        );
    }

    private User findUserByUsername(String username) {
        User user = userRepository.findByUsernameForSecurity(username);
        if (user == null) {
            throw new UsernameNotFoundException(USER_NOT_FOUND_MESSAGE);
        }
        return user;
    }
}