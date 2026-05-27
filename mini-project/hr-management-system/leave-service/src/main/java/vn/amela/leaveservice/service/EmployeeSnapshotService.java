package vn.amela.leaveservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.amela.leaveservice.client.EmployeeClient;
import vn.amela.leaveservice.dto.response.EmployeeSnapshotResponse;
import vn.amela.leaveservice.exception.BusinessException;

@Service
@RequiredArgsConstructor
public class EmployeeSnapshotService {
    private final EmployeeClient  employeeClient;

    public EmployeeSnapshotResponse getEmployeeSnapshotByAuthUserId(Long authUserId) {
        EmployeeSnapshotResponse employee = employeeClient.findByAuthUserId(authUserId);

        if (employee == null) {
            throw new BusinessException("Employee not found");
        }

        if (!"ACTIVE".equalsIgnoreCase(employee.status())) {
            throw new BusinessException("Employee is not active");
        }

        return employee;
    }
}
