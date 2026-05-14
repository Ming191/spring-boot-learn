package vn.amela.authservice.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter responseWriter;

    @Override
    public void handle(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull AccessDeniedException accessDeniedException
    ) throws IOException {

        responseWriter.write(
            request,
            response,
            HttpStatus.FORBIDDEN,
            "ACCESS_DENIED",
            "You do not have permission to access this resource"
        );
    }
}
