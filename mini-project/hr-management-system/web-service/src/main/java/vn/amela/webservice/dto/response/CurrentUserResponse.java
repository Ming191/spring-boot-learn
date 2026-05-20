package vn.amela.webservice.dto.response;

import lombok.Builder;
import vn.amela.webservice.entity.enums.Role;

@Builder
public record CurrentUserResponse(
    Long userId,
    String username,
    Role role
) {}
