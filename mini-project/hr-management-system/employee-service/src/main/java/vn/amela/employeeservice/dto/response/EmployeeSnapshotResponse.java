package vn.amela.employeeservice.dto.response;

import lombok.Builder;

@Builder
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
