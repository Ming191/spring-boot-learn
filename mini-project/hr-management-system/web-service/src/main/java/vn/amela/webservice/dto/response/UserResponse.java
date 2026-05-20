package vn.amela.webservice.dto.response;

import lombok.Builder;
import vn.amela.webservice.entity.enums.Role;

@Builder
public record UserResponse(
    Long id,
    String username,
    String email,
    String fullName,
    Role role,
    Boolean isActive
) {}
