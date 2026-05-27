package vn.amela.leaveservice.security;

public record CurrentUser(
        String username,
        Long userId,
        String role
) {
    public Boolean isHr() {
        return "HR".equalsIgnoreCase(role);
    }

    public Boolean isEmployee() {
        return "EMPLOYEE".equalsIgnoreCase(role);
    }
}
