package vn.amela.webservice.dto.response;

import lombok.Builder;

@Builder
public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn
) {}
