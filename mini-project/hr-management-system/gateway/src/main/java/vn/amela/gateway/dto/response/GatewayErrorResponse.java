package vn.amela.gateway.dto.response;

import java.util.List;

public record GatewayErrorResponse(
    String timestamp,
    int status,
    String code,
    String message,
    String path,
    List<String> errors
) {}