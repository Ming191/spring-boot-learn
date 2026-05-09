package vn.amela.authservice.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import vn.amela.authservice.entity.enums.Role;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String fullName;
    private Boolean isActive;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
