package vn.amela.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.amela.authservice.dto.request.RegisterRequest;
import vn.amela.authservice.dto.response.UserResponse;
import vn.amela.authservice.entity.User;
import vn.amela.authservice.entity.enums.Role;
import vn.amela.authservice.mapper.UserMapper;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest request) {
        if (userMapper.selectByUserName(request.getUsername()) != null) {
            throw new RuntimeException("Username already exists");
        }

        if (userMapper.selectByEmail(request.getEmail()) != null) {
            throw new RuntimeException("Email already exists");
        }

        Role role = request.getRole() == null ? Role.EMPLOYEE : request.getRole();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setIsActive(true);

        userMapper.insert(user);

        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .isActive(user.getIsActive())
            .build();
    }
}