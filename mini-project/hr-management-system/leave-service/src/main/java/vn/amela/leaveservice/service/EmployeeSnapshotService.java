package vn.amela.leaveservice.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.amela.leaveservice.client.EmployeeClient;
import vn.amela.leaveservice.dto.response.EmployeeSnapshotResponse;
import vn.amela.leaveservice.exception.BusinessException;
import vn.amela.leaveservice.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class EmployeeSnapshotService {
    private final EmployeeClient employeeClient;

    public EmployeeSnapshotResponse getEmployeeSnapshotByAuthUserId(Long authUserId) {
        EmployeeSnapshotResponse employee;

        try {
            employee = employeeClient.findByAuthUserId(authUserId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Employee not found");
        } catch (FeignException e) {
            throw new BusinessException("Failed to fetch employee: " + e.getMessage());
        }

        if (!"ACTIVE".equalsIgnoreCase(employee.status())) {
            throw new BusinessException("Employee is not active");
        }

        return employee;
    }
}
