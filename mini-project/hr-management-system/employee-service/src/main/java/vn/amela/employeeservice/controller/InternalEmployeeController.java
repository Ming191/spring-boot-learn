package vn.amela.employeeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.amela.employeeservice.dto.response.EmployeeSnapshotResponse;
import vn.amela.employeeservice.entity.Department;
import vn.amela.employeeservice.entity.Employee;
import vn.amela.employeeservice.exception.ResourceNotFoundException;
import vn.amela.employeeservice.mapper.DepartmentMapper;
import vn.amela.employeeservice.mapper.EmployeeMapper;

@RestController
@RequestMapping("/internal/employees")
@RequiredArgsConstructor
public class InternalEmployeeController {

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;

    @GetMapping("/by-auth-user/{authUserId}")
    public EmployeeSnapshotResponse findByAuthUserId(@PathVariable Long authUserId) {
        Employee employee = employeeMapper.findByAuthUserId(authUserId);
        if (employee == null) {
            throw new ResourceNotFoundException("Employee profile not found for auth user");
        }

        Department department = departmentMapper.findById(employee.getDepartmentId());
        return EmployeeSnapshotResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .position(employee.getPosition())
                .status(employee.getStatus() == null ? null : employee.getStatus().name())
                .authUserId(employee.getAuthUserId())
                .departmentId(employee.getDepartmentId())
                .departmentName(department == null ? null : department.getName())
                .build();
    }
}
