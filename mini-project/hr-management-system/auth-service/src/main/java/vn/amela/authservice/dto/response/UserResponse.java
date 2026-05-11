package vn.amela.authservice.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.amela.authservice.entity.enums.Role;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private Boolean isActive;
}