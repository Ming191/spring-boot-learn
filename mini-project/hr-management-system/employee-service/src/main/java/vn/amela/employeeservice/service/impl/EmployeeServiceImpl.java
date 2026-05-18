package vn.amela.employeeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.amela.employeeservice.dto.request.CreateEmployeeRequest;
import vn.amela.employeeservice.dto.request.EmployeeFilterRequest;
import vn.amela.employeeservice.dto.request.UpdateContactRequest;
import vn.amela.employeeservice.dto.request.UpdateEmployeeRequest;
import vn.amela.employeeservice.dto.response.EmployeeResponse;
import vn.amela.employeeservice.dto.response.PageResponse;
import vn.amela.employeeservice.entity.Department;
import vn.amela.employeeservice.entity.Employee;
import vn.amela.employeeservice.exception.BusinessException;
import vn.amela.employeeservice.exception.ResourceNotFoundException;
import vn.amela.employeeservice.mapper.DepartmentMapper;
import vn.amela.employeeservice.mapper.EmployeeMapper;
import vn.amela.employeeservice.service.EmployeeService;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    protected final EmployeeMapper employeeMapper;
    protected final DepartmentMapper departmentMapper;

    @Override
    public EmployeeResponse create(CreateEmployeeRequest request) {
        return null;
    }

    @Override
    public EmployeeResponse getById(Long id, Long requesterId, String requesterRole) {
        return null;
    }

    @Override
    public PageResponse<EmployeeResponse> search(EmployeeFilterRequest filter) {
        return null;
    }

    @Override
    public EmployeeResponse updateByHr(Long id, UpdateEmployeeRequest request) {
        return null;
    }

    @Override
    public EmployeeResponse updateContact(Long id, UpdateContactRequest request, Long requesterId) {
        return null;
    }

    @Override
    public void deactivate(Long id) {
    }

    protected String normalizeEmail(String email) {
        return normalizeRequiredText(email, "Email").toLowerCase(Locale.ROOT);
    }

    protected String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(fieldName + " cannot be empty");
        }
        return value.trim();
    }

    protected Department requireActiveDepartment(Long departmentId) {
        if (departmentId == null) {
            throw new ResourceNotFoundException("Department ID is required");
        }

        Department department = departmentMapper.findById(departmentId);
        if (department == null || !Boolean.TRUE.equals(department.getIsActive())) {
            throw new ResourceNotFoundException("Department not found");
        }

        return department;
    }

    protected EmployeeResponse toResponse(Employee employee, String departmentName) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .position(employee.getPosition())
                .departmentId(employee.getDepartmentId())
                .departmentName(departmentName)
                .authUserId(employee.getAuthUserId())
                .salary(employee.getSalary())
                .startDate(employee.getStartDate())
                .status(employee.getStatus())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }
}
