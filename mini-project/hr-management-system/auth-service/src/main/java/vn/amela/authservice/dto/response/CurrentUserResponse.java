package vn.amela.authservice.dto.response;

import lombok.Builder;
import vn.amela.authservice.entity.enums.Role;

@Builder
public record CurrentUserResponse(
    Long userId,
    String username,
    Role role
) {}
