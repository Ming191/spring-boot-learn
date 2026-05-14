package vn.amela.authservice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RefreshToken {
    private Long id;
    private Long userId;
    private String tokenHash;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
}
