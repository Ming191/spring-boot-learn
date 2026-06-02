package vn.amela.authservice.security;

import vn.amela.authservice.entity.enums.Role;

public record AuthenticatedUser(
    Long userId,
    String username,
    Role role
) {}
