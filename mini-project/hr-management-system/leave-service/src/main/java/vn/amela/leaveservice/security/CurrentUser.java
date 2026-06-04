package vn.amela.leaveservice.security;

public record CurrentUser(
        String username,
        Long userId,
        String role
) {
    public boolean isHr() {
        return "HR".equalsIgnoreCase(role);
    }

    public boolean isEmployee() {
        return "EMPLOYEE".equalsIgnoreCase(role);
    }
}
