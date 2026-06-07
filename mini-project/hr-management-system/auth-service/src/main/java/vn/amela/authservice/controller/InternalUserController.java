package vn.amela.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.amela.authservice.dto.response.UserResponse;
import vn.amela.authservice.entity.User;
import vn.amela.authservice.mapper.UserMapper;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserMapper userMapper;

    @GetMapping("/by-username-or-email/{usernameOrEmail}")
    public ResponseEntity<UserResponse> findByUsernameOrEmail(@PathVariable String usernameOrEmail) {
        User user = userMapper.selectByUserNameOrEmail(usernameOrEmail);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .build());
    }
}
