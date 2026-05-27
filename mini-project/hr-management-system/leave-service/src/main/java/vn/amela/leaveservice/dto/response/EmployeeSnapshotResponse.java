package vn.amela.leaveservice.dto.response;

public record EmployeeSnapshotResponse(
        Long id,
        String employeeCode,
        String fullName,
        String email,
        String phone,
        String position,
        String status,
        Long authUserId,
        Long departmentId,
        String departmentName
) {
}
